package de.diskanalyzer.ui;

import de.diskanalyzer.logic.DiskScanner;
import de.diskanalyzer.model.FileNode;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

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
        setLocationRelativeTo(null);
        applyLightTheme();
        buildUI();
    }

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

    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        topBar.setBackground(new Color(0xE8EDF5));

        JLabel titleLabel = new JLabel("DISK ANALYZER");
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        titleLabel.setForeground(new Color(0x1A73E8));

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

        FileNode placeholder = new FileNode(new File("."));
        treeModel = new FileTreeModel(placeholder);
        renderer  = new SizeTreeCellRenderer();

        tree = new JTree(treeModel);
        tree.setCellRenderer(renderer);
        tree.setBackground(Color.WHITE);
        tree.setRowHeight(26);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        tree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent e) {
                updateMaxSizeForParent((FileNode) e.getPath().getLastPathComponent());
                tree.repaint();
            }
            public void treeCollapsed(TreeExpansionEvent e) {}
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    tree.setSelectionPath(path);
                    FileNode node = (FileNode) path.getLastPathComponent();
                    showContextMenu(node, path, e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xDDE3EC)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

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

        SwingUtilities.invokeLater(this::startScan);
    }

    // ── KONTEXTMENÜ ──────────────────────────────────────────
    private void showContextMenu(FileNode node, TreePath path, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        // Rescan (nur Verzeichnisse)
        if (node.isDirectory()) {
            JMenuItem rescanItem = new JMenuItem("  \u27f3  Rescan");
            rescanItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
            rescanItem.addActionListener(e -> rescanNode(node));
            menu.add(rescanItem);
            menu.addSeparator();
        }

        // Öffnen
        String openLabel = node.isDirectory() ? "  \uD83D\uDCC2  Im Explorer oeffnen" : "  \u2197  Oeffnen";
        JMenuItem openItem = new JMenuItem(openLabel);
        openItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
        openItem.addActionListener(e -> openNode(node));
        menu.add(openItem);

        menu.addSeparator();

        // Löschen
        JMenuItem deleteItem = new JMenuItem("  \uD83D\uDDD1  Loeschen...");
        deleteItem.setFont(new Font("Monospaced", Font.PLAIN, 12));
        deleteItem.setForeground(new Color(0xCC2200));
        deleteItem.addActionListener(e -> deleteNode(node, path));
        menu.add(deleteItem);

        menu.show(tree, x, y);
    }

    // ── LÖSCHEN ──────────────────────────────────────────────
    private void deleteNode(FileNode node, TreePath path) {
        File file = node.getFile();
        String typ = node.isDirectory() ? "Verzeichnis" : "Datei";

        String msg = "<html><b>" + typ + " wirklich loeschen?</b><br><br>"
            + file.getAbsolutePath() + "<br><br>"
            + "Groesse: " + node.getSizeFormatted()
            + (node.isDirectory() ? "<br><font color='red'>Der gesamte Inhalt wird geloescht!</font>" : "")
            + "</html>";

        int result = JOptionPane.showConfirmDialog(this, msg,
            "Loeschen bestaetigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result != JOptionPane.YES_OPTION) return;

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Loesche: " + file.getAbsolutePath());
        scanButton.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return deleteRecursive(file);
            }
            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        // Elternknoten ermitteln und Knoten entfernen
                        TreePath parentPath = path.getParentPath();
                        if (parentPath != null) {
                            FileNode parent = (FileNode) parentPath.getLastPathComponent();
                            parent.getChildren().remove(node);
                            rebuildParentSizes((FileNode) treeModel.getRoot());
                            treeModel.fireTreeStructureChanged();
                            updateMaxSizeForParent(parent);
                            tree.repaint();
                        }
                        statusLabel.setText("Geloescht: " + file.getAbsolutePath());
                    } else {
                        statusLabel.setText("Loeschen fehlgeschlagen (teilweise): " + file.getName());
                        JOptionPane.showMessageDialog(DiskAnalyzerWindow.this,
                            "Einige Dateien konnten nicht geloescht werden.\n"
                            + "(Moeglicherweise fehlende Rechte oder Dateien in Benutzung)",
                            "Loeschen teilweise fehlgeschlagen", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Fehler beim Loeschen: " + ex.getMessage());
                }
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                scanButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    /** Löscht rekursiv. Gibt true zurück wenn alles gelöscht wurde. */
    private boolean deleteRecursive(File file) {
        if (!file.exists()) return true;
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                boolean allOk = true;
                for (File entry : entries) {
                    if (!deleteRecursive(entry)) allOk = false;
                }
                if (!allOk) return false;
            }
        }
        return file.delete();
    }

    // ── ÖFFNEN ───────────────────────────────────────────────
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
    private void rescanNode(FileNode node) {
        statusLabel.setText("Rescan: " + node.getFile().getAbsolutePath());
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        scanButton.setEnabled(false);

        SwingWorker<FileNode, String> worker = new SwingWorker<FileNode, String>() {
            @Override
            protected FileNode doInBackground() {
                return DiskScanner.scan(node.getFile(), p -> publish(p));
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
                    FileNode fresh = get();
                    node.getChildren().clear();
                    node.getChildren().addAll(fresh.getChildren());
                    node.setTotalSize(fresh.getTotalSize());
                    node.setLoaded(true);
                    rebuildParentSizes((FileNode) treeModel.getRoot());
                    treeModel.fireTreeStructureChanged();
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

    private long rebuildParentSizes(FileNode node) {
        if (!node.isDirectory() || node.getChildren().isEmpty()) return node.getTotalSize();
        long total = 0;
        for (FileNode child : node.getChildren()) total += rebuildParentSizes(child);
        node.setTotalSize(total);
        return total;
    }

    private void updateMaxSizeForParent(FileNode parent) {
        if (parent == null || parent.getChildren().isEmpty()) return;
        long max = parent.getChildren().stream()
            .mapToLong(FileNode::getTotalSize).max().orElse(1);
        renderer.setMaxSize(max);
    }
}