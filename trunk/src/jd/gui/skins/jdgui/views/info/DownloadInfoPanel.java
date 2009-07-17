package jd.gui.skins.jdgui.views.info;

import jd.controlling.DownloadController;
import jd.controlling.DownloadInformations;
import jd.gui.skins.simple.GuiRunnable;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DownloadInfoPanel extends InfoPanel {

    public static final String JDL_PREFIX = "jd.gui.skins.jdgui.views.info.DownloadInfoPanel.";
    private DownloadInformations ds;
    private DownloadController dlc;
    private int speed;

    public DownloadInfoPanel() {
        super();
        this.setIcon(JDTheme.II("gui.images.taskpanes.download", 32, 32));
//        this.setTitle(JDL.L(JDL_PREFIX + "title", "Downloadlist"));

        addInfoEntry(JDL.L(JDL_PREFIX + "packages", "Package(s)"), "0", 0, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "links", "Links(s)"), "0", 0, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "size", "Total size"), "0", 1, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "speed", "Downloadspeed"), "0", 2, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "eta", "Download complete in"), "0", 2, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "progress", "Progress"), "0", 3, 0);

        ds = new DownloadInformations();
        dlc = JDUtilities.getDownloadController();

        Thread fadeTimer = new Thread() {
            public void run() {
                this.setName("DownloadTask: infoupdate");
                while (true) {// TODO

                    update();

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        fadeTimer.start();

    }

    private void update() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                dlc.getDownloadStatus(ds);
                speed = JDUtilities.getController().getSpeedMeter();
                updateInfo(JDL.L(JDL_PREFIX + "speed", "Downloadspeed"), Formatter.formatReadable(speed) + "/s");
                updateInfo(JDL.L(JDL_PREFIX + "eta", "Download complete in"), Formatter.formatSeconds(speed == 0 ? -1 : (ds.getTotalDownloadSize() - ds.getCurrentDownloadSize()) / speed));

                updateInfo(JDL.L(JDL_PREFIX + "packages", "Package(s)"), ds.getPackagesCount());
                updateInfo(JDL.L(JDL_PREFIX + "links", "Links(s)"), ds.getDownloadCount());
                updateInfo(JDL.L(JDL_PREFIX + "size", "Total size"), Formatter.formatReadable(ds.getTotalDownloadSize()));

                updateInfo(JDL.L(JDL_PREFIX + "progress", "Progress"), Math.round((ds.getCurrentDownloadSize() * 10000.0) / ds.getTotalDownloadSize()) / 100.0 + "%");
                return null;
            }
        }.start();
    }

}
