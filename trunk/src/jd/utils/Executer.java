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

package jd.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Executer extends Thread {
    class StreamObserver extends Thread {

        private BufferedReader reader;
        private StringBuffer sb;
        private boolean started;

        public StreamObserver(InputStream stream, StringBuffer sb) {
            reader = new BufferedReader(new InputStreamReader(stream));
            this.sb = sb;
        }

        @Override
        public void run() {
            this.started = true;
            String line;
            try {
                while ((line = readLine(reader)) != null) {
                    if (line.length() > 0) {
                        fireEvent(line, sb);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }

        private String readLine(BufferedReader reader2) throws IOException, InterruptedException {
            StringBuffer s = new StringBuffer();
            char[] buffer = new char[1];
            for (;;) {

                if (this.isInterrupted()) {

                throw new InterruptedException(); }

                if (reader2.read(buffer) < 0) { return s.length() == 0 ? null : s.toString(); }
                sb.append(buffer);
                fireEvent(sb);
                if (buffer[0] == '\b' || buffer[0] == '\r' || buffer[0] == '\n') {

                    if (s.length() > 0) return s.toString();
                } else {
                    s.append(buffer);
                }
            }

        }

        public boolean isStarted() {
            return started;
        }

    }

    private String command;
    private Logger logger = JDUtilities.getLogger();
    private ArrayList<String> parameter;
    private String runIn;
    private StringBuffer inputStreamBuffer;
    private StringBuffer errorStreamBuffer;
    private ArrayList<ProcessListener> listener = new ArrayList<ProcessListener>();
    private int waitTimeout = 60;
    private int exitValue = -1;

    private Process process;
    private StreamObserver sbeObserver;
    private StreamObserver sbObserver;

    public Executer(String command) {
        this.command = command;
        parameter = new ArrayList<String>();
        inputStreamBuffer = new StringBuffer();
        errorStreamBuffer = new StringBuffer();
        setName("Executer: " + command);

    }

    public void addParameter(String par) {
        parameter.add(par);
    }

    public void addParameters(String[] par) {
        for (String p : par) {
            parameter.add(p);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getErrorStream() {
        return errorStreamBuffer.toString();
    }

    public ArrayList<String> getParameter() {
        return parameter;
    }

    public String getRunin() {
        return runIn;
    }

    public String getStream() {
        return inputStreamBuffer.toString();
    }

    public int getWaitTimeout() {
        return waitTimeout;
    }

    @Override
    public void run() {
        if (command == null || command.trim().length() == 0) {
            logger.severe("Execute Parameter error: No Command");
            return;
        }

        ArrayList<String> params = new ArrayList<String>();
        params.add(command);
        params.addAll(parameter);

        
        String out="";
        for(String p:params){
            out+=p+" ";
        }
        System.out.println(out + "");
        ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[] {}));
//        List<String> g = pb.command();
//        pb.command(out);
//        g = pb.command();
       
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                if (new File(params.get(0)).getParentFile().exists()) {
                    // logger.info("Run in: " + new
                    // File(params.get(0)).getParentFile());
                    pb.directory(new File(params.get(0)).getParentFile());
                } else {
                    logger.severe("Working drectory " + runIn + " does not exist!");
                }
            }
        }

        try {
          
            process = pb.start();

            if (waitTimeout == 0) { return; }
            sbeObserver = new StreamObserver(process.getErrorStream(), errorStreamBuffer);
            sbObserver = new StreamObserver(process.getInputStream(), inputStreamBuffer);
            sbeObserver.start();
            sbObserver.start();

            long waiter = System.currentTimeMillis() + waitTimeout * 1000;
            if (waitTimeout < 0) waiter = Long.MAX_VALUE;
            while ((!sbObserver.isStarted() || !sbeObserver.isStarted()) || (waiter > System.currentTimeMillis() && (sbeObserver.isAlive() || sbObserver.isAlive()))) {

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }

            }
            if (sbObserver.isAlive()) {
                logger.severe("Timeout " + waitTimeout + " kill observerthread(input)");
                sbObserver.interrupt();
            }
            if (sbeObserver.isAlive()) {
                logger.severe("Timeout " + waitTimeout + " kill observerthread(error)");
                sbeObserver.interrupt();
            }
           logger.finer("Destroy Subprocesses if still running.");
            try {
                process.destroy();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            exitValue = process.exitValue();

           
        } catch (IOException e1) {

            e1.printStackTrace();
            return;
        }
    }

    public Process getProcess() {
        return process;
    }

    public void interrupt() {
        super.interrupt();
        if (sbeObserver != null) this.sbeObserver.interrupt();
        if (sbObserver != null) this.sbObserver.interrupt();
        process.destroy();
    }

    public StringBuffer getInputStreamBuffer() {
        return inputStreamBuffer;
    }

    public StringBuffer getErrorStreamBuffer() {
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
    }

    public int getExitValue() {
        return exitValue;
    }

    public void addProcessListener(ProcessListener listener) {
        this.removeProcessListener(listener);
        this.listener.add(listener);

    }

    private void fireEvent(String line, StringBuffer sb) {

        for (ProcessListener listener : this.listener) {
            listener.onProcess(this, line, sb);
        }
    }

    private void fireEvent(StringBuffer sb) {
        for (ProcessListener listener : this.listener) {
            listener.onBufferChanged(this, sb);
        }

    }

    private void removeProcessListener(ProcessListener listener) {
        this.listener.remove(listener);

    }

}
