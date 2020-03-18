/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tr.edu.metu.ceng.ceng232.others;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 *
 * @author kerem
 */
public class ICDraw {
    static final int PIN_SPACING = 10;

    public static enum ICPinType {
        PIN,
        INVERSEPIN,
        CLOCK,
    }
    public static class ICPin {
        public ICPin(ICPinType t, String n) {
            type = t; name = n;
        }
        public ICPinType type;
        public String name;
    }
    public static class ICDescriptor {
        public ICDescriptor(ICPin[] w, ICPin[] e, ICPin[] n, ICPin[] s, String name) {
            pinsWest = w;
            pinsEast = e;
            pinsNorth = n;
            pinsSouth = s;
            this.name = name;
        }

        public ICPin[] pinsWest;
        public ICPin[] pinsEast;
        public ICPin[] pinsNorth;
        public ICPin[] pinsSouth;
        public String name;

        public int getN() { return pinsNorth.length; }
        public int getS() { return pinsSouth.length; }
        public int getW() { return pinsWest.length; }
        public int getE() { return pinsEast.length; }
        public int getPinW() { int a = getN() > getS() ? getN() : getS(); if (a < 5) return 5; else return a;}
        public int getPinH() { int a = getW() > getE() ? getW() : getE(); if (a < 5) return 5; else return a;}
    };


    public static void draw(ICDescriptor desc, ComponentDrawContext context, Color color, int x, int y, Direction facing) {
        Graphics g = context.getGraphics();
        g.setColor(color);

        Bounds bds = getBounds(desc, facing).translate(x, y);
        g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());

        
        GraphicsUtil.drawCenteredText(g, desc.name,
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight() / 2);

        drawPins(g, desc, x, y, facing, Direction.EAST);
        drawPins(g, desc, x, y, facing, Direction.WEST);
        drawPins(g, desc, x, y, facing, Direction.NORTH);
        drawPins(g, desc, x, y, facing, Direction.SOUTH);
    }

    public static void drawPins(Graphics g, ICDescriptor desc, int locX, int locY, Direction facing, Direction dir) {
        ICPin[] pins;

        if (dir == Direction.EAST) pins = desc.pinsEast;
        else if (dir == Direction.NORTH) pins = desc.pinsNorth;
        else if (dir == Direction.WEST) pins = desc.pinsWest;
        else pins = desc.pinsSouth;

        Direction pinFacing = getPinFacing(facing, dir);

        Font font = g.getFont();
        font = new Font(font.getName(), Font.PLAIN, font.getSize()/2);

        for (int i = 0; i < pins.length; i++) {
            ICPin pin = pins[i];
            Location loc = getPinLoc(desc, locX, locY, facing, dir, i);

            int halign = GraphicsUtil.H_CENTER;
            int valign = GraphicsUtil.V_CENTER;
            int yOffset = 0;
            int xOffset = 0;

            Graphics2D g2d = (g instanceof Graphics2D) ? (Graphics2D)g : null;
            AffineTransform originalTransform = null;
            if (g2d != null)
                originalTransform = g2d.getTransform();

            if (pinFacing == Direction.EAST) {
                halign = GraphicsUtil.H_RIGHT;
                xOffset = -g.getFontMetrics().charWidth(' ') / 2;
            }
            else if (pinFacing == Direction.WEST) {
                halign = GraphicsUtil.H_LEFT;
                xOffset = g.getFontMetrics().charWidth(' ') / 2;
            }
            else if (pinFacing == Direction.NORTH) {
                valign = GraphicsUtil.V_TOP;
                halign = GraphicsUtil.H_LEFT;
                yOffset = g.getFontMetrics().getAscent() / 4;
                if (g2d != null) {
                    AffineTransform northTransform = new AffineTransform();
                    northTransform.rotate(Math.PI/2, xOffset + loc.getX(), yOffset + loc.getY());
                    g2d.transform(northTransform);
                }
            }
            else {
                valign = GraphicsUtil.V_TOP;
                halign = GraphicsUtil.H_LEFT;
                yOffset = - g.getFontMetrics().getAscent()/4;
                if (g2d != null) {
                    AffineTransform southTransform = new AffineTransform();
                    southTransform.rotate(-Math.PI/2, xOffset + loc.getX(), yOffset + loc.getY());
                    g2d.transform(southTransform);
                }
            }

            String n = "";
            if (pin.type == ICPinType.CLOCK)
                n = "CLK";
            else if (pin.type == ICPinType.INVERSEPIN)
                n = "/";

            n = pin.name;

            GraphicsUtil.drawText(g, font, n, xOffset + loc.getX(), yOffset + loc.getY(), halign, valign);

            if (originalTransform != null)
                g2d.setTransform(originalTransform);
        }
    }

    public static Bounds getBounds(ICDescriptor desc, Direction facing) {
        int width = (desc.getPinW()+1) * PIN_SPACING;
        int height = (desc.getPinH()+1) * PIN_SPACING;

        return Bounds.create(0, 0, width, height).rotate(Direction.EAST, facing, 0, 0);
    }

    public static Location getPinLoc(ICDescriptor desc, int locX, int locY, Direction facing, Direction dir, int i) {
        int x=0,y=0;
        if (dir == Direction.WEST) {
            x = 0;
            y = PIN_SPACING+i*PIN_SPACING;
        }
        else if (dir == Direction.EAST) {
            x = PIN_SPACING+desc.getPinW()*PIN_SPACING;
            y = PIN_SPACING+i*PIN_SPACING;
        }
        else if (dir == Direction.NORTH) {
            x = PIN_SPACING+i*PIN_SPACING;
            y = 0;
        }
        else if (dir == Direction.SOUTH) {
            x = PIN_SPACING+i*PIN_SPACING;
            y = PIN_SPACING+desc.getPinH()*PIN_SPACING;
        }

        Bounds b = Bounds.create(x, y, 0, 0).rotate(Direction.EAST, facing, 0, 0);
        return Location.create(locX + b.getX(), locY + b.getY());
    }

    public static Direction getPinFacing(Direction icFacing, Direction pinDir) {
        if (icFacing == Direction.EAST)
            return pinDir;
        else if (icFacing == Direction.NORTH) {
            if (pinDir == Direction.EAST) return Direction.NORTH;
            else if (pinDir == Direction.NORTH) return Direction.WEST;
            else if (pinDir == Direction.WEST) return Direction.SOUTH;
            else return Direction.EAST;
        }
        else if (icFacing == Direction.WEST) {
            if (pinDir == Direction.EAST) return Direction.WEST;
            else if (pinDir == Direction.NORTH) return Direction.SOUTH;
            else if (pinDir == Direction.WEST) return Direction.EAST;
            else return Direction.NORTH;
        }
        else { // there's a bug with rotate, west/east is mirrored
            if (pinDir == Direction.EAST) return Direction.SOUTH;
            else if (pinDir == Direction.NORTH) return Direction.WEST;
            else if (pinDir == Direction.WEST) return Direction.NORTH;
            else return Direction.EAST;
        }
    }


    
}
