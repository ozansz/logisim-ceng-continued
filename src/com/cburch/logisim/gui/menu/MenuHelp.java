/* Copyright (c) 2006, 2009, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package com.cburch.logisim.gui.menu;

import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

class MenuHelp extends JMenu implements ActionListener {
    private static final String HELPSET_LOC = "doc/doc.hs";

    private LogisimMenuBar menubar;
    private JMenuItem tutorial = new JMenuItem();
    private JMenuItem guide = new JMenuItem();
    private JMenuItem library = new JMenuItem();
    private JMenuItem about = new JMenuItem();
    private HelpSet helpSet;
    private HelpBroker helpBroker;

    public MenuHelp(LogisimMenuBar menubar) {
        this.menubar = menubar;

        tutorial.addActionListener(this);
        guide.addActionListener(this);
        library.addActionListener(this);
        about.addActionListener(this);

        add(tutorial);
        add(guide);
        add(library);
        if(!MacCompatibility.isAboutAutomaticallyPresent()) {
            addSeparator();
            add(about);
        }
    }

    public void localeChanged() {
        this.setText(Strings.get("helpMenu"));
        tutorial.setText(Strings.get("helpTutorialItem"));
        guide.setText(Strings.get("helpGuideItem"));
        library.setText(Strings.get("helpLibraryItem"));
        about.setText(Strings.get("helpAboutItem"));
        if(helpBroker != null) {
            helpBroker.setLocale(Locale.getDefault());
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if(src == guide) {
            showHelp("guide");
        } else if(src == tutorial) {
            showHelp("tutorial");
        } else if(src == library) {
            showHelp("libs");
        } else if(src == about) {
            About.showAboutDialog(menubar.getParentWindow());
        }
    }

    private void showHelp(String target) {
        if(helpSet == null || helpBroker == null) {
            ClassLoader cl = MenuHelp.class.getClassLoader();
            try {
                URL hsURL = HelpSet.findHelpSet(cl, HELPSET_LOC);
                if(hsURL == null) {
                    disableHelp();
                    JOptionPane.showMessageDialog(menubar.getParentWindow(),
                            Strings.get("helpNotFoundError"));
                    return;
                }
                helpSet = new HelpSet(null, hsURL);
                helpBroker = helpSet.createHelpBroker();
            } catch (Exception e) {
                disableHelp();
                e.printStackTrace();
                JOptionPane.showMessageDialog(menubar.getParentWindow(),
                        Strings.get("helpUnavailableError"));
                return;
            }
        }
        try {
            helpBroker.setCurrentID(target);
            helpBroker.setViewDisplayed(true);
            helpBroker.setDisplayed(true);
        } catch(Exception e) {
            disableHelp();
            e.printStackTrace();
            JOptionPane.showMessageDialog(menubar.getParentWindow(),
                    Strings.get("helpDisplayError"));
        }
    }

    private void disableHelp() {
        guide.setEnabled(false);
        tutorial.setEnabled(false);
        library.setEnabled(false);
    }
}
