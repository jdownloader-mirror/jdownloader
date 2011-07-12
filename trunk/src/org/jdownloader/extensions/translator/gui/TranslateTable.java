package org.jdownloader.extensions.translator.gui;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.translator.TranslateEntry;

/**
 * Table for all entries
 * 
 * @author thomas
 * 
 */
public class TranslateTable extends BasicJDTable<TranslateEntry> {

    public TranslateTable(TranslateTableModel tableModel) {
        super(tableModel);
        this.setSearchEnabled(true);

    }

    // @Override
    // protected void onDoubleClick(MouseEvent e, TranslateEntry obj) {
    // super.onDoubleClick(e, obj);
    // }

}
