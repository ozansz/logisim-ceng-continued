/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Subcircuit;
import com.cburch.logisim.comp.Component;

class ComponentSelector extends JTree {
    private class CircuitNode implements TreeNode, CircuitListener, Comparator {
        private CircuitNode parent;
        private CircuitState circuitState;
        private Subcircuit subcircuit;
        private ArrayList children;
        
        public CircuitNode(CircuitNode parent, CircuitState circuitState,
                Subcircuit subcircuit) {
            this.parent = parent;
            this.circuitState = circuitState;
            this.subcircuit = subcircuit;
            this.children = new ArrayList();
            circuitState.getCircuit().addCircuitListener(this);
            computeChildren();
        }
        
        public String toString() {
            String ret = circuitState.getCircuit().getDisplayName();
            if(subcircuit != null) {
                ret += subcircuit.getLocation();
            }
            return ret;
        }

        public TreeNode getChildAt(int index) {
            return (TreeNode) children.get(index);
        }

        public int getChildCount() {
            return children.size();
        }

        public TreeNode getParent() {
            return parent;
        }

        public int getIndex(TreeNode node) {
            return children.indexOf(node);
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public boolean isLeaf() {
            return false;
        }

        public Enumeration children() {
            return Collections.enumeration(children);
        }

        public void circuitChanged(CircuitEvent event) {
            int action = event.getAction();
            DefaultTreeModel model = (DefaultTreeModel) getModel();
            if(action == CircuitEvent.ACTION_SET_NAME) {
                model.nodeChanged(this);
            } else {
                if(computeChildren()) {
                    model.nodeStructureChanged(this);
                } else if(action == CircuitEvent.ACTION_INVALIDATE) {
                    Object o = event.getData();
                    for(int i = children.size() - 1; i >= 0; i--) {
                        Object o2 = children.get(i);
                        if(o2 instanceof ComponentNode) {
                            ComponentNode n = (ComponentNode) o2;
                            if(n.comp == o) {
                                int[] changed = { i };
                                children.remove(i);
                                model.nodesWereRemoved(this, changed, new Object[] { n });
                                children.add(i, new ComponentNode(this, n.comp));
                                model.nodesWereInserted(this, changed);
                            }
                        }
                    }
                }
            }
        }
        
        // returns true if changed
        private boolean computeChildren() {
            ArrayList newChildren = new ArrayList();
            ArrayList subcircs = new ArrayList();
            for(Iterator it = circuitState.getCircuit().getNonWires().iterator();
                    it.hasNext(); ) {
                Component comp = (Component) it.next();
                if(comp instanceof Subcircuit) {
                    subcircs.add(comp);
                } else {
                    Object o = comp.getFeature(Loggable.class);
                    if(o != null) {
                        ComponentNode toAdd = null;
                        for(Iterator it2 = children.iterator(); it2.hasNext(); ) {
                            Object o2 = it2.next();
                            if(o2 instanceof ComponentNode) {
                                ComponentNode n = (ComponentNode) o2;
                                if(n.comp == comp) { toAdd = n; break; }
                            }
                        }
                        if(toAdd == null) toAdd = new ComponentNode(this, comp);
                        newChildren.add(toAdd);
                    }
                }
            }
            Collections.sort(newChildren, this);
            Collections.sort(subcircs, this);
            for(Iterator it = subcircs.iterator(); it.hasNext(); ) {
                Subcircuit comp = (Subcircuit) it.next();
                CircuitState state = (CircuitState) comp.getSubstate(circuitState);
                CircuitNode toAdd = null;
                for(Iterator it2 = children.iterator(); it2.hasNext(); ) {
                    Object o = it2.next();
                    if(o instanceof CircuitNode) {
                        CircuitNode n = (CircuitNode) o;
                        if(n.circuitState == state) { toAdd = n; break; }
                    }
                }
                if(toAdd == null) {
                    toAdd = new CircuitNode(this, state, comp);
                }
                newChildren.add(toAdd);
            }
            
            if(!children.equals(newChildren)) {
                children = newChildren;
                return true;
            } else {
                return false;
            }
        }
        
        public int compare(Object a, Object b) {
            if(a instanceof Subcircuit) {
                Subcircuit x = (Subcircuit) a;
                Subcircuit y = (Subcircuit) b;
                int ret = x == y ? 0 : x.getFactory().getDisplayName().compareToIgnoreCase(y.getFactory().getDisplayName());
                return ret != 0 ? ret : x.getLocation().toString().compareTo(y.getLocation().toString());
            } else {
                return a.toString().compareToIgnoreCase(b.toString());
            }
        }
    }
    
    private class ComponentNode implements TreeNode {
        private CircuitNode parent;
        private Component comp;
        private OptionNode[] opts;
        
        public ComponentNode(CircuitNode parent, Component comp) {
            this.parent = parent;
            this.comp = comp;
            this.opts = null;
            
            Loggable log = (Loggable) comp.getFeature(Loggable.class);
            if(log != null) {
                Object[] opts = log.getLogOptions(parent.circuitState);
                if(opts != null && opts.length > 0) {
                    this.opts = new OptionNode[opts.length];
                    for(int i = 0; i < opts.length; i++) {
                        this.opts[i] = new OptionNode(this, opts[i]);
                    }
                }
            }
        }
        
