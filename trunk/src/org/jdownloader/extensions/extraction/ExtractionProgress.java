package org.jdownloader.extensions.extraction;

import java.awt.Color;

import jd.plugins.PluginProgress;

public class ExtractionProgress extends PluginProgress {

    protected long lastCurrent    = -1;
    protected long lastTotal      = -1;
    protected long startTimeStamp = -1;

    public ExtractionProgress(long current, long total, Color color) {
        super(current, total, color);
    }

    @Override
    public void updateValues(long current, long total) {
        super.updateValues(current, total);
        if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
            lastTotal = total;
            lastCurrent = current;
            startTimeStamp = System.currentTimeMillis();
            this.setETA(-1);
            return;
        }
        long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
        if (currentTimeDifference <= 0) return;
        long speed = (current * 10000) / currentTimeDifference;
        if (speed == 0) return;
        long eta = ((total - current) * 10000) / speed;
        this.setETA(eta);
    }

}
