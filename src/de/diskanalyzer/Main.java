package de.diskanalyzer;

import de.diskanalyzer.ui.DiskAnalyzerWindow;
import javax.swing.SwingUtilities;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>DiskAnalyzer ist eine reine Swing-Desktop-Anwendung. Das Fenster wird
 * &ndash; wie bei Swing vorgeschrieben &ndash; auf dem Event-Dispatch-Thread (EDT)
 * erzeugt und angezeigt, damit alle UI-Operationen im richtigen Thread laufen.
 */
public class Main {

    /**
     * Startet die Anwendung.
     *
     * @param args werden nicht ausgewertet
     */
    public static void main(String[] args) {
        // Fenster erst auf dem EDT erzeugen (Swing-Thread-Regel).
        SwingUtilities.invokeLater(() -> {
            DiskAnalyzerWindow window = new DiskAnalyzerWindow();
            window.setVisible(true);
        });
    }
}
