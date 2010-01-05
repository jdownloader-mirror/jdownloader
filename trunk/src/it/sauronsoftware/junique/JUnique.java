/*
 * JUnique - Helps in preventing multiple instances of the same application
 * 
 * Copyright (C) 2008 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.junique;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import jd.utils.JDUtilities;

/**
 * Point-of-entry of the JUnique library.
 * 
 * @author Carlo Pelliccia
 */
public final class JUnique {

    /**
     * The directory where lock files are stored.
     */
    private static final File LOCK_FILES_DIR = JDUtilities.getResourceFile(".junique");

    /**
     * A global lock file, to perform extra-JVM lock operations.
     */
    private static final File GLOBAL_LOCK_FILE = new File(LOCK_FILES_DIR, "global.lock");

    /**
     * The global file channel.
     */
    private static FileChannel globalFileChannel = null;

    /**
     * The global file lock.
     */
    private static FileLock globalFileLock = null;

    /**
     * Locks table. Normalized IDs are placed in the key side, while the value
     * is a {@link Lock} object representing the lock details.
     */
    private static final Hashtable<String, Lock> LOCKS = new Hashtable<String, Lock>();

    static {
        // Creates the lock files dir, if it yet doesn't exist.
        if (!LOCK_FILES_DIR.exists()) {
            LOCK_FILES_DIR.mkdirs();
        }
        // Adds a shutdown hook releasing any unreleased lock at JVM shutdown.
        // Runtime rt = Runtime.getRuntime();
        // rt.addShutdownHook(new Thread(new ShutdownHook()));
        /*
         * do not use shutdownhook because it can lockup java instances (new
         * java instances locks, while old one wants to clean up but cant get
         * lock = deadlock)
         */
    }

    /**
     * This method tries to acquire a lock in the user-space for a given ID.
     * 
     * @param id
     *            The lock ID.
     * @throws AlreadyLockedException
     *             If the lock cannot be acquired, since it has been already
     *             taken in the user-space.
     */
    public static void acquireLock(final String id) throws AlreadyLockedException {
        acquireLock(id, null);
    }

    /**
     * This method tries to acquire a lock in the user-space for a given ID.
     * 
     * @param id
     *            The lock ID.
     * @param messageHandler
     *            An optional message handler that will be used after the lock
     *            has be acquired to handle incoming messages on the lock
     *            channel.
     * @throws AlreadyLockedException
     *             If the lock cannot be acquired, since it has been already
     *             taken in the user-space.
     */
    public static void acquireLock(final String id, final MessageHandler messageHandler) throws AlreadyLockedException {
        // Some usefull references.
        final File lockFile;
        final File portFile;
        final FileChannel fileChannel;
        final FileLock fileLock;
        final Server server;
        // ID normalization.
        final String nid = normalizeID(id);
        // Locks JUnique.
        j_lock();
        try {
            // Gets file paths.
            lockFile = getLockFileForNID(nid);
            portFile = getPortFileForNID(nid);
            // Tries to open the lock file in write mode.
            LOCK_FILES_DIR.mkdirs();
            try {
                final RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
                fileChannel = raf.getChannel();
                fileLock = fileChannel.tryLock();
                if (fileLock == null) {
                    // The file is already locked.
                    throw new AlreadyLockedException(id);
                }
            } catch (Throwable t) {
                // The file cannot be locked.
                throw new AlreadyLockedException(id);
            }
            // Starts a lock server for this lock.
            server = new Server(id, messageHandler);
            // The lock has been taken. Let's remember it!
            Lock lock = new Lock(id, lockFile, portFile, fileChannel, fileLock, server);
            LOCKS.put(nid, lock);
            // Starts the lock server.
            server.start();
            // Writes the port file.
            Writer portWriter = null;
            try {
                portWriter = new FileWriter(portFile);
                portWriter.write(String.valueOf(server.getListenedPort()));
                portWriter.flush();
            } catch (Throwable t) {
            } finally {
                if (portWriter != null) {
                    try {
                        portWriter.close();
                    } catch (Throwable t) {
                    }
                }
            }
        } finally {
            // Releases the lock on JUnique.
            j_unlock();
        }
    }

    /**
     * It releases a previously acquired lock on an ID. Please note that a lock
     * can be realeased only by the same JVM that has previously acquired it. If
     * the given ID doens't correspond to a lock that belongs to the current
     * JVM, no action will be taken.
     * 
     * @param id
     *            The lock ID.
     */
    public static void releaseLock(final String id) {
        // ID normalization.
        final String nid = normalizeID(id);
        // Locks JUnique.
        j_lock();
        try {
            // Searches for the Lock instance.
            final Lock lock = LOCKS.remove(nid);
            // Is it ok?
            if (lock != null) {
                releaseLock(lock);
            }
        } finally {
            // Unlocks JUnique.
            j_unlock();
        }
    }

