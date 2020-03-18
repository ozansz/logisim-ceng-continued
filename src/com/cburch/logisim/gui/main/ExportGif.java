/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GifEncoder;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

class ExportGif {
    private ExportGif() { }

    static void doExport(Project proj) {
        // First display circuit/parameter selection dialog
        Frame frame = proj.getFrame();
        CircuitJList list = new CircuitJList(proj, true);
        if(list.getModel().getSize() == 0) {
            JOptionPane.showMessageDialog(proj.getFrame(),
                    Strings.get("exportEmptyCircuitsMessage"),
                    Strings.get("exportEmptyCircuitsTitle"),
                    JOptionPane.YES_NO_OPTION);
            return;
        }
        GifPanel gifPanel = new GifPanel(list);
        int action = JOptionPane.showConfirmDialog(frame,
                gifPanel, Strings.get("exportGifSelect"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if(action != JOptionPane.OK_OPTION) return;
        List circuits = list.getSelectedCircuits();
        double scale = gifPanel.getScale();
        boolean printerView = gifPanel.getPrinterView();
        if(circuits.isEmpty()) return;
        
        // Then display file chooser
        Loader loader = proj.getLogisimFile().getLoader();
        JFileChooser chooser = loader.createChooser();
        if(circuits.size() > 1) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(Strings.get("exportGifDirectorySelect"));
        } else {
            chooser.setFileFilter(new GifFilter());
            chooser.setDialogTitle(Strings.get("exportGifFileSelect"));
        }
        int returnVal = chooser.showDialog(frame, Strings.get("exportGifButton"));
        if(returnVal != JFileChooser.APPROVE_OPTION) return;

        // Determine whether destination is valid
        File dest = chooser.getSelectedFile();
        chooser.setCurrentDirectory(dest.isDirectory() ? dest : dest.getParentFile());
        if(dest.exists()) {
            if(!dest.isDirectory()) {
                int confirm = JOptionPane.showConfirmDialog(proj.getFrame(),
                    Strings.get("confirmOverwriteMessage"),
                    Strings.get("confirmOverwriteTitle"),
                    JOptionPane.YES_NO_OPTION);
                if(confirm != JOptionPane.YES_OPTION) return;
            }
        } else {
            if(circuits.size() > 1) {
                boolean created = dest.mkdir();
                if(!created) {
                    JOptionPane.showMessageDialog(proj.getFrame(),
                            Strings.get("exportNewDirectoryErrorMessage"),
                            Strings.get("exportNewDirectoryErrorTitle"),
                            JOptionPane.YES_NO_OPTION);
                    return;
                }
            } else {
                String name = dest.getName();
                if(name.indexOf('.') < 0) {
                    dest = new File(dest.getParentFile(), name + EXTENSION);
                }
            }
        }

        // Create the progress monitor
        ProgressMonitor monitor = new ProgressMonitor(frame,
                Strings.get("exportGifProgress"),
                null,
                0, 10000);
        monitor.setMillisToDecideToPopup(100);
        monitor.setMillisToPopup(200);
        monitor.setProgress(0);

        // And start a thread to actually perform the operation
        // (This is run in a thread so that Swing will update the
        // monitor.)
        new ExportThread(frame, frame.getCanvas(), dest,
                circuits, scale, printerView, monitor).start();

    }
    
    private static final int SLIDER_DIVISIONS = 6;

    private static final String EXTENSION = ".gif";
    
    private static final int BORDER_SIZE = 5;

    private static class GifPanel extends JPanel implements ChangeListener {
        JSlider slider;
        JLabel curScale;
        JCheckBox printerView;
        GridBagLayout gridbag;
        GridBagConstraints gbc;
        Dimension curScaleDim;

        GifPanel(JList list) {
            // set up components
            slider = new JSlider(JSlider.HORIZONTAL,
                    -3 * SLIDER_DIVISIONS, 3 * SLIDER_DIVISIONS, 0);
            slider.setMajorTickSpacing(10);
            slider.addChangeListener(this);
            curScale = new JLabel("222%");
            curScale.setHorizontalAlignment(SwingConstants.RIGHT);
            curScale.setVerticalAlignment(SwingConstants.CENTER);
            curScaleDim = new Dimension(curScale.getPreferredSize());
            curScaleDim.height = Math.max(curScaleDim.height,
                    slider.getPreferredSize().height);
            stateChanged(null);

            printerView = new JCheckBox();
            printerView.setSelected(true);

            // set up panel
            gridbag = new GridBagLayout();
            gbc = new GridBagConstraints();
            setLayout(gridbag);

            // now add components into panel
            gbc.gridy = 0;
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(5, 0, 5, 0);
            gbc.fill = GridBagConstraints.NONE;
            addGb(new JLabel(Strings.get("labelCircuits") + " "));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addGb(new JScrollPane(list));
            gbc.fill = GridBagConstraints.NONE;
            
            gbc.gridy++;
            addGb(new JLabel(Strings.get("labelScale") + " "));
            addGb(slider);
            addGb(curScale);
            
            gbc.gridy++;
            addGb(new JLabel(Strings.get("labelPrinterView") + " "));
            addGb(printerView);
        }
        
        private void addGb(JComponent comp) {
            gridbag.setConstraints(comp, gbc);
            add(comp);
        }

        double getScale() {
            return Math.pow(2.0, (double) slider.getValue() / SLIDER_DIVISIONS);
        }
        
        boolean getPrinterView() { return printerView.isSelected(); }

        public void stateChanged(ChangeEvent e) {
            double scale = getScale();
            curScale.setText((int) Math.round(100.0 * scale) + "%");
            if(curScaleDim != null) curScale.setPreferredSize(curScaleDim);
        }
    }
    
    private static class GifFilter extends FileFilter {
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(EXTENSION);
        }

        public String getDescription() {
            return Strings.get("exportGifFilter");
        }
    }

