package jd.wizardry7.view;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class _WizardDialog extends JFrame {

    private static final long serialVersionUID = 2135079888883294152L;

    public _WizardDialog() {
        super("JDownloader Settings Wizard");
        setPreferredSize(new Dimension(550, 700));
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
