package de.diskanalyzer.ui;

import de.diskanalyzer.logic.DiskScanner;
import de.diskanalyzer.model.FileNode;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hauptfenster der Anwendung.
 *
 * <p>Aufbau (von oben nach unten):
 * <ul>
 *   <li><b>Kopfleiste</b> &ndash; Titel, Pfadeingabe, Buttons "Laufwerke" und "Scan".</li>
 *   <li><b>Baum</b> &ndash; der gescannte Verzeichnisbaum ({@code JTree} mit
 *       {@link FileTreeModel} und {@link SizeTreeCellRenderer}).</li>
 *   <li><b>Statusleiste</b> &ndash; Statustext und Fortschrittsbalken.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Alle UI-Zugriffe laufen auf dem Event-Dispatch-Thread (EDT).
 * Laenger laufende Arbeiten (Scannen, Loeschen) werden in {@link SwingWorker}
 * ausgelagert; deren {@code done()} bzw. {@code process()} laufen wieder auf dem EDT
 * und duerfen die UI gefahrlos anfassen.
 */
public class DiskAnalyzerWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTree tree;
    private FileTreeModel treeModel;
    private SizeTreeCellRenderer renderer;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton scanButton;
    private JTextField pathField;

    public DiskAnalyzerWindow() {
        super("Disk Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 720);
        setLocationRelativeTo(null);   // mittig auf dem Bildschirm
        applyLightTheme();
        buildUI();
    }

    /**
     * Setzt das System-Look-and-Feel und ueberschreibt einige UIManager-Farben,
     * um ein helles, einheitliches Erscheinungsbild zu erhalten. Muss vor dem
     * Erzeugen der Komponenten laufen.
     */
    private void applyLightTheme() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Tree.background",           Color.WHITE);
        UIManager.put("Tree.foreground",           new Color(0x1A1D23));
        UIManager.put("Tree.selectionBackground",  new Color(0x1A73E8));
        UIManager.put("Tree.selectionForeground",  Color.WHITE);
        UIManager.put("Panel.background",          new Color(0xF5F7FA));
        UIManager.put("ScrollPane.background",     Color.WHITE);
        UIManager.put("Viewport.background",       Color.WHITE);
        UIManager.put("TextField.background",      Color.WHITE);
        UIManager.put("TextField.foreground",      new Color(0x1A1D23));
        UIManager.put("TextField.caretForeground", new Color(0x1A1D23));
        UIManager.put("Button.background",         new Color(0x1A73E8));
        UIManager.put("Button.foreground",         Color.WHITE);
        UIManager.put("Label.foreground",          new Color(0x1A1D23));
        UIManager.put("ProgressBar.background",    new Color(0xE0E0E0));
        UIManager.put("ProgressBar.foreground",    new Color(0x1A73E8));
    }

    /** Baut alle Komponenten zusammen und startet anschliessend einen ersten Scan. */
    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Kopfleiste: Titel | Pfadeingabe | Buttons ──────────
        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        topBar.setBackground(new Color(0xE8EDF5));

        JLabel titleLabel = new JLabel("DISK ANALYZER");
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        titleLabel.setForeground(new Color(0x1A73E8));

        // Vorbelegung: unter Windows "C:\", sonst die Wurzel "/".
        String startPath = new File("C:\\").exists() ? "C:\\" : "/";
        pathField = new JTextField(startPath);
        pathField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xBBCCDD), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        scanButton = new JButton("  \u25b6  SCAN  ");
        scanButton.setFont(new Font("Monospaced", Font.BOLD, 12));
        scanButton.setFocusPainted(false);
        scanButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scanButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        scanButton.addActionListener(e -> startScan());

        JButton rootsButton = new JButton("  \u229e  Laufwerke  ");
        rootsButton.setFont(new Font("Monospaced", Font.BOLD, 12));
        rootsButton.setFocusPainted(false);
        rootsButton.setBackground(new Color(0xCCDAEE));
        rootsButton.setForeground(new Color(0x1A1D23));
        rootsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rootsButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        rootsButton.addActionListener(e -> showRootChooser());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(rootsButton);
        btnPanel.add(scanButton);

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(pathField,  BorderLayout.CENTER);
        topBar.add(btnPanel,   BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Baum ───────────────────────────────────────────────
        // Platzhalter-Wurzel; wird beim ersten Scan ersetzt.
        FileNode placeholder = new FileNode(new File("."));
        treeModel = new FileTreeModel(placeholder);
        renderer  = new SizeTreeCellRenderer();

        tree = new JTree(treeModel);
        tree.setCellRenderer(renderer);
        tree.setBackground(Color.WHITE);
        tree.setRowHeight(26);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Beim Aufklappen den Balken-Bezugswert fuer die Kinder aktualisieren.
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent e) {
                updateMaxSizeForParent((FileNode) e.getPath().getLastPathComponent());
                tree.repaint();
            }
            public void treeCollapsed(TreeExpansionEvent e) {}
        });

        // Mehrfachauswahl erlauben (Strg-/Shift-Klick), damit mehrere Knoten
        // gemeinsam geloescht werden koennen.
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Entf-Taste loescht die aktuell ausgewaehlten Knoten.
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelection");
        tree.getActionMap().put("deleteSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteNodes(tree.getSelectionPaths());
            }
        });

        // Rechtsklick -> Kontextmenue. Ist der getroffene Knoten nicht Teil der
        // aktuellen (Mehrfach-)Auswahl, wird er einzeln ausgewaehlt; gehoert er
        // bereits zur Auswahl, bleibt die Mehrfachauswahl erhalten.
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    if (!tree.isPathSelected(path)) tree.setSelectionPath(path);
                    showContextMenu(e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xDDE3EC)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // ── Statusleiste ───────────────────────────────────────
        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        statusBar.setBackground(new Color(0xE8EDF5));

        statusLabel = new JLabel("Bereit. Pfad eingeben und SCAN druecken.");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(0x555E6E));

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 14));

        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // Direkt nach dem Aufbau einen ersten Scan des Startpfads anstossen.
        SwingUtilities.invokeLater(this::startScan);
    }

    // ── KONTEXTMENÜ ──────────────────────────────────────────
    /**
     * Baut das Rechtsklick-Menue passend zur aktuellen Auswahl. Rescan und Oeffnen
     * gibt es nur bei genau einem ausgewaehlten Knoten; Loeschen funktioniert fuer
     * beliebig viele.
     */
    private void showContextMenu(int x, int y) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) return;

        JPopupMenu menu = new JPopupMenu();

        // Einzelauswahl: Rescan (nur Verzeichnis) und Oeffnen anbieten.
        if (paths.length == 1) {
            final TreePath path = paths[0];
            final FileNode node = (FileNode) path.getLastPathComponent();

            if (node.isDirectory()) {
                JMenuItem rescanItem = new JMenuItem("  \u27f3  Rescan");
                rescanItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
                rescanItem.addActionListener(e -> rescanNode(node, path));
                menu.add(rescanItem);
                menu.addSeparator();
            }

            String openLabel = node.isDirectory() ? "  \uD83D\uDCC2  Im Explorer oeffnen" : "  \u2197  Oeffnen";
            JMenuItem openItem = new JMenuItem(openLabel);
            openItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
            openItem.addActionListener(e -> openNode(node));
            menu.add(openItem);

            menu.addSeparator();
        }

        // Loeschen (rot, da destruktiv) - fuer ein oder mehrere Elemente.
        String delLabel = (paths.length == 1)
            ? "  \uD83D\uDDD1  Loeschen..."
            : "  \uD83D\uDDD1  " + paths.length + " Elemente loeschen...";
        JMenuItem deleteItem = new JMenuItem(delLabel);
        deleteItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
        deleteItem.setForeground(new Color(0xCC2200));
        deleteItem.addActionListener(e -> deleteNodes(tree.getSelectionPaths()));
        menu.add(deleteItem);

        menu.show(tree, x, y);
    }

    // ── LÖSCHEN ──────────────────────────────────────────────
    /**
     * Loescht alle uebergebenen Knoten nach einer Rueckfrage endgueltig von der
     * Platte (im Hintergrund) und entfernt sie anschliessend aus dem Baum. Groessen
     * werden nur entlang der betroffenen Elternketten aktualisiert, und die
     * aufgeklappten Zweige des restlichen Baums bleiben erhalten.
     *
     * @param selected ausgewaehlte Pfade (z. B. aus {@code tree.getSelectionPaths()});
     *                 darf {@code null} oder leer sein
     */
    private void deleteNodes(TreePath[] selected) {
        // Keine neue Loeschung starten, solange schon eine Operation laeuft
        // (scanButton dient als Beschaeftigt-Anzeige).
        if (!scanButton.isEnabled()) return;

        // Verschachtelte Auswahl bereinigen: liegt ein Knoten unter einem anderen
        // ausgewaehlten Knoten, reicht es, den oberen zu loeschen.
        final List<TreePath> paths = topLevelPaths(selected);
        if (paths.isEmpty()) return;

        // Gesamtgroesse fuer die Rueckfrage aufsummieren.
        long totalSize = 0;
        for (TreePath p : paths) totalSize += ((FileNode) p.getLastPathComponent()).getTotalSize();

        // Bestaetigungsdialog: bei einem Element detailliert, bei mehreren zusammengefasst.
        String msg;
        if (paths.size() == 1) {
            FileNode node = (FileNode) paths.get(0).getLastPathComponent();
            String typ = node.isDirectory() ? "Verzeichnis" : "Datei";
            msg = "<html><b>" + typ + " wirklich loeschen?</b><br><br>"
                + node.getFile().getAbsolutePath() + "<br><br>"
                + "Groesse: " + node.getSizeFormatted()
                + (node.isDirectory() ? "<br><font color='red'>Der gesamte Inhalt wird geloescht!</font>" : "")
                + "</html>";
        } else {
            msg = "<html><b>" + paths.size() + " Elemente wirklich loeschen?</b><br><br>"
                + "Gesamtgroesse: " + formatSize(totalSize)
                + "<br><font color='red'>Verzeichnisse werden mit ihrem gesamten Inhalt geloescht!</font>"
                + "</html>";
        }

        int result = JOptionPane.showConfirmDialog(this, msg,
            "Loeschen bestaetigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;

        // UI in den "beschaeftigt"-Zustand versetzen.
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Loesche " + paths.size() + " Element(e)...");
        scanButton.setEnabled(false);

        // Loeschen im Hintergrund. Ergebnis: die tatsaechlich entfernten Knoten.
        SwingWorker<List<FileNode>, String> worker = new SwingWorker<List<FileNode>, String>() {
            @Override
            protected List<FileNode> doInBackground() {
                List<FileNode> deleted = new ArrayList<>();
                for (TreePath p : paths) {
                    FileNode n = (FileNode) p.getLastPathComponent();
                    if (deleteTree(n.getFile(), s -> publish(s))) deleted.add(n);
                }
                return deleted;
            }
            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String last = chunks.get(chunks.size() - 1);
                    statusLabel.setText(last.length() > 90
                        ? "..." + last.substring(last.length() - 87) : last);
                }
            }
            @Override
            protected void done() {
                try {
                    List<FileNode> deleted = get();

                    // Aufklapp-Zustand sichern, BEVOR der Baum umgebaut wird.
                    List<TreePath> expanded = currentExpansion();

                    // Geloeschte Knoten entfernen und Eltern-Groessen aktualisieren.
                    TreePath selectAfter = null;
                    for (TreePath p : paths) {
                        FileNode n = (FileNode) p.getLastPathComponent();
                        if (!deleted.contains(n)) continue;     // nicht (ganz) geloescht -> drin lassen
                        TreePath parentPath = p.getParentPath();
                        if (parentPath == null) continue;
                        FileNode parent = (FileNode) parentPath.getLastPathComponent();
                        parent.getChildren().remove(n);
                        updateAncestorSizes(parentPath);
                        selectAfter = parentPath;               // Elternordner als Folge-Auswahl
                    }

                    treeModel.fireTreeStructureChanged();
                    restoreExpansion(expanded);                 // Zweige wieder aufklappen

                    // Sinnvolle Folge-Auswahl: der Elternordner des Geloeschten.
                    if (selectAfter != null) {
                        tree.expandPath(selectAfter);
                        tree.setSelectionPath(selectAfter);
                        tree.scrollPathToVisible(selectAfter);
                        updateMaxSizeForParent((FileNode) selectAfter.getLastPathComponent());
                    }
                    tree.repaint();

                    int failed = paths.size() - deleted.size();
                    if (failed == 0) {
                        statusLabel.setText("Geloescht: " + deleted.size() + " Element(e)");
                    } else {
                        statusLabel.setText("Teilweise geloescht: " + deleted.size()
                            + " von " + paths.size());
                        JOptionPane.showMessageDialog(DiskAnalyzerWindow.this,
                            failed + " Element(e) konnten nicht (vollstaendig) geloescht werden.\n"
                            + "(Moeglicherweise fehlende Rechte oder Dateien in Benutzung)",
                            "Loeschen teilweise fehlgeschlagen", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Fehler beim Loeschen: " + ex.getMessage());
                }
                // UI-Zustand zuruecksetzen.
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                scanButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * Reduziert eine Auswahl auf ihre obersten Knoten: Pfade, die unterhalb eines
     * anderen ausgewaehlten Pfades liegen, werden entfernt (sie werden ohnehin
     * mitgeloescht und wuerden sonst doppelt verarbeitet). Liefert nie {@code null}.
     */
    private List<TreePath> topLevelPaths(TreePath[] selected) {
        List<TreePath> result = new ArrayList<>();
        if (selected == null) return result;
        for (TreePath p : selected) {
            boolean covered = false;
            for (TreePath q : selected) {
                // q ist ein echter Vorfahr von p? (isDescendant gilt auch fuer sich selbst)
                if (q != p && q.isDescendant(p)) { covered = true; break; }
            }
            if (!covered) result.add(p);
        }
        return result;
    }

    /**
     * Loescht eine Datei oder einen kompletten Verzeichnisbaum endgueltig.
     *
     * <p>Verwendet {@link Files#walkFileTree}, das Verknuepfungen standardmaessig
     * <b>nicht verfolgt</b>: Symlinks und (unter Windows) Verzeichnis-Junctions /
     * Reparse-Points werden als Eintrag selbst geloescht, aber nicht durchlaufen.
     * Das behebt zwei Probleme des frueheren rekursiven Ansatzes:
     * <ul>
     *   <li><b>Aufhaengen durch Zyklen:</b> eine Junction, die wieder nach oben im
     *       Baum zeigt (typisch unter {@code C:\Users\...}), liess die alte
     *       Rekursion endlos laufen.</li>
     *   <li><b>Datenverlust ausserhalb der Auswahl:</b> ueber einen verfolgten Link
     *       konnten Dateien geloescht werden, auf die nur verwiesen wurde.</li>
     * </ul>
     *
     * @param root     zu loeschende Datei oder Wurzel des Verzeichnisbaums
     * @param progress optionaler Callback je geloeschtem Eintrag (fuer die Statusanzeige)
     * @return {@code true}, wenn restlos alles geloescht werden konnte; sonst {@code false}
     */
    private boolean deleteTree(File root, Consumer<String> progress) {
        Path start = root.toPath();
        // Schon weg? Dann ist nichts zu tun (z. B. parallel bereits geloescht).
        if (!Files.exists(start, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return true;
        // Merker fuer Teilfehler (in der inneren Klasse veraenderbar -> Array).
        final boolean[] allOk = { true };

        try {
            // walkFileTree folgt per Default KEINEN symbolischen Links/Junctions.
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) {
                    deleteOne(f, progress, allOk);     // Dateien und Links direkt loeschen
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path f, IOException exc) {
                    // Eintrag nicht lesbar: trotzdem versuchen zu loeschen, dann weiter.
                    deleteOne(f, progress, allOk);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    deleteOne(dir, progress, allOk);   // Ordner erst nach seinem Inhalt
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            allOk[0] = false;
        }
        return allOk[0];
    }

    /** Loescht einen einzelnen Eintrag, meldet ihn und vermerkt etwaige Fehler. */
    private void deleteOne(Path p, Consumer<String> progress, boolean[] allOk) {
        try {
            if (progress != null) progress.accept(p.toString());
            Files.deleteIfExists(p);
        } catch (IOException | RuntimeException e) {
            // Einzelnen Fehler vermerken (z. B. fehlende Rechte, Datei in Benutzung),
            // aber den Rest weiter loeschen.
            allOk[0] = false;
        }
    }

    // ── ÖFFNEN ───────────────────────────────────────────────
    /**
     * Oeffnet den Knoten im Betriebssystem: Verzeichnisse im Dateimanager, Dateien
     * mit dem zugeordneten Standardprogramm. Es wird plattformabhaengig der jeweils
     * passende Befehl gewaehlt (Windows/macOS/Linux).
     */
    private void openNode(FileNode node) {
        File file = node.getFile();
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (node.isDirectory()) {
                if (os.contains("win")) {
                    new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", file.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
                }
            } else {
                // Bevorzugt die portable Desktop-API; sonst plattformspezifischer Fallback.
                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                } else if (os.contains("win")) {
                    new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", file.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
                }
            }
            statusLabel.setText("Geoeffnet: " + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Konnte nicht geoeffnet werden:\n" + ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── RESCAN ───────────────────────────────────────────────
    /**
     * Liest einen einzelnen Verzeichnisknoten im Hintergrund neu ein und uebernimmt
     * das Ergebnis in den bestehenden Baum (inklusive Aktualisierung der Eltern-Groessen
     * entlang der Pfadkette).
     */
    private void rescanNode(FileNode node, TreePath path) {
        statusLabel.setText("Rescan: " + node.getFile().getAbsolutePath());
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        scanButton.setEnabled(false);

        SwingWorker<FileNode, String> worker = new SwingWorker<FileNode, String>() {
            @Override
            protected FileNode doInBackground() {
                // publish(p) ist thread-sicher; der Scanner ruft den Callback ggf. nebenlaeufig auf.
                return DiskScanner.scan(node.getFile(), p -> publish(p));
            }
            @Override
            protected void process(List<String> chunks) {
                // Nur den juengsten Pfad zeigen (gekuerzt), um die UI nicht zu fluten.
                if (!chunks.isEmpty()) {
                    String last = chunks.get(chunks.size() - 1);
                    statusLabel.setText(last.length() > 90
                        ? "..." + last.substring(last.length() - 87) : last);
                }
            }
            @Override
            protected void done() {
                try {
                    FileNode fresh = get();
                    // Aufklapp-Zustand sichern, BEVOR der Teilbaum ersetzt wird.
                    List<TreePath> expanded = currentExpansion();

                    node.getChildren().clear();
                    node.getChildren().addAll(fresh.getChildren());
                    node.setTotalSize(fresh.getTotalSize());
                    node.setLoaded(true);
                    // Nur die Vorfahren des neu gescannten Knotens anpassen (nicht den ganzen Baum).
                    updateAncestorSizes(path.getParentPath());
                    treeModel.fireTreeStructureChanged();

                    // Zweige wieder aufklappen; den neu gescannten Knoten in jedem
                    // Fall oeffnen, damit das frische Ergebnis sichtbar ist.
                    restoreExpansion(expanded);
                    tree.expandPath(path);
                    tree.scrollPathToVisible(path);

                    updateMaxSizeForParent(node);
                    tree.repaint();
                    statusLabel.setText(String.format("Rescan fertig: %s  |  %s",
                        node.getName(), node.getSizeFormatted()));
                } catch (Exception ex) {
                    statusLabel.setText("Fehler beim Rescan: " + ex.getMessage());
                }
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                scanButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    // ── VOLLSTÄNDIGER SCAN ───────────────────────────────────
    /**
     * Startet einen vollstaendigen Scan des im Pfadfeld eingetragenen Verzeichnisses.
     * Die eigentliche Arbeit laeuft im Hintergrund ({@link SwingWorker}); Fortschritt
     * und Ergebnis werden auf dem EDT in die UI uebernommen.
     */
    private void startScan() {
        String path = pathField.getText().trim();
        File root = new File(path);
        if (!root.exists() || !root.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Verzeichnis nicht gefunden:\n" + path,
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        scanButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Scanne: " + path);

        SwingWorker<FileNode, String> worker = new SwingWorker<FileNode, String>() {
            @Override
            protected FileNode doInBackground() {
                // Der Scan selbst parallelisiert intern ueber einen ForkJoinPool.
                return DiskScanner.scan(root, p -> publish(p));
            }
            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String last = chunks.get(chunks.size() - 1);
                    statusLabel.setText(last.length() > 90
                        ? "..." + last.substring(last.length() - 87) : last);
                }
            }
            @Override
            protected void done() {
                try {
                    FileNode rootNode = get();
                    treeModel.setRoot(rootNode);
                    updateMaxSizeForParent(rootNode);
                    tree.repaint();
                    statusLabel.setText(String.format(
                        "Fertig. Gesamtgroesse: %s  |  %d Eintraege",
                        rootNode.getSizeFormatted(), rootNode.getChildren().size()));
                } catch (Exception ex) {
                    statusLabel.setText("Fehler: " + ex.getMessage());
                }
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                scanButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    // ── HILFSMETHODEN ────────────────────────────────────────

    /** Zeigt ein Menue mit allen verfuegbaren Laufwerken; Auswahl startet sofort einen Scan. */
    private void showRootChooser() {
        File[] roots = File.listRoots();
        if (roots == null || roots.length == 0) return;
        JPopupMenu menu = new JPopupMenu();
        for (File r : roots) {
            JMenuItem item = new JMenuItem(r.getAbsolutePath());
            item.setFont(new Font("Monospaced", Font.PLAIN, 12));
            item.addActionListener(e -> { pathField.setText(r.getAbsolutePath()); startScan(); });
            menu.add(item);
        }
        menu.show(scanButton, 0, scanButton.getHeight());
    }

    /**
     * Aktualisiert die Groessen nur entlang der Elternkette eines geaenderten
     * Knotens (vom uebergebenen Pfad bis zur Wurzel). Jeder Knoten erhaelt die
     * Summe der Groessen seiner aktuellen Kinder.
     *
     * <p>Bewusst <b>nicht</b> der ganze Baum: nach dem Loeschen sind nur die
     * Vorfahren des entfernten Knotens betroffen. Das haelt die Aktualisierung
     * auch bei sehr grossen Scans schnell und vermeidet ein Stocken der UI.
     */
    private void updateAncestorSizes(TreePath fromPath) {
        TreePath p = fromPath;
        while (p != null) {
            FileNode n = (FileNode) p.getLastPathComponent();
            long total = 0;
            for (FileNode child : n.getChildren()) total += child.getTotalSize();
            n.setTotalSize(total);
            p = p.getParentPath();
        }
    }

    /**
     * Setzt im Renderer den Bezugswert fuer die Groessenbalken auf die groesste
     * Kind-Groesse des Knotens. Dadurch fuellt der groesste Eintrag den Balken voll
     * aus und die uebrigen werden relativ dazu dargestellt.
     */
    private void updateMaxSizeForParent(FileNode parent) {
        if (parent == null || parent.getChildren().isEmpty()) return;
        long max = parent.getChildren().stream()
            .mapToLong(FileNode::getTotalSize).max().orElse(1);
        renderer.setMaxSize(max);
    }

    /**
     * Liefert die aktuell aufgeklappten Pfade (ab der Wurzel) als Liste. Wird vor
     * einem Strukturumbau aufgerufen, um die Aufklapp-Zustaende anschliessend wieder
     * herstellen zu koennen. Nie {@code null}.
     */
    private List<TreePath> currentExpansion() {
        java.util.Enumeration<TreePath> e =
            tree.getExpandedDescendants(new TreePath(treeModel.getRoot()));
        return (e == null) ? new ArrayList<>() : java.util.Collections.list(e);
    }

    /**
     * Klappt die uebergebenen Pfade wieder auf. Pfade, deren Knoten nicht mehr im
     * Baum vorhanden sind (z. B. geloeschte Zweige), werden von Swing ignoriert.
     */
    private void restoreExpansion(List<TreePath> paths) {
        for (TreePath p : paths) tree.expandPath(p);
    }

    /** Formatiert eine Groesse in Bytes menschenlesbar (B/KB/MB/GB). */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
