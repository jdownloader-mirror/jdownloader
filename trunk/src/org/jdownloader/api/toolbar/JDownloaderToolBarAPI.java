package org.jdownloader.api.toolbar;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.storage.config.annotations.AllowStorage;

@ApiNamespace("toolbar")
public interface JDownloaderToolBarAPI extends RemoteAPIInterface {

    @AllowStorage(value = { Object.class })
    public Object getStatus();

    public boolean isAvailable();

    public boolean startDownloads();

    public boolean stopDownloads();

    public boolean toggleDownloadSpeedLimit();

    public boolean togglePremium();

    public boolean toggleClipboardMonitoring();

    public boolean toggleAutomaticReconnect();

    public boolean toggleStopAfterCurrentDownload();

    public long[] getSpeedMeter();

}
