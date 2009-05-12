package jd.plugins.optional.jdtrayicon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class TrayMouseAdapter implements MouseListener, MouseMotionListener {

    private JDLightTray deligate;
    private boolean mouseover;
    private Thread mouseLocationObserver;
    // private TrayIcon trayIcon;
    private Point min;
    private Point max;
    private Dimension size;
    private MouseEvent lastEvent;
    private Component dummy;
    private static int TOOLTIP_DELAY = 1000;

    public TrayMouseAdapter(JDLightTray lightTray, TrayIcon trayIcon) {
        deligate = lightTray;
        // this.trayIcon = trayIcon;
        dummy = new Component() {
            private static final long serialVersionUID = 1L;
        };
        size = trayIcon.getSize();
    }

    public void mouseClicked(MouseEvent e) {
        deligate.mouseClicked(e);

    }

    public void mouseEntered(MouseEvent e) {
        mouseover = true;
        final long enterTime = System.currentTimeMillis();
        mouseLocationObserver = new Thread() {
            public void run() {
                try {
                    boolean mouseStay = false;
                    while (true) {
                        Point point = MouseInfo.getPointerInfo().getLocation();
                        if (!isOver(point)) {
                            MouseEvent me;
                            me = new MouseEvent(dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                            me.setSource(lastEvent.getSource());
                            mouseExited(me);

                            return;

                        } else {
                            if ((System.currentTimeMillis() - enterTime) >= TOOLTIP_DELAY && !mouseStay) {
                                mouseStay = true;
                                MouseEvent me;
                                me = new MouseEvent(dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                                me.setSource(TrayMouseAdapter.this);
                                deligate.mouseStay(me);
                            }
                        }

                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    mouseLocationObserver = null;
                }
            }

        };
        mouseLocationObserver.start();

        deligate.mouseEntered(e);

    }

    public void mouseExited(MouseEvent e) {
        mouseover = false;

        min = max = null;
        deligate.mouseExited(e);

    }

    public void mousePressed(MouseEvent e) {
        deligate.mousePressed(e);

    }

    public void mouseReleased(MouseEvent e) {
        deligate.mouseReleased(e);

    }

    public void mouseDragged(MouseEvent e) {
        deligate.mouseDragged(e);

    }

    public void mouseMoved(MouseEvent e) {
        lastEvent = e;
        /**
         * the more the user moves over the tray, the better we know it's
         * location *
         */

        if (this.min == null) {
            this.min = new Point(e.getPoint().x, e.getPoint().y);
            this.max = new Point(e.getPoint().x, e.getPoint().y);
        } else {
            min.x = Math.min(e.getPoint().x, min.x);
            min.y = Math.min(e.getPoint().y, min.y);
            max.x = Math.max(e.getPoint().x, max.x);
            max.y = Math.max(e.getPoint().y, max.y);
            // System.out.println(min+" - "+max);
        }

        if (!this.mouseover) {
            this.mouseEntered(e);
        } else {

            deligate.mouseMoved(e);
        }

    }

    public Point getEstimatedTopLeft() {
        int midx = (max.x + min.x) / 2;
        int midy = (max.y + min.y) / 2;

        return new Point(midx - size.width / 2, midy - size.height / 2);
    }

    /**
     * Passt die iconsize in die festgestellte geschätzte position ein. und
     * prüft ob point darin ist
     * 
     * @param point
     * @return
     */
    protected boolean isOver(Point point) {
        int midx = (max.x + min.x) / 2;
        int midy = (max.y + min.y) / 2;

        int width = Math.min(size.width, max.x - min.x);
        int height = Math.min(size.height, max.y - min.y);

        int minx = midx - width / 2;
        int miny = midy - height / 2;
        int maxx = midx + width / 2;
        int maxy = midy + height / 2;
        // java.awt.Point[x=1274,y=1175] - java.awt.Point[x=1309,y=1185]
        if (point.x >= minx && point.x <= maxx) {
            if (point.y >= miny && point.y <= maxy) { return true; }

        }
        return false;
    }

}
