package de.diskanalyzer.ui;

import de.diskanalyzer.model.FileNode;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Zeichnet eine einzelne Zeile des Baums: Groesse, Name und (bei Dateien)
 * Aenderungsdatum, jeweils farblich abgesetzt, plus einen kleinen Balken am
 * unteren Rand, der die relative Groesse innerhalb der Geschwister visualisiert.
 *
 * <p>Der Text wird ueber einfaches HTML formatiert; der Groessenbalken und die
 * abgerundete Auswahl-Hervorhebung werden in {@link #paintComponent} selbst
 * gezeichnet.
 */
public class SizeTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Color DIR_COLOR  = new Color(0x1A73E8);  // Verzeichnisse: Blau
    private static final Color FILE_COLOR = new Color(0x555E6E);  // Dateien: Grau
    private static final Color DATE_COLOR = new Color(0x999EAA);  // Datum: helles Grau

    /** Groesste Geschwister-Groesse; dient als 100%-Bezug fuer den Balken. */
    private long maxSize = 1;

    /** Aktuell gerenderter Knoten (in getTreeCellRendererComponent gesetzt). */
    private FileNode nodeRef;

    /** Ob die aktuelle Zeile ausgewaehlt ist (steuert Hervorhebung in paintComponent). */
    private boolean isSelected;

    /**
     * Setzt den Bezugswert fuer den Groessenbalken. 0 wird auf 1 angehoben, um
     * eine Division durch Null zu vermeiden.
     */
    public void setMaxSize(long maxSize) { this.maxSize = maxSize == 0 ? 1 : maxSize; }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        nodeRef = (FileNode) value;
        isSelected = sel;
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Groesse links buendig auf feste Breite -> Namen stehen sauber untereinander.
        String sizeStr  = String.format("%-10s", nodeRef.getSizeFormatted());
        String name     = nodeRef.getName();
        String dateStr  = "";

        // Aenderungsdatum nur bei Dateien anzeigen.
        if (!nodeRef.isDirectory()) {
            String lm = nodeRef.getLastModifiedFormatted();
            if (!lm.isEmpty()) {
                dateStr = "&nbsp;&nbsp;<span style='color:#999;font-size:10px'>" + lm + "</span>";
            }
        }

        // Zeileninhalt als kleines HTML zusammensetzen (Groesse | Name | Datum).
        setText("<html>"
            + "<span style='color:#888;font-size:10px'>" + sizeStr + "</span>"
            + "&nbsp;&nbsp;" + name
            + dateStr
            + "</html>");

        // Icon und Textfarbe je nach Typ; bei Auswahl weisser Text fuer Kontrast.
        if (nodeRef.isDirectory()) {
            setForeground(sel ? Color.WHITE : DIR_COLOR);
            setIcon(UIManager.getIcon("FileView.directoryIcon"));
        } else {
            setForeground(sel ? Color.WHITE : FILE_COLOR);
            setIcon(UIManager.getIcon("FileView.fileIcon"));
        }
        // Nicht-opak, damit wir den Hintergrund in paintComponent selbst zeichnen.
        setOpaque(false);
        return this;
    }

    /**
     * Zeichnet zuerst Auswahl-Hintergrund und Groessenbalken, dann (ueber
     * {@code super}) den eigentlichen Zeileninhalt darueber.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // Abgerundete Hervorhebung der ausgewaehlten Zeile.
        if (isSelected) {
            g2.setColor(new Color(0x1A73E8));
            g2.fillRoundRect(0, 1, w - 2, h - 2, 6, 6);
        }

        // Groessenbalken am unteren Rand: Breite proportional zur groessten Geschwister-Groesse.
        if (nodeRef != null && maxSize > 0) {
            double ratio = Math.min(1.0, (double) nodeRef.getTotalSize() / maxSize);
            int barW = (int) (ratio * (w - 4));
            // Ordner blau, Dateien gruen; halbtransparent (bei Auswahl etwas kraeftiger).
            Color base = nodeRef.isDirectory() ? new Color(0x1A73E8) : new Color(0x34A853);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                                  isSelected ? 80 : 35));
            g2.fillRoundRect(2, h - 5, barW, 3, 2, 2);
        }
        g2.dispose();

        // Text/Icon zuletzt, damit sie ueber Hintergrund und Balken liegen.
        super.paintComponent(g);
    }
}
