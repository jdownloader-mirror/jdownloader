//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.update;

public class PackageData extends Property {

    private static final long serialVersionUID = 313280647294844981L;
    private boolean preselected = false;
    private boolean selected;
    private int id;
    private int installedVersion = 0;
    private boolean updating = false;
    private int sortID = -1;
    private boolean downloaded;
    private boolean installed;;

    public void setPreselected(boolean b) {
        this.preselected = b;

    }

    public boolean isPreselected() {
        return preselected;
    }

    public void setSelected(boolean b) {
        this.selected = b;

    }

    public boolean isSelected() {
        return selected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInstalledVersion() {
        return this.installedVersion;
    }

    public boolean isUptodate() {
        return Integer.parseInt(this.getStringProperty("version")) == getInstalledVersion();
    }

    public void setInstalledVersion(int installed) {
        this.installedVersion = installed;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    public int getSortID() {
        return this.sortID;

    }

    public void setSortID(int sortID) {
        this.sortID = sortID;
    }

    public void setDownloaded(boolean b) {
        this.downloaded = b;

    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setInstalled(boolean b) {
        installed = b;

    }

    public boolean isInstalled() {
        return installed;
    }

}
