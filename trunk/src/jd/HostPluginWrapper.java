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

package jd;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.ImageIcon;

import jd.controlling.JDLogger;
import jd.gui.swing.components.JDLabelContainer;
import jd.nutils.JDFlags;
import jd.plugins.PluginForHost;

import org.jdownloader.DomainInfo;

public class HostPluginWrapper extends PluginWrapper implements JDLabelContainer {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    private static final ReentrantReadWriteLock       lock         = new ReentrantReadWriteLock();
    public static final ReadLock                      readLock     = lock.readLock();
    public static final WriteLock                     writeLock    = lock.writeLock();

    static {
        try {
            writeLock.lock();
            JDInit.loadPluginForHost();
        } catch (Throwable e) {
            JDLogger.exception(e);
        } finally {
            writeLock.unlock();
        }
    }

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        return HOST_WRAPPER;
    }

    public static boolean hasPlugin(final String data) {
        for (HostPluginWrapper w : getHostWrapper()) {
            if (w.canHandle(data)) return true;
        }
        return false;
    }

    public HostPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags, revision);
        try {
            writeLock.lock();
            for (HostPluginWrapper plugin : HOST_WRAPPER) {
                if (plugin.getID().equalsIgnoreCase(this.getID()) && plugin.getPattern().equals(this.getPattern())) {
                    if (JDFlags.hasNoFlags(flags, ALLOW_DUPLICATE)) {
                        logger.severe("Cannot add HostPlugin! HostPluginID " + getID() + " already exists!");
                        return;
                    }
                }
            }
            HOST_WRAPPER.add(this);
        } finally {
            writeLock.unlock();
        }
    }

    public HostPluginWrapper(final String host, final String simpleName, final String pattern, final int flags, final String revision) {
        this(host, "jd.plugins.hoster.", simpleName, pattern, flags, revision);
    }

    @Override
    public PluginForHost getPlugin() {
        return (PluginForHost) super.getPlugin();
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        return (PluginForHost) super.getNewPluginInstance();
    }

    public boolean isPremiumEnabled() {
        return this.isLoaded() && this.getPlugin().isPremiumEnabled();
    }

    @Override
    public String toString() {
        return getHost();
    }

    public ImageIcon getIcon() {
        return DomainInfo.getInstance(getHost()).getFavIcon();
    }

    public String getLabel() {
        return toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof HostPluginWrapper)) return false;
        return this.getID().equalsIgnoreCase(((HostPluginWrapper) obj).getID());
    }

    @Override
    public int hashCode() {
        final String id = this.getID();
        return id == null ? 0 : id.hashCode();
    }

}
