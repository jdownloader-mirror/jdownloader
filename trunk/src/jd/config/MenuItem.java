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

import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.plugins.Plugin;

public class MenuItem extends Property {
    public static final int CONTAINER = 0;
    public static final int NORMAL = 1;
    public static final int SEPARATOR = 3;
    /**
     * 
     */
    private static final long serialVersionUID = 9205555751462125274L;
    public static final int TOGGLE = 2;
    private int actionID;
    private ActionListener actionListener;
    private boolean enabled = true;
    private int id = NORMAL;
    private ArrayList<MenuItem> items;
    private Plugin plugin;
    private boolean selected;
    private String title;
    String accelerator = null;
    private ImageIcon icon;

    public MenuItem(int id) {
        this(id, null, -1);
    }

    public MenuItem(int id, String title, int actionID) {
        this.id = id;
        this.actionID = actionID;
        this.title = title;
    }

    public MenuItem(String title, int actionID) {
        this(NORMAL, title, actionID);
    }

    public void setAccelerator(String accelerator) {
        this.accelerator = accelerator;
    }

    public String getAccelerator() {
        return this.accelerator;
    }

    public void addMenuItem(MenuItem m) {
        if (id != CONTAINER) {
            logger.severe("I am not a Container MenuItem!!");
        }
        if (items == null) {
            items = new ArrayList<MenuItem>();
        }
        items.add(m);

    }

    public MenuItem get(int i) {
        if (items == null) { return null; }
        return items.get(i);
    }

    public int getActionID() {

        return actionID;
    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public int getID() {

        return id;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public int getSize() {

        if (items == null) { return 0; }
        return items.size();
    }

    public String getTitle() {
        return title;
    }

    public boolean isEnabled() {

        return enabled;
    }

    public boolean isSelected() {

        return selected;
    }

    public MenuItem setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public MenuItem setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MenuItem setItems(ArrayList<MenuItem> createMenuitems) {
        items = createMenuitems;
        return this;

    }

    public MenuItem setPlugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public MenuItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public MenuItem setIcon(ImageIcon ii) {
        this.icon=ii;
        return this;
        
    }

    public ImageIcon getIcon() {
        return icon;
    }

}