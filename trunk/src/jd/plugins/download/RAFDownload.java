//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RAFDownload extends DownloadInterface {
    private Boolean writeType;

    public RAFDownload(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        super(plugin, downloadLink, urlConnection);
        this.writeType = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("USEWRITERTHREAD", false);
    }

    protected long writeTimer = System.currentTimeMillis();

    protected long writeCount = 0;

    protected long hdWritesPerSecond;

    protected FileChannel[] channels;

    protected File[] partFiles;

    private FileChannel outputChannel;

    private RandomAccessFile outputFile;
    private ArrayList<ChunkBuffer> bufferList = new ArrayList<ChunkBuffer>();

    private WriterWorker writer;

    protected boolean writeChunkBytes(Chunk chunk) {
     

            if (writeType) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(chunk.buffer.limit());
                buffer.put(chunk.buffer);
                buffer.flip();
                synchronized (bufferList) {
                    bufferList.add(new ChunkBuffer(buffer, chunk.getWritePosition(),(int) chunk.getCurrentBytesPosition() - 1,chunk.getID()));
                    // logger.info("new buffer. size: " + bufferList.size());
                }

                if (this.writer == null) {
                    writer = new WriterWorker();

                }

                synchronized (writer) {
                    if (writer.waitFlag) {
                        writer.waitFlag = false;
                        writer.notify();
                    }

                }
//            } else {
//                try {
//                    synchronized (outputChannel) {
//                        outputFile.seek(chunk.getWritePosition());
//                        outputChannel.write(chunk.buffer);
//                        if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.getCurrentBytesPosition() - 1;
//                        
//                        return true;
//                    }
//
//                } catch (Exception e) {
//
//                    // e.printStackTrace();
//                    error(ERROR_LOCAL_IO);
//                    addException(e);
//                    return false;
//                }
//
//            }

        } else {
            chunk.buffer.clear();
        }
        return true;
        // try {
        // // int limit = chunk.buffer.limit()-chunk.buffer.position();
        //
        // if (maxBytes < 0) {
        // synchronized (outputChannel) {
        // outputFile.seek(chunk.getWritePosition());
        // outputChannel.write(chunk.buffer);
        // }
        // } else {
        // chunk.buffer.clear();
        // }
        //
        // } catch (Exception e) {
        //
        // e.printStackTrace();
        // error(ERROR_LOCAL_IO);
        // addException(e);
        // }

    }

    @Override
    protected void setupChunks() {
        try {

            boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", true);
            fileSize = getFileSize();
            if (correctChunks) {

                int tmp = Math.min(Math.max(1, (int) (fileSize / Chunk.MIN_CHUNKSIZE)), getChunkNum());
                if (tmp != getChunkNum()) {
                    logger.finer("Corrected Chunknum: " + getChunkNum() + " -->" + tmp);
                    this.setChunkNum(tmp);
                }
            }

            linkStatus.addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            downloadLink.setDownloadMax((int) fileSize);
            setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
            if (checkResumabled() && plugin.getFreeConnections() >= getChunkNum() ) {
                logger.info("Resume: " + fileSize);
                long parts = fileSize / getChunkNum();

                Chunk chunk;

                outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                outputChannel = outputFile.getChannel();
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {
                    if (i == (getChunkNum() - 1)) {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i] + 1, -1, connection);
                        chunk.setLoaded((int) (downloadLink.getChunksProgress()[i] - i * parts + 1));
                    } else {
                        chunk = new Chunk(downloadLink.getChunksProgress()[i] + 1, (i + 1) * parts - 1, connection);
                        chunk.setLoaded((int) (downloadLink.getChunksProgress()[i] - i * parts + 1));
                    }

                    addChunk(chunk);
                }

            } else {
          
                this.setChunkNum(Math.min(getChunkNum(), plugin.getFreeConnections()));
                this.totaleLinkBytesLoaded = 0;
                downloadLink.setDownloadCurrent(0);
                long parts = fileSize > 0 ? fileSize / getChunkNum() : -1;
                if (parts == -1) {
                    logger.warning("Could not get Filesize.... reset chunks to 1");
                    setChunkNum(1);
                }
                logger.finer("Start Download in " + getChunkNum() + " chunks. Chunksize: " + parts);

                // downloadLink.setChunksProgress(new int[chunkNum]);
                Chunk chunk;
                if (!new File(downloadLink.getFileOutput()).getParentFile().exists()) {
                    new File(downloadLink.getFileOutput()).getParentFile().mkdirs();
                }
                outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");

                outputChannel = outputFile.getChannel();
                downloadLink.setChunksProgress(new int[chunkNum]);
                logger.info("Filesize = " + fileSize);
                logger.info("Partsize = " + parts);
                // int total=0;
                addToChunksInProgress(getChunkNum());
                for (int i = 0; i < getChunkNum(); i++) {

                    if (i == (getChunkNum() - 1)) {

                       
                            chunk = new Chunk(i * parts, -1, connection);
                            // total+=(fileSize-i * parts);
                            logger.info("+part " + (fileSize - i * parts));
                    

                    } else {
                        chunk = new Chunk(i * parts, (i + 1) * parts - 1, connection);
                        // total+=((i + 1) * parts-i * parts);
                        logger.info("+part " + ((i + 1) * parts - i * parts));
                    }

                    addChunk(chunk);
                }
                // logger.info("Total splitted size: "+total);
            }

        } catch (Exception e) {
            try {
                outputChannel.force(false);
                logger.info("CLOSE HD FILE");
                outputFile.close();
                outputChannel.close();
                e.printStackTrace();
            } catch (Exception e2) {
                e.printStackTrace();
            }
            e.printStackTrace();
            addException(e);
        }

    }

    private boolean checkResumabled() {

        if (!isResume() || downloadLink.getChunksProgress() == null) return false;

        int loaded = 0;
        int fileSize = (int) getFileSize();
        int chunks = downloadLink.getChunksProgress().length;
        int part = fileSize / chunks;

        for (int i = 0; i < chunks; i++) {
            loaded += downloadLink.getChunksProgress()[i] - i * part;
        }
        if (chunks > 0) {

            this.setChunkNum(chunks);
            logger.info("Resume with " + chunks + " chunks");

            return true;
        }
        return false;

    }

    @Override
    protected void onChunksReady() {
        if (writer != null) {
            synchronized (writer) {
                if (writer.waitFlag) {
                    writer.waitFlag = false;
                    writer.notify();
                }

            }
            if (!handleErrors()) {

                writer.interrupt();
            }

            while (writer.isAlive() && bufferList.size() > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        System.gc();
        System.runFinalization();
        //
        logger.info("CLOSE HD FILE");
        try {
          this.outputChannel.force(false);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            outputFile.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            outputChannel.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        if (!handleErrors()) {

        return; }
        try {
            if (!new File(downloadLink.getFileOutput() + ".part").renameTo(new File(downloadLink.getFileOutput()))) {

                logger.severe("Could not rename file " + new File(downloadLink.getFileOutput() + ".part") + " to " + downloadLink.getFileOutput());
                error(LinkStatus.ERROR_LOCAL_IO,JDLocale.L("system.download.errors.couldnotrename","Could not rename partfile"));

            }
            DownloadLink sfv;
            if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_DO_CRC, false) && (sfv = downloadLink.getFilePackage().getSFV()) != null) {
                if (sfv.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    long crc = JDUtilities.getCRC(new File(downloadLink.getFileOutput()));

                    String sfvText = JDUtilities.getLocalFile(new File(sfv.getFileOutput()));
                    if (sfvText != null && sfvText.toLowerCase().contains(new File(downloadLink.getFileOutput()).getName().toLowerCase())) {
                        String[] l = Regex.getLines(sfvText);
                        boolean c = false;
                        for (String line : l) {
                            logger.info(line + " - " + Long.toHexString(crc).toUpperCase());
                            if (line.trim().endsWith(Long.toHexString(crc).toUpperCase()) || line.trim().endsWith(Long.toHexString(crc).toLowerCase())) {
                                c = true;
                                logger.info("CRC CHECK SUCCESS");
                                break;
                            }
                        }
                        if (c) {
                            downloadLink.setCrcStatus(DownloadLink.CRC_STATUS_OK);
                        } else {
                            downloadLink.setCrcStatus(DownloadLink.CRC_STATUS_BAD);
                            error(LinkStatus.ERROR_DOWNLOAD_FAILED,JDLocale.L("system.download.errors.crcfailed","CRC Check failed"));

                        }

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            addException(e);
        }

    }

    class WriterWorker extends Thread {

        public boolean waitFlag = true;

        public WriterWorker() {
            start();
        }

        public void run() {
            ChunkBuffer buf;
            while (!isInterrupted() || bufferList.size() > 0) {
                synchronized (this) {

                    while (!isInterrupted() && waitFlag) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // e.printStackTrace();
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
                            
                            if (buf.chunkID >= 0) downloadLink.getChunksProgress()[buf.chunkID] = buf.chunkPosition;
                            
                            // logger.info("Wrote buffer. rest: " +
                            // bufferList.size());
                        }

                    } catch (Exception e) {

                        // e.printStackTrace();
                        error(LinkStatus.ERROR_LOCAL_IO,JDUtilities.convertExceptionReadable(e));

                        addException(e);
                    }
                }
                waitFlag = true;

            }

        }
    }

    class ChunkBuffer {
        public long position;
        public ByteBuffer buffer;
        public int chunkPosition;
        public int chunkID;

        public ChunkBuffer(ByteBuffer buffer, long position, int chunkposition, int chunkid) {
            this.buffer = buffer;
            this.position = position;
            this.chunkPosition=chunkposition;
            this.chunkID=chunkid;
        }
    }

}
