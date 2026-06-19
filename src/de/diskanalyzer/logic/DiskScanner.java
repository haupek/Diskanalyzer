package de.diskanalyzer.logic;

import de.diskanalyzer.model.FileNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;

/**
 * Liest einen Verzeichnisbaum rekursiv ein und berechnet fuer jeden Knoten die
 * aufsummierte Groesse aller darin enthaltenen Dateien.
 *
 * <h2>Warum parallel?</h2>
 * Das Einlesen grosser Laufwerke ist fast vollstaendig <b>I/O-gebunden</b>: die
 * meiste Zeit wartet der Scan auf Antworten von Festplatte bzw. Dateisystem, nicht
 * auf die CPU. Ein rein sequentieller Scan laesst dabei einen Grossteil der
 * moeglichen Parallelitaet ungenutzt &ndash; waehrend auf einen Ordner gewartet
 * wird, koennten laengst weitere Ordner gelesen werden.
 *
 * <h2>Ein Task pro Ordner &ndash; aber kein Thread pro Ordner</h2>
 * Fuer <b>jeden Ordner</b> wird eine eigene Teilaufgabe erzeugt
 * ({@link DirectoryScanTask}). Bewusst werden dafuer <i>keine</i> echten
 * {@link Thread}-Objekte pro Ordner gestartet: ein komplettes Laufwerk kann leicht
 * mehrere hunderttausend Verzeichnisse enthalten, und ebenso viele Threads wuerden
 * den Rechner durch Speicher- und Context-Switch-Overhead ausbremsen oder zum
 * Absturz bringen.
 *
 * <p>Stattdessen kommt ein {@link ForkJoinPool} zum Einsatz. Jeder Ordner wird als
 * Task in den Pool eingereiht ({@code fork()}); der Pool verteilt diese Tasks per
 * <i>Work-Stealing</i> auf eine begrenzte, fest definierte Anzahl Worker-Threads.
 * So bleibt das gewuenschte Modell &bdquo;ein Task pro Ordner&ldquo; erhalten, ohne
 * dass die Thread-Zahl explodiert.
 *
 * <h2>Thread-Sicherheit</h2>
 * Jeder {@link DirectoryScanTask} erzeugt und befuellt ausschliesslich seinen
 * <i>eigenen</i> {@link FileNode}; kein Knoten wird von mehreren Threads gleichzeitig
 * veraendert. Die Ergebnisse der Unterordner werden erst per {@code join()}
 * eingesammelt &ndash; das stellt zugleich die noetige Speicher-Sichtbarkeit
 * (happens-before) sicher. Der optionale {@code progressCallback} kann allerdings
 * aus mehreren Threads <b>gleichzeitig</b> aufgerufen werden und muss daher
 * thread-sicher sein (die UI uebergibt hier {@code SwingWorker.publish}, das diese
 * Garantie erfuellt).
 */
public final class DiskScanner {

    /**
     * Parallelitaetsgrad des Scan-Pools (Anzahl Worker-Threads).
     *
     * <p>Da der Scan I/O-gebunden ist, lohnt es sich, spuerbar <i>mehr</i> Worker
     * als CPU-Kerne zu verwenden: waehrend ein Thread auf die Platte wartet, koennen
     * andere bereits weitere Ordner abarbeiten. Der Faktor 4 ist ein pragmatischer
     * Kompromiss; das Minimum von 4 sorgt auch auf Single-Core-Systemen fuer etwas
     * Parallelitaet.
     */
    private static final int PARALLELISM =
            Math.max(4, Runtime.getRuntime().availableProcessors() * 4);

    /** Utility-Klasse &ndash; nicht instanziierbar. */
    private DiskScanner() { }

