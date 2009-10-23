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

package jd.nutils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Executer extends Thread implements Runnable {
    public static final String CODEPAGE = OSDetector.isWindows() ? "ISO-8859-1" : "UTF-8";
    private boolean debug = true;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private String codepage = CODEPAGE;

    public String getCodepage() {
        return codepage;
    }

    public void setCodepage(String codepage) {
        this.codepage = codepage;
    }

    class StreamObserver extends Thread implements Runnable {

        private BufferedInputStream reader;

        private DynByteBuffer dynbuf;
        private boolean started;
        /**
         * is set to true if the observer is waiting for data
         */
        private boolean idle = true;
        /**
         * is set to true of the reader returned -1
         */
        private boolean endOfFileReceived = false;

        private InputStream stream;

        public StreamObserver(InputStream stream, DynByteBuffer buffer) {
            this.stream = stream;
            reader = new BufferedInputStream(stream);

            dynbuf = buffer;
        }

        @Override
        public void run() {

            this.started = true;
            int num;
            try {
                fireEvent(dynbuf, 0, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);

                while (isInterrupted() || reader.available() <= 0) {
                    if (isInterrupted()) return;
                }
                this.idle = false;
                while (!endOfFileReceived) {
                    num = readLine();

                    String line;

                    try {
                        line = new String(dynbuf.getLast(num), codepage).trim();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        line = new String(dynbuf.getLast(num)).trim();

                    }
                    if (line.length() > 0) {
                        if (isDebug()) {
                            System.out.println(this + ": " + line + "");

                        }

                        fireEvent(line, dynbuf, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // e.printStackTrace();
            } finally {
                System.out.println("END");
            }

        }

        private int readLine() throws IOException, InterruptedException {
            int i = 0;
            byte[] buffer = new byte[1];
            // some processes to not return an errorstream which leads
            // reader.read to lock. This lock cannot be released unter windows
            // so we start reading as soon as available() marks some bytes as

            // lock until bytes are available
            // if the stream does not provide data, the observer is set to idle.
            // if the concerned process has finished, and the stream does not
            // provide data, we can interrupt the observer
            // use isIdle() to check observer status
            this.idle = true;

            this.idle = false;
            for (;;) {

                int read;

                if (isInterrupted()) throw new InterruptedException();

                if ((read = reader.read(buffer)) < 0) {
                    this.endOfFileReceived = true;
                    return i;
                }
                i += read;
                dynbuf.put(buffer, read);
                if (buffer[0] == '\b' || buffer[0] == '\r' || buffer[0] == '\n') { return i; }
                fireEvent(dynbuf, read, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);
            }

        }

        public boolean isStarted() {
            return started;
        }

        public void requestInterrupt() {
            // use this flag to give the reader a chance to finish reading

            this.interrupt();
            // to interrupt a running read
            try {
                stream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        /**
         * @return the {@link Executer.StreamObserver#idle}
         * @see Executer.StreamObserver#idle
         */
        public boolean isIdle() {
            return idle;
        }

    }

    public static int LISTENER_ERRORSTREAM = 1;
    public static int LISTENER_STDSTREAM = 1 << 1;

    private String command;

    private ArrayList<String> parameter;
    private String runIn;
    private DynByteBuffer inputStreamBuffer;
    private DynByteBuffer errorStreamBuffer;
    private ArrayList<ProcessListener> listener = new ArrayList<ProcessListener>();
    private ArrayList<ProcessListener> elistener = new ArrayList<ProcessListener>();

    private int waitTimeout = 60;
    private int exitValue = -1;

    private Process process;
    private StreamObserver sbeObserver;
    private StreamObserver sboObserver;
    private OutputStream outputStream = null;
    private Exception exception = null;

    public Executer(String command) {

        this.command = command;
        parameter = new ArrayList<String>();

        inputStreamBuffer = new DynByteBuffer(1024 * 4);
        errorStreamBuffer = new DynByteBuffer(1024 * 4);
        setName("Executer: " + command);

    }

    public void addParameter(String par) {
        parameter.add(par);
    }

    public void addParameters(String[] par) {
        if (par == null) return;
        for (String p : par) {
            parameter.add(p);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getErrorStream() {
        return errorStreamBuffer.toString(this.codepage);
    }

    public ArrayList<String> getParameter() {
        return parameter;
    }

    public String getRunin() {
        return runIn;
    }

    public String getOutputStream() {
        return inputStreamBuffer.toString(this.codepage);
    }

    public int getWaitTimeout() {
        return waitTimeout;
    }

    @Override
    public void run() {
        if (command == null || command.trim().length() == 0) {
            System.out.println("Execute Parameter error: No Command");
            return;
        }

        ArrayList<String> params = new ArrayList<String>();
        params.add(command);
        params.addAll(parameter);
        if (isDebug()) {
            StringBuilder out = new StringBuilder();
            for (String p : params) {
                out.append(p);
                out.append(' ');
            }
            System.out.println("Execute: " + out + " in " + runIn);
        }
        ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[] {}));
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                if (new File(params.get(0)).getParentFile().exists()) {
                    // logger.info("Run in: " + new
                    // File(params.get(0)).getParentFile());
                    pb.directory(new File(params.get(0)).getParentFile());
                } else {
                    System.out.println("Working directory " + runIn + " does not exist!");
                }
            }
        }

        try {

            process = pb.start();

            if (waitTimeout == 0) { return; }
            outputStream = process.getOutputStream();
            sbeObserver = new StreamObserver(process.getErrorStream(), errorStreamBuffer);
            sbeObserver.setName(this.getName() + " ERRstreamobserver");
            sboObserver = new StreamObserver(process.getInputStream(), inputStreamBuffer);
            sboObserver.setName(this.getName() + " STDstreamobserver");
            sbeObserver.setName(this.getName() + " ERRstreamobserver");
            sbeObserver.start();
            sboObserver.start();

            if (waitTimeout > 0) {
                Thread timeoutThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(waitTimeout * 1000);
                        } catch (InterruptedException e) {
                        }
                        // interrupt on timeout. this handles and timeout like
                        // an external interrupt
                        Executer.this.interrupt();

                    }
                };
                timeoutThread.start();
            }

            try {
                process.waitFor();
                exitValue = process.exitValue();
            } catch (InterruptedException e1) {
                process.destroy();
                exitValue = -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // stream did not return -1 yet, and so the observer sill still
            // waiting for data. we interrupt him
            if (sboObserver.isIdle()) {

                sboObserver.requestInterrupt();

            }
            if (sbeObserver.isIdle()) {
                sbeObserver.requestInterrupt();
            }

            // must be called to clear interrupt flag
            interrupted();
            while ((sbeObserver != null && this.sbeObserver.isAlive()) || (sboObserver != null && this.sboObserver.isAlive())) {
                Thread.sleep(50);
            }

        } catch (IOException e1) {
            this.exception = e1;
            return;
        } catch (InterruptedException e) {
            this.exception = e;
        }
    }

    public Exception getException() {
        return exception;
    }

    public Process getProcess() {
        return process;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (sbeObserver != null) this.sbeObserver.requestInterrupt();
        if (sboObserver != null) this.sboObserver.requestInterrupt();

        process.destroy();
    }

    public DynByteBuffer getInputStreamBuffer() {
        return inputStreamBuffer;
    }

    public void writetoOutputStream(String data) {
        if (data == null || data.length() == 0) data = "";

        try {
            outputStream.write(data.getBytes());
            outputStream.write("\n".getBytes());
            if (isDebug()) System.out.println("Out>" + data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DynByteBuffer getErrorStreamBuffer() {
        return errorStreamBuffer;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setParameter(ArrayList<String> parameter) {
        this.parameter = parameter;
    }

    public void setRunin(String runin) {
        runIn = runin;
    }

    public void setWaitTimeout(int waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public void waitTimeout() {
        while (isAlive()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("bla");
    }

    public int getExitValue() {
        return exitValue;
    }

    /**
     * for compatibility reasons.... can be refactored someday
     * 
     * @deprecated Use {@link #getExitValue()} instead
     */
    @Deprecated
    public boolean gotInterrupted() {
        return getExitValue() == -1;
    }

    public void addProcessListener(ProcessListener listener, int flag) {
        this.removeProcessListener(listener, flag);

        if ((flag & Executer.LISTENER_STDSTREAM) > 0) this.listener.add(listener);
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) this.elistener.add(listener);

    }

    private void fireEvent(String line, DynByteBuffer sb, int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) for (ProcessListener listener : this.listener) {
            listener.onProcess(this, line, sb);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) for (ProcessListener elistener : this.elistener) {
            elistener.onProcess(this, line, sb);
        }
    }

    private void fireEvent(DynByteBuffer buffer, int read, int flag) {
        if (this.isInterrupted()) return;
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) for (ProcessListener listener : this.listener) {
            listener.onBufferChanged(this, buffer, read);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) for (ProcessListener elistener : this.elistener) {
            elistener.onBufferChanged(this, buffer, read);
        }
    }

    public void removeProcessListener(ProcessListener listener, int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) this.listener.remove(listener);
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) this.elistener.remove(listener);

    }

}