    /**
     * Internal lock release routine.
     * 
     * @param lock
     *            The lock to release.
     */
    private static void releaseLock(final Lock lock) {
        // Shuts down the lock server.
        lock.getServer().stop();
        // Releases the locked resources.
        try {
            lock.getLockFileLock().release();
        } catch (Throwable t) {
        }
        try {
            lock.getLockFileChannel().close();
        } catch (Throwable t) {
        }
        // Deletes the port file.
        lock.getPortFile().delete();
        // Deletes the lock file.
        lock.getLockFile().delete();
    }

    /**
     * It sends a message to the JVM process that has previously locked the
     * given ID. The message will be delivered only if the lock for the given ID
     * has been actually acquired, and only if who has acquired it is interested
     * in message handling.
     * 
     * @param id
     *            The lock ID.
     * @param message
     *            The message.
     * @return A response for the message. It returns null if the message cannot
     *         be delivered. It returns an empty string if the message has been
     *         delivered but the recipient hasn't supplied a response for it.
     */
    public static String sendMessage(final String id, final String message) {
        int port = -1;
        // Locks JUnique.
        j_lock();
        try {
            // ID normalization.
            final String nid = normalizeID(id);
            // Port file.
            final File portFile = getPortFileForNID(nid);
            // Tries to acquire the port number.
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(portFile));
                final String line = reader.readLine();
                if (line != null) {
                    port = Integer.parseInt(line);
                }
            } catch (Throwable t) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable t) {
                    }
                }
            }
        } finally {
            // Unlocks JUnique.
            j_unlock();
        }
        // A place holder for the response.
        String response = null;
        // Is the port number ok?
        if (port > 0) {
            // Ok, let's try a client connection.
            Socket socket = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                socket = new Socket(localhost, port);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                Message.write(message, outputStream);
                response = Message.read(inputStream);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable t) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable t) {
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable t) {
                    }
                }
            }
        }
        // Ok.
        return response;
    }

    /**
     * It returns a "normalized" version of an ID.
     * 
     * @param id
     *            The source ID.
     * @return The normalized ID.
     */
    private static String normalizeID(final String id) {
        final int hashcode = id.hashCode();
        final boolean positive = hashcode >= 0;
        final long longcode = positive ? (long) hashcode : -(long) hashcode;
        final StringBuffer hexstring = new StringBuffer(Long.toHexString(longcode));
        while (hexstring.length() < 8) {
            hexstring.insert(0, '0');
        }
        if (positive) {
            hexstring.insert(0, '0');
        } else {
            hexstring.insert(0, '1');
        }
        return hexstring.toString();
    }

    /**
     * It returns the lock file associated to a normalized ID.
     * 
     * @param nid
     *            The normalized ID.
     * @return The lock file for this normalized ID.
     */
    private static File getLockFileForNID(final String nid) {
        final String filename = normalizeID(nid) + ".lock";
        return new File(LOCK_FILES_DIR, filename);
    }

    /**
     * It returns the port file associated to a normalized ID.
     * 
     * @param nid
     *            The corresponding normalized ID.
     * @return The port file for this normalized ID.
     */
    private static File getPortFileForNID(final String nid) {
        final String filename = normalizeID(nid) + ".port";
        return new File(LOCK_FILES_DIR, filename);
    }

    /**
     * This one performs a cross-JVM lock on all JUnique instances. Calling this
     * lock causes the acquisition of an exclusive extra-JVM access to JUnique
     * file system resources.
     */
    private static void j_lock() {
        do {
            LOCK_FILES_DIR.mkdirs();
            try {
                final RandomAccessFile raf = new RandomAccessFile(GLOBAL_LOCK_FILE, "rw");
                final FileChannel channel = raf.getChannel();
                final FileLock lock = channel.lock();
                globalFileChannel = channel;
                globalFileLock = lock;
                break;
            } catch (Throwable t) {
            }
        } while (true);
    }

    /**
     * Release a previously acquired extra-JVM JUnique lock.
     */
    private static void j_unlock() {
        final FileChannel channel = globalFileChannel;
        final FileLock lock = globalFileLock;
        globalFileChannel = null;
        globalFileLock = null;
        try {
            lock.release();
        } catch (Throwable t) {
        }
        try {
            channel.close();
        } catch (Throwable t) {
        }
    }

    /**
     * Some shutdown hook code, releasing any unreleased lock on JVM regular
     * shutdown.
     */
    @SuppressWarnings("unused")
    private static class ShutdownHook implements Runnable {

        public void run() {
            // Cross-JVM lock.
            System.out.println("Release Instance: LOCK");
            j_lock();
            try {
                // Collects nids.
                final ArrayList<String> nids = new ArrayList<String>();
                for (final Enumeration<String> e = LOCKS.keys(); e.hasMoreElements();) {
                    final String nid = e.nextElement();
                    nids.add(nid);
                }
                // Releases any unreleased lock.
                for (final Iterator<String> i = nids.iterator(); i.hasNext();) {
                    final String nid = i.next();
                    final Lock lock = LOCKS.remove(nid);
                    releaseLock(lock);
                }
            } finally {
                // Releases the cross-JVM lock.
                j_unlock();
            }
            System.out.println("Release Instance: UNLOCK");
        }

    }

}
