/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.file.LibraryEventSource;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.JTreeDragController;
import com.cburch.logisim.util.JTreeUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class Explorer extends JTree implements LocaleListener {
    private static final String DIRTY_MARKER = "*";
    
    public static class Event {
        private TreePath path;
        
        private Event(TreePath path) {
            this.path = path;
        }
        
        public TreePath getTreePath() {
            return path;
        }
        
        public Object getTarget() {
            return path == null ? null : path.getLastPathComponent();
        }
    }
    
    public static interface Listener {
        public void selectionChanged(Event event);
        public void doubleClicked(Event event);
        public void moveRequested(Event event, AddTool dragged, AddTool target);
        public void deleteRequested(Event event);
        public JPopupMenu menuRequested(Event event);
    }

    private class MyModel implements TreeModel {
        ArrayList listeners = new ArrayList();

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }
        public Object getRoot() {
            return proj.getLogisimFile();
        }
        private List getChildren(Object parent) {
            if(parent == proj.getLogisimFile()) {
                return ((Library) parent).getElements();
            } else if(parent instanceof Library) {
                return ((Library) parent).getTools();
            } else {
                return Collections.EMPTY_LIST;
            }
        }
        public Object getChild(Object parent, int index) {
            return getChildren(parent).get(index);
        }
        public int getChildCount(Object parent) {
            return getChildren(parent).size();
        }
        public int getIndexOfChild(Object parent, Object child) {
            if(parent == null || child == null) return -1;
            Iterator it = getChildren(parent).iterator();
            int count = 0;
            while(it.hasNext()) {
                if(it.next() == child) return count;
                ++count;
            }
            return -1;
        }
        public boolean isLeaf(Object node) {
            return node != proj && !(node instanceof Library);
        }
        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException();
        }

        void fireTreeStructureChanged() {
            TreeModelEvent e = new TreeModelEvent(Explorer.this,
                new Object[] { model.getRoot() });
            Iterator it = listeners.iterator();
            while(it.hasNext()) {
                ((TreeModelListener) it.next()).treeStructureChanged(e);
            }
            Explorer.this.repaint();
        }

    }

    private class ToolIcon implements Icon {
        Tool tool;
        ComponentFactory circ = null;

        ToolIcon(Tool tool) {
            this.tool = tool;
            if(tool instanceof AddTool) {
                circ = ((AddTool) tool).getFactory();
            }
        }

        public int getIconHeight() {
            return 20;
        }

        public int getIconWidth() {
            return 20;
        }

        public void paintIcon(java.awt.Component c, Graphics g,
                int x, int y) {
            // draw halo if appropriate
            if(tool == haloedTool && proj.getFrame().getShowHalo()) {
                g.setColor(AttributeTable.HALO_COLOR);
                g.fillRoundRect(x, y, 20, 20, 10, 10);
                g.setColor(Color.BLACK);
            }

            // draw tool icon
            Graphics gIcon = g.create();
            ComponentDrawContext context = new ComponentDrawContext(Explorer.this, null, null, g, gIcon);
            tool.paintIcon(context, x, y);
            gIcon.dispose();

            // draw magnifying glass if appropriate
            if(circ == proj.getCurrentCircuit()) {
                int tx = x + 13;
                int ty = y + 13;
                int[] xp = { tx - 1, x + 18, x + 20, tx + 1 };
                int[] yp = { ty + 1, y + 20, y + 18, ty - 1 };
                g.setColor(java.awt.Color.black);
                g.drawOval(x + 5, y + 5, 10, 10);
                g.fillPolygon(xp, yp, xp.length);
            }
        }
    }

    private class MyCellRenderer extends DefaultTreeCellRenderer {
        public java.awt.Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            java.awt.Component ret;
            ret = super.getTreeCellRendererComponent(tree, value,
                selected, expanded, leaf, row, hasFocus);

            if(ret instanceof JComponent) {
                JComponent comp = (JComponent) ret;
                comp.setToolTipText(null);
            }
            if(value instanceof AddTool) {
                AddTool tool = (AddTool) value;
                if(ret instanceof JLabel) {
                    ((JLabel) ret).setText(((Tool) value).getDisplayName());
                    ((JLabel) ret).setIcon(new ToolIcon(tool));
                    ((JLabel) ret).setToolTipText(tool.getDescription());
                }
            } else if(value instanceof Tool) {
                Tool tool = (Tool) value;
                if(ret instanceof JLabel) {
                    ((JLabel) ret).setText(tool.getDisplayName());
                    ((JLabel) ret).setIcon(new ToolIcon(tool));
                    ((JLabel) ret).setToolTipText(tool.getDescription());
                }
            } else if(value instanceof Library) {
                if(ret instanceof JLabel) {
                    Library lib = (Library) value;
                    String text = lib.getDisplayName();
                    if(lib.isDirty()) text += DIRTY_MARKER;
                    ((JLabel) ret).setText(text);
                }
            }
            return ret;
        }
    }

    private class MySelectionModel extends DefaultTreeSelectionModel {
        public void addSelectionPath(TreePath path) {
            if(isPathValid(path)) super.addSelectionPath(path);
        }

        public void setSelectionPath(TreePath path) {
            if(isPathValid(path)) super.setSelectionPath(path);
        }

        public void addSelectionPaths(TreePath[] paths) {
            paths = getValidPaths(paths);
            if(paths != null) super.addSelectionPaths(paths);
        }

        public void setSelectionPaths(TreePath[] paths) {
            paths = getValidPaths(paths);
            if(paths != null) super.setSelectionPaths(paths);
        }

        private TreePath[] getValidPaths(TreePath[] paths) {
            int count = 0;
            for(int i = 0; i < paths.length; i++) {
                if(isPathValid(paths[i])) ++count;
            }
            if(count == 0) {
                return null;
            } else if(count == paths.length) {
                return paths;
            } else {
                TreePath[] ret = new TreePath[count];
                int j = 0;
                for(int i = 0; i < paths.length; i++) {
                    if(isPathValid(paths[i])) ret[j++] = paths[i];
                }
                return ret;
            }
        }

        private boolean isPathValid(TreePath path) {
            if(path == null || path.getPathCount() > 3) return false;
            Object last = path.getLastPathComponent();
            return last instanceof Tool;
        }
    }
    
    private class DragController implements JTreeDragController {
        public boolean canPerformAction(JTree targetTree,
                Object draggedNode, int action, Point location) {
            TreePath pathTarget = targetTree.getPathForLocation(location.x, location.y);
            if(pathTarget == null) {
                targetTree.setSelectionPath(null);
                return false;
            }
            targetTree.setSelectionPath(pathTarget);
            if(action == DnDConstants.ACTION_COPY) {
                return false;
            } else if(action == DnDConstants.ACTION_MOVE) {
                Object targetNode = pathTarget.getLastPathComponent();
                return canMove(draggedNode, targetNode);
            } else {
                return false;
            }
        }

        public boolean executeDrop(JTree targetTree, Object draggedNode,
                Object targetNode, int action) {
            if(action == DnDConstants.ACTION_COPY) {
                return false;
            } else if(action == DnDConstants.ACTION_MOVE) {
                if(canMove(draggedNode, targetNode)) {
                    if(draggedNode == targetNode) return true;
                    listener.moveRequested(new Event(null), (AddTool) draggedNode, (AddTool) targetNode);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        private boolean canMove(Object draggedNode, Object targetNode) {
            if(listener == null) return false;
            if(!(draggedNode instanceof AddTool) || !(targetNode instanceof AddTool)) return false;
            LogisimFile file = proj.getLogisimFile();
            AddTool dragged = (AddTool) draggedNode;
            AddTool target = (AddTool) targetNode;
            int draggedIndex = file.getTools().indexOf(dragged);
            int targetIndex = file.getTools().indexOf(target);
            if(targetIndex < 0 || draggedIndex < 0) return false;
            return true;
        }
    }
    
    private class DeleteAction extends AbstractAction {
        public void actionPerformed(ActionEvent event) {
            TreePath path = getSelectionPath();
            if(listener != null && path != null && path.getPathCount() == 2) {
                listener.deleteRequested(new Event(path));
            }
            Explorer.this.grabFocus();
        }
    }

    private class MyListener
            implements MouseListener, TreeSelectionListener,
                ProjectListener, LibraryListener, CircuitListener, PropertyChangeListener {
        //
        // MouseListener methods
        //
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
        public void mousePressed(MouseEvent e) {
            Explorer.this.grabFocus();
            checkForPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
            checkForPopup(e);
        }
        private void checkForPopup(MouseEvent e) {
            if(e.isPopupTrigger()) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if(path != null && listener != null) {
                    JPopupMenu menu = listener.menuRequested(new Event(path));
                    if(menu != null) {
                        menu.show(Explorer.this, e.getX(), e.getY());
                    }
                }
            }
        }
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if(path != null && listener != null) {
                    listener.doubleClicked(new Event(path));
                }
            }
        }

        //
        // TreeSelectionListener methods
        //
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getNewLeadSelectionPath();
            if(listener != null) {
                listener.selectionChanged(new Event(path));
            }
        }
        
        //
        // project/library file/circuit listener methods
        //
        public void projectChanged(ProjectEvent event) {
            int act = event.getAction();
            if(act == ProjectEvent.ACTION_SET_TOOL) {
                TreePath path = getSelectionPath();
                if(path != null && path.getLastPathComponent() != event.getTool()) {
                    clearSelection();
                }
            } else if(act == ProjectEvent.ACTION_SET_FILE) {
                setFile(event.getLogisimFile());
            } else if(act == ProjectEvent.ACTION_SET_CURRENT) {
                Explorer.this.repaint();
            }
        }

        public void libraryChanged(LibraryEvent event) {
            int act = event.getAction();
            if(act == LibraryEvent.ADD_TOOL) {
                if(event.getData() instanceof AddTool) {
                    AddTool tool = (AddTool) event.getData();
                    if(tool.getFactory() instanceof Circuit) {
                        Circuit circ = (Circuit) tool.getFactory();
                        circ.addCircuitListener(this);
                    }
                }
            } else if(act == LibraryEvent.REMOVE_TOOL) {
                if(event.getData() instanceof AddTool) {
                    AddTool tool = (AddTool) event.getData();
                    if(tool.getFactory() instanceof Circuit) {
                        Circuit circ = (Circuit) tool.getFactory();
                        circ.removeCircuitListener(this);
                    }
                }
            } else if(act == LibraryEvent.ADD_LIBRARY) {
                if(event.getData() instanceof LibraryEventSource) {
                    ((LibraryEventSource) event.getData()).addLibraryListener(subListener);
                }
            } else if(act == LibraryEvent.REMOVE_LIBRARY) {
                if(event.getData() instanceof LibraryEventSource) {
                    ((LibraryEventSource) event.getData()).removeLibraryListener(subListener);
                }
            }
            model.fireTreeStructureChanged();
        }

        public void circuitChanged(CircuitEvent event) {
            int act = event.getAction();
            if(act == CircuitEvent.ACTION_SET_NAME) {
                model.fireTreeStructureChanged();
            }
        }

        private void setFile(LogisimFile lib) {
            model.fireTreeStructureChanged();
            expandRow(0);

            for(Iterator it = lib.getTools().iterator(); it.hasNext(); ) {
                Object o = it.next();
                if(o instanceof AddTool) {
                    AddTool tool = (AddTool) o;
                    ComponentFactory source = tool.getFactory();
                    if(source instanceof Circuit) {
                        ((Circuit) source).addCircuitListener(this);
                    }
                }
            }
            
            subListener = new SubListener(); // create new one so that old listeners die away
            for(Iterator it = lib.getLibraries().iterator(); it.hasNext(); ) {
                Object o = it.next();
                if(o instanceof LibraryEventSource) {
                    ((LibraryEventSource) o).addLibraryListener(subListener);
                }
            }
        }
        
        //
        // PropertyChangeListener methods
        //
        public void propertyChange(PropertyChangeEvent e) {
            if(e.getPropertyName().equals(LogisimPreferences.GATE_SHAPE)) {
                repaint();
            }
        }
    }
    
    private class SubListener implements LibraryListener {
        public void libraryChanged(LibraryEvent event) {
            model.fireTreeStructureChanged();
        }
    }

    private Project proj;
    private MyListener myListener = new MyListener();
    private SubListener subListener = new SubListener();
    private MyModel model = new MyModel();
    private MyCellRenderer renderer = new MyCellRenderer();
    private DeleteAction deleteAction = new DeleteAction();
    private Listener listener = null;
    private Tool haloedTool = null;

    public Explorer(Project proj) {
        super();
        this.proj = proj;

        setModel(model);
        setRootVisible(true);
        addMouseListener(myListener);
        ToolTipManager.sharedInstance().registerComponent(this);

        MySelectionModel selector = new MySelectionModel();
        selector.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setSelectionModel(selector);
        setCellRenderer(renderer);
        JTreeUtil.configureDragAndDrop(this, new DragController());
        addTreeSelectionListener(myListener);
        
        InputMap imap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), deleteAction);
        ActionMap amap = getActionMap();
        amap.put(deleteAction, deleteAction);

        proj.addProjectListener(myListener);
        proj.addLibraryListener(myListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.GATE_SHAPE, myListener);
        myListener.setFile(proj.getLogisimFile());
        LocaleManager.addLocaleListener(this);
    }
    
    public Tool getSelectedTool() {
        TreePath path = getSelectionPath();
        if(path == null) return null;
        Object last = path.getLastPathComponent();
        return last instanceof Tool ? (Tool) last : null;
    }
    
    public void setListener(Listener value) {
        listener = value;
    }

    public void setHaloedTool(Tool t) {
        if(haloedTool == t) return;
        haloedTool = t;
        repaint();
    }

    public void localeChanged() {
        model.fireTreeStructureChanged();
    }
}
