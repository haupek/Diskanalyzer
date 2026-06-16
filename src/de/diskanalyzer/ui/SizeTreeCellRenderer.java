package de.diskanalyzer.ui;

import de.diskanalyzer.model.FileNode;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class SizeTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Color DIR_COLOR  = new Color(0x1A73E8);
    private static final Color FILE_COLOR = new Color(0x555E6E);
    private static final Color DATE_COLOR = new Color(0x999EAA);
    private long maxSize = 1;
    private FileNode nodeRef;
    private boolean isSelected;

    public void setMaxSize(long maxSize) { this.maxSize = maxSize == 0 ? 1 : maxSize; }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        nodeRef = (FileNode) value;
        isSelected = sel;
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        String sizeStr  = String.format("%-10s", nodeRef.getSizeFormatted());
        String name     = nodeRef.getName();
        String dateStr  = "";

        if (!nodeRef.isDirectory()) {
            String lm = nodeRef.getLastModifiedFormatted();
            if (!lm.isEmpty()) {
                dateStr = "&nbsp;&nbsp;<span style='color:#999;font-size:10px'>" + lm + "</span>";
            }
        }

        setText("<html>"
            + "<span style='color:#888;font-size:10px'>" + sizeStr + "</span>"
            + "&nbsp;&nbsp;" + name
            + dateStr
            + "</html>");

        if (nodeRef.isDirectory()) {
            setForeground(sel ? Color.WHITE : DIR_COLOR);
            setIcon(UIManager.getIcon("FileView.directoryIcon"));
        } else {
            setForeground(sel ? Color.WHITE : FILE_COLOR);
            setIcon(UIManager.getIcon("FileView.fileIcon"));
        }
        setOpaque(false);
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        if (isSelected) {
            g2.setColor(new Color(0x1A73E8));
            g2.fillRoundRect(0, 1, w - 2, h - 2, 6, 6);
        }
        if (nodeRef != null && maxSize > 0) {
            double ratio = Math.min(1.0, (double) nodeRef.getTotalSize() / maxSize);
            int barW = (int) (ratio * (w - 4));
            Color base = nodeRef.isDirectory() ? new Color(0x1A73E8) : new Color(0x34A853);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                                  isSelected ? 80 : 35));
            g2.fillRoundRect(2, h - 5, barW, 3, 2, 2);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}