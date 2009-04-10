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

package jd.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class DatabaseConnector implements Serializable {

    private static final long serialVersionUID = 8074213660382482620L;

    private static Logger logger = jd.controlling.JDLogger.getLogger();

    private static String configpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";

    private HashMap<String, Object> dbdata = new HashMap<String, Object>();

    private static Connection con = null;

    static {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    /**
     * Constructor
     */
    @SuppressWarnings("unused")
    public DatabaseConnector() {
        try {
            if (con != null) return;
            logger.finer("Loading database");

            if (!checkDatabaseHeader()) {
                logger.severe("Database broken! Creating fresh Database");
                if (!new File(configpath + "database.script").delete()) {
                    logger.severe("Could not delete broken Database");
                    JOptionPane.showMessageDialog(null, "Could not delete broken database. Please remove the JD_HOME/config directory and restart JD");
                    System.exit(1);

                }
            }

            con = DriverManager.getConnection("jdbc:hsqldb:file:" + configpath + "database;shutdown=true", "sa", "");
            con.setAutoCommit(true);
            con.createStatement().executeUpdate("SET LOGSIZE 1");

            if (!new File(configpath + "database.script").exists()) {
                logger.finer("No configuration database found. Creating new one.");

                con.createStatement().executeUpdate("CREATE TABLE config (name VARCHAR(256), obj OTHER)");
                con.createStatement().executeUpdate("CREATE TABLE links (name VARCHAR(256), obj OTHER)");

                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                logger.finer("Starting database wrapper");

                File f = null;
                for (String tmppath : new File(configpath).list()) {
                    try {
                        if (tmppath.endsWith(".cfg")) {
                            logger.finest("Wrapping " + tmppath);

                            Object props = JDIO.loadObject(null, f = JDUtilities.getResourceFile("config/" + tmppath), false);

                            if (props != null) {
                                pst.setString(1, tmppath.split(".cfg")[0]);
                                pst.setObject(2, props);
                                pst.execute();
                            }
                        }
                    } catch (Exception e) {
                        jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                    }
                }
            }

        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
    }

    /**
     * Checks the database of inconsistency
     */
    private boolean checkDatabaseHeader() {
        logger.finer("Checking database");
        File f = new File(configpath + "database.script");
        if (!f.exists()) return true;
        boolean databaseok = true;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = "";
            int counter = 0;

            while (counter < 7) {
                line = in.readLine();

                switch (counter) {
                case 0:
                    if (!line.equals("CREATE SCHEMA PUBLIC AUTHORIZATION DBA")) {
                        databaseok = false;
                    }
                    break;
                case 1:
                    if (!line.equals("CREATE MEMORY TABLE CONFIG(NAME VARCHAR(256),OBJ OBJECT)")) {
                        databaseok = false;
                    }
                    break;
                case 2:
                    if (!line.equals("CREATE MEMORY TABLE LINKS(NAME VARCHAR(256),OBJ OBJECT)")) {
                        databaseok = false;
                    }
                    break;
                case 3:
                    if (!line.equals("CREATE USER SA PASSWORD \"\"")) {
                        databaseok = false;
                    }
                    break;
                case 4:
                    if (!line.equals("GRANT DBA TO SA")) {
                        databaseok = false;
                    }
                    break;
                case 5:
                    if (!line.equals("SET WRITE_DELAY 10")) {
                        databaseok = false;
                    }
                    break;
                case 6:
                    if (!line.equals("SET SCHEMA PUBLIC")) {
                        databaseok = false;
                    }
                    break;
                }
                counter++;
            }

            while (((line = in.readLine()) != null)) {
                if (!line.matches("INSERT INTO .*? VALUES\\('.*?','.*?'\\)")) {
                    databaseok = false;
                    break;
                }
            }
            in.close();
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        return databaseok;
    }

    /**
     * Returns a configuration
     */
    public synchronized Object getData(String name) {
        Object ret = dbdata.get(name);
        try {
            if (ret == null) {
                // try to init the table
                ResultSet rs = con.createStatement().executeQuery("SELECT * FROM config WHERE name = '" + name + "'");
                if (rs.next()) {
                    ret = rs.getObject(2);
                    dbdata.put(rs.getString(1), ret);
                }

            }
        } catch (Exception e) {
          
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }

        return ret;
    }

    /**
     * Saves a configuration into the database
     */
    public void saveConfiguration(String name, Object data) {
        dbdata.put(name, data);
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(name) FROM config WHERE name = '" + name + "'");
            rs.next();
            if (rs.getInt(1) > 0) {
                PreparedStatement pst = con.prepareStatement("UPDATE config SET obj = ? WHERE name = '" + name + "'");
                pst.setObject(1, data);
                pst.execute();
            } else {
                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                pst.setString(1, name);
                pst.setObject(2, data);
                pst.execute();
            }
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
    }

    /**
     * Shutdowns the database
     */
    public void shutdownDatabase() {
        try {

            con.close();
        } catch (SQLException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
    }

    /**
     * Returns the saved linklist
     */
    public Object getLinks() {
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM links");
            rs.next();
            return rs.getObject(2);
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        return null;
    }

    /**
     * Saves the linklist into the database
     */
    public void saveLinks(Object obj) {
        try {
            if (getLinks() == null) {
                PreparedStatement pst = con.prepareStatement("INSERT INTO links VALUES (?,?)");
                pst.setString(1, "links");
                pst.setObject(2, obj);
                pst.execute();
            } else {
                PreparedStatement pst = con.prepareStatement("UPDATE links SET obj=? WHERE name='links'");
                pst.setObject(1, obj);
                pst.execute();
            }
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
    }

    /**
     * Returns the connection to the database
     */
    public Connection getDatabaseConnection() {
        return con;
    }
}