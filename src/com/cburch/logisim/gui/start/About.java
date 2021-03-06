/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.start;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.cburch.logisim.Main;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;

public class About {
    private static final int IMAGE_BORDER = 10;
    private static final int IMAGE_WIDTH = 380;
    private static final int IMAGE_HEIGHT = 284;
    
    private static class PanelThread extends Thread {
        private MyPanel panel;
        private int count = 1;
        private boolean running = true;
        
        PanelThread(MyPanel panel) {
            this.panel = panel;
        }
        
        public void run() {
            while(running) {
                panel.upper = (count == 2 || count == 3) ? Value.TRUE : Value.FALSE;
                panel.lower = (count == 1 || count == 2) ? Value.TRUE : Value.FALSE;
                count = (count + 1) % 4;
                panel.repaint();
                try {
                    Thread.sleep(500);
                } catch(InterruptedException ex) { }
            }
        }
    }

    private static class MyPanel extends JPanel implements AncestorListener {
        private final String LOGO_LOC = "com/cburch/logisim/resources/hendrix.png";
        private final Color fadeColor = new Color(255, 255, 255, 128);
        private final Color headerColor = new Color(143, 0, 0);
        private final Color authorColor = new Color(0, 0, 176);
        private final Color gateColor = Color.DARK_GRAY;
        private final Font headerFont = new Font("Monospaced", Font.BOLD, 72);
        private final Font versionFont = new Font("Serif", Font.PLAIN | Font.ITALIC, 32);
        private final Font copyrightFont = new Font("Serif", Font.ITALIC, 18);
        private final Font authorFont = new Font("Serif", Font.PLAIN, 24);
        private final Font urlFont = new Font("Serif", Font.ITALIC, 24);
        
        private Image logo = null;
        private Value upper = Value.FALSE;
        private Value lower = Value.TRUE;
        private PanelThread thread = null;

        public MyPanel() {
            setPreferredSize(new Dimension(IMAGE_WIDTH + 2 * IMAGE_BORDER,
                    IMAGE_HEIGHT + 2 * IMAGE_BORDER));
            setBackground(Color.WHITE);
            addAncestorListener(this);
            
            URL url = About.class.getClassLoader().getResource(LOGO_LOC);
            if(url != null) {
                logo = getToolkit().createImage(url);
            }
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            try {
                int x = IMAGE_BORDER;
                int y = IMAGE_BORDER;
                drawCircuit(g, x + 10, y + 55);
                g.setColor(fadeColor);
                g.fillRect(x, y, IMAGE_WIDTH, IMAGE_HEIGHT);
                drawText(g, x, y);
                if(logo != null) g.drawImage(logo, x + 330, y + 185, this);
            } catch(Throwable t) { }
        }
        
        private void drawCircuit(Graphics g, int x0, int y0) {
            if(g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(5.0f));
            }
            drawWires(g, x0, y0);
            g.setColor(gateColor);
            drawNot(g, x0, y0, 70, 10);
            drawNot(g, x0, y0, 70, 110);
            drawAnd(g, x0, y0, 130, 30);
            drawAnd(g, x0, y0, 130, 90);
            drawOr(g, x0, y0, 220, 60);
        }

        private void drawWires(Graphics g, int x0, int y0) {
            Value upperNot = upper.not();
            Value lowerNot = lower.not();
            Value upperAnd = upperNot.and(lower);
            Value lowerAnd = lowerNot.and(upper);
            Value out = upperAnd.or(lowerAnd);
            int x;
            int y;
            
            g.setColor(upper.getColor());
            x = toX(x0, 20);
            y = toY(y0, 10);
            g.fillOval(x - 7, y - 7, 14, 14);
            g.drawLine(toX(x0, 0), y, toX(x0, 40), y);
            g.drawLine(x, y, x, toY(y0, 70));
            y = toY(y0, 70);
            g.drawLine(x, y, toX(x0, 80), y);
            g.setColor(upperNot.getColor());
            y = toY(y0, 10);
            g.drawLine(toX(x0, 70), y, toX(x0, 80), y);
            
            g.setColor(lower.getColor());
            x = toX(x0, 30);
            y = toY(y0, 110);
            g.fillOval(x - 7, y - 7, 14, 14);
            g.drawLine(toX(x0, 0), y, toX(x0, 40), y);
            g.drawLine(x, y, x, toY(y0, 50));
            y = toY(y0, 50);
            g.drawLine(x, y, toX(x0, 80), y);
            g.setColor(lowerNot.getColor());
            y = toY(y0, 110);
            g.drawLine(toX(x0, 70), y, toX(x0, 80), y);
            
            g.setColor(upperAnd.getColor());
            x = toX(x0, 150);
            y = toY(y0, 30);
            g.drawLine(toX(x0, 130), y, x, y);
            g.drawLine(x, y, x, toY(y0, 45));
            y = toY(y0, 45);
            g.drawLine(x, y, toX(x0, 174), y);
            g.setColor(lowerAnd.getColor());
            y = toY(y0, 90);
            g.drawLine(toX(x0, 130), y, x, y);
            g.drawLine(x, y, x, toY(y0, 75));
            y = toY(y0, 75);
            g.drawLine(x, y, toX(x0, 174), y);
            
            g.setColor(out.getColor());
            y = toY(y0, 60);
            g.drawLine(toX(x0, 220), y, toX(x0, 240), y);
        }
        
        private void drawNot(Graphics g, int x0, int y0, int x, int y) {
            int[] xp = new int[4];
            int[] yp = new int[4];
            xp[0] = toX(x0, x - 10); yp[0] = toY(y0, y);
            xp[1] = toX(x0, x - 29); yp[1] = toY(y0, y - 7);
            xp[2] = xp[1]; yp[2] = toY(y0, y + 7);
            xp[3] = xp[0]; yp[3] = yp[0];
            g.drawPolyline(xp, yp, 4);
            int diam = toDim(10);
            g.drawOval(xp[0], yp[0] - diam / 2, diam, diam);
        }
        
        private void drawAnd(Graphics g, int x0, int y0, int x, int y) {
            int[] xp = new int[4];
            int[] yp = new int[4];
            xp[0] = toX(x0, x - 25); yp[0] = toY(y0, y - 25);
            xp[1] = toX(x0, x - 50); yp[1] = yp[0];
            xp[2] = xp[1]; yp[2] = toY(y0, y + 25);
            xp[3] = xp[0]; yp[3] = yp[2];
            int diam = toDim(50);
            g.drawArc(xp[1], yp[1], diam, diam, -90, 180);
            g.drawPolyline(xp, yp, 4);
        }
        
        private void drawOr(Graphics g, int x0, int y0, int x, int y) {
            int cx = toX(x0, x - 50);
            int cd = toDim(62);
            GraphicsUtil.drawCenteredArc(g, cx, toY(y0, y - 37), cd, -90, 53);
            GraphicsUtil.drawCenteredArc(g, cx, toY(y0, y + 37), cd, 90, -53);
            GraphicsUtil.drawCenteredArc(g, toX(x0, x - 93), toY(y0, y), toDim(50), -30, 60);
        }

        private static int toX(int x0, int offs) {
            return x0 + offs * 3 / 2;
        }
        
        private static int toY(int y0, int offs) {
            return y0 + offs * 3 / 2;
        }
        
        private static int toDim(int offs) {
            return offs * 3 / 2;
        }
            
        private void drawText(Graphics g, int x, int y) {
            FontMetrics fm;
            String str;
            
            g.setColor(headerColor);
            g.setFont(headerFont);
            g.drawString("Logisim", x, y + 45);
            g.setFont(copyrightFont); fm = g.getFontMetrics();
            str = "\u00a9 2009";
            g.drawString(str, x + IMAGE_WIDTH - fm.stringWidth(str), y + 16);
            g.setFont(versionFont); fm = g.getFontMetrics();
            str = "Version " + Main.VERSION_NAME;
            g.drawString(str, x + IMAGE_WIDTH - fm.stringWidth(str), y + 75);
            
            g.setColor(authorColor);
            g.setFont(authorFont); fm = g.getFontMetrics();
            str = "Carl Burch";
            g.drawString(str, x + (IMAGE_WIDTH - fm.stringWidth(str)) / 2, y + 224);
            str = "Hendrix College";
            g.drawString(str, x + (IMAGE_WIDTH - fm.stringWidth(str)) / 2, y + 251);
            g.setFont(urlFont); fm = g.getFontMetrics();
            str = "www.cburch.com/logisim/";
            g.drawString(str, x + (IMAGE_WIDTH - fm.stringWidth(str)) / 2, y + 277);
        }

        public void ancestorAdded(AncestorEvent arg0) {
            if(thread == null) {
                thread = new PanelThread(this);
                thread.start();
            }
        }

        public void ancestorRemoved(AncestorEvent arg0) {
            if(thread != null) {
                thread.running = false;
            }
        }

        public void ancestorMoved(AncestorEvent arg0) { }
    }

    private About() { }

    public static JPanel getImagePanel() {
        return new MyPanel();
    }

    public static void showAboutDialog(JFrame owner) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(getImagePanel());
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        JOptionPane.showMessageDialog(owner, panel,
                "Logisim " + Main.VERSION_NAME, JOptionPane.PLAIN_MESSAGE);
    }
}

