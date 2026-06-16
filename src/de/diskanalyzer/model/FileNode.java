package de.diskanalyzer.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileNode {
    private final File file;
    private long totalSize;
    private final List<FileNode> children = new ArrayList<>();
    private boolean loaded = false;

    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public FileNode(File file) {
        this.file = file;
        this.totalSize = file.isFile() ? file.length() : 0;
    }

    public File getFile() { return file; }
    public String getName() {
        return file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
    }
    public boolean isDirectory() { return file.isDirectory(); }
    public boolean isLoaded() { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long size) { this.totalSize = size; }
    public List<FileNode> getChildren() { return children; }

    public String getLastModifiedFormatted() {
        long lm = file.lastModified();
        if (lm == 0) return "";
        return DATE_FMT.format(new Date(lm));
    }

    public String getSizeFormatted() {
        if (totalSize < 1024) return totalSize + " B";
        if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
        if (totalSize < 1024L * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
        return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
    }

    @Override
    public String toString() { return getName(); }
}