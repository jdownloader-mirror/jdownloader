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

package jd.gui.skins.simple;

import java.awt.EventQueue;

import javax.swing.SwingUtilities;

/**
 * This calss invokes a Runnable EDT Save and is able to return values.
 * 
 * @author coalado
 * 
 */
public abstract class GuiRunnable<T> implements Runnable {
    /**
     * 
     */
    private static final long serialVersionUID = 7777074589566807490L;

    private T returnValue;
    private final Object lock = new Object();

    private boolean started = false;
    private boolean done = false;

    public boolean isStarted() {
        return started;
    }

    private void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * If this method mis calls, the thread waits until THD EDT has innvoked the
     * runnable.. it ensures that the return value is available
     * 
     * @return
     */
    public T getReturnValue() {

        waitForEDT();
        return returnValue;
    }

    /**
     * This method waits until the EDT has invoked the runnable. If the Runnable
     * is not started yet.. the start method gets called
     */
    public void waitForEDT() {
        if (done) return;
        if (!isStarted()) start();
        if (!SwingUtilities.isEventDispatchThread()) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    //jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }
        }
        done = true;
    }

    public void run() {
        this.returnValue = this.runSave();
        synchronized (lock) {
            lock.notify();
        }
    }

    abstract public T runSave();

    /**
     * Starts the Runnable and adds it to the ETD
     */
    public void start() {
//        new Exception().printStackTrace();
        setStarted(true);
        if (SwingUtilities.isEventDispatchThread()) {
            run();
        } else {
            EventQueue.invokeLater(this);

        }

    }
}
