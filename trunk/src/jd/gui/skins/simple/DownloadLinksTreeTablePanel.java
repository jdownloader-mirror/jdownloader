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

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXCollapsiblePane;

import jd.event.ControlEvent;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2FilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2FilePackageInfo;
import jd.gui.skins.simple.components.treetable.DownloadTreeTable;
import jd.gui.skins.simple.components.treetable.DownloadTreeTableModel;
import jd.gui.skins.simple.info.PackageInfo;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class DownloadLinksTreeTablePanel extends DownloadLinksView {

    private static final long serialVersionUID = 1L;

    private DownloadTreeTable internalTreeTable;

    private PackageInfo packageInfo;

    private JXCollapsiblePane collapsepane;

    public DownloadLinksTreeTablePanel(SimpleGUI parent) {
        super(parent, new BorderLayout());
        internalTreeTable = new DownloadTreeTable(new DownloadTreeTableModel(this), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        packageInfo = new PackageInfo();
        collapsepane = new JXCollapsiblePane();
        collapsepane.setCollapsed(true);

        collapsepane.add(packageInfo);
        this.add(collapsepane, BorderLayout.SOUTH);
        this.add(scrollPane);
    }

    public void showFilePackageInfo(FilePackage fp) {
        packageInfo.setPackage(fp);
        collapsepane.setCollapsed(false);
    }

    public void hideFilePackageInfo() {
        collapsepane.setCollapsed(true);
    }

    @Override
    public synchronized void fireTableChanged(int id, Object param) {
        internalTreeTable.fireTableChanged(id, param);
    }

    public void moveSelectedItems(int id) {
        internalTreeTable.moveSelectedItems(id);
    }

    public void removeSelectedLinks() {
        Vector<DownloadLink> links = internalTreeTable.getSelectedDownloadLinks();
        Vector<FilePackage> fps = internalTreeTable.getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            links.addAll(filePackage.getDownloadLinks());
        }
        JDUtilities.getController().removeDownloadLinks(links);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
    }

    public long countSelectedLinks() {
        Vector<DownloadLink> links = internalTreeTable.getSelectedDownloadLinks();
        Vector<FilePackage> fps = internalTreeTable.getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            links.addAll(filePackage.getDownloadLinks());
        }
        return links.size();
    }

    public long countSelectedPackages() {
        Vector<FilePackage> fps = internalTreeTable.getSelectedFilePackages();
        return fps.size();
    }

    @Override
    public void onDisplay() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onHide() {
        // TODO Auto-generated method stub

    }
}
