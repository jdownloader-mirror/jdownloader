package org.jdownloader.extensions.vlcstreaming;

import java.io.File;
import java.util.logging.Logger;

import jd.http.Browser;
import jd.http.BrowserSettings;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterfaceFactory;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.io.streamingio.Streaming;
import org.appwork.utils.io.streamingio.StreamingInputStream;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.Input2OutputStreamForwarder;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;

public class VLCStreamingThread extends Thread implements BrowserSettings, DownloadInterfaceFactory {

    private RemoteAPIResponse    response;
    private RemoteAPIRequest     request;
    private StreamingInputStream sis = null;

    public RemoteAPIResponse getResponse() {
        return response;
    }

    public RemoteAPIRequest getRequest() {
        return request;
    }

    private HTTPProxy proxy;
    private LogSource logger;
    private Streaming streaming;

    public VLCStreamingThread(RemoteAPIResponse response, RemoteAPIRequest request, Streaming streaming) {
        this.response = response;
        this.request = request;
        proxy = Browser._getGlobalProxy();
        logger = LogController.getInstance().getLogger("VLCStreaming");
        logger.setAllowTimeoutFlush(false);
        this.streaming = streaming;
        setName("Streaming: " + new File(getStreaming().getOutputFile()).getName());
    }

    @Override
    public void run() {
        Runnable cleanup = new Runnable() {

            @Override
            public void run() {
                logger.close();
                try {
                    sis.close();
                } catch (final Throwable e) {
                }
                try {
                    response.closeConnection();
                } catch (final Throwable e) {
                }
            }
        };
        boolean doCleanup = true;
        try {
            final HTTPHeader rangeRequest = getRequest().getRequestHeaders().get("Range");
            long startPosition = 0;
            long stopPosition = -1;
            if (rangeRequest != null) {
                String start = new Regex(rangeRequest.getValue(), "(\\d+).*?-").getMatch(0);
                String stop = new Regex(rangeRequest.getValue(), "-.*?(\\d+)").getMatch(0);
                if (start != null) startPosition = Long.parseLong(start);
                if (stop != null) stopPosition = Long.parseLong(stop);
            }
            sis = getStreaming().getInputStream(startPosition, stopPosition);
            long completeSize = getStreaming().getFinalFileSize();
            if (sis == null) {
                getResponse().setResponseCode(ResponseCode.ERROR_NOT_FOUND);
                getResponse().getOutputStream();
            } else {
                getResponse().getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ACCEPT_RANGES, "ranges"));
                if (rangeRequest == null) {
                    getResponse().setResponseCode(ResponseCode.SUCCESS_OK);
                    if (completeSize != -1) getResponse().getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, completeSize + ""));
                } else {
                    getResponse().setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                    if (stopPosition == -1) {
                        getResponse().getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (completeSize - 1) + "/" + completeSize));
                    } else {
                        getResponse().getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + stopPosition + "/" + completeSize));
                    }
                    getResponse().getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, completeSize + ""));
                }
                Input2OutputStreamForwarder forwarder = new Input2OutputStreamForwarder(sis, getResponse().getOutputStream()) {
                    {
                        if (rangeRequest != null) {
                            thread.setName("Streaming: " + new File(getStreaming().getOutputFile()).getName() + "-" + rangeRequest.getValue());
                        } else {
                            thread.setName("Streaming: " + new File(getStreaming().getOutputFile()).getName());
                        }
                    }
                };
                doCleanup = false;
                forwarder.forward(cleanup);
            }
        } catch (final Throwable e) {
            logger.log(e);
            logger.flush();
        } finally {
            if (doCleanup) cleanup.run();
        }
    }

    @Override
    public HTTPProxy getCurrentProxy() {
        return proxy;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isDebug() {
        return true;
    }

    @Override
    public boolean isVerbose() {
        return true;
    }

    @Override
    public void setCurrentProxy(HTTPProxy proxy) {
    }

    @Override
    public void setDebug(boolean b) {
    }

    @Override
    public void setLogger(Logger logger) {
    }

    @Override
    public void setVerbose(boolean b) {
    }

    @Override
    public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request) throws Exception {
        return new VLCStreamingDownloadInterface(this, downloadLink.getLivePlugin(), downloadLink, request);
    }

    @Override
    public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request, boolean resume, int chunks) throws Exception {
        return new VLCStreamingDownloadInterface(this, downloadLink.getLivePlugin(), downloadLink, request);
    }

    /**
     * @return the streaming
     */
    public Streaming getStreaming() {
        return streaming;
    }

}