    private static class ExportThread extends Thread {
        Frame frame;
        Canvas canvas;
        File dest;
        List circuits;
        double scale;
        boolean printerView;
        ProgressMonitor monitor;

        ExportThread(Frame frame, Canvas canvas, File dest,
                List circuits, double scale, boolean printerView,
                ProgressMonitor monitor) {
            this.frame = frame;
            this.canvas = canvas;
            this.dest = dest;
            this.circuits = circuits;
            this.scale = scale;
            this.printerView = printerView;
            this.monitor = monitor;
        }

        public void run() {
            for(Iterator it = circuits.iterator(); it.hasNext(); ) {
                export((Circuit) it.next());
            }
        }
        
        private void export(Circuit circuit) {
            Bounds bds = circuit.getBounds(canvas.getGraphics())
                .expand(BORDER_SIZE);
            int width = (int) Math.round(bds.getWidth() * scale);
            int height = (int) Math.round(bds.getHeight() * scale);
            Image img = canvas.createImage(width, height);
            if(img == null) {
                JOptionPane.showMessageDialog(frame,
                        Strings.get("couldNotCreateGifImage"));
                monitor.close();
                return;
            }
            Graphics base = img.getGraphics();
            Graphics g = base.create();
            if(g instanceof Graphics2D) {
                ((Graphics2D) g).scale(scale, scale);
                ((Graphics2D) g).translate(-bds.getX(), -bds.getY());
            } else {
                bds = bds.expand(-BORDER_SIZE);
                img = canvas.createImage(bds.getX() + bds.getWidth() + BORDER_SIZE, bds.getY() + bds.getHeight() + BORDER_SIZE);
                if(img == null) {
                    JOptionPane.showMessageDialog(frame,
                            Strings.get("couldNotCreateGifImage"));
                    monitor.close();
                    return;
                }
            }

            CircuitState circuitState = canvas.getProject().getCircuitState(circuit);
            ComponentDrawContext context = new ComponentDrawContext(canvas,
                    circuit, circuitState, base, g, printerView);
            circuit.draw(context, null);

            File where;
            if(dest.isDirectory()) {
                where = new File(dest, circuit.getName() + EXTENSION);
            } else {
                where = dest;
            }
            try {
                GifEncoder.toFile(img, where, monitor);
            } catch(Exception e) {
                JOptionPane.showMessageDialog(frame,
                        Strings.get("couldNotCreateGifFile"));
                monitor.close();
                return;
            }
            g.dispose();
            monitor.close();
        }
    }
}
