/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.file;


import java.util.Iterator;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.IntegerFactory;

public class Options {
    public static final Attribute showgrid_attr
        = Attributes.forBoolean("showgrid", Strings.getter("showGridOption"));
    public static final Attribute preview_attr
        = Attributes.forBoolean("preview", Strings.getter("printPreviewOption"));
    public static final Attribute showghosts_attr
        = Attributes.forBoolean("showghosts", Strings.getter("showGhostsOption"));
    public static final Attribute showhalo_attr
        = Attributes.forBoolean("showhalo", Strings.getter("showHaloOption"));
    public static final Attribute showtips_attr
        = Attributes.forBoolean("showhalo", Strings.getter("showHaloOption"));
    public static final Attribute zoom_attr
        = Attributes.forDouble("zoom", Strings.getter("zoomFactorOption"));
    public static final Attribute sim_limit_attr
        = Attributes.forInteger("simlimit", Strings.getter("simLimitOption"));
    public static final Attribute sim_rand_attr
        = Attributes.forInteger("simrand", Strings.getter("simRandomOption"));
    public static final Attribute ATTR_RADIX_1 = RadixOption.ATTRIBUTE;
    public static final Attribute ATTR_RADIX_2
        = Attributes.forOption("radix2", Strings.getter("radix2Option"), RadixOption.OPTIONS);
    
    public static final Object TOOLBAR_HIDDEN = new AttributeOption("hidden", "hidden", Strings.getter("toolbarHidden"));
    public static final Object TOOLBAR_DOWN_MIDDLE = new AttributeOption("middle", "middle", Strings.getter("toolbarDownMiddle"));
    public static final Object[] OPTIONS_TOOLBAR_LOC =
        { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            TOOLBAR_DOWN_MIDDLE, TOOLBAR_HIDDEN };
    public static final Attribute ATTR_TOOLBAR_LOC
        = Attributes.forOption("toolbarloc", Strings.getter("toolbarlocOption"),
                OPTIONS_TOOLBAR_LOC);
    
    public static final Integer sim_rand_dflt = IntegerFactory.create(32);

    private static final Attribute[] ATTRIBUTES = {
        showgrid_attr, preview_attr, showghosts_attr, showhalo_attr,
            showtips_attr, zoom_attr, sim_limit_attr, sim_rand_attr,
            ATTR_RADIX_1, ATTR_RADIX_2, ATTR_TOOLBAR_LOC,
    };
    private static final Object[] DEFAULTS = {
        Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE,
            Boolean.TRUE, new Double(1.0), IntegerFactory.create(1000),
            IntegerFactory.ZERO,
            RadixOption.RADIX_2, RadixOption.RADIX_10_SIGNED, Direction.NORTH,
    };
    
    private AttributeSet attrs = AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    private MouseMappings mmappings = new MouseMappings();
    private ToolbarData toolbar = new ToolbarData();

    public Options() { }

    public AttributeSet getAttributeSet() {
        return attrs;
    }

    public MouseMappings getMouseMappings() {
        return mmappings;
    }

    public ToolbarData getToolbarData() {
        return toolbar;
    }

    public void copyFrom(Options other, LogisimFile dest) {
        Iterator it = other.attrs.getAttributes().iterator();
        while(it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            this.attrs.setValue(attr, other.attrs.getValue(attr));
        }

        this.toolbar.copyFrom(other.toolbar, dest);
        this.mmappings.copyFrom(other.mmappings, dest);
    }
}
