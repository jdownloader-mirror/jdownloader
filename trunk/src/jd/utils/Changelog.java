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

package jd.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import jd.nutils.svn.Subversion;
import jd.parser.Regex;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

public class Changelog {

    /**
     * @param args
     * @throws SVNException
     */
    public static void main(String[] args) throws SVNException {
        new Changelog().load();

    }

    private void load() throws SVNException {

        Subversion svn = new Subversion("https://www.syncom.org/svn/jdownloader/trunk/src/");
        ArrayList<SVNLogEntry> entries = svn.getChangeset(6111, 6193);
        HashMap<String, Change> map = new HashMap<String, Change>();
        for (SVNLogEntry logEntry : entries) {

            System.out.println("---------------------------------------------");
            System.out.println("revision: " + logEntry.getRevision());
            System.out.println("author: " + logEntry.getAuthor());
            System.out.println("date: " + logEntry.getDate());
            System.out.println("log message: " + logEntry.getMessage());

            if (logEntry.getChangedPaths().size() > 0) {
                System.out.println();
                System.out.println("changed paths:");
                Set<?> changedPathsSet = logEntry.getChangedPaths().keySet();

                for (Iterator<?> changedPaths = changedPathsSet.iterator(); changedPaths.hasNext();) {
                    SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
                    
                    System.out.println(" " + entryPath.getType() + " " + entryPath.getPath() + ((entryPath.getCopyPath() != null) ? " (from " + entryPath.getCopyPath() + " revision " + entryPath.getCopyRevision() + ")" : ""));
                    Change c;
                    if (map.containsKey(entryPath.getPath())) {
                        c = map.get(entryPath.getPath());
                    } else {

                        c = new Change(entryPath.getPath());
                        if (!c.getName().contains("test")&&!c.getName().contains("Test")&&c.getCategory()!=null&&c.getCategory().trim().length()>2&&c.getName().trim().length()>3&&logEntry.getMessage() != null && logEntry.getMessage().trim().length() > 0&&!logEntry.getMessage().contains("Merged")&&!logEntry.getMessage().contains("*nochangelog*")) {

                            map.put(entryPath.getPath(), c);

                        }
                    }
                    c.setType(entryPath.getType());
                    c.addRevision(logEntry.getRevision());
                    c.addAuthor(logEntry.getAuthor());

                }

            }
        }
        System.out.println("^Type^Module^Package^Author(s)^Info^");
        for (Iterator<Entry<String, Change>> it = map.entrySet().iterator(); it.hasNext();) {
            Change next = it.next().getValue();

            String changesets = "[[changeset/";
            int i = 0;
            for (Long ref : next.getRevisions()) {
                if (i > 0) changesets += "-";
                changesets += ref;
                i++;
            }
            changesets += "|Details]]";
            changesets = changesets.trim();
            String authors = "";
            for (String a : next.getAuthors()) {
                if (!authors.contains(a)) authors += " " + a;
            }
            authors = authors.trim();
            if(!next.getCategory().equalsIgnoreCase("Decrypter"))            System.out.println("|" + next.getType() + "|" + next.getName() + "|" + next.getCategory() + "|" + authors + "|" + changesets + "|");
        }
    }

    private class Change {
        private ArrayList<Long> revisions;

        public ArrayList<Long> getRevisions() {
            return revisions;
        }

        public void setRevisions(ArrayList<Long> revisions) {
            this.revisions = revisions;
        }

        public ArrayList<String> getAuthors() {
            return authors;
        }

        public void setAuthors(ArrayList<String> authors) {
            this.authors = authors;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getType() {
            switch (type) {
            case 'M':
                return "Update";
            case 'A':
                return "New";
            }
            return type + "";
        }

        private ArrayList<String> authors;
        private char type = 'M';
        private String category = "";
        private String name;

        public Change(String path) {
            this.revisions = new ArrayList<Long>();
            this.authors = new ArrayList<String>();
            String name = new Regex(path, ".*/(.+)").getMatch(0);
            name = name.replaceAll("([a-z0-9])([A-Z][a-z0-9])", "$1 $2").trim();
            name = name.replaceAll("(.*)\\..+", "$1");
            this.name = name;
            this.category = getCategory(path);
            if (category == null) {
                category = "";
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String getCategory(String path) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("/trunk/src/jd/plugins/optional", "Addon");
            map.put("/trunk/src/jd/plugins/decrypt", "Decrypter");
            map.put("/trunk/src/jd/plugins/host", "Hoster");
            map.put("/trunk/src/jd/gui", "GUI");
            map.put("/trunk/src/jd/captcha", "OCR");
            map.put("/trunk/ressourcen/jd/languages", "Translation");
            map.put("/trunk/src/jd/controlling/reconnect", "Reconnect");
            map.put("/trunk/src/jd/controlling", "Controlling");

            while (path != null) {
                path = new Regex(path, "(.*)/.*?").getMatch(0);
                if (map.containsKey(path)) { return map.get(path); }
            }
            return null;
        }

        public void setType(char type) {
            if (type != 'M') {
                this.type = type;
            }

        }

        public void addAuthor(String author) {
            authors.add(author);

        }

        public void addRevision(long revision) {
            revisions.add(revision);

        }

    }
}
