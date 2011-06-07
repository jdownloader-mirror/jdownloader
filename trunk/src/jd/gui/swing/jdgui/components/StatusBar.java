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

package jd.gui.swing.jdgui.components;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.components.JDSpinner;
import jd.gui.swing.jdgui.components.modules.ModuleStatus;
import jd.gui.swing.jdgui.components.premiumbar.PremiumStatus;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.Formatter;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class StatusBar extends JPanel implements ChangeListener, ConfigEventListener {

    private static final long serialVersionUID = 3676496738341246846L;

    private JDSpinner         spMaxChunks;

    private JDSpinner         spMaxDls;

    private JDSpinner         spMaxSpeed;

    private GeneralSettings   config;

    public StatusBar() {

        config = JsonConfig.create(GeneralSettings.class);
        config.getStorageHandler().getEventSender().addListener(this);
        initGUI();
    }

    private void initGUI() {
        setLayout(new MigLayout("ins 0", "[fill,grow,left][fill,grow,right][][shrink,right][shrink,right][shrink,right]", "[22!]"));
        if (LookAndFeelController.getInstance().getLAFOptions().isPaintStatusbarTopBorder()) {
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));

        } else {
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, getBackground().darker()));

        }

        spMaxSpeed = new JDSpinner(_GUI._.gui_statusbar_speed());
        spMaxSpeed.getSpinner().addChangeListener(this);
        spMaxSpeed.getSpinner().setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 50));
        try {
            spMaxSpeed.setValue(JsonConfig.create(GeneralSettings.class).getDownloadSpeedLimit());
        } catch (Throwable e) {
            spMaxSpeed.setValue(0);
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            //
            // dlConfig.save();
            JsonConfig.create(GeneralSettings.class).setDownloadSpeedLimit(0);
        }
        spMaxSpeed.setToolTipText(_GUI._.gui_tooltip_statusbar_speedlimiter());
        colorizeSpinnerSpeed();

        spMaxDls = new JDSpinner(_GUI._.gui_statusbar_sim_ownloads(), "h 20!");
        spMaxDls.getSpinner().setModel(new SpinnerNumberModel(2, 1, 20, 1));
        try {
            spMaxDls.setValue(JsonConfig.create(GeneralSettings.class).getMaxSimultaneDownloads());
        } catch (Throwable e) {
            spMaxDls.setValue(2);
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
            // 2);
            // dlConfig.save();
            JsonConfig.create(GeneralSettings.class).setMaxSimultaneDownloads(2);
        }
        spMaxDls.setToolTipText(_GUI._.gui_tooltip_statusbar_simultan_downloads());
        spMaxDls.getSpinner().addChangeListener(this);

        spMaxChunks = new JDSpinner(_GUI._.gui_statusbar_maxChunks(), "h 20!");
        spMaxChunks.getSpinner().setModel(new SpinnerNumberModel(2, 1, 20, 1));
        try {
            spMaxChunks.setValue(JsonConfig.create(GeneralSettings.class).getMaxChunksPerFile());
        } catch (Throwable e) {
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2);
            // dlConfig.save();
            JsonConfig.create(GeneralSettings.class).setMaxChunksPerFile(2);
        }
        spMaxChunks.setToolTipText(_GUI._.gui_tooltip_statusbar_max_chunks());
        spMaxChunks.getSpinner().addChangeListener(this);

        add(PremiumStatus.getInstance());
        add(new ModuleStatus());
        add(new JSeparator(JSeparator.VERTICAL), "growy");
        add(spMaxChunks);
        add(spMaxDls);
        add(spMaxSpeed);

    }

    private void colorizeSpinnerSpeed() {
        /* färbt den spinner ein, falls speedbegrenzung aktiv */
        if (spMaxSpeed.getValue() > 0) {
            spMaxSpeed.setColor(new Color(255, 12, 3));
        } else {
            spMaxSpeed.setColor(null);
        }
    }

    /**
     * Setzt die Downloadgeschwindigkeit
     * 
     * @param speed
     *            bytes pro sekunde
     */
    public void setSpeed(int speed) {
        if (speed <= 0) {
            spMaxSpeed.setText(_GUI._.gui_statusbar_speed());
        } else {
            spMaxSpeed.setText("(" + Formatter.formatReadable(speed) + "/s)");
        }
    }

    public void setSpinnerSpeed(Integer speed) {
        try {
            spMaxSpeed.setValue(speed);
            colorizeSpinnerSpeed();
        } catch (Throwable e) {
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spMaxSpeed.getSpinner()) {
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
            // spMaxSpeed.getValue());
            // dlConfig.save();
            config.setDownloadSpeedLimit(spMaxSpeed.getValue());
        } else if (e.getSource() == spMaxDls.getSpinner()) {
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
            // spMaxDls.getValue());
            // dlConfig.save();
            config.setMaxSimultaneDownloads(spMaxDls.getValue());
        } else if (e.getSource() == spMaxChunks.getSpinner()) {
            // dlConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,
            // spMaxChunks.getValue());
            // dlConfig.save();
            config.setMaxChunksPerFile(spMaxChunks.getValue());
        }
    }

    public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
    }

    public void onConfigValueModified(ConfigInterface config2, String key, Object newValue) {

        if ("downloadSpeedLimit".equalsIgnoreCase(key)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    DownloadWatchDog.getInstance().getConnectionManager().setIncommingBandwidthLimit(config.getDownloadSpeedLimit() * 1024);
                    setSpinnerSpeed(config.getDownloadSpeedLimit());
                }
            });
        } else if ("MaxSimultaneDownloads".equalsIgnoreCase(key)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        spMaxDls.setValue(config.getMaxSimultaneDownloads());
                    } catch (Throwable e) {
                        config.setMaxSimultaneDownloads(2);
                        spMaxDls.setValue(2);
                    }
                }
            });
        } else if ("MaxChunksPerFile".equalsIgnoreCase(key)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        spMaxChunks.setValue(config.getMaxChunksPerFile());
                    } catch (Throwable e) {
                        spMaxChunks.setValue(1);
                        config.setMaxChunksPerFile(1);
                    }
                }
            });
        }
    }

}