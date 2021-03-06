// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;

public class UnselectAllAction extends JosmAction {

    public UnselectAllAction() {
        super(tr("Unselect All"), "unselectall", tr("Unselect all objects."),
            Shortcut.registerShortcut("edit:unselectall", tr("Edit: {0}",
            tr("Unselect All")), KeyEvent.VK_ESCAPE, Shortcut.DIRECT), true);

        putValue("help", ht("/Action/UnselectAll"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        getCurrentDataSet().setSelected();
    }
    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }
}
