package de.diskanalyzer.logic;

import de.diskanalyzer.model.FileNode;
import java.io.File;
import java.util.Comparator;
import java.util.function.Consumer;

public class DiskScanner {

    public static FileNode scan(File root, Consumer<String> progressCallback) {
        FileNode node = new FileNode(root);

        // Datei existiert nicht mehr - defensiv abfangen
        if (!root.exists()) {
            node.setTotalSize(0);
            return node;
        }

        if (root.isFile()) {
            // Groesse nochmal fresh lesen - Datei koennte sich geaendert haben
            node.setTotalSize(root.length());
            return node;
        }

        if (!root.isDirectory()) {
            // Weder Datei noch Verzeichnis (z.B. Symlink-Ziel weg)
            return node;
        }

        File[] entries;
        try {
            entries = root.listFiles();
        } catch (SecurityException e) {
            // Kein Zugriff - Knoten leer zurueckgeben
            node.setLoaded(true);
            return node;
        }

        if (entries == null) {
            // listFiles() gibt null bei I/O-Fehler oder fehlendem Zugriff
            node.setLoaded(true);
            return node;
        }

        long total = 0;
        for (File entry : entries) {
            // Zwischen listFiles() und dem Zugriff koennte die Datei geloescht worden sein
            if (!entry.exists()) continue;

            if (progressCallback != null) {
                try { progressCallback.accept(entry.getAbsolutePath()); }
                catch (Exception ignored) {}
            }

            try {
                FileNode child = scan(entry, progressCallback);
                node.getChildren().add(child);
                total += child.getTotalSize();
            } catch (Exception e) {
                // Einzelnen fehlerhaften Eintrag ueberspringen, Rest weiterscannen
            }
        }

        node.getChildren().sort(Comparator.comparingLong(FileNode::getTotalSize).reversed());
        node.setTotalSize(total);
        node.setLoaded(true);
        return node;
    }

    public static void loadChildren(FileNode node, Consumer<String> progressCallback) {
        if (node.isLoaded() || !node.isDirectory()) return;

        if (!node.getFile().exists()) {
            node.setLoaded(true);
            return;
        }

        File[] entries;
        try {
            entries = node.getFile().listFiles();
        } catch (SecurityException e) {
            node.setLoaded(true);
            return;
        }

        if (entries == null) {
            node.setLoaded(true);
            return;
        }

        node.getChildren().clear();
        for (File entry : entries) {
            if (!entry.exists()) continue;
            try {
                FileNode child = scan(entry, null);
                node.getChildren().add(child);
            } catch (Exception e) {
                // Eintrag ueberspringen
            }
        }

        node.getChildren().sort(Comparator.comparingLong(FileNode::getTotalSize).reversed());
        node.setTotalSize(node.getChildren().stream().mapToLong(FileNode::getTotalSize).sum());
        node.setLoaded(true);
    }
}