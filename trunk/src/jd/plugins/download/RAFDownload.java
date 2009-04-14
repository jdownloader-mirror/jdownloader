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

package jd.plugins.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.http.requests.Request;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RAFDownload extends DownloadInterface {
    class ChunkBuffer {
        public ByteBuffer buffer;
        public int chunkID;
        public long chunkPosition;
        public long position;

        public ChunkBuffer(ByteBuffer buffer, long position, long chunkposition, int chunkid) {
            this.buffer = buffer;
            this.position = position;
            chunkPosition = chunkposition;
            chunkID = chunkid;
        }
    }

    class WriterWorker extends Thread {

        public boolean waitFlag = true;

        public WriterWorker() {
            this.setName("RAFWriterWorker");
            start();
        }

        @Override
        public void run() {
            ChunkBuffer buf;
            while (!isInterrupted() || bufferList.size() > 0) {
                synchronized (this) {

                    while (!isInterrupted() && waitFlag) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                            // return;
                        }
                    }
                }

                while (bufferList.size() > 0) {
                    synchronized (bufferList) {
                        buf = bufferList.remove(0);
                    }
                    try {

                        synchronized (outputChannel) {
                            outputFile.seek(buf.position);
                            outputChannel.write(buf.buffer);

                            if (buf.chunkID >= 0) {
                                downloadLink.getChunksProgress()[buf.chunkID] = buf.chunkPosition;
                            }

                            // logger.info("Wrote buffer. rest: " +
                            // bufferList.size());
                        }

                    } catch (Exception e) {

                        // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                        error(LinkStatus.ERROR_LOCAL_IO, JDUtilities.convertExceptionReadable(e));

                        addException(e);
                    }
                }
                waitFlag = true;

            }

        }
    }

    private ArrayList<ChunkBuffer> bufferList = new ArrayList<ChunkBuffer>();

    protected FileChannel[] channels;

    protected long hdWritesPerSecond;

    private FileChannel outputChannel;

    private RandomAccessFile outputFile;

    protected File[] partFiles;

    protected long writeCount = 0;
    private WriterWorker writer;

    protected long writeTimer = System.currentTimeMillis();

    private Boolean writeType;

    // public RAFDownload(PluginForHost plugin, DownloadLink downloadLink,
    // HTTPConnection urlConnection) {
    // super(plugin, downloadLink, urlConnection);
    // writeType =
    // JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("USEWRITERTHREAD"
    // , false);
    // }

    public RAFDownload(PluginForHost plugin, DownloadLink downloadLink, Request request) throws IOException, PluginException {

        super(plugin, downloadLink, request);
        writeType = SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("USEWRITERTHREAD", false);

    }

    @Override
    protected void onChunksReady() {
        logger.finer("onCHunksReady");
        if (writer != null) {
            synchronized (writer) {
                if (writer.waitFlag) {
                    writer.waitFlag = false;
                    writer.notify();
                }

            }
            downloadLink.getLinkStatus().setStatusText(null);
            if (!handleErrors()) {

                writer.interrupt();
            }

            while (writer.isAlive() && bufferList.size() > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                }
            }
        }

        System.gc();
        System.runFinalization();
        //
        logger.info("CLOSE HD FILE");
        try {
            outputChannel.force(false);
        } catch (Exception e) {
            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        try {
            outputFile.close();
        } catch (Exception e) {
            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        try {
            outputChannel.close();
        } catch (Exception e) {
            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        if (!handleErrors()) {

        return; }
        try {
            logger.finest("no errors : rename");
            if (!new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))) {

                logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
                error(LinkStatus.ERROR_LOCAL_IO, JDLocale.L("system.download.errors.couldnotrename", "Could not rename partfile"));

            }
            DownloadLink sfv;

            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_DO_CRC, false) && (sfv = downloadLink.getFilePackage().getSFV()) != null) {
                if (sfv.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2", "CRC-Check running(%s)", "CRC32"));
                    downloadLink.requestGuiUpdate();

                    long crc = JDUtilities.getCRC(new File(downloadLink.getFileOutput()));

                    String sfvText = JDIO.getLocalFile(new File(sfv.getFileOutput()));
                    if (sfvText != null && sfvText.toLowerCase().contains(new File(downloadLink.getFileOutput()).getName().toLowerCase())) {
                        String[] l = Regex.getLines(sfvText);
                        boolean c = false;
                        for (String line : l) {
                            // logger.info(line + " - " +
                            // Long.toHexString(crc).toUpperCase());
                            if (line.trim().endsWith(Long.toHexString(crc).toUpperCase()) || line.trim().endsWith(Long.toHexString(crc).toLowerCase())) {
                                c = true;
                                logger.info("CRC CHECK SUCCESS");
                                break;
                            }
                        }
                        if (c) {
                            downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2.success", "CRC-Check OK(%s)", "CRC32"));
                            downloadLink.requestGuiUpdate();
                        } else {
                            downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                            downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2.failed", "CRC-Check FAILED(%s)", "CRC32"));
                            downloadLink.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                            downloadLink.requestGuiUpdate();
                            error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.LF("system.download.doCRC2.failed", "CRC-Check FAILED(%s)", "CRC32"));

                        }

                    }
                }

            } else if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_DO_CRC, false)) {

                String linkHash = null;
                String localHash = null;
                String hashType = null;

                if (downloadLink.getMD5Hash() != null) {
                    localHash = JDHash.getMD5(new File(downloadLink.getFileOutput()));
                    linkHash = downloadLink.getMD5Hash();
                    hashType = "MD5";

                }
                if (downloadLink.getSha1Hash() != null) {
                    localHash = JDHash.getSHA1(new File(downloadLink.getFileOutput()));
                    linkHash = downloadLink.getSha1Hash();
                    hashType = "SHA1";
                }

                if (hashType != null) {
                    downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2", "CRC-Check running(%s)", hashType));
                    downloadLink.requestGuiUpdate();
                    if (localHash.equalsIgnoreCase(linkHash)) {
                        downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2.success", "CRC-Check OK(%s)", hashType));
                        downloadLink.requestGuiUpdate();
                    } else {
                        downloadLink.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                        downloadLink.getLinkStatus().setStatusText(JDLocale.LF("system.download.doCRC2.failed", "CRC-Check FAILED(%s)", hashType));
                        downloadLink.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                        downloadLink.requestGuiUpdate();
                        error(LinkStatus.ERROR_DOWNLOAD_FAILED, JDLocale.LF("system.download.doCRC2.failed", "CRC-Check FAILED(%s)", hashType));
                    }

                }

            }
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            addException(e);
        }

    }

    @Override
    protected void setupChunks() throws Exception {
        try {

            if (isResume() && checkResumabled()) {
                logger.finer("Setup resume");
                this.setupResume();

            } else {
                logger.finer("Setup virgin download");
                this.setupVirginStart();
            }

        } catch (Exception e) {
            try {
                if (outputChannel != null) outputChannel.force(false);
                logger.info("CLOSE HD FILE");
                if (outputFile != null) outputFile.close();
                if (outputChannel != null) outputChannel.close();
            } catch (Exception e2) {
                JDLogger.exception(e2);
            }
            addException(e);
            throw e;

        }

    }

    private void setupVirginStart() throws FileNotFoundException {
        Chunk chunk;

        totaleLinkBytesLoaded = 0;
        downloadLink.setDownloadCurrent(0);
        long partSize = fileSize / getChunkNum();

        if (connection.getRange() != null) {
            if (connection.getRange()[1] == connection.getRange()[2] - 1) {
                logger.warning("Chunkload protection. this may cause traffic errors");
                partSize = fileSize / getChunkNum();
            } else {
                // Falls schon der 1. range angefordert wurde.... werden die
                // restlichen chunks angepasst
                partSize = (fileSize - connection.getLongContentLength()) / (getChunkNum() - 1);
            }

        }
        if (partSize <= 0) {
            logger.warning("Could not get Filesize.... reset chunks to 1");
            setChunkNum(1);
        }
        logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + partSize);

        // downloadLink.setChunksProgress(new int[chunkNum]);

        createOutputChannel();
        downloadLink.setChunksProgress(new long[chunkNum]);

        addToChunksInProgress(getChunkNum());
        int start = 0;

        long rangePosition = 0;
        if (connection.getRange() != null && connection.getRange()[1] != connection.getRange()[2] - 1) {
            // Erster range schon angefordert

            chunk = new Chunk(0, rangePosition = connection.getRange()[1], connection, this);
            rangePosition++;
            logger.finer("Setup chunk " + "0" + ": " + chunk);
            addChunk(chunk);
            start++;
        }

        for (int i = start; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {
                chunk = new Chunk(rangePosition, -1, connection, this);

            } else {
                chunk = new Chunk(rangePosition, rangePosition + partSize - 1, connection, this);
                rangePosition = rangePosition + partSize;
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }
        // logger.info("Total splitted size: "+total);

    }

    private void createOutputChannel() throws FileNotFoundException {
        if (!new File(downloadLink.getFileOutput()).getParentFile().exists()) {
            new File(downloadLink.getFileOutput()).getParentFile().mkdirs();
        }
        outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

        outputChannel = outputFile.getChannel();
    }

    private void setupResume() throws FileNotFoundException {

        long parts = fileSize / getChunkNum();
        logger.info("Resume: " + fileSize + " partsize: " + parts);
        Chunk chunk;
        this.createOutputChannel();
        addToChunksInProgress(getChunkNum());

        for (int i = 0; i < getChunkNum(); i++) {
            if (i == getChunkNum() - 1) {

                chunk = new Chunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, -1, connection, this);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
            } else {

                chunk = new Chunk(downloadLink.getChunksProgress()[i] == 0 ? 0 : downloadLink.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection, this);
                chunk.setLoaded((downloadLink.getChunksProgress()[i] - i * parts + 1));
            }
            logger.finer("Setup chunk " + i + ": " + chunk);
            addChunk(chunk);
        }

    }

    @Override
    protected boolean writeChunkBytes(Chunk chunk) {
        if (writeType) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(chunk.buffer.limit());
            buffer.put(chunk.buffer);
            buffer.flip();
            synchronized (bufferList) {
                bufferList.add(new ChunkBuffer(buffer, chunk.getWritePosition(), chunk.getCurrentBytesPosition() - 1, chunk.getID()));
                // logger.info("new buffer. size: " + bufferList.size());
            }

            if (writer == null) {
                writer = new WriterWorker();

            }

            synchronized (writer) {
                if (writer.waitFlag) {
                    writer.waitFlag = false;
                    writer.notify();
                }

            }
        } else {
            try {
                synchronized (outputChannel) {
                    outputFile.seek(chunk.getWritePosition());
                    outputChannel.write(chunk.buffer);
                    if (chunk.getID() >= 0) {
                        downloadLink.getChunksProgress()[chunk.getID()] = chunk.getCurrentBytesPosition() - 1;
                    }

                    return true;
                }

            } catch (Exception e) {
                error(LinkStatus.ERROR_LOCAL_IO, JDUtilities.convertExceptionReadable(e));
                addException(e);
                return false;
            }

        }
        return true;

    }

    /**
     * 
     * @param downloadLink
     *            downloadlink der geladne werden soll (wird zur darstellung
     *            verwendet)
     * @param request
     *            Verbindung die geladen werden soll
     * @param b
     *            Resumefähige verbindung
     * @param i
     *            max chunks. für negative werte wirden die chunks aus der
     *            config verwendet. Bsp: -3 : Min(3,Configwert);
     * @return
     * @throws IOException
     * @throws PluginException
     */

    public static DownloadInterface download(DownloadLink downloadLink, Request request, boolean b, int i) throws IOException, PluginException {

        DownloadInterface dl = new RAFDownload(downloadLink.getPlugin(), downloadLink, request);
        downloadLink.getPlugin().setDownloadInterface(dl);
        dl.setResume(b);
        if (i == 0) {
            dl.setChunkNum(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        } else {
            dl.setChunkNum(i < 0 ? Math.min(i * -1, SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2)) : i);

        }

        return dl;

    }

    public static DownloadInterface download(DownloadLink downloadLink, Request request) throws Exception {

        // TODO Auto-generated method stub
        return download(downloadLink, request, false, 1);
    }
}