    /**
     * Scannt {@code root} vollstaendig und liefert den Wurzelknoten des fertig
     * berechneten Baums zurueck (inklusive aufsummierter Groessen und absteigend
     * nach Groesse sortierter Kinder).
     *
     * @param root             Datei oder Verzeichnis, das eingelesen werden soll
     * @param progressCallback optional; wird (ggf. nebenlaeufig) mit dem absoluten
     *                         Pfad jedes besuchten Eintrags aufgerufen &ndash; etwa
     *                         fuer eine Statusanzeige. Darf {@code null} sein.
     * @return der Wurzelknoten; nie {@code null}
     */
    public static FileNode scan(File root, Consumer<String> progressCallback) {
        FileNode node = new FileNode(root);

        // --- Sonderfaelle, fuer die kein Thread-Pool noetig ist ---

        if (!root.exists()) {
            // Pfad existiert nicht (mehr) -> leerer Knoten der Groesse 0.
            node.setTotalSize(0);
            return node;
        }
        if (root.isFile()) {
            // Einzelne Datei: Groesse frisch lesen (koennte sich geaendert haben).
            node.setTotalSize(root.length());
            return node;
        }
        if (!root.isDirectory()) {
            // Weder Datei noch Verzeichnis (z. B. ein toter Symlink) -> so belassen.
            return node;
        }

        // --- Verzeichnis: parallel einlesen ---
        // Pro Top-Level-Scan ein eigener Pool. Das kapselt die Worker-Threads sauber
        // und garantiert ueber den finally-Block ein definiertes Ende (shutdown).
        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);
        try {
            return pool.invoke(new DirectoryScanTask(root, progressCallback));
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Laedt die direkten Kinder eines bisher noch nicht eingelesenen Verzeichnis-
     * knotens nach (Lazy Loading fuer den Baum). Intern wird der Knoten dafuer
     * &ndash; ebenfalls parallel &ndash; komplett neu gescannt.
     *
     * @param node             der nachzuladende Knoten
     * @param progressCallback optionale Fortschrittsmeldung (siehe {@link #scan})
     */
    public static void loadChildren(FileNode node, Consumer<String> progressCallback) {
        // Nur einmal laden, und nur fuer Verzeichnisse.
        if (node.isLoaded() || !node.isDirectory()) return;

        // Den Knoten selbst (parallel) scannen und die Ergebnisse uebernehmen.
        FileNode fresh = scan(node.getFile(), progressCallback);
        node.getChildren().clear();
        node.getChildren().addAll(fresh.getChildren());
        node.setTotalSize(fresh.getTotalSize());
        node.setLoaded(true);
    }

    /**
     * Fork/Join-Aufgabe fuer genau <b>einen</b> Ordner.
     *
     * <p>Ablauf von {@link #compute()}:
     * <ol>
     *   <li>Eintraege des Ordners auflisten.</li>
     *   <li>Fuer jeden Unterordner einen neuen {@code DirectoryScanTask} forken
     *       (zur parallelen Bearbeitung einreihen).</li>
     *   <li>Dateien direkt verarbeiten &ndash; dafuer lohnt sich kein eigener Task.</li>
     *   <li>Per {@code join()} auf die Unterordner warten und Groessen aufsummieren.</li>
     *   <li>Kinder absteigend nach Groesse sortieren.</li>
     * </ol>
     */
    private static final class DirectoryScanTask extends RecursiveTask<FileNode> {

        private static final long serialVersionUID = 1L;

        private final File dir;
        private final Consumer<String> progressCallback;

        DirectoryScanTask(File dir, Consumer<String> progressCallback) {
            this.dir = dir;
            this.progressCallback = progressCallback;
        }

        @Override
        protected FileNode compute() {
            FileNode node = new FileNode(dir);

            File[] entries;
            try {
                entries = dir.listFiles();
            } catch (SecurityException e) {
                // Kein Lesezugriff -> als geladen markieren und leer zurueckgeben.
                node.setLoaded(true);
                return node;
            }
            if (entries == null) {
                // listFiles() liefert null bei I/O-Fehler oder fehlendem Zugriff.
                node.setLoaded(true);
                return node;
            }

            // Unterordner laufen als eigene Tasks, Dateien werden sofort verbucht.
            List<DirectoryScanTask> subTasks = new ArrayList<>();
            long total = 0;

            for (File entry : entries) {
                // Zwischen listFiles() und Zugriff kann ein Eintrag verschwinden.
                if (!entry.exists()) continue;

                report(entry);

                if (entry.isDirectory()) {
                    // Unterordner -> eigener Task, parallel zur weiteren Schleife.
                    DirectoryScanTask task = new DirectoryScanTask(entry, progressCallback);
                    subTasks.add(task);
                    task.fork();
                } else {
                    // Datei -> Groesse direkt ermitteln, kein eigener Task noetig.
                    try {
                        FileNode child = new FileNode(entry);
                        child.setTotalSize(entry.length());
                        child.setLoaded(true);
                        node.getChildren().add(child);
                        total += child.getTotalSize();
                    } catch (Exception ignored) {
                        // Einzelnen fehlerhaften Eintrag ueberspringen.
                    }
                }
            }

            // Auf die geforkten Unterordner warten und ihre Ergebnisse einsammeln.
            // join() liefert das fertige Teilergebnis und sorgt fuer Sichtbarkeit.
            for (DirectoryScanTask task : subTasks) {
                try {
                    FileNode child = task.join();
                    node.getChildren().add(child);
                    total += child.getTotalSize();
                } catch (Exception ignored) {
                    // Fehlerhaften Unterbaum ueberspringen, Rest bleibt gueltig.
                }
            }

            // Groesste zuerst -> die "Speicherfresser" stehen oben.
            node.getChildren().sort(
                    Comparator.comparingLong(FileNode::getTotalSize).reversed());
            node.setTotalSize(total);
            node.setLoaded(true);
            return node;
        }

        /** Meldet einen besuchten Eintrag an den (optionalen) Fortschritts-Callback. */
        private void report(File entry) {
            if (progressCallback == null) return;
            try {
                progressCallback.accept(entry.getAbsolutePath());
            } catch (Exception ignored) {
                // Eine fehlerhafte UI-Rueckmeldung darf den Scan nicht stoppen.
            }
        }
    }
}
