package de.diskanalyzer.ui;

import de.diskanalyzer.logic.DiskScanner;
import de.diskanalyzer.model.FileNode;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link TreeModel}-Adapter, der einen {@link FileNode}-Baum an einen
 * {@code JTree} anbindet.
 *
 * <p>Das Modell unterstuetzt <b>Lazy Loading</b>: Verzeichnisse, deren Kinder noch
 * nicht eingelesen wurden, werden erst beim ersten Zugriff (Aufklappen) ueber
 * {@link DiskScanner#loadChildren} nachgeladen. Nach einem vollstaendigen Scan ist
 * in der Regel bereits alles geladen; der Lazy-Pfad greift dann nur noch fuer
 * Knoten, die bewusst als "nicht geladen" markiert wurden.
 */
public class FileTreeModel implements TreeModel {

    private FileNode root;

    /** Bei Strukturaenderungen zu benachrichtigende Listener (i. d. R. der JTree). */
    private final List<TreeModelListener> listeners = new ArrayList<>();

    public FileTreeModel(FileNode root) { this.root = root; }

    /** Setzt eine neue Wurzel (z. B. nach einem frischen Scan) und meldet die Aenderung. */
    public void setRoot(FileNode root) {
        this.root = root;
        fireTreeStructureChanged();
    }

    @Override public Object getRoot() { return root; }

    @Override
    public Object getChild(Object parent, int index) {
        FileNode node = (FileNode) parent;
        triggerLoadIfNeeded(node);
        return node.getChildren().get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        FileNode node = (FileNode) parent;
        if (!node.isDirectory()) return 0;
        triggerLoadIfNeeded(node);
        return node.getChildren().size();
    }

    /** Dateien sind Blaetter, Verzeichnisse nicht. */
    @Override public boolean isLeaf(Object node) { return !((FileNode) node).isDirectory(); }

    @Override public int getIndexOfChild(Object parent, Object child) {
        return ((FileNode) parent).getChildren().indexOf(child);
    }

    // Im Baum wird nichts editiert -> keine Aktion noetig.
    @Override public void valueForPathChanged(TreePath path, Object newValue) {}

    @Override public void addTreeModelListener(TreeModelListener l) { listeners.add(l); }
    @Override public void removeTreeModelListener(TreeModelListener l) { listeners.remove(l); }

    /**
     * Laedt die Kinder eines Verzeichnisses nach, falls das noch nicht geschehen
     * ist, und benachrichtigt anschliessend den Baum.
     */
    private void triggerLoadIfNeeded(FileNode node) {
        if (node.isDirectory() && !node.isLoaded()) {
            DiskScanner.loadChildren(node, null);
            fireTreeStructureChanged();
        }
    }

    /**
     * Meldet allen Listenern, dass sich die Baumstruktur (ab der Wurzel) geaendert
     * hat. Bewusst grob gehalten: der JTree zeichnet sich danach komplett neu, was
     * fuer diese Anwendung ausreichend und unkompliziert ist.
     */
    public void fireTreeStructureChanged() {
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{root});
        for (TreeModelListener l : listeners) l.treeStructureChanged(event);
    }
}
