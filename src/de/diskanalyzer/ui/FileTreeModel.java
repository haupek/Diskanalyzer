package de.diskanalyzer.ui;

import de.diskanalyzer.logic.DiskScanner;
import de.diskanalyzer.model.FileNode;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class FileTreeModel implements TreeModel {

    private FileNode root;
    private final List<TreeModelListener> listeners = new ArrayList<>();

    public FileTreeModel(FileNode root) { this.root = root; }

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

    @Override public boolean isLeaf(Object node) { return !((FileNode) node).isDirectory(); }
    @Override public int getIndexOfChild(Object parent, Object child) {
        return ((FileNode) parent).getChildren().indexOf(child);
    }
    @Override public void valueForPathChanged(TreePath path, Object newValue) {}
    @Override public void addTreeModelListener(TreeModelListener l) { listeners.add(l); }
    @Override public void removeTreeModelListener(TreeModelListener l) { listeners.remove(l); }

    private void triggerLoadIfNeeded(FileNode node) {
        if (node.isDirectory() && !node.isLoaded()) {
            DiskScanner.loadChildren(node, null);
            fireTreeStructureChanged();
        }
    }

    public void fireTreeStructureChanged() {
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{root});
        for (TreeModelListener l : listeners) l.treeStructureChanged(event);
    }
}