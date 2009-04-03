//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple;

import java.awt.EventQueue;
import java.awt.LayoutManager;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public abstract class DownloadLinksView extends JTabbedPanel implements ControlListener {

    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public static final int REFRESH_SPECIFIED_LINKS = 2;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Der Logger für Meldungen
     */
    protected Logger logger = JDUtilities.getLogger();
    /**
     * contains all packages we have downloadlinks for
     */
    private Vector<FilePackage> packages = new Vector<FilePackage>();

    protected DownloadLinksView(SimpleGUI parent, LayoutManager layout) {
        super(layout);
        JDUtilities.getController().addControlListener(this);
    }

    public void controlEvent(final ControlEvent event) {
        if (event == null) {
            logger.warning("event == null");
            return;
        }
        switch (event.getID()) {
        case ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED:
            fireTableChanged(REFRESH_SPECIFIED_LINKS, event.getParameter());
            break;
        case ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED:
            fireTableChanged(REFRESH_ALL_DATA_CHANGED, null);
            break;
        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            if (event.getSource().getClass() == JDController.class) {
                DownloadLinksView.this.setPackages(JDUtilities.getController().getPackages());
            }
            fireTableChanged(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
            break;
        }
    }

    abstract public void fireTableChanged(int id, Object object);

    public Vector<FilePackage> getPackages() {
        return packages;
    }

    private void setPackages(Vector<FilePackage> packages) {
        this.packages = packages;
    }

}
