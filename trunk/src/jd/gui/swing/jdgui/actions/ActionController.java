//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.actions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlIDListener;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.views.downloadview.DownloadLinksPanel;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.nutils.JDFlags;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

/**
 * Class to control toolbar actions
 * 
 * @author Coalado
 */
public class ActionController {
    public static final String JDL_PREFIX = "jd.gui.swing.jdgui.actions.ActionController.";
    private static ArrayList<ToolBarAction> TOOLBAR_ACTION_LIST = new ArrayList<ToolBarAction>();

    public static void register(ToolBarAction action) {
        synchronized (TOOLBAR_ACTION_LIST) {
            if (TOOLBAR_ACTION_LIST.contains(action)) return;
            for (ToolBarAction act : TOOLBAR_ACTION_LIST) {
                if (act.getID().equalsIgnoreCase(action.getID())) return;
            }
            TOOLBAR_ACTION_LIST.add(action);
        }
    }
    public static void unRegister(ToolBarAction action) {
        synchronized (TOOLBAR_ACTION_LIST) {
            if (!TOOLBAR_ACTION_LIST.contains(action)) return;
        
            TOOLBAR_ACTION_LIST.remove(action);
        }
    }
    /**
     * Defines all possible actions
     */
    public static void initActions() {

        new ToolBarAction("toolbar.separator", "-") {
            private static final long serialVersionUID = -4628452328096482738L;

            @Override
            public void onAction(ActionEvent e) {
            }

            @Override
            public void initDefaults() {
                setType(ToolBarAction.Types.SEPARATOR);
            }

            @Override
            public void init() {
            }

        };

        new ThreadedAction("toolbar.control.start", "gui.images.next") {
            private static final long serialVersionUID = 1683169623090750199L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                switch (event.getID()) {
                                case ControlEvent.CONTROL_DOWNLOAD_START:
                                    setEnabled(false);
                                    break;
                                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                                    setEnabled(true);
                                    break;
                                }
                                return null;
                            }

                        }.start();

                    }
                });
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                    synchronized (LinkGrabberController.ControllerLock) {
                        synchronized (LinkGrabberPanel.getLinkGrabber()) {
                            for (LinkGrabberFilePackage fp : fps) {
                                LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, -1);
                            }
                        }
                    }
                    fps = null;
                    UserIF.getInstance().requestPanel(UserIF.Panels.DOWNLOADLIST, null);
                }
                DownloadWatchDog.getInstance().startDownloads();
            }

        };
        new ToolBarAction("toolbar.control.pause", "gui.images.break") {
            private static final long serialVersionUID = 7153300370492212502L;

            @Override
            public void onAction(ActionEvent e) {
                boolean b = ActionController.getToolBarAction("toolbar.control.pause").isSelected();
                DownloadWatchDog.getInstance().pauseDownloads(b);
            }

            @Override
            public void initDefaults() {
                this.setEnabled(false);
                setType(ToolBarAction.Types.TOGGLE);
                setToolTipText(JDL.LF("gui.menu.action.break2.desc", "Pause downloads. Limits global speed to %s kb/s", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + ""));
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                switch (event.getID()) {
                                case ControlEvent.CONTROL_DOWNLOAD_START:
                                    setEnabled(true);
                                    setSelected(false);
                                    break;
                                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                                    setEnabled(false);
                                    setSelected(false);
                                    break;
                                }
                                return null;
                            }
                        }.start();
                    }
                });
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        setToolTipText(JDL.LF("gui.menu.action.break2.desc", "Pause downloads. Limits global speed to %s kb/s", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_PAUSE_SPEED, 10) + ""));
                    }
                });
            }

        };

        new ThreadedAction("toolbar.control.stop", "gui.images.stop") {
            private static final long serialVersionUID = 1409143759105090751L;

            @Override
            public void initDefaults() {
                this.setEnabled(false);
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;
                JDUtilities.getController().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, ControlEvent.CONTROL_DOWNLOAD_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {

                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                switch (event.getID()) {
                                case ControlEvent.CONTROL_DOWNLOAD_START:
                                    setEnabled(true);
                                    break;
                                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                                    setEnabled(false);
                                    break;
                                }
                                return null;
                            }
                        }.start();
                    }
                });
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                final ProgressController pc = new ProgressController(JDL.L("gui.downloadstop", "Stopping current downloads..."), null);
                Thread test = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            pc.increase(1);
                            try {
                                sleep(1000);
                            } catch (InterruptedException e) {
                                break;
                            }
                            if (DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.NOT_RUNNING) break;
                        }
                    }
                };
                test.start();
                DownloadWatchDog.getInstance().stopDownloads();
                test.interrupt();
                pc.doFinalize();
            }

        };

        new ThreadedAction("toolbar.interaction.reconnect", "gui.images.reconnect") {
            private static final long serialVersionUID = -1295253607970814759L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;
                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_RECONNECT_OKAY) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (!source.getBooleanProperty(key, true)) {
                            setIcon("gui.images.reconnect_warning");
                            setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                            getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnect.notconfigured.tooltip", "Your Reconnect is not configured correct"));
                        } else {
                            setToolTipText(JDL.L("gui.menu.action.reconnectman.desc", "Manual reconnect. Get a new IP by resetting your internet connection"));
                            setIcon("gui.images.reconnect");
                            getToolBarAction("toolbar.quickconfig.reconnecttoggle").setToolTipText(JDL.L("gui.menu.action.reconnectauto.desc", "Auto reconnect. Get a new IP by resetting your internet connection"));
                        }
                    }
                });
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {

                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?")), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                            new Thread(new Runnable() {
                                public void run() {
                                    Reconnecter.doManualReconnect();
                                }
                            }).start();
                        }
                        return null;
                    }
                }.start();
            }

        };

        new ThreadedAction("toolbar.interaction.update", "gui.images.update") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                WebUpdate.doUpdateCheck(true);
            }

        };

        new ToolBarAction("toolbar.quickconfig.clipboardoberserver", "gui.images.clipboard_enabled") {
            private static final long serialVersionUID = -6442494647304101403L;

            @Override
            public void onAction(ActionEvent e) {
                ClipboardHandler.getClipboard().setEnabled(this.isSelected());
            }

            @Override
            public void initDefaults() {
                this.setEnabled(true);
                setType(ToolBarAction.Types.TOGGLE);
                boolean b = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
                setSelected(b);
                setIcon(b ? "gui.images.clipboard_enabled" : "gui.images.clipboard_disabled");
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;

                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.clipboard_enabled" : "gui.images.clipboard_disabled");
                        }
                    }
                });

                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (source.getBooleanProperty(key, true)) {
                            setSelected(true);
                        } else {
                            setSelected(false);
                        }
                    }
                });
            }
        };

        new ToolBarAction("toolbar.quickconfig.reconnecttoggle", "gui.images.reconnect_disabled") {
            private static final long serialVersionUID = -2942320816429047941L;

            @Override
            public void onAction(ActionEvent e) {
                Reconnecter.toggleReconnect();
            }

            @Override
            public void initDefaults() {
                this.setEnabled(true);
                setType(ToolBarAction.Types.TOGGLE);
                setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
                setIcon(isSelected() ? "gui.images.reconnect_enabled" : "gui.images.reconnect_disabled");
            }

            @Override
            public void init() {
                if (inited) return;
                this.inited = true;

                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.reconnect_enabled" : "gui.images.reconnect_disabled");
                        }
                    }
                });

                JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_ALLOW_RECONNECT) {
                    @Override
                    public void onPropertyChanged(Property source, final String key) {
                        if (source.getBooleanProperty(key, true)) {
                            setSelected(true);
                        } else {
                            setSelected(false);
                        }
                    }
                });
            }

        };

        new ToolBarAction("action.opendlfolder", "gui.images.package_opened") {
            private static final long serialVersionUID = -60944746807335951L;

            @Override
            public void onAction(ActionEvent e) {
                String dlDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
                if (dlDir == null) return;
                JDUtilities.openExplorer(new File(dlDir));
            }

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

        };

        new ThreadedAction("toolbar.control.stopmark", "gui.images.stopmark.disabled") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L(JDL_PREFIX + "toolbar.control.stopmark.tooltip", "Stop after current Downloads"));
                this.setEnabled(false);
                setType(ToolBarAction.Types.TOGGLE);
                this.setSelected(false);
                this.setIcon("gui.images.stopmark.disabled");
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            setIcon((Boolean) evt.getNewValue() ? "gui.images.stopmark.enabled" : "gui.images.stopmark.disabled");
                        }
                    }
                });
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (DownloadWatchDog.getInstance().isStopMarkSet()) {
                    DownloadWatchDog.getInstance().setStopMark(null);
                } else if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
                    Object obj = DownloadWatchDog.getInstance().getRunningDownloads().get(0);
                    DownloadWatchDog.getInstance().setStopMark(obj);
                } else {
                    setSelected(false);
                }
                if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING && !DownloadWatchDog.getInstance().isStopMarkSet()) setEnabled(false);
            }

        };

        new ThreadedAction("action.downloadview.movetobottom", "gui.images.go_bottom") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_BOTTOM);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_BOTTOM);
                }
            }
        };
        new ThreadedAction("action.downloadview.movetotop", "gui.images.go_top") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_TOP);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_TOP);
                }
            }
        };

        new ThreadedAction("action.downloadview.moveup", "gui.images.up") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_UP);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_UP);
                }
            }
        };
        new ThreadedAction("action.downloadview.movedown", "gui.images.down") {
            private static final long serialVersionUID = 6181260839200699153L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    LinkGrabberPanel.getLinkGrabber().move(LinkGrabberController.MOVE_DOWN);
                    LinkGrabberController.getInstance().throwRefresh();
                } else if (!DownloadLinksPanel.getDownloadLinksPanel().isNotVisible()) {
                    DownloadLinksPanel.getDownloadLinksPanel().move(DownloadController.MOVE_DOWN);
                }
            }
        };
    }

    /**
     * Returns the action for the givven key
     * 
     * @param keyid
     * @return
     */
    public static ToolBarAction getToolBarAction(String keyid) {
        synchronized (TOOLBAR_ACTION_LIST) {
            for (ToolBarAction a : TOOLBAR_ACTION_LIST) {
                if (a.getID().equals(keyid)) return a;
            }
            return null;
        }
    }

    /**
     * returns a fresh copy of all toolbaractions
     * 
     * @return
     */
    public static ArrayList<ToolBarAction> getActions() {
        ArrayList<ToolBarAction> ret = new ArrayList<ToolBarAction>();
        synchronized (TOOLBAR_ACTION_LIST) {
            ret.addAll(TOOLBAR_ACTION_LIST);
        }
        return ret;

    }

}