        public String toString() {
            Loggable log = (Loggable) comp.getFeature(Loggable.class);
            if(log != null) {
                String ret = log.getLogName(null);
                if(ret != null && !ret.equals("")) return ret;
            }
            return comp.getFactory().getDisplayName() + " " + comp.getLocation();
        }

        public TreeNode getChildAt(int index) {
            return opts[index];
        }

        public int getChildCount() {
            return opts == null ? 0 : opts.length;
        }

        public TreeNode getParent() {
            return parent;
        }

        public int getIndex(TreeNode n) {
            for(int i = 0; i < opts.length; i++) {
                if(opts[i] == n) return i;
            }
            return -1;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public boolean isLeaf() {
            return opts == null || opts.length == 0;
        }

        public Enumeration children() {
            return Collections.enumeration(Arrays.asList(opts));
        }
    }
    
    private class OptionNode implements TreeNode {
        private ComponentNode parent;
        private Object option;
        
        public OptionNode(ComponentNode parent, Object option) {
            this.parent = parent;
            this.option = option;
        }
        
        public String toString() {
            return option.toString();
        }

        public TreeNode getChildAt(int arg0) {
            return null;
        }

        public int getChildCount() {
            return 0;
        }

        public TreeNode getParent() {
            return parent;
        }

        public int getIndex(TreeNode n) {
            return -1;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public boolean isLeaf() {
            return true;
        }

        public Enumeration children() {
            return Collections.enumeration(Collections.EMPTY_SET);
        }
    }
    
    private class MyCellRenderer extends DefaultTreeCellRenderer {
        public java.awt.Component getTreeCellRendererComponent(JTree tree,
                Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            java.awt.Component ret = super.getTreeCellRendererComponent(tree,
                    value, selected, expanded, leaf, row, hasFocus);
            if(ret instanceof JLabel && value instanceof ComponentNode) {
                ComponentNode node = (ComponentNode) value;
                ComponentIcon icon = new ComponentIcon(node.comp);
                if(node.getChildCount() > 0) {
                    icon.setTriangleState(expanded
                            ? ComponentIcon.TRIANGLE_OPEN : ComponentIcon.TRIANGLE_CLOSED);
                }
                ((JLabel) ret).setIcon(icon);
            }
            return ret;
        }
    }
    
    private Model logModel;
    
    public ComponentSelector(Model logModel) {
        DefaultTreeModel model = new DefaultTreeModel(null);
        model.setAsksAllowsChildren(false);
        setModel(model);
        setRootVisible(false);
        setLogModel(logModel);
        setCellRenderer(new MyCellRenderer());
    }
    
    public void setLogModel(Model value) {
        this.logModel = value;
        
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        CircuitNode curRoot = (CircuitNode) model.getRoot();
        CircuitState state = logModel == null ? null : logModel.getCircuitState();
        if(state == null) {
            if(curRoot != null) model.setRoot(null);
            return;
        }
        if(curRoot == null || curRoot.circuitState != state) {
            curRoot = new CircuitNode(null, state, null);
            model.setRoot(curRoot);
        }
    }
    
    public List getSelectedItems() {
        TreePath[] sel = getSelectionPaths();
        if(sel == null || sel.length == 0) return Collections.EMPTY_LIST;
        
        ArrayList ret = new ArrayList();
        for(int i = 0; i < sel.length; i++) {
            TreePath path = sel[i];
            Object last = path.getLastPathComponent();
            ComponentNode n = null;
            Object opt = null;
            if(last instanceof OptionNode) {
                OptionNode o = (OptionNode) last;
                n = o.parent;
                opt = o.option;
            } else if(last instanceof ComponentNode) {
                n = (ComponentNode) last;
                if(n.opts != null) n = null;
            }
            if(n != null) {
                int count = 0;
                for(CircuitNode cur = n.parent; cur != null; cur = cur.parent) {
                    count++;
                }
                Subcircuit[] nPath = new Subcircuit[count - 1];
                CircuitNode cur = n.parent;
                for(int j = nPath.length - 1; j >= 0; j--) {
                    nPath[j] = cur.subcircuit;
                }
                ret.add(new SelectionItem(logModel, nPath, n.comp, opt));
            }
        }
        return ret.size() == 0 ? null : ret;
    }
    
    public boolean hasSelectedItems() {
        TreePath[] sel = getSelectionPaths();
        if(sel == null || sel.length == 0) return false;
        
        for(int i = 0; i < sel.length; i++) {
            Object last = sel[i].getLastPathComponent();
            if(last instanceof OptionNode) {
                return true;
            } else if(last instanceof ComponentNode) {
                if(((ComponentNode) last).opts == null) return true;
            }
        }
        return false;
    }
    
    public void localeChanged() {
        repaint();
    }
}
