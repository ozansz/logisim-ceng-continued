/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.circuit.WidthIncompatibilityData;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.proj.LogisimPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

import java.util.Iterator;
import java.util.Set;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class Canvas extends JPanel
        implements LocaleListener, Scrollable {
    private static final int BOUNDS_BUFFER = 70;
        // pixels shown in canvas beyond outermost boundaries
    static final double SQRT_2 = Math.sqrt(2.0);
    private static final int BUTTONS_MASK = InputEvent.BUTTON1_MASK
        | InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK;

    private class MyListener
            implements MouseInputListener, KeyListener, PopupMenuListener, ComponentListener,
                PropertyChangeListener {
        boolean menu_on = false;

        //
        // MouseListener methods
        //
        public void mouseClicked(MouseEvent e) { }

        public void mouseMoved(MouseEvent e) {
            if((e.getModifiers() & BUTTONS_MASK) != 0) {
                // If the control key is down while the mouse is being
                // dragged, mouseMoved is called instead. This may well be
                // an issue specific to the MacOS Java implementation,
                // but it exists there in the 1.4 and 5.0 versions.
                mouseDragged(e);
                return;
            }
            
            Tool tool = getToolFor(e);
            if(tool != null) {
                repairMouseEvent(e);
                tool.mouseMoved(Canvas.this, getGraphics(), e);
            }
        }

        public void mouseDragged(MouseEvent e) {
            if(drag_tool != null) {
                repairMouseEvent(e);
                drag_tool.mouseDragged(Canvas.this, getGraphics(), e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            if(drag_tool != null) {
                repairMouseEvent(e);
                drag_tool.mouseEntered(Canvas.this, getGraphics(), e);
            } else {
                Tool tool = getToolFor(e);
                if(tool != null) {
                    repairMouseEvent(e);
                    tool.mouseEntered(Canvas.this, getGraphics(), e);
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            if(drag_tool != null) {
                repairMouseEvent(e);
                drag_tool.mouseExited(Canvas.this, getGraphics(), e);
            } else {
                Tool tool = getToolFor(e);
                if(tool != null) {
                    repairMouseEvent(e);
                    tool.mouseExited(Canvas.this, getGraphics(), e);
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            viewport.setErrorMessage(null);
            proj.setStartupScreen(false);
            Canvas.this.grabFocus();
            drag_tool = getToolFor(e);
            if(drag_tool != null) {
                repairMouseEvent(e);
                drag_tool.mousePressed(Canvas.this, getGraphics(), e);
            }
            
            completeAction();
        }

        public void mouseReleased(MouseEvent e) {
            if(drag_tool != null) {
                repairMouseEvent(e);
                drag_tool.mouseReleased(Canvas.this, getGraphics(), e);
                drag_tool = null;
            }

            Tool tool = proj.getTool();
            if(tool != null) {
                tool.mouseMoved(Canvas.this, getGraphics(), e);
            }

            completeAction();
        }

        private Tool getToolFor(MouseEvent e) {
            if(menu_on) return null;

            Tool ret = mappings.getToolFor(e);
            if(ret == null) return proj.getTool();
            else return ret;
        }

        private void repairMouseEvent(MouseEvent e) {
            if(zoomFactor != 1.0) {
                int oldx = e.getX();
                int oldy = e.getY();
                int newx = (int) Math.round(e.getX() / zoomFactor);
                int newy = (int) Math.round(e.getY() / zoomFactor);
                e.translatePoint(newx - oldx, newy - oldy);
            }
        }

        //
        // KeyListener methods
        //
        public void keyPressed(KeyEvent e) {
            Tool tool = proj.getTool();
            if(tool != null) tool.keyPressed(Canvas.this, e);
        }
        public void keyReleased(KeyEvent e) {
            Tool tool = proj.getTool();
            if(tool != null) tool.keyReleased(Canvas.this, e);
        }
        public void keyTyped(KeyEvent e) {
            Tool tool = proj.getTool();
            if(tool != null) tool.keyTyped(Canvas.this, e);
        }

        //
        // PopupMenuListener mtehods
        //
        public void popupMenuCanceled(PopupMenuEvent e) {
            menu_on = false;
        }
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            menu_on = false;
        }
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

        //
        // ComponentListener methods
        //
        public void componentResized(ComponentEvent arg0) {
            computeSize();
        }

        public void componentMoved(ComponentEvent arg0) { }
        public void componentShown(ComponentEvent arg0) { }
        public void componentHidden(ComponentEvent arg0) { }

        public void propertyChange(PropertyChangeEvent event) {
            String prop = event.getPropertyName();
            if(prop.equals(LogisimPreferences.GATE_SHAPE)) {
                repaint();
            }
        }
    }

    private class MyProjectListener
            implements ProjectListener, LibraryListener, CircuitListener,
                AttributeListener, SimulatorListener, Selection.Listener {
        public void projectChanged(ProjectEvent event) {
            int act = event.getAction();
            if(act == ProjectEvent.ACTION_SET_CURRENT) {
                viewport.setErrorMessage(null);
                if(haloedComponent != null) {
                    proj.getFrame().viewComponentAttributes(null, null);
                }
            } else if(act == ProjectEvent.ACTION_SET_FILE) {
                LogisimFile old = (LogisimFile) event.getOldData();
                if(old != null) old.getOptions().getAttributeSet().removeAttributeListener(this);
                LogisimFile file = (LogisimFile) event.getData();
                if(file != null) {
                    AttributeSet attrs = file.getOptions().getAttributeSet();
                    attrs.addAttributeListener(this);
                    loadOptions(attrs);
                    mappings = file.getOptions().getMouseMappings();
                }
            } else if(act == ProjectEvent.ACTION_SET_TOOL) {
                viewport.setErrorMessage(null);
                
                Tool t = event.getTool();
                if(t == null)   setCursor(Cursor.getDefaultCursor());
                else            setCursor(t.getCursor());
            }

            if(act != ProjectEvent.ACTION_SELECTION) {
                completeAction();
            }
        }
        
        public void libraryChanged(LibraryEvent event) {
            if(event.getAction() == LibraryEvent.REMOVE_TOOL) {
                Object t = event.getData();
                if(t instanceof AddTool) t = ((AddTool) t).getFactory();
                if(t == proj.getCurrentCircuit() && t != null) {
                    proj.setCurrentCircuit(proj.getLogisimFile().getMainCircuit());
                }
            }
        }

        public void circuitChanged(CircuitEvent event) {
            int act = event.getAction();
            if(act == CircuitEvent.ACTION_REMOVE) {
                Component c = (Component) event.getData();
                if(c == haloedComponent) {
                    proj.getFrame().viewComponentAttributes(null, null);
                }
            } else if(act == CircuitEvent.ACTION_CLEAR) {
                if(haloedComponent != null) {
                    proj.getFrame().viewComponentAttributes(null, null);
                }
            } else if(act == CircuitEvent.ACTION_INVALIDATE) {
                completeAction();
            }
        }

        public void propagationCompleted(SimulatorEvent e) {
            /* This was a good idea for a while... but it leads to problems
             * when a repaint is done just before a user action takes place.
            // repaint - but only if it's been a while since the last one
            long now = System.currentTimeMillis();
            if(now > lastRepaint + repaintDuration) {
                lastRepaint = now; // (ensure that multiple requests aren't made
                repaintDuration = 15 + (int) (20 * Math.random());
                    // repaintDuration is for jittering the repaints to
                    // reduce aliasing effects
                repaint();
            }
            */
            repaint();
        }
        public void tickCompleted(SimulatorEvent e) {
            waitForRepaintDone();
        }
        public void simulatorStateChanged(SimulatorEvent e) { }

        public void attributeListChanged(AttributeEvent e) { }
        public void attributeValueChanged(AttributeEvent e) {
            Attribute attr = e.getAttribute();
            Object val = e.getValue();
            if(attr == Options.zoom_attr) {
                zoomFactor = ((Double) val).doubleValue();
                repaint();
            } else if(attr == Options.showgrid_attr) {
                showGrid = ((Boolean) val).booleanValue();
                repaint();
            } else if(attr == Options.preview_attr) {
                printerView = ((Boolean) val).booleanValue();
                repaint();
            } else if(attr == Options.showghosts_attr) {
                showGhosts = ((Boolean) val).booleanValue();
                repaint();
            } else if(attr == Options.showhalo_attr) {
                showHalo = ((Boolean) val).booleanValue();
                repaint();
            } else if(attr == Options.showtips_attr) {
                showTips = ((Boolean) val).booleanValue();
                setToolTipText(showTips ? "" : null);
            }
        }

        public void selectionChanged(Selection.Event event) {
            repaint();
        }
    }

    private class MyViewport extends JViewport {
        StringGetter errorMessage = null;
        String widthMessage = null;
        boolean isNorth = false;
        boolean isSouth = false;
        boolean isWest = false;
        boolean isEast = false;
        boolean isNortheast = false;
        boolean isNorthwest = false;
        boolean isSoutheast = false;
        boolean isSouthwest = false;

        MyViewport() { }

        void setErrorMessage(StringGetter msg) {
            if(errorMessage != msg) {
                errorMessage = msg;
                repaint();
            }
        }

        void setWidthMessage(String msg) {
            widthMessage = msg;
            isNorth = false;
            isSouth = false;
            isWest = false;
            isEast = false;
            isNortheast = false;
            isNorthwest = false;
            isSoutheast = false;
            isSouthwest = false;
        }
        void setNorth(boolean value) { isNorth = value; }
        void setSouth(boolean value) { isSouth = value; }
        void setEast(boolean value) { isEast = value; }
        void setWest(boolean value) { isWest = value; }
        void setNortheast(boolean value) { isNortheast = value; }
        void setNorthwest(boolean value) { isNorthwest = value; }
        void setSoutheast(boolean value) { isSoutheast = value; }
        void setSouthwest(boolean value) { isSouthwest = value; }

        public void paintChildren(Graphics g) {
            super.paintChildren(g);
            paintContents(g);
        }

        public Color getBackground() {
            return getView() == null ? super.getBackground() : getView().getBackground();
        }

        void paintContents(Graphics g) {
            /* TODO this is for the SimulatorPrototype class
            int speed = proj.getSimulator().getSimulationSpeed();
            String speedStr;
            if(speed >= 10000000) {
                speedStr = (speed / 1000000) + " MHz";
            } else if(speed >= 1000000) {
                speedStr = (speed / 100000) / 10.0 + " MHz";
            } else if(speed >= 10000) {
                speedStr = (speed / 1000) + " KHz";
            } else if(speed >= 10000) {
                speedStr = (speed / 100) / 10.0 + " KHz";
            } else {
                speedStr = speed + " Hz";
            }
            FontMetrics fm = g.getFontMetrics();
            g.drawString(speedStr, getWidth() - 10 - fm.stringWidth(speedStr),
                    getHeight() - 10);
            */
            
            if(errorMessage != null) {
                g.setColor(Color.RED);
                paintString(g, errorMessage.get());
                return;
            }
            
            /* TODO
            if(proj.getSimulator().isOscillating()) {
                g.setColor(Color.RED);
                paintString(g, Strings.get("canvasOscillationError"));
                return;
            } */
            
            if(proj.getSimulator().isExceptionEncountered()) {
                g.setColor(Color.RED);
                paintString(g, Strings.get("canvasExceptionError"));
                return;
            }

            computeViewportContents();
            Dimension sz = getSize();
            g.setColor(Value.WIDTH_ERROR_COLOR);

            if(widthMessage != null) {
                paintString(g, widthMessage);
            }

            GraphicsUtil.switchToWidth(g, 3);
            if(isNorth)     GraphicsUtil.drawArrow(g, sz.width / 2, 20,
                                sz.width / 2, 2, 10, 30);
            if(isSouth)     GraphicsUtil.drawArrow(g, sz.width / 2, sz.height - 20,
                                sz.width / 2, sz.height -  2, 10, 30);
            if(isEast)      GraphicsUtil.drawArrow(g, sz.width - 20, sz.height / 2,
                                sz.width -  2, sz.height / 2, 10, 30);
            if(isWest)      GraphicsUtil.drawArrow(g, 20, sz.height / 2,
                                 2, sz.height / 2, 10, 30);
            if(isNortheast) GraphicsUtil.drawArrow(g, sz.width - 14, 14,
                                sz.width -  2, 2, 10, 30);
            if(isNorthwest) GraphicsUtil.drawArrow(g, 14, 14,
                                2,  2, 10, 30);
            if(isSoutheast) GraphicsUtil.drawArrow(g, sz.width - 14, sz.height - 14,
                                sz.width -  2, sz.height -  2, 10, 30);
            if(isSouthwest) GraphicsUtil.drawArrow(g, 14, sz.height - 14,
                                2, sz.height -  2, 10, 30);

            GraphicsUtil.switchToWidth(g, 1);
            g.setColor(Color.BLACK);
        }
        
        private void paintString(Graphics g, String msg) {
            Font old = g.getFont();
            g.setFont(old.deriveFont(Font.BOLD).deriveFont(18.0f));
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            if(x < 0) x = 0;
            g.drawString(msg, x, getHeight() - 23);
            g.setFont(old);
            return;
        }
    }

    private Project proj;
    private Tool drag_tool;
    private MouseMappings mappings;
    private JScrollPane parent = null;
    private MyListener myListener = new MyListener();
    private MyViewport viewport = new MyViewport();
    private MyProjectListener myProjectListener = new MyProjectListener();

    private boolean showGrid = true;
    private boolean printerView = false;
    private boolean showGhosts = true;
    private boolean showHalo = true;
    private boolean showTips = true;
    private double zoomFactor = 1.0;
    private boolean paintDirty = false; // only for within paintComponent
    private boolean inPaint = false; // only for within paintComponent
    private Object repaintLock = new Object(); // for waitForRepaintDone

    private Component haloedComponent = null;
    private Circuit haloedCircuit = null;

    public Canvas(Project proj) {
        this.proj = proj;
        this.mappings = proj.getOptions().getMouseMappings();

        setBackground(Color.white);
        addMouseListener(myListener);
        addMouseMotionListener(myListener);
        addKeyListener(myListener);

        proj.addProjectListener(myProjectListener);
        proj.addLibraryListener(myProjectListener);
        proj.addCircuitListener(myProjectListener);
        proj.getSelection().addListener(myProjectListener);
        LocaleManager.addLocaleListener(this);

        AttributeSet options = proj.getOptions().getAttributeSet();
        options.addAttributeListener(myProjectListener);
        LogisimPreferences.addPropertyChangeListener(LogisimPreferences.GATE_SHAPE, myListener);
        loadOptions(options);
    }
    
    private void loadOptions(AttributeSet options) {
        printerView = ((Boolean) options.getValue(Options.preview_attr)).booleanValue();
        showGrid = ((Boolean) options.getValue(Options.showgrid_attr)).booleanValue();
        showGhosts = ((Boolean) options.getValue(Options.showghosts_attr)).booleanValue();
        showHalo = ((Boolean) options.getValue(Options.showhalo_attr)).booleanValue();
        showTips = ((Boolean) options.getValue(Options.showtips_attr)).booleanValue();
        zoomFactor = ((Double) options.getValue(Options.zoom_attr)).doubleValue();
        setToolTipText(showTips ? "" : null);

        proj.getSimulator().removeSimulatorListener(myProjectListener);
        proj.getSimulator().addSimulatorListener(myProjectListener);
    }

    public void repaint() {
        if(inPaint) paintDirty = true;
        else        super.repaint();
    }
    
    public void setErrorMessage(StringGetter message) {
        viewport.setErrorMessage(message);
    }

    //
    // access methods
    //
    public Circuit getCircuit() {
        return proj.getCurrentCircuit();
    }

    public CircuitState getCircuitState() {
        return proj.getCircuitState();
    }

    public Project getProject() {
        return proj;
    }

    public boolean getShowGhosts() { return showGhosts; }

    public boolean getShowHalo() { return showHalo; }

    //
    // graphics methods
    //
    Component getHaloedComponent() {
        return haloedComponent;
    }
    
    void setHaloedComponent(Circuit circ, Component comp) {
        if(comp == haloedComponent) return;
        Graphics g = getGraphics();
        exposeHaloedComponent(g);
        haloedCircuit = circ;
        haloedComponent = comp;
        exposeHaloedComponent(g);
    }

    private void exposeHaloedComponent(Graphics g) {
        Component c = haloedComponent;
        if(c == null) return;
        Bounds bds = c.getBounds(g).expand(7);
        int w = bds.getWidth();
        int h = bds.getHeight();
        double a = SQRT_2 * w;
        double b = SQRT_2 * h;
        repaint((int) Math.round(bds.getX() + w/2.0 - a/2.0),
            (int) Math.round(bds.getY() + h/2.0 - b/2.0),
            (int) Math.round(a), (int) Math.round(b));
    }

    public void showPopupMenu(JPopupMenu menu, int x, int y) {
        if(zoomFactor != 1.0) {
            x = (int) Math.round(x * zoomFactor);
            y = (int) Math.round(y * zoomFactor);
        }
        myListener.menu_on = true;
        menu.addPopupMenuListener(myListener);
        menu.show(this, x, y);
    }
    private void completeAction() {
        computeSize();
        // TODO for SimulatorPrototype: proj.getSimulator().releaseUserEvents();
        proj.getSimulator().requestPropagate();
        // repaint will occur after propagation completes
    }
    public void setScrollPane(JScrollPane value) {
        if(parent != null) {
            parent.removeComponentListener(myListener);
        }
        parent = value;
        if(parent != null) {
            parent.setViewport(viewport);
            viewport.setView(this);
            setOpaque(false);
            parent.addComponentListener(myListener);
        }
        computeSize();
    }
    public void computeSize() {
        Bounds bounds = proj.getCurrentCircuit().getBounds();
        int width = bounds.getX() + bounds.getWidth() + BOUNDS_BUFFER;
        int height = bounds.getY() + bounds.getHeight() + BOUNDS_BUFFER;
        if(zoomFactor != 1.0) {
            width = (int) Math.ceil(width * zoomFactor);
            height = (int) Math.ceil(height * zoomFactor);
        }
        if(parent != null) {
            Dimension min_size = new Dimension();
            parent.getViewport().getSize(min_size);
            if(min_size.width > width) width = min_size.width;
            if(min_size.height > height) height = min_size.height;
        }
        setPreferredSize(new Dimension(width, height));
        revalidate();
    }
    private void waitForRepaintDone() {
        synchronized(repaintLock) {
            try {
                while(inPaint) {
                    repaintLock.wait();
                }
            } catch(InterruptedException e) { }
        }
    }
    public void paintComponent(Graphics g) {
        inPaint = true;
        try {
            super.paintComponent(g);
            do {
                paintContents(g);
            } while(paintDirty);
            if(parent == null) viewport.paintContents(g);
        } finally {
            inPaint = false;
            synchronized(repaintLock) {
                repaintLock.notifyAll();
            }
        }
    }
    private void paintContents(Graphics g) {
        Rectangle clip = g.getClipBounds();
        Dimension size = this.getSize();
        if(clip == null || paintDirty) {
            clip = new Rectangle(0, 0, size.width, size.height);
            paintDirty = false;
        }
        g.setColor(Color.white);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        if(showGrid) {
            g.setColor(Color.gray);
            if(zoomFactor == 1.0) {
                int start_x = ((clip.x + 9) / 10) * 10;
                int start_y = ((clip.y + 9) / 10) * 10;
                for(int x = 0; x < clip.width; x += 10) {
                    for(int y = 0; y < clip.height; y += 10) {
                        g.drawLine(start_x + x, start_y + y,
                            start_x + x, start_y + y);
                    }
                }
            } else {
                double f = zoomFactor;
                double x0 = 10 * Math.ceil(clip.x / f / 10);
                double x1 = x0 + clip.width / f;
                double y0 = 10 * Math.ceil(clip.y / f / 10);
                double y1 = y0 + clip.height / f;
                for(double x = x0; x < x1; x += 10) {
                    for(double y = y0; y < y1; y += 10) {
                        int sx = (int) Math.round(f * x);
                        int sy = (int) Math.round(f * y);
                        g.drawLine(sx, sy, sx, sy);
                    }
                }
            }
        }
        g.setColor(Color.black);

        Graphics gScaled = g.create();
        if(zoomFactor != 1.0 && gScaled instanceof Graphics2D) {
            ((Graphics2D) gScaled).scale(zoomFactor, zoomFactor);
        }
        drawWithUserState(g, gScaled, printerView);
        drawWidthIncompatibilityData(g, gScaled);
        Circuit circ = proj.getCurrentCircuit();
        
        CircuitState circState = proj.getCircuitState();
        ComponentDrawContext ptContext = new ComponentDrawContext(this,
                circ, circState, g, gScaled);
        gScaled.setColor(Color.RED);
        circState.drawOscillatingPoints(ptContext);
        gScaled.setColor(Color.BLUE);
        proj.getSimulator().drawStepPoints(ptContext);
        gScaled.dispose();
    }
    private void drawWithUserState(Graphics base, Graphics g, boolean printView) {
        Circuit circ = proj.getCurrentCircuit();
        Selection sel = proj.getSelection();
        Collection hidden = sel.getHiddenComponents();

        // draw halo around component whose attributes we are viewing
        if(showHalo && haloedComponent != null && haloedCircuit == circ
                && !hidden.contains(haloedComponent)) {
            GraphicsUtil.switchToWidth(g, 3);
            g.setColor(AttributeTable.HALO_COLOR);
            Bounds bds = haloedComponent.getBounds(g).expand(5);
            int w = bds.getWidth();
            int h = bds.getHeight();
            double a = SQRT_2 * w;
            double b = SQRT_2 * h;
            g.drawOval((int) Math.round(bds.getX() + w/2.0 - a/2.0),
                (int) Math.round(bds.getY() + h/2.0 - b/2.0),
                (int) Math.round(a), (int) Math.round(b));
            GraphicsUtil.switchToWidth(g, 1);
            g.setColor(Color.BLACK);
        }

        // draw circuit and selection
        CircuitState circState = proj.getCircuitState();
        ComponentDrawContext context = new ComponentDrawContext(this,
                circ, circState, base, g, printView);
        circ.draw(context, hidden);
        sel.draw(context);

        // draw tool
        Tool tool = drag_tool != null ? drag_tool : proj.getTool();
        if(tool != null && !myListener.menu_on) {
            Graphics gCopy = g.create();
            context.setGraphics(gCopy);
            tool.draw(this, context);
            gCopy.dispose();
        }
    }
    private void drawWidthIncompatibilityData(Graphics base, Graphics g) {
        Set exceptions = proj.getCurrentCircuit().getWidthIncompatibilityData();
        if(exceptions == null || exceptions.size() == 0) return;

        g.setColor(Value.WIDTH_ERROR_COLOR);
        GraphicsUtil.switchToWidth(g, 2);
        FontMetrics fm = base.getFontMetrics(g.getFont());
        for(Iterator it = exceptions.iterator(); it.hasNext(); ) {
            WidthIncompatibilityData ex = (WidthIncompatibilityData) it.next();
            for(int i = 0; i < ex.size(); i++) {
                Location p = ex.getPoint(i);
                BitWidth w = ex.getBitWidth(i);

                // ensure it hasn't already been drawn
                boolean drawn = false;
                for(int j = 0; j < i; j++) {
                    if(ex.getPoint(j).equals(p)) { drawn = true; break; }
                }
                if(drawn) continue;

                // compute the caption combining all similar points
                String caption = "" + w.getWidth();
                for(int j = i + 1; j < ex.size(); j++) {
                    if(ex.getPoint(j).equals(p)) { caption += "/" + ex.getBitWidth(j); break; }
                }
                g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
                g.drawString(caption, p.getX() + 5, p.getY() + 2 + fm.getAscent());
            }
        }
        g.setColor(Color.BLACK);
        GraphicsUtil.switchToWidth(g, 1);
    }
    private void computeViewportContents() {
        Set exceptions = proj.getCurrentCircuit().getWidthIncompatibilityData();
        if(exceptions == null || exceptions.size() == 0) {
            viewport.setWidthMessage(null);
            return;
        }

        Rectangle viewableBase;
        Rectangle viewable;
        if(parent != null) {
            viewableBase = parent.getViewport().getViewRect();
        } else {
            Bounds bds = proj.getCurrentCircuit().getBounds();
            viewableBase = new Rectangle(0, 0, bds.getWidth(), bds.getHeight());
        }
        if(zoomFactor == 1.0) {
            viewable = viewableBase;
        } else {
            viewable = new Rectangle((int) (viewableBase.x / zoomFactor),
                    (int) (viewableBase.y / zoomFactor),
                    (int) (viewableBase.width / zoomFactor),
                    (int) (viewableBase.height / zoomFactor));
        }

        viewport.setWidthMessage(Strings.get("canvasWidthError")
                + (exceptions.size() == 1 ? "" : " (" + exceptions.size() + ")"));
        for(Iterator it = exceptions.iterator(); it.hasNext(); ) {
            WidthIncompatibilityData ex = (WidthIncompatibilityData) it.next();

            // See whether any of the points are on the canvas.
            boolean isWithin = false;
            for(int i = 0; i < ex.size(); i++) {
                Location p = ex.getPoint(i);
                int x = p.getX();
                int y = p.getY();
                if(x >= viewable.x && x < viewable.x + viewable.width
                        && y >= viewable.y && y < viewable.y + viewable.height) {
                    isWithin = true;
                    break;
                }
            }

            // If none are, insert an arrow.
            if(!isWithin) { 
                Location p = ex.getPoint(0);
                int x = p.getX();
                int y = p.getY();
                boolean isWest = x < viewable.x;
                boolean isEast = x >= viewable.x + viewable.width;
                boolean isNorth = y < viewable.y;
                boolean isSouth = y >= viewable.y + viewable.height;

                if(isNorth) {
                    if(isEast)      viewport.setNortheast(true);
                    else if(isWest) viewport.setNorthwest(true);
                    else            viewport.setNorth(true);
                } else if(isSouth) {
                    if(isEast)      viewport.setSoutheast(true);
                    else if(isWest) viewport.setSouthwest(true);
                    else            viewport.setSouth(true);
                } else {
                    if(isEast)      viewport.setEast(true);
                    else if(isWest) viewport.setWest(true);
                }
            }
        }
    }

    public void repaint(Rectangle r) {
        if(zoomFactor == 1.0) {
            super.repaint(r);
        } else {
            this.repaint(r.x, r.y, r.width, r.height);
        }
    }

    public void repaint(int x, int y, int width, int height) {
        if(zoomFactor != 1.0) {
            x = (int) Math.round(x * zoomFactor);
            y = (int) Math.round(y * zoomFactor);
            width = (int) Math.round(width * zoomFactor);
            height = (int) Math.round(height * zoomFactor);
        }
        super.repaint(x, y, width, height);
    }
    
    public String getToolTipText(MouseEvent event) {
        if(showTips) {
            Canvas.snapToGrid(event);
            Location loc = Location.create(event.getX(), event.getY());
            ComponentUserEvent e = null;
            for(Iterator it = getCircuit().getAllContaining(loc).iterator(); it.hasNext(); ) {
                Component comp = (Component) it.next();
                Object makerObj = comp.getFeature(ToolTipMaker.class);
                if(makerObj != null && makerObj instanceof ToolTipMaker) {
                    ToolTipMaker maker = (ToolTipMaker) makerObj;
                    if(e == null) {
                        e = new ComponentUserEvent(this, loc.getX(), loc.getY());
                    }
                    String ret = maker.getToolTip(e);
                    if(ret != null) return ret;
                }
            }
        }
        return null;
    }

    //
    // Scrollable methods
    //
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        int unit = getScrollableUnitIncrement(visibleRect, orientation,
            direction);
        if(direction == SwingConstants.VERTICAL) {
            return visibleRect.height / unit * unit;
        } else {
            return visibleRect.width / unit * unit;
        }
    }
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return (int) Math.round(10 * zoomFactor);
    }

    //
    // static methods
    //
    public static int snapXToGrid(int x) {
        if(x < 0) {
            return -((-x + 5) / 10) * 10;
        } else {
            return ((x + 5) / 10) * 10;
        }
    }
    public static int snapYToGrid(int y) {
        if(y < 0) {
            return -((-y + 5) / 10) * 10;
        } else {
            return ((y + 5) / 10) * 10;
        }
    }
    public static void snapToGrid(MouseEvent e) {
        int old_x = e.getX();
        int old_y = e.getY();
        int new_x = snapXToGrid(old_x);
        int new_y = snapYToGrid(old_y);
        e.translatePoint(new_x - old_x, new_y - old_y);
    }

    public void localeChanged() {
        repaint();
    }

}
