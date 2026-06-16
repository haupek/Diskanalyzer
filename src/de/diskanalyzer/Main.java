package de.diskanalyzer;

import de.diskanalyzer.ui.DiskAnalyzerWindow;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DiskAnalyzerWindow window = new DiskAnalyzerWindow();
            window.setVisible(true);
        });
    }
}