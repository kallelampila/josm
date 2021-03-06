// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Exit the application. May ask for permission first (if something has changed).
 *
 * @author imi
 */
public class ExitAction extends JosmAction {
    /**
     * Construct the action with "Exit" as label
     */
    public ExitAction() {
        super(tr("Exit"), "exit", tr("Exit the application."),
                Shortcut.registerShortcut("system:menuexit", tr("Exit"), KeyEvent.VK_Q, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Exit"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Main.exitJosm(true, 0);
    }
}
