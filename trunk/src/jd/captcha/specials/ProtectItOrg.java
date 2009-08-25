package jd.captcha.specials;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;

import javax.imageio.ImageIO;

import jd.captcha.ColorLetterComperator;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.JDImage;

public class ProtectItOrg {
    static ArrayList<PixelObject> getObjects(PixelGrid grid, int neighbourradius) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] == -3151923) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, neighbourradius, neighbourradius)) {
                        merge.add(o);
                    }
                }
                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }

            }
        }

        return ret;
    }

    @SuppressWarnings("unused")
    private static void loadImagesIfNotExists(String code, Captcha captcha) {
        if (!captcha.owner.getResourceFile("images/r/" + code + ".png").exists()) {
            captcha.reset();
            BackGroundImageManager bgit = new BackGroundImageManager(captcha);
            bgit.clearCaptchaAll();
            captcha.crop(0, 0, 0, 10);
            ArrayList<PixelObject> obj = getObjects(captcha, 7);

            for (PixelObject pixelObject : obj) {
                Image img = pixelObject.toColoredLetter().getImage();
                File retf = captcha.owner.getResourceFile("images/" + code + System.currentTimeMillis() + ".png");
                try {
                    ImageIO.write((RenderedImage) img, "png", retf);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

    public static Letter[] getLetters(Captcha captcha) throws Exception {
        captcha.crop(257, 140, 0, 0);
        ArrayList<Letter> let = new ArrayList<Letter>();
        Vector<PixelObject> objs = captcha.getObjects(0.7, 0.7);
        // EasyCaptcha.mergeObjectsBasic(objs, captcha, 3);
        Collections.sort(objs);
        for (PixelObject pixelObject : objs) {
            if (pixelObject.getArea() > 3) let.add(pixelObject.toLetter());
        }
        int c = 0;
        for (ListIterator<Letter> iterator = let.listIterator(let.size()); iterator.hasPrevious() && c++ < 10;) {
            iterator.previous();
            iterator.remove();
        }
        Letter[] lets = let.toArray(new Letter[] {});
        String code = "";
        for (Letter letter : lets) {
            LetterComperator r = captcha.owner.getLetter(letter);
            code += r.getDecodedValue();
            letter.detected = r;
        }
        File imageFile = captcha.owner.getResourceFile("images/" + code + ".png");
        if (imageFile.exists()) {
            captcha.reset();
            Captcha pixToFind = captcha.owner.createCaptcha(JDImage.getImage(imageFile));
            Letter l = captcha.createLetter();
            l.setGrid(pixToFind.grid);

            BackGroundImageManager bgit = new BackGroundImageManager(captcha);
            bgit.clearCaptchaAll();
            captcha.crop(0, 0, 0, 10);
            ArrayList<PixelObject> obj = getObjects(captcha, 3);

            ColorLetterComperator mainCLC = new ColorLetterComperator(l, l);
            double bestVal = 10000000.0;
            Letter bestLetter = null;
            for (PixelObject pixelObject : obj) {
                if (pixelObject.getWidth() > 63 || pixelObject.getHeight() > 63) {
                        PixelObject[] objb = pixelObject.splitAt(62);
                        for (PixelObject pixelObject2 : objb) {
                            if(pixelObject.getHeight() > 63)
                            {
                                PixelObject[] objb2 = pixelObject2.horizintalSplitAt(62);
                                for (PixelObject pixelObject3 : objb2) {
                                    Letter b = pixelObject3.toColoredLetter();
                                    mainCLC.setLetterB(b);
                                    double val = mainCLC.run();
                                    if (val < bestVal) {
                                        bestLetter = b;
                                        bestVal = val;
                                    }
                                }
                            }
                            else
                            {
                            Letter b = pixelObject2.toColoredLetter();
                            mainCLC.setLetterB(b);
                            double val = mainCLC.run();

                            if (val < bestVal) {
                                bestLetter = b;
                                bestVal = val;
                            }
                            }
                        }
                } else {
                    Letter b = pixelObject.toColoredLetter();
                    mainCLC.setLetterB(b);
                    double val = mainCLC.run();

                    if (val < bestVal) {
                        bestLetter = b;
                        bestVal = val;
                    }
                }

            }
            int x = bestLetter.getLocation()[0] + (bestLetter.getWidth() / 2);
            int y = bestLetter.getLocation()[1] + (bestLetter.getHeight() / 2);
            char[] tx = (x + ":" + y).toCharArray();
            Letter[] ret = new Letter[tx.length];
            for (int i = 0; i < tx.length; i++) {
                Letter re = new Letter();
                re.setDecodedValue(""+tx[i]);
                LetterComperator let1 = new LetterComperator(re, re);
                let1.setValityPercent(0);
                re.detected=let1;
                ret[i]=re;
            }
            return ret;
        }

        // Letter[] let = new Letter[obj.size()];
        // for (int j = 0; j < let.length; j++) {
        // let[j]=toLetter(obj.get(j), captcha);
        // BasicWindow.showImage(obj.get(j).toLetter().getFullImage());

        // }
        return null;
    }
    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }
}
