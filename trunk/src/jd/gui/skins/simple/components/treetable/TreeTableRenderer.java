package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableCellRenderer;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class TreeTableRenderer extends DefaultTableCellRenderer {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3912572910439565199L;

    private JLabel            label;

    private JProgressBar      progress;

    private DownloadLink      dLink;

    private FilePackage       fp;

    private DecimalFormat   c= new DecimalFormat("0.00"); ;
    
    TreeTableRenderer() {
        this.label = new JLabel();
        this.label.setOpaque(false);
     
    
        this.progress = new JProgressBar();
        TreeProgressBarUI ui;
        progress.setUI(ui=new TreeProgressBarUI());
        ui.setSelectionBackground(Color.BLACK);
        ui.setSelectionForeground(Color.BLACK);
        progress.setBorderPainted(false);
        progress.setOpaque(false);

    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
       // if (column == DownloadTreeTableModel.COL_PROGRESS) {
            if (value instanceof DownloadLink) {
                dLink = (DownloadLink) value;
                if ( dLink.getRemainingWaittime() == 0 && (int) dLink.getDownloadCurrent() > 0 && ((int) dLink.getDownloadCurrent() <= (int) dLink.getDownloadMax() || dLink.getDownloadMax() < 0)) {

                    if ((int) dLink.getDownloadMax() < 0) {
                        progress.setMaximum(1);
                        progress.setValue(1);
                        progress.setStringPainted(true);
                        progress.setBackground(Color.WHITE);
                        progress.setString(JDUtilities.formatBytesToMB((int) dLink.getDownloadCurrent()));

                    }
                    else {
                        if(!dLink.isInProgress()){
                            progress.setMaximum((int) dLink.getDownloadMax());
                            progress.setStringPainted(true);
                            progress.setForeground(Color.GRAY);
                            progress.setValue((int) dLink.getDownloadCurrent());
                            progress.setString("");
                        
                        }else{
                        progress.setMaximum((int) dLink.getDownloadMax());
                        progress.setStringPainted(true);
                        progress.setForeground(new Color(0x94baff));
                        //progress.setBackground(new Color(255, 0, 0, 80));
                        progress.setValue((int) dLink.getDownloadCurrent());
                        progress.setString(c.format(10000 * progress.getPercentComplete()/100.0) + "% (" + JDUtilities.formatBytesToMB(progress.getValue()) + "/" + JDUtilities.formatBytesToMB(progress.getMaximum()) + ")");
                   
                        }}
                    return progress;
                }
                else if (dLink.getRemainingWaittime() > 0 && dLink.getWaitTime() >= dLink.getRemainingWaittime()) {
                    progress.setMaximum(dLink.getWaitTime());
                    progress.setForeground(new Color(255, 0, 0, 80));
                    progress.setStringPainted(true);
                    progress.setValue((int) dLink.getRemainingWaittime());
                    progress.setString(c.format(10000 * progress.getPercentComplete()/100.0) + "% (" + progress.getValue() / 1000 + "/" + progress.getMaximum() / 1000 + " sek)");
                    return progress;
                }
                return new JLabel();
            }
            else if (value instanceof FilePackage) {
                fp = (FilePackage) value;
                progress.setMaximum(Math.max(1,(int) fp.getEstimatedPackageSize()));
                progress.setStringPainted(true);
                progress.setForeground(new Color(0x2f63ad));
                
                progress.setBackground(Color.BLACK);
                progress.setValue((int) fp.getTotalLoadedPackageBytes());
                progress.setString(c.format(10000 * progress.getPercentComplete()/100.0) + "% (" + JDUtilities.formatBytesToMB(progress.getValue()) + "/" + JDUtilities.formatBytesToMB(progress.getMaximum()) + ")");

                return progress;
            }

       // }
        
      
      
       return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    }
   
}