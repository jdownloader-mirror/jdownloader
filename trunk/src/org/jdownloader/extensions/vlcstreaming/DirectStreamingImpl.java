package org.jdownloader.extensions.vlcstreaming;

import java.io.IOException;
import java.io.InputStream;

import jd.plugins.DownloadLink;

import org.appwork.utils.io.streamingio.Streaming;

public class DirectStreamingImpl implements StreamingInterface {

    private Streaming streaming;

    public DirectStreamingImpl(VLCStreamingExtension extension, DownloadLink link) throws IOException {
        streaming = extension.getStreamProvider().getStreamingProvider(link);
    }

    @Override
    public boolean isRangeRequestSupported() {
        return true;
    }

    @Override
    public long getFinalFileSize() {
        return streaming.getFinalFileSize();
    }

    @Override
    public InputStream getInputStream(long startPosition, long stopPosition) throws IOException {
        return streaming.getInputStream(startPosition, stopPosition);
    }

    @Override
    public void close() {
        streaming.close();
    }

}
