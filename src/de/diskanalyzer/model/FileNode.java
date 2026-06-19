package de.diskanalyzer.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Ein Knoten im Verzeichnisbaum &ndash; gehoert zu genau einer {@link File}
 * (Datei oder Ordner) und kennt seine aufsummierte Groesse sowie seine Kinder.
 *
 * <p>Der Knoten dient sowohl als Datenmodell fuer den Scan ({@code DiskScanner})
 * als auch als Anzeigemodell fuer den {@code JTree} ueber {@code FileTreeModel}.
 *
 * <p><b>Hinweis zur Nebenlaeufigkeit:</b> Waehrend des parallelen Scans wird jeder
 * Knoten nur von <i>einem</i> Thread befuellt; danach lesen ihn UI-Threads. Es gibt
 * also keinen gleichzeitigen Schreibzugriff von mehreren Threads auf denselben
 * Knoten (siehe {@code DiskScanner}).
 */
public class FileNode {

    /** Die zugrundeliegende Datei bzw. das Verzeichnis. Unveraenderlich. */
    private final File file;

    /** Aufsummierte Groesse: bei Dateien die Dateigroesse, bei Ordnern die Summe aller Kinder. */
    private long totalSize;

    /** Direkte Kinder dieses Knotens (nur bei Verzeichnissen befuellt). */
    private final List<FileNode> children = new ArrayList<>();

    /** true, sobald die Kinder eingelesen wurden (steuert das Lazy Loading im Baum). */
    private boolean loaded = false;

    /**
     * Datumsformat fuer die "zuletzt geaendert"-Anzeige.
     * <p>Wird ausschliesslich aus dem UI-Thread (Renderer) heraus genutzt;
     * {@link SimpleDateFormat} ist nicht thread-sicher, hier aber unkritisch.
     */
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd.MM.yyyy HH:mm");

    /**
     * Erzeugt einen Knoten fuer die angegebene Datei. Bei Dateien wird die Groesse
     * sofort gesetzt, bei Verzeichnissen erst spaeter durch den Scan aufsummiert.
     */
    public FileNode(File file) {
        this.file = file;
        this.totalSize = file.isFile() ? file.length() : 0;
    }

    public File getFile() { return file; }

    /** Anzeigename: der Dateiname, fuer Laufwerkswurzeln (leerer Name) der volle Pfad. */
    public String getName() {
        return file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
    }

    public boolean isDirectory() { return file.isDirectory(); }
    public boolean isLoaded() { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long size) { this.totalSize = size; }
    public List<FileNode> getChildren() { return children; }

    /**
     * Liefert das Aenderungsdatum formatiert ("dd.MM.yyyy HH:mm") oder einen leeren
     * String, falls kein Datum verfuegbar ist.
     */
    public String getLastModifiedFormatted() {
        long lm = file.lastModified();
        if (lm == 0) return "";
        return DATE_FMT.format(new Date(lm));
    }

    /**
     * Liefert die Groesse menschenlesbar mit passender Einheit (B, KB, MB, GB).
     * Die Schwellen folgen der binaeren Konvention (1 KB = 1024 B).
     */
    public String getSizeFormatted() {
        if (totalSize < 1024) return totalSize + " B";
        if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
        if (totalSize < 1024L * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
        return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
    }

    /** Im Baum wird der Knoten ueber seinen Namen dargestellt. */
    @Override
    public String toString() { return getName(); }
}
