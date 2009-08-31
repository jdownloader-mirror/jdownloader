package jd.captcha.easy.load;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jd.captcha.easy.EasyMethodeFile;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.dialog.ProgressDialog;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class LoadCaptchas {
    private static final long serialVersionUID = 1L;
    public String host;
    public boolean opendir = false;
    private LoadInfo loadinfo;
    private Browser br = new Browser();
    {
        br.setFollowRedirects(true);
    }
    private ArrayList<LoadImage> images;
    private LoadImage selectedImage;
    private JFrame owner;
    public int maxHeight = 400;
    public int maxWeight = 600;
    /**
     * Ordner in den die Bilder geladen werden (default: jdCaptchaFolder/host)
     * 
     */
    private String dir = null;

    /**
     * start aufrufen um den ladeprozess zu initialisieren
     * 
     * @return
     */
    public LoadCaptchas(JFrame owner) {
        this(owner, null);
    }

    /**
     * 
     * @param hostname
     *            wenn der Hostname = null ist wird er aus dem Link erstellt
     * @return
     */
    public LoadCaptchas(JFrame owner, String host) {
        this(owner, host, false);
    }

    /**
     * 
     * @param hostname
     *            wenn der Hostname = null ist wird er aus dem Link erstellt
     * @param opendir
     *            ob am ende der Captchaordner im Browser geöffnet werden soll
     * @return
     */
    public LoadCaptchas(JFrame owner, String host, boolean opendir) {
        this.host = host;
        this.opendir = opendir;
        this.owner = owner;
    }

    /**
     * @return true wenn erfolgreich geladen wurde
     */
    public boolean start() {
        try {
            selectedImage = LoadImage.loadFile(host);
            loadinfo = getLoadInfo(selectedImage);
            if (loadinfo == null) return false;
            final JDialog dialog = new GuiRunnable<JDialog>() {
                public JDialog runSave() {
                    return new JDialog(owner);
                }
            }.getReturnValue();
            dialog.setModal(true);
            dialog.setAlwaysOnTop(true);
            br.getPage(loadinfo.link);
            if (host == null) {
                host = br.getHost().toLowerCase();
                if (host.matches(".*\\..*\\..*")) host = host.substring(host.indexOf('.') + 1);
            }
            if (dir == null) dir = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + host + "/";
            new File(dir).mkdir();
            if (loadDirect()) {
                if (opendir) openDir(dir);
                new EasyMethodeFile(host).copyExampleImage();
                return true;
            }
            if (selectedImage != null)
                selectedImage.load(host);
            else {
                dialog.setTitle(JDL.L("easycaptcha.loadcaptchas.clickoncaptcha", "click on the captcha"));
                images = getAllImages(br);
                loadImages();
                dialog.addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {
                    }

                    public void windowClosed(WindowEvent e) {

                    }

                    public void windowClosing(WindowEvent e) {
                        for (LoadImage loadImage : images) {
                            loadImage.file.delete();
                        }
                        dialog.dispose();
                    }

                    public void windowDeactivated(WindowEvent e) {
                    }

                    public void windowDeiconified(WindowEvent e) {
                    }

                    public void windowIconified(WindowEvent e) {
                    }

                    public void windowOpened(WindowEvent e) {
                    }
                });

                final ArrayList<JButton> bts = new ArrayList<JButton>();
                for (int j = 0; j < images.size(); j++) {
                    final LoadImage f = images.get(j);
                    if (f == null || f.file == null || !f.file.exists() || f.file.length() < 100) continue;
                    final BufferedImage captchaImage = JDImage.getImage(f.file);
                    if (captchaImage == null) {
                        f.file.delete();
                        continue;
                    }
                    int area = captchaImage.getHeight(null) * captchaImage.getHeight(null);
                    if (area < 50 || captchaImage.getHeight(null) > maxHeight || captchaImage.getWidth(null) > maxWeight || captchaImage.getWidth(null) < 10 || captchaImage.getHeight(null) < 5) {
                        f.file.delete();
                        continue;
                    }
                    double faktor = Math.max((double) captchaImage.getWidth(null) / 100, (double) captchaImage.getHeight(null) / 100);
                    final int width = (int) (captchaImage.getWidth(null) / faktor);
                    final int height = (int) (captchaImage.getHeight(null) / faktor);
                    try {
                        JButton ic = new GuiRunnable<JButton>() {
                            public JButton runSave() {
                                return new JButton(new ImageIcon(captchaImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
                            }
                        }.getReturnValue();
                        ic.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                selectedImage = f;
                                dialog.dispose();
                            }
                        });
                        bts.add(ic);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                final JPanel panel = new GuiRunnable<JPanel>() {
                    public JPanel runSave() {
                        return new JPanel(new GridLayout((int) Math.ceil(((double) bts.size()) / 5), 5));
                    }
                }.getReturnValue();
                for (JButton button : bts) {
                    panel.add(button);

                }
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        dialog.add(new JScrollPane(panel));

                        dialog.pack();
                        dialog.setLocation(Screen.getCenterOfComponent(owner, dialog));
                        dialog.setAlwaysOnTop(true);
                        dialog.setVisible(true);

                        return null;
                    }
                }.waitForEDT();
            }
            if (selectedImage != null && selectedImage.file != null) {
                loadProcess();
                if (opendir) openDir(dir);
                new EasyMethodeFile(host).copyExampleImage();
                return dir.length() > 0;
            } else
                return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Teilt Forms mit mehreren Submitbuttons (wegen Premium und Free Button
     * notwendig)
     * 
     * @param browser
     * @return Form[]
     */
    static Form[] getForms(Browser browser) {
        ArrayList<Form> retForms = new ArrayList<Form>();
        Form[] forms = browser.getForms();
        for (Form form : forms) {
            ArrayList<InputField> fi = form.getInputFieldsByType("submit");
            if (fi.size() > 1) {
                for (int i = 1; i < fi.size(); i++) {
                    Form fo = new Form(form.getHtmlCode());
                    fo.getInputFields().remove(fo.getInputFieldsByType("submit").get(i));
                    if (!retForms.contains(fo)) retForms.add(fo);
                }
                form.getInputFields().remove(fi.get(0));
            }
            if (!retForms.contains(form)) retForms.add(form);
        }
        return retForms.toArray(new Form[] {});
    }

    /**
     * gibt alle Bildadressen einer Seite aus
     * 
     * @param br
     * @return String[]
     * @throws Exception
     */
    static String[] getImages(Browser br) throws Exception {
        ArrayList<String> ret = new ArrayList<String>();
        Pattern[] basePattern = new Pattern[] { Pattern.compile("(?is)<[ ]?input[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s][^>]*?type=.?image[^>]*?", Pattern.CASE_INSENSITIVE), Pattern.compile("(?is)<[ ]?input[^>]*?type=.?image[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE), Pattern.compile("(?is)<[ ]?IMG[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE) };
        for (Pattern element : basePattern) {
            Matcher m = element.matcher(br.toString());
            while (m.find()) {
                try {
                    String src = m.group(1);
                    if (!src.startsWith("http")) {
                        if (src.charAt(0) == '/') {
                            src = "http://" + br.getHost() + src;
                        } else if (src.charAt(0) == '#') {
                            src = "http://" + br.getURL() + src;
                        } else {
                            src = br.getBaseURL() + src;
                        }
                    }
                    if (!ret.contains(src)) ret.add(src);
                } catch (Exception e) {
                }

            }
        }

        return ret.toArray(new String[] {});
    }

    /**
     * öffnet einen Ordner
     * 
     * @param dir
     */
    private static void openDir(final String dir) {
        new GuiRunnable<Object>() {
            public Object runSave() {
                if (JOptionPane.showConfirmDialog(null, "Captcha Ordner:" + dir + " jetzt öffnen?") == JOptionPane.YES_OPTION) JDUtilities.openExplorer(new File(dir));

                return null;
            }
        }.waitForEDT();

    }

    /**
     * Dialog fragt nach dem Link und der anzahl der zu ladenden Captchas
     * 
     * @return
     */
    private LoadInfo getLoadInfo(final LoadImage loadImage) {
        final JDialog dialog = new GuiRunnable<JDialog>() {
            public JDialog runSave() {
                return new JDialog(owner);
            }
        }.getReturnValue();
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);

        final JPanel p = new GuiRunnable<JPanel>() {
            public JPanel runSave() {
                JPanel ret = new JPanel(new GridLayout(5, 2));
                ret.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.link", "Link") + ":"));
                return ret;

            }
        }.getReturnValue();

        final JDTextField tfl = new GuiRunnable<JDTextField>() {
            public JDTextField runSave() {
                return new JDTextField();
            }
        }.getReturnValue();
        tfl.setBorder(BorderFactory.createEtchedBorder());

        p.add(tfl);
        JSpinner sm = new GuiRunnable<JSpinner>() {
            public JSpinner runSave() {
                p.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.howmuch", "How much captchas you need") + ":"));
                if(loadImage!=null)
                    tfl.setText(loadImage.baseUrl);
                return new JSpinner(new SpinnerNumberModel(100, 1, 4000, 1));
            }
        }.getReturnValue();
        p.add(sm);
        JCheckBox followLinks = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                p.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.followlinks", "follow normal Links (very slow)") + ":"));
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(false);
                p.add(checkBox);
                return checkBox;
            }
        }.getReturnValue();
        JCheckBox loadDirect = new GuiRunnable<JCheckBox>() {
            public JCheckBox runSave() {
                p.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.loaddirect", "Load direct if possible (much faster)") + ":"));
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(true);
                p.add(checkBox);
                return checkBox;
            }
        }.getReturnValue();
        JButton ok = new GuiRunnable<JButton>() {
            public JButton runSave() {
                return new JButton(JDL.L("gui.btn_ok", "OK"));
            }
        }.getReturnValue();
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        p.add(ok);
        WindowListener l = new WindowListener() {
            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {

            }

            public void windowClosing(WindowEvent e) {
                tfl.setText("");
                dialog.dispose();
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }
        };
        JButton cancel = new GuiRunnable<JButton>() {
            public JButton runSave() {
                return new JButton(JDL.L("gui.btn_cancel", "Cancel"));
            }
        }.getReturnValue();
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                tfl.setText("");
                dialog.dispose();
            }
        });
        p.add(cancel);

        dialog.addWindowListener(l);
        dialog.add(p);
        new GuiRunnable<Object>() {
            public Object runSave() {
                dialog.setLocation(Screen.getCenterOfComponent(owner, dialog));
                dialog.pack();
                dialog.setAlwaysOnTop(true);

                dialog.setVisible(true);

                return null;
            }
        }.waitForEDT();
        String link = tfl.getText();
        if (link == null || link.matches("\\s*")) return null;
        int menge = (Integer) sm.getValue();
        dialog.dispose();
        LoadInfo retLI = new LoadInfo(link, menge);
        retLI.followLinks = followLinks.isSelected();
        retLI.directLoad = loadDirect.isSelected();
        return retLI;

    }

    /**
     * gibt die Bildendung die im Header steht zurück z.B.: .jpg
     * 
     * @param br
     * @return
     */
    private static String getImageExtentionFromHeader(Browser br) {
        String ret = null;
        String contentType = br.getHttpConnection().getContentType();

        if (contentType != null && contentType.toLowerCase().contains("image")) {
            if (contentType.toLowerCase().equals("image/jpeg"))
                ret = ".jpg";
            else {
                ret = contentType.toLowerCase().replaceFirst("image/", ".");
            }
        }
        return ret;
    }

    /**
     * läd direkt wenn die URL ein Bild ist
     * 
     * @param dir
     * @param br
     * @param loadinfo
     * @return true wenn die url ein Bild ist
     */
    private boolean loadDirect() {

        final String imageType = getImageExtentionFromHeader(br);
        if (imageType != null) {
            final ProgressDialog pd = new GuiRunnable<ProgressDialog>() {
                public ProgressDialog runSave() {

                    return new ProgressDialog(owner, JDL.L("easycaptcha.loadcaptchas.loadimages", "load images please wait"), null, false, true);
                }
            }.getReturnValue();
            pd.setAlwaysOnTop(true);
            Runnable runnable = new Runnable() {
                public void run() {
                    for (int k = 0; k < loadinfo.menge; k++) {
                        try {
                            File f2 = new File(dir + System.currentTimeMillis() + imageType);
                            br.getDownload(f2, loadinfo.link);
                            final int c = k;
                            new GuiRunnable<Object>() {
                                public Object runSave() {
                                    pd.setValue(c);
                                    return null;
                                }
                            }.waitForEDT();
                        } catch (Exception ev) {
                            ev.printStackTrace();
                        }

                    }
                    pd.dispose();
                }
            };
            final Thread th = new Thread(runnable);
            th.start();
            new GuiRunnable<Object>() {
                public Object runSave() {
                    pd.setThread(th);
                    pd.setVisible(true);
                    return null;
                }
            }.waitForEDT();

            return true;
        }
        return false;
    }

    /**
     * LoadImageliste einer Seite (folgt Forms)
     * 
     * @param br
     * @return ArrayList<LoadImage>
     */
    private ArrayList<LoadImage> getAllImages(Browser br) {
        ArrayList<LoadImage> images = new ArrayList<LoadImage>();
        String[] imagea;
        try {
            imagea = getImages(br);
            for (int i = 0; i < imagea.length; i++) {
                LoadImage li = new LoadImage(loadinfo, imagea[i], br);
                li.form = -1;
                li.location = i;
                images.add(li);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loadinfo.followLinks) {
            String[] links = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            for (int b = 0; b < links.length; b++) {
                String string = links[b];
                try {
                    Browser brc = br.cloneBrowser();
                    brc.getPage(string);
                    imagea = getImages(brc);
                    for (int i = 0; i < imagea.length; i++) {
                        LoadImage li = new LoadImage(loadinfo, imagea[i], brc);
                        li.form = -1;
                        li.location = i;
                        li.followUrl = b;
                        images.add(li);
                    }

                } catch (Exception e) {
                }
            }

        }

        Form[] forms = getForms(br);
        for (int i = 0; i < forms.length; i++) {
            try {
                Form form = forms[i];
                Browser brc = br.cloneBrowser();

                brc.submitForm(form);

                imagea = getImages(brc);
                for (int b = 0; b < imagea.length; b++) {

                    LoadImage li = new LoadImage(loadinfo, imagea[b], brc);
                    li.form = i;
                    li.location = b;
                    if (images.contains(li)) continue;
                    images.add(li);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return images;
    }

    /**
     * Läd die Bilder eines ArrayList<LoadImage> und zeigt den Fortschritt in
     * einem Progressdialog an
     * 
     */
    private void loadImages() {
        final ProgressDialog pd = new GuiRunnable<ProgressDialog>() {
            public ProgressDialog runSave() {

                return new ProgressDialog(owner, JDL.L("easycaptcha.loadcaptchas.loadimages", "load images please wait"), null, false, true);
            }
        }.getReturnValue();
        pd.setAlwaysOnTop(true);
        final Thread th = new Thread(new Runnable() {
            public void run() {
                final Thread[] jb = new Thread[images.size()];
                for (int j = 0; j < images.size(); j++) {
                    final int i = j;
                    jb[i] = new Thread(new Runnable() {

                        public void run() {
                            LoadImage image = images.get(i);
                            image.directCaptchaLoad(dir);
                            synchronized (jb[i]) {
                                jb[i].notify();
                            }
                        }
                    });
                    jb[i].start();
                }
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        pd.setMaximum(images.size());
                        return null;
                    }
                }.waitForEDT();

                int c = 0;
                for (Thread thread : jb) {
                    while (thread.isAlive()) {
                        synchronized (thread) {
                            try {
                                thread.wait(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    final int d = c++;
                    new GuiRunnable<Object>() {
                        public Object runSave() {
                            pd.setValue(d);
                            return null;
                        }
                    }.waitForEDT();
                }
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        pd.dispose();
                        return null;
                    }
                }.waitForEDT();
            }
        });

        th.start();
        new GuiRunnable<Object>() {
            public Object runSave() {
                pd.setThread(th);
                pd.setVisible(true);
                return null;
            }
        }.waitForEDT();
    }

    private void loadProcess() {

        final ProgressDialog pd = new GuiRunnable<ProgressDialog>() {
            public ProgressDialog runSave() {

                return new ProgressDialog(owner, JDL.L("easycaptcha.loadcaptchas.loadimages", "load images please wait"), null, false, true);
            }
        }.getReturnValue();
        pd.setAlwaysOnTop(true);
        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    if(images!=null)
                    {
                    for (LoadImage loadImage : images) {
                        if (!loadImage.file.equals(selectedImage.file)) loadImage.file.delete();
                    }
                    }
                    boolean direct = selectedImage.directCaptchaLoad(dir);
                    LoadImage.save(selectedImage, host);
                    new GuiRunnable<Object>() {
                        public Object runSave() {
                            pd.setValue(1);
                            return null;
                        }
                    }.waitForEDT();
                    if (direct && loadinfo.directLoad) {
                        for (int k = 1; k < loadinfo.menge - 1; k++) {
                            selectedImage.directCaptchaLoad(dir);
                            final int d = k;
                            new GuiRunnable<Object>() {
                                public Object runSave() {
                                    pd.setValue(d);
                                    return null;
                                }
                            }.waitForEDT();
                        }
                    } else {
                        selectedImage.file.delete();
                        for (int k = 1; k < loadinfo.menge - 1; k++) {
                            selectedImage.load(host);
                            final int d = k;
                            new GuiRunnable<Object>() {
                                public Object runSave() {
                                    pd.setValue(d);
                                    return null;
                                }
                            }.waitForEDT();

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new GuiRunnable<Object>() {
                    public Object runSave() {
                        pd.dispose();
                        return null;
                    }
                }.waitForEDT();
            }
        };
        new GuiRunnable<Object>() {
            public Object runSave() {
                Thread th2 = new Thread(runnable);
                th2.start();
                pd.setMaximum(loadinfo.menge - 1);
                pd.setThread(th2);
                pd.setVisible(true);
                return null;
            }
        }.waitForEDT();

    }

}