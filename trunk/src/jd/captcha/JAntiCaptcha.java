package jd.captcha;

import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jd.captcha.configuration.JACScript;
import jd.captcha.gui.BasicWindow;
import jd.captcha.gui.ImageComponent;
import jd.captcha.gui.ScrollPaneWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Diese Klasse stellt alle public Methoden zur captcha erkennung zur Verfügung.
 * Sie verküpft Letter und captcha Klassen. Gleichzeitig dient sie als
 * Parameter-Dump.
 * 
 * @author coalado
 */
public class JAntiCaptcha {
 
    /**
     * Logger
     */
    private static Logger            logger       = UTILITIES.getLogger();

    /**
     * Name des Authors der entsprechenden methode. Wird aus der jacinfo.xml
     * Datei geladen
     */
    private String                   methodAuthor;

    /**
     * Methodenname. Wird aus der jacinfo.xml geladen
     */
    private String                   methodName;

    /**
     * Bildtyp. Falls dieser von jpg unterschiedlich ist muss zuerst konvertiert
     * werden
     */

    private String                   imageType;

    /**
     * Anzahl der Buchstaben im Captcha. Wird aus der jacinfo.xml gelesen
     */
    private int                      letterNum;

    /**
     * Pfad zum SourceBild (Standalone). Wird aus der jacinfo.xml gelesen
     */
    private String                   sourceImage;

    /**
     * Pfad zur Resulotfile. dort wird der Captchacode hineingeschrieben.
     * (standalone mode)
     */
    private String                   resultFile;

    /**
     * XML Dokument für die MTH File
     */

    private Document                 mth;

    /**
     * Vector mit den Buchstaben aus der MTHO File
     */
    private Vector<Letter>           letterDB;

    /**
     * Static counter. Falls zu debug zecen mal global ein counter gebraucht
     * wird
     */
    public static int                counter      = 0;

    /**
     * Static counter. Falls zu debug zecen mal global ein counter gebraucht
     * wird
     */
    public static int                counterB     = 0;

    /**
     * ordnername der methode
     */
    private String                   methodDir;

    /**
     * fenster die eigentlich nur zur entwicklung sind um Basic GUI Elemente zu
     * haben
     */
    @SuppressWarnings("unused")
    private BasicWindow              bw1;

    @SuppressWarnings("unused")
    private BasicWindow              bw2;

    @SuppressWarnings("unused")
    private BasicWindow              bw3;
    @SuppressWarnings("unused")
    private BasicWindow              bw4;
    private Vector<ScrollPaneWindow> spw          = new Vector<ScrollPaneWindow>();
    private boolean                  showDebugGui = false;
    /**
     * jas Script Instanz. Sie verarbneitet das JACScript und speichert die
     * Parameter
     */
    public JACScript                 jas;

    private int[][]                  letterMap    = null;

    URLClassLoader                   cl;

    /**
     * @param methodsPath
     * @param methodName
     */

    public JAntiCaptcha(String methodsPath, String methodName) {
        methodsPath= UTILITIES.getFullPath(new String[] {methodsPath, methodName, "" });
        cl = UTILITIES.getURLClassLoader(methodsPath);
if(cl!=null){
        logger.fine("Benutze Classloader: " + cl.getResource("."));
        this.methodDir = methodName;

        if (cl != null) {
            getJACInfo();

            jas = new JACScript(this, cl, methodDir);
            loadMTHFile();
            logger.fine("letter DB loaded: Buchstaben: " + letterDB.size());
        } else {
            logger.severe("Die Methode " + methodsPath + " kann nicht gefunden werden. JAC Pfad falsch?");

        }
}else{
    
    logger.severe("Classloader konnte nicht initialisiert werden!");
}
    }

    /**
     * Gibt zurück ob die entsprechende Methode verfügbar ist.
     * 
     * @param methodsPath
     *           
     * @param methodName
     * @return true/false
     */
    public static boolean hasMethod(String methodsPath, String methodName) {
      
      
      
        return UTILITIES.getURLClassLoader(UTILITIES.getFullPath(new String[] {methodsPath, methodName, "script.jas" })) != null&&UTILITIES.getURLClassLoader(UTILITIES.getFullPath(new String[] {methodsPath, methodName })).getResource(".")!=null;
    }

    /**
     * Exportiert die aktelle Datenbank als PONG einzelbilder
     */
    public void exportDB() {
        File path = UTILITIES.directoryChooser("");
        File file;
        BufferedImage img;
        for (int i = 0; i < letterDB.size(); i++) {
            img = (BufferedImage) letterDB.elementAt(i).getFullImage();
            file = new File(path + "/letterDB_" + this.getMethodName() + "/" + i + "_" + letterDB.elementAt(i).getDecodedValue() + ".png");
            file.mkdirs();
            try {
                logger.info("Write Db: " + file);
                ImageIO.write(img, "png", file);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
    }

    /**
     * Importiert pNG einzelbilder aus einem ordner und erstellt daraus eine neue db
     */
    public void importDB() {
        File path = UTILITIES.directoryChooser("");
        letterDB = new Vector<Letter>();
        this.getResourceFile("letters.mth").delete();
        Image image;
        Letter letter;
        File[] images = getImages(path.getAbsolutePath());
        for (int i = 0; i < images.length; i++) {
            image = UTILITIES.loadImage(images[i]);
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            if (width <= 0 || height <= 0) {
                logger.severe("ERROR: Image nicht korrekt.");
            }
            PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);
            try {
                pg.grabPixels();
            } catch (Exception e) {
                e.printStackTrace();
            }
            letter = new Letter();

            letter.setOwner(this);

            ColorModel cm = pg.getColorModel();

            if (!(cm instanceof IndexColorModel)) {
                // not an indexed file (ie: not a gif file)

                int[] pixel = (int[]) pg.getPixels();
                int[][] newGrid = new int[width][height];
                int px = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        while(pixel[px]<0)pixel[px]+=0xffffff+1;
                       // logger.info("- "+pixel[px]);
                        newGrid[x][y] = pixel[px++]<100?0:PixelGrid.getMaxPixelValue(this);
                       
                    }
                }
                letter.setGrid(newGrid);   
                letter=              letter.align(-40, +40);
                letter.setSourcehash(UTILITIES.getLocalHash(images[i]));
                letter.setDecodedValue(images[i].getName().split("\\_")[1].split("\\.")[0]);
                letter.clean();
                
              
                letterDB.add(letter);

               letter.resizetoHeight(25);
                
            } else {

                logger.severe("Bildformat von ImportDB nicht unterstützt");
            }

            // BasicWindow.showImage(ret.getImage());

        }
        this.sortLetterDB();
        this.saveMTHFile();
    }

    /**
     * Diese methode wird aufgerufen um alle captchas im Ordner
     * methods/Methodname/captchas zu trainieren
     * 
     * @param path
     */
    public void trainAllCaptchas(String path) {

        int successFull = 0;
        int total = 0;
        File[] images = getImages(path);
        if (images == null)
            return;
        int newLetters;
        for (int i = 0; i < images.length; i++) {
            logger.fine(images[i].toString());

            newLetters = trainCaptcha(images[i], getLetterNum());

            logger.fine("Erkannt: " + newLetters + "/" + getLetterNum());
            if (newLetters > 0) {
                successFull += newLetters;
                total += getLetterNum();
                logger.info("Erkennungsrate: " + ((100 * successFull / total)));
            }

        }

    }

    /**
     * MTH File wird geladen und verarbeitet
     */
    private void loadMTHFile() {

        InputStream is = cl.getResourceAsStream("letters.mth");
        if (is == null) {
            logger.severe("MTH FILE NOT AVAILABLE.");
            // return;
        } else {

            mth = UTILITIES.parseXmlFile(is, false);

        }

        createLetterDBFormMTH();
        // sortLetterDB();

    }

    /**
     * Aus gründen der geschwindigkeit wird die MTH XMl in einen vector
     * umgewandelt
     */
    private void createLetterDBFormMTH() {
        letterDB = new Vector<Letter>();
        long start1 = UTILITIES.getTimer();
        try {

            if (mth == null || mth.getFirstChild() == null)
                return;
            NodeList nl = mth.getFirstChild().getChildNodes();
            Letter tmp;
            for (int i = 0; i < nl.getLength(); i++) {
                // Get child node
                Node childNode = nl.item(i);
                if (childNode.getNodeName().equals("letter")) {
                    NamedNodeMap att = childNode.getAttributes();

                    tmp = new Letter();
                    tmp.setOwner(this);
                    if (!tmp.setTextGrid(childNode.getTextContent()))
                        continue;;
                    String id = UTILITIES.getAttribute(childNode, "id");
                    if (id != null) {
                        tmp.id = Integer.parseInt(id);
                    }
                    tmp.setSourcehash(att.getNamedItem("captchaHash").getNodeValue());
                    tmp.setDecodedValue(att.getNamedItem("value").getNodeValue());
                    tmp.setBadDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "bad")));
                    tmp.setGoodDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "good")));
                    letterDB.add(tmp);
                } else if (childNode.getNodeName().equals("map")) {
                    logger.fine("Parse LetterMap");
                    long start2 = UTILITIES.getTimer();
                    String[] map = childNode.getTextContent().split("\\|");
                    letterMap = new int[map.length][map.length];
                    for (int x = 0; x < map.length; x++) {
                        String[] row = map[x].split("\\,");
                        for (int y = 0; y < map.length; y++) {
                            letterMap[x][y] = Integer.parseInt(row[y]);
                        }

                    }
                    logger.fine("LetterMap Parsing time: " + (UTILITIES.getTimer() - start2));
                }
            }
        } catch (Exception e) {
            logger.severe("Fehler mein lesen der MTHO Datei!!. Methode kann nicht funktionieren!");

        }
        logger.fine("Mth Parsing time: " + (UTILITIES.getTimer() - start1));
    }

    /*
     * Die Methode parst die jacinfo.xml
     */
    private void getJACInfo() {

        InputStream is = cl.getResourceAsStream("jacinfo.xml");
        if (is == null) {
            logger.severe("" + "jacinfo.xml" + " is missing1");
            return;
        }

        Document doc;
        doc = UTILITIES.parseXmlFile(is, false);

        NodeList nl = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            // Get child node
            Node childNode = nl.item(i);

            if (childNode.getNodeName().equals("method")) {

                this.setMethodAuthor(UTILITIES.getAttribute(childNode, "author"));
                this.setMethodName(UTILITIES.getAttribute(childNode, "name"));

            }
            if (childNode.getNodeName().equals("format")) {

                this.setLetterNum(Integer.parseInt(UTILITIES.getAttribute(childNode, "letterNum")));
                this.setImageType(UTILITIES.getAttribute(childNode, "type"));

            }
            if (childNode.getNodeName().equals("source")) {

                this.setSourceImage(UTILITIES.getAttribute(childNode, "file"));

            }
            if (childNode.getNodeName().equals("result")) {

                this.setResultFile(UTILITIES.getAttribute(childNode, "file"));

            }

        }

    }

    /**
     * Diese methode trainiert einen captcha
     * 
     * @param captchafile
     *            File zum Bild
     * @param letterNum
     *            Anzahl der Buchstaben im captcha
     * @return int -1: Übersprungen Sonst: anzahl der richtig erkanten Letter
     */

    private Document createXMLFromLetterDB() {

        Document xml = UTILITIES.parseXmlString("<jDownloader/>", false);
        if (letterMap != null) {
            Element element = xml.createElement("map");
            xml.getFirstChild().appendChild(element);
            element.appendChild(xml.createTextNode(this.getLetterMapString()));
        }

        Letter letter;
        for (int i = 0; i < letterDB.size(); i++) {
            letter = letterDB.elementAt(i);
            Element element = xml.createElement("letter");
            xml.getFirstChild().appendChild(element);
            element.appendChild(xml.createTextNode(letter.getPixelString()));
            element.setAttribute("id", i + "");
            element.setAttribute("value", letter.getDecodedValue());
            element.setAttribute("captchaHash", letter.getSourcehash());
            element.setAttribute("good", letter.getGoodDetections() + "");
            element.setAttribute("bad", letter.getBadDetections() + "");

        }
        return xml;

    }

//    /**
//     * TestFunktion - Annimierte Gifs verarbeiten
//     * 
//     * @param path
//     */
//    public void mergeGif(File path) {
//        getJas().setColorType("G");
//        GifDecoder d = new GifDecoder();
//        d.read(path.getAbsolutePath());
//        int n = d.getFrameCount();
//
//        logger.fine("Found Frames: " + n);
//        int width = (int) d.getFrameSize().getWidth();
//        int height = (int) d.getFrameSize().getHeight();
//        Captcha merged = new Captcha(width, height);
//        merged.setOwner(this);
//        Captcha tmp;
//
//        for (int i = 0; i < n; i++) {
//            BufferedImage frame = d.getFrame(i);
//            tmp = new Captcha(width, height);
//            tmp.setOwner(this);
//            PixelGrabber pg = new PixelGrabber(frame, 0, 0, width, height, false);
//            try {
//                pg.grabPixels();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            ColorModel cm = pg.getColorModel();
//            tmp.setColorModel(cm);
//
//            if (!(cm instanceof IndexColorModel)) {
//                // not an indexed file (ie: not a gif file)
//                tmp.setPixel((int[]) pg.getPixels());
//            } else {
//
//                tmp.setPixel((byte[]) pg.getPixels());
//            }
//            merged.concat(tmp);
//
//        }
//
//        merged.crop(6, 12, 6, 12);
//        // merged.removeSmallObjects(0.3, 0.3);
//        // merged.invert();
//        merged.toBlackAndWhite(0.4);
//        merged.removeSmallObjects(0.3, 0.3, 20);
//        merged.reduceBlackNoise(4, 0.45);
//        merged.toBlackAndWhite(1);
//        // merged.reduceBlackNoise(6, 1.6);
//        // merged.reduceBlackNoise(6, 1.6);
//        // getJas().setBackgroundSampleCleanContrast(0.1);
//        // merged.cleanBackgroundBySample(4, 4, 7, 7);
//
//        BasicWindow.showImage(merged.getImage(4), 160, 60);
//
//    }

    /**
     * Debug Methode. Zeigt den Captcha in verschiedenen bearbeitungsstadien an
     * 
     * @param captchafile
     */
    public void showPreparedCaptcha(File captchafile) {

        if (!captchafile.exists()) {
            logger.severe(captchafile.getAbsolutePath() + " existiert nicht");
            return;
        }

        Image captchaImage;
        // if (!this.getImageType().equalsIgnoreCase("jpg")) {
        // captchafile=UTILITIES.toJPG(captchafile);
        // captchaImage = UTILITIES.loadImage(captchafile);
        // logger.info("Bild umgewandelt: "+captchafile.getAbsolutePath());
        // captchafile.delete();
        // } else {
        captchaImage = UTILITIES.loadImage(captchafile);
        // }

        if (bw3 != null) {
            bw3.destroy();
        }
        bw3 = BasicWindow.showImage(captchaImage, "Captchas");
        bw3.add(new JLabel("ORIGINAL"), UTILITIES.getGBC(2, 0, 2, 2));
        bw3.setLocationByScreenPercent(50, 70);
        Captcha captcha = createCaptcha(captchaImage);
        bw3.add(new ImageComponent(captcha.getImage(1)), UTILITIES.getGBC(0, 2, 2, 2));
        bw3.add(new JLabel("Farbraum Anpassung"), UTILITIES.getGBC(2, 2, 2, 2));
        jas.executePrepareCommands(captcha);

        bw3.add(new ImageComponent(captcha.getImage(1)), UTILITIES.getGBC(0, 4, 2, 2));
        bw3.add(new JLabel("Prepare Code ausgeführt"), UTILITIES.getGBC(2, 4, 2, 2));

        // Hole die letters aus dem neuen captcha
        Letter[] letters = captcha.getLetters(letterNum);
        // UTILITIES.wait(40000);
        // prüfe auf Erfolgreiche Lettererkennung
        if (letters == null) {

            logger.severe("2. Lettererkennung ist fehlgeschlagen!");

            return;

        }

        bw3.add(new ImageComponent(captcha.getImageWithGaps(1)), UTILITIES.getGBC(0, 6, 2, 2));
        bw3.add(new JLabel("Buchstaben freistellen"), UTILITIES.getGBC(2, 6, 2, 2));
        bw3.refreshUI();
        if (bw2 != null) {
            bw2.destroy();
        }
        bw2 = new BasicWindow();
        bw2.setTitle("Freigestellte Buchstaben");
        bw2.setLayout(new GridBagLayout());
        bw2.setSize(300, 300);
        logger.info("display freistellen");
        bw2.setAlwaysOnTop(true);
        bw2.setLocationByScreenPercent(50, 5);
        bw2.add(new JLabel("Aus Captcha:"), UTILITIES.getGBC(0, 0, 2, 2));

        for (int i = 0; i < letters.length; i++) {

            bw2.add(new ImageComponent(letters[i].getImage(jas.getInteger("simplifyFaktor"))), UTILITIES.getGBC(i * 2 + 2, 0, 2, 2));

        }
        bw2.setVisible(true);
        bw2.pack();
        bw2.setSize(300, bw2.getSize().height);

        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters
        checkCaptcha(captcha);
        LetterComperator[] lcs = captcha.getLetterComperators();
        for (int i = 0; i < lcs.length; i++) {
            if (lcs[i] == null)
                continue;
            bw2.add(new JLabel("Aus Datenbank:"), UTILITIES.getGBC(0, 6, 2, 2));
            bw2.add(new ImageComponent(lcs[i].getB().getImage(jas.getInteger("simplifyFaktor"))), UTILITIES.getGBC(i * 2 + 2, 6, 2, 2));
            bw2.add(new JLabel("Wert:"), UTILITIES.getGBC(0, 8, 2, 2));
            bw2.add(new JLabel(lcs[i].getDecodedValue()), UTILITIES.getGBC(i * 2 + 2, 8, 2, 2));
            bw2.add(new JLabel("Proz.:"), UTILITIES.getGBC(0, 10, 2, 2));
            bw2.add(new JLabel(lcs[i].getValityPercent() + "%"), UTILITIES.getGBC(i * 2 + 2, 10, 2, 2));

        }
        bw2.pack();
        bw2.repack();
    }

    private void destroyScrollPaneWindows() {
        while (spw.size() > 0) {
            spw.remove(0).destroy();
        }
    }

    int trainCaptcha(File captchafile, int letterNum) {

        if (!captchafile.exists()) {
            logger.severe(captchafile.getAbsolutePath() + " existiert nicht");
            return -1;
        }
        if (this.isShowDebugGui()) {
            destroyScrollPaneWindows();
        }
        // Lade das Bild
        Image captchaImage = UTILITIES.loadImage(captchafile);
        // Erstelle hashwert
        String captchaHash = UTILITIES.getLocalHash(captchafile);

        // Prüfe ob dieser captcha schon aufgenommen wurde und überspringe ihn
        // falls ja
        if (isCaptchaInMTH(captchaHash)) {
            logger.fine("Captcha schon aufgenommen" + captchafile);
            return -1;
        }
        // captcha erstellen
        Captcha captcha = createCaptcha(captchaImage);

        String code = null;
        String guess = "";
        // Zeige das OriginalBild
        if (bw3 != null) {
            bw3.destroy();
        }
        bw3 = BasicWindow.getWindow("JAC Trainer", 600, 450);

        bw3.setText(0, 0, "original captcha");
        bw3.setImage(1, 0, captcha.getImage());
        bw3.setSize(600, 400);
        bw3.setLocationByScreenPercent(50, 5);
        bw3.repack();

        // Führe das Prepare aus
        jas.executePrepareCommands(captcha);
        // Hole die letters aus dem neuen captcha
        Letter[] letters = captcha.getLetters(letterNum);
        // UTILITIES.wait(40000);
        // prüfe auf Erfolgreiche Lettererkennung
        if (letters == null || letters.length != this.getLetterNum()) {
            File file = getResourceFile("detectionErrors1/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            logger.severe("2. Lettererkennung ist fehlgeschlagen!");
            return -1;
        }
        for (int i = 0; i < letters.length; i++) {
            if (letters[i] == null || letters[i].getWidth() < 2 || letters[i].getHeight() < 2) {
                File file = getResourceFile("detectionErrors5/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
                file.getParentFile().mkdirs();
                captchafile.renameTo(file);
                logger.severe("Letter detection error");
                return -1;
            }
        }

        // Zeige das After-prepare Bild an
        bw3.setText(0, 1, "Gap detection captcha");
        bw3.setImage(1, 1, captcha.getImageWithGaps(1));

        bw3.setText(0, 2, "Freigestellt:");
        for (int i = 0; i < letters.length; i++) {
            bw3.setImage(i + 1, 2, letters[i].getImage(jas.getInteger("simplifyFaktor")));
        }
        bw3.repack();
        // Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der
        // letters
        guess = checkCaptcha(captcha);
        LetterComperator[] lcs = captcha.getLetterComperators();
        if (guess != null && guess.length() == getLetterNum()) {
            bw3.setText(0, 3, "DB Letter:");
            bw3.setText(0, 4, "Decoded:");
            bw3.setText(0, 5, "UNSicherheit:");
            for (int i = 0; i < lcs.length; i++) {
                if (lcs[i] != null && lcs[i].getB() != null)
                    bw3.setImage(i + 1, 3, lcs[i].getB().getImage(jas.getInteger("simplifyFaktor")));
                if (lcs[i] != null && lcs[i].getB() != null)
                    bw3.setText(i + 1, 4, lcs[i].getDecodedValue());
                if (lcs[i] != null && lcs[i].getB() != null)
                    bw3.setText(i + 1, 5, (double) Math.round(10 * lcs[i].getValityPercent()) / 10.0);

            }
            bw3.repack();
        } else {
            logger.warning("Erkennung fehlgeschlagen");
        }

        logger.info("Decoded Captcha: " + guess + " Vality: " + captcha.getValityPercent());
        if (captcha.getValityPercent() >= 0||true) {

            // if (guess == null) {
            // File file = getResourceFile("detectionErrors2/" +
            // (UTILITIES.getTimer()) + "_" + captchafile.getName());
            // file.getParentFile().mkdirs();
            // captchafile.renameTo(file);
            // logger.severe("Letter erkennung fehlgeschlagen");
            // return -1;
            //
            // }
            if (getCodeFromFileName(captchafile.getName(), captchaHash) == null || true) {
                code = UTILITIES.prompt("Bitte Captcha Code eingeben (Press enter to confirm " + guess, guess);
                if (code != null && code.equals(guess))
                    code = "";

            } else {
                code = getCodeFromFileName(captchafile.getName(), captchaHash);
                logger.warning("captcha code für " + captchaHash + " verwendet: " + code);

            }

        } else {
            logger.info("100% ERkennung.. automatisch übernommen");
            // code = guess;
        }
        if (code == null) {
            File file = getResourceFile("detectionErrors3/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            logger.severe("Captcha Input error");
            return -1;
        }
        if (code.length() == 0) {
            code = guess;
        }
        if (code.length() != letters.length) {
            File file = getResourceFile("detectionErrors4/" + (UTILITIES.getTimer()) + "_" + captchafile.getName());
            file.getParentFile().mkdirs();
            captchafile.renameTo(file);
            logger.severe("Captcha Input error3");
            return -1;
        }
        if (code.indexOf("-") < 0) {
            String[] oldName = captchafile.getName().split("\\.");
            String ext = oldName[oldName.length - 1];
            String newName = captchafile.getParentFile().getAbsolutePath() + UTILITIES.FS + "captcha_" + this.getMethodDir() + "_code" + code + "." + ext;
            captchafile.renameTo(new File(newName));
        }
        int ret = 0;
        for (int i = 0; i < letters.length; i++) {
            if (!code.substring(i, i + 1).equals("-")) {
                if (guess != null && code.length() > i && guess.length() > i && code.substring(i, i + 1).equals(guess.substring(i, i + 1))) {
                    ret++;
                    if (lcs[i] != null) {
                        lcs[i].getB().markGood();
                    }

                    if (!jas.getBoolean("TrainOnlyUnknown")) {
                        letters[i].setOwner(this);
                        // letters[i].setTextGrid(letters[i].getPixelString());
                        letters[i].setSourcehash(captchaHash);
                        letters[i].setDecodedValue(code.substring(i, i + 1));
                        letterDB.add(letters[i]);
                        bw3.setText(i + 1, 6, "ok +");
                    } else {
                        bw3.setText(i + 1, 6, "ok -");
                    }
                    bw3.repack();
                } else {
                    if (lcs[i] != null && letterDB.size() > 30) {
                        lcs[i].getB().markBad();
                    }
                    letters[i].setOwner(this);
                    // letters[i].setTextGrid(letters[i].getPixelString());
                    letters[i].setSourcehash(captchaHash);
                    letters[i].setDecodedValue(code.substring(i, i + 1));
                    letterDB.add(letters[i]);
                    bw3.setText(i + 1, 6, "no +");
                    bw3.repack();
                }
            } else {
                bw3.setText(i + 1, 6, "-");
                bw3.repack();
            }
            // mth.appendChild(element);
        }
        sortLetterDB();
        saveMTHFile();
        return ret;
    }

    private String getCodeFromFileName(String name, String captchaHash) {
        // captcha_share.gulli.com_codeph2.gif

        String[] matches = UTILITIES.getMatches(name, "captcha_°_code°.°");
        if (matches != null && matches.length > 0)
            return matches[1];

        return null;
    }

    /**
     * Sortiert die letterDB Nach den bad Detections. Der Sortieralgo gehört
     * dringend überarbeitet!!! Diese Sortieren hilft die GUten Letter zuerst zu
     * prüfen.
     * 
     * @TODO Sortoer ALGO ändern. zu langsam!!
     */
    private void sortLetterDB() {

        Vector<Letter> ret = new Vector<Letter>();
        for (int i = 0; i < letterDB.size(); i++) {
            Letter akt = letterDB.elementAt(i);
            for (int x = 0; x < ret.size(); x++) {

                // if ((akt.getGoodDetections() - 5 * akt.getBadDetections()) >
                // (ret.elementAt(x).getGoodDetections() - 5 *
                // ret.elementAt(x).getBadDetections())) {
                // ret.add(x, akt);
                // akt = null;
                // break;
                // }
              
                if (akt.getDecodedValue().compareToIgnoreCase(ret.elementAt(x).getDecodedValue()) > 0) {
                    ret.add(x, akt);
                    akt = null;
                    break;
                }
            }
            if (akt != null)
                ret.add(akt);

        }

        letterDB = ret;

    }

    /**
     * Factory Methode zur Captcha erstellung
     * 
     * @param captchaImage
     *            Image instanz
     * @return captcha
     */
    public Captcha createCaptcha(Image captchaImage) {
        if (captchaImage.getWidth(null) <= 0 || captchaImage.getHeight(null) <= 0) {
            logger.severe("Image Dimensionen zu klein. Image hat keinen Inahlt. Pfad/Url prüfen!");
            return null;
        }
        Captcha ret = Captcha.getCaptcha(captchaImage, this);
        if(ret==null)return null;
        ret.setOwner(this);
        return ret;
    }

    /**
     * Gibt ein FileOebject zu einem resourcstring zurück
     * 
     * @param arg
     * @return File zu arg
     */
    public File getResourceFile(String arg) {
        String fileName = cl.getResource(".")  + arg;
        try {
            fileName = URLDecoder.decode(fileName, "UTF8");
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        try {
            return new File(new URI(UTILITIES.urlEncode(fileName)));
        } catch (URISyntaxException e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     * Speichert die MTH File
     */
    public void saveMTHFile() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(createXMLFromLetterDB());

            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();

            if (!UTILITIES.writeLocalFile(this.getResourceFile("letters.mth"), xmlString)) {
                logger.severe("MTHO file Konnte nicht gespeichert werden");
            }

        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    /**
     * Gibt den Captchacode zurück
     * 
     * @param img
     * @param methodPath
     * @param methodname
     * @return Captchacode
     */
    public static String getCaptchaCode(Image img, String methodPath, String methodname) {
        JAntiCaptcha jac = new JAntiCaptcha(methodPath, methodname);
        // BasicWindow.showImage(img);
        Captcha cap = jac.createCaptcha(img);
        if(cap==null){
           logger.severe("Captcha Bild konnte nicht eingelesen werden");
           return "JACerror";
        }
        // BasicWindow.showImage(cap.getImageWithGaps(2));
        String ret = jac.checkCaptcha(cap);
        logger.info("captcha text:" + ret);
        return ret;
    }



    /**
     * Prüft ob der übergeben hash in der MTH file ist
     * 
     * @param captchaHash
     * @return true/false
     */
    private boolean isCaptchaInMTH(String captchaHash) {
        if (letterDB == null)
            return false;

        for (int i = 0; i < letterDB.size(); i++) {
            if (letterDB.elementAt(i).getSourcehash().equals(captchaHash))
                return true;
        }

        return false;

    }

    /**
     * Gibt den erkannten CaptchaText zurück
     * 
     * @param captchafile
     *            Pfad zum Bild
     * @return CaptchaCode
     */
    public String checkCaptcha(File captchafile) {
        logger.finer("check " + captchafile);
        Image captchaImage = UTILITIES.loadImage(captchafile);
        Captcha captcha = createCaptcha(captchaImage);
        // captcha.printCaptcha();
        return checkCaptcha(captcha);
    }

    /**
     * prüft den übergebenen Captcha und gibtd en Code als String zurück das
     * lettersarray des Catchas wird dabei bearbeitet. es werden decoedvalue,
     * avlityvalue und parent gesetzt WICHTIG: Nach dem Decoden eines Captcha
     * herrscht verwirrung. Es stehen unterschiedliche methoden zur verfügung um
     * an bestimmte Informationen zu kommen: captcha.getDecodedLetters() gibt
     * Die letter aus der datenbank zurück. Deren werte sind nicht fest. Auf den
     * Wert von getvalityvalue und getValityPercent kann man sich absolut nicht
     * verlassen. Einzig getDecodedValue() lässt sich zuverlässig auslesen
     * captcha.getLetters() gibt die Wirklichen Letter des captchas zurück. Hier
     * lassen sich alle wichtigen infos abfragen. z.B. ValityValue,
     * ValityPercent, Decodedvalue, etc. Wer immer das hier liest sollte auf
     * keinen fall den fehler machen und sich auf Wert aus dem getdecodedLetters
     * array verlassen
     * 
     * @param captcha
     *            Captcha instanz
     * @return CaptchaCode
     */
    public String checkCaptcha(Captcha captcha) {

        // Führe prepare aus
        jas.executePrepareCommands(captcha);

        LetterComperator[] newLetters = new LetterComperator[this.getLetterNum()];
        String ret = "";
        double correct = 0;
        LetterComperator akt;
        Letter[] letters = captcha.getLetters(getLetterNum());
        if (letters == null) {
            captcha.setValityPercent(100.0);
            logger.severe("Captcha konnte nicht erkannt werden!1");
            return null;
        }
        if (letters.length != this.getLetterNum()) {
            captcha.setValityPercent(100.0);
            logger.severe("Captcha konnte nicht erkannt werden!2");
            return null;
        }

        for (int i = 0; i < letters.length; i++) {
            letters[i].id = i;
            akt = getLetter(letters[i]);

            newLetters[i] = akt;
            if (akt == null || akt.getValityPercent() >= 100.0) {
                correct += 100.0;
            } else {

                correct += akt.getValityPercent();

            }
            logger.finer("Validty: " + correct);
            if (newLetters[i] != null) {
                ret += akt.getDecodedValue();
            } else {
                ret += "_";
            }

        }

        captcha.setLetterComperators(newLetters);
        if (letters.length != this.getLetterNum() || ret.length() != this.getLetterNum()) {
            captcha.setValityPercent(100.0);
            logger.severe("Captcha konnte nicht erkannt werden!");
            return null;
        } else {
            logger.finer("Vality: " + ((int) (correct / letters.length)));
            captcha.setValityPercent(correct / (double) letters.length);
        }
        return ret;
    }

    /**
     * Vergleicht a und b und gibt eine vergleichszahl zurück. a und b werden
     * gegeneinander verschoben und b wird über die parameter gedreht. Praktisch
     * heißt das, dass derjenige TReffer als gut eingestuft wird, bei dem der
     * Datenbank datensatz möglichst optimal überdeckt wird.
     * 
     * @param a
     *            Original Letter
     * @param b
     *            Vergleichsletter
     * @return int 0(super)-0xffffff (ganz übel)
     */
    private LetterComperator getLetter(Letter letter) {

        long startTime = UTILITIES.getTimer();
        LetterComperator res = null;
        double lastPercent = 100.0;
        int bvX, bvY;
        try {

            if (letterDB == null) {
                logger.severe("letterDB nicht vorhanden");
                return null;
            }

            Letter tmp;

            LetterComperator lc;
            ScrollPaneWindow w = null;
            if (this.isShowDebugGui()) {
                w = new ScrollPaneWindow(this);

                w.setTitle(" Letter " + letter.id);
            }
            bvX = jas.getInteger("borderVarianceX");
            bvY = jas.getInteger("borderVarianceY");
            int line = 0;
            for (int i = 0; i < letterDB.size(); i++) {
                tmp = letterDB.elementAt(i);
                if (Math.abs(tmp.getHeight() - letter.getHeight()) > bvX || Math.abs(tmp.getWidth() - letter.getWidth()) > bvY) {
                    // continue;
                }
                lc = new LetterComperator(letter, tmp);
                lc.setScanVariance(0, 0);
                lc.setOwner(this);
                lc.run();
                if (this.isShowDebugGui()) {
                    w.setText(0, line, "0° Quick");
                    w.setImage(1, line, lc.getA().getImage(2));
                    w.setText(2, line, lc.getA().getDim());
                    w.setImage(3, line, lc.getB().getImage(2));
                    w.setText(4, line, lc.getB().getDim());
                    w.setImage(5, line, lc.getIntersectionLetter().getImage(2));
                    w.setText(6, line, lc.getIntersectionLetter().getDim());
                    w.setText(7, line, lc);
                    line++;
                }
                if (res == null || lc.getValityPercent() < res.getValityPercent()) {
                    if (res != null && res.getValityPercent() < lastPercent) {
                        lastPercent = res.getValityPercent();
                    }
                    res = lc;
                    if (jas.getDouble("LetterSearchLimitPerfectPercent") >= lc.getValityPercent()) {
                        logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);
                        res.setDetectionType(LetterComperator.QUICKSCANPERFECTMATCH);
                        res.setReliability(lastPercent - res.getValityPercent());
                        return res;
                    }
                    logger.finer("dim " + lc.getA().getDim() + "|" + lc.getB().getDim() + " New Best value: " + lc.getDecodedValue() + " " + lc.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + lc);
                } else if (res != null) {
                    if (lc.getValityPercent() < lastPercent) {
                        lastPercent = lc.getValityPercent();
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        if (res != null) {
            logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());
            logger.fine("Letter erkannt in: " + (UTILITIES.getTimer() - startTime) + " ms");
            res.setReliability(lastPercent - res.getValityPercent());
            if (res.getReliability() >= jas.getDouble("quickScanReliabilityLimit") && res.getValityPercent() < jas.getDouble("quickScanValityLimit")) {
                res.setDetectionType(LetterComperator.QUICKSCANMATCH);
                return res;
            } else {
                logger.warning("Letter nicht ausreichend erkannt. Try Extended " + res.getReliability() + " - " + jas.getDouble("quickScanReliabilityLimit") + " /" + res.getValityPercent() + "-" + jas.getDouble("quickScanValityLimit"));
                return getLetterExtended(letter);
            }
        } else {
            logger.warning("Letter nicht erkannt. Try Extended");
            return getLetterExtended(letter);
        }

    }

    /**
     * Sucht in der MTH ANch dem besten übereinstimmenem letter
     * 
     * @param letter
     *            (refferenz)
     * @return Letter. Beste Übereinstimmung
     */
    private LetterComperator getLetterExtended(Letter letter) {
        long startTime = UTILITIES.getTimer();
        LetterComperator res = null;

        double lastPercent = 100.0;

        try {

            if (letterDB == null) {
                logger.severe("letterDB nicht vorhanden");
                return null;
            }

            Letter tmp;
            int leftAngle = jas.getInteger("scanAngleLeft");
            int rightAngle = jas.getInteger("scanAngleRight");
            if (leftAngle > rightAngle) {
                int temp = leftAngle;
                leftAngle = rightAngle;
                rightAngle = temp;
                logger.warning("param.scanAngleLeft>paramscanAngleRight");
            }
            int steps = Math.max(1, jas.getInteger("scanAngleSteps"));

            int angle;
            Letter orgLetter = letter;
            LetterComperator lc;

            ScrollPaneWindow w = null;
            if (this.isShowDebugGui()) {
                w = new ScrollPaneWindow(this);

                w.setTitle(" Letter " + letter.id);
            }
            int line = 0;
            for (angle = UTILITIES.getJumperStart(leftAngle, rightAngle); UTILITIES.checkJumper(angle, leftAngle, rightAngle); angle = UTILITIES.nextJump(angle, leftAngle, rightAngle, steps)) {

                letter = orgLetter.turn(angle);
                logger.finer(" Angle " + angle+" : "+letter.getDim());
                for (int i = 0; i < letterDB.size(); i++) {
                    tmp = letterDB.elementAt(i);
                    if (Math.abs(tmp.getHeight() - letter.getHeight()) > jas.getInteger("borderVarianceY") || Math.abs(tmp.getWidth() - letter.getWidth()) > jas.getInteger("borderVarianceX")) {
                        // continue;
                    }

                    lc = new LetterComperator(letter, tmp);
                    lc.setOwner(this);
                    lc.run();
//logger.info("Duration: "+(UTILITIES.getTimer()-timer) +" Loops: "+lc.loopCounter);
                    if (this.isShowDebugGui()) {
                        w.setText(0, line, angle + "°");
                        w.setImage(1, line, lc.getA().getImage(2));
                        w.setText(2, line, lc.getA().getDim());
                        w.setImage(3, line, lc.getB().getImage(2));
                        w.setText(4, line, lc.getB().getDim());
                        w.setImage(5, line, lc.getIntersectionLetter().getImage(2));
                        w.setText(6, line, lc.getIntersectionLetter().getDim());
                        w.setText(7, line, lc);
                        line++;
                    }

                    if (res == null || lc.getValityPercent() < res.getValityPercent()) {
                        if (res != null && res.getValityPercent() < lastPercent) {
                            lastPercent = res.getValityPercent();
                        }
                        res = lc;

                        if (jas.getDouble("LetterSearchLimitPerfectPercent") >= lc.getValityPercent()) {
                            res.setDetectionType(LetterComperator.PERFECTMATCH);
                            res.setReliability(lastPercent - res.getValityPercent());
                            logger.finer(" Perfect Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + res);

                            return res;
                        }

                        logger.finer("Angle " + angle + "dim " + lc.getA().getDim() + "|" + lc.getB().getDim() + " New Best value: " + lc.getDecodedValue() + " " + lc.getValityPercent() + " good:" + tmp.getGoodDetections() + " bad: " + tmp.getBadDetections() + " - " + lc);

                    } else if (res != null) {
                        if (lc.getValityPercent() < lastPercent) {
                            lastPercent = lc.getValityPercent();
                        }
                    }

                }
                // logger.info("Full Angle scan in
                // "+(UTILITIES.getTimer()-startTime2));
            }
            // w.refreshUI();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (res != null) {
            logger.finer(" Normal Match: " + res.getB().getDecodedValue() + " " + res.getValityPercent() + " good:" + res.getB().getGoodDetections() + " bad: " + res.getB().getBadDetections());
            logger.fine("Letter erkannt in: " + (UTILITIES.getTimer() - startTime) + " ms");
            res.setReliability(lastPercent - res.getValityPercent());
        } else {

            logger.severe("Letter nicht erkannt");
        }

        return res;

    }

    /**
     * Zeigt die Momentane Library an. buchstaben können gelöscht werden
     */
    public void displayLibrary() {
        Letter tmp;
        final BasicWindow w = BasicWindow.getWindow("Library: " + letterDB.size() + " Datensätze", 400, 300);
        sortLetterDB();
        w.setLocationByScreenPercent(5, 5);
        final JAntiCaptcha jac = this;
        int x = 0;
        int y = 0;
        for (int i = letterDB.size() - 1; i >= 0; i--) {
            tmp = letterDB.elementAt(i);

            w.setText(x, y, tmp.getId() + ": " + tmp.getDecodedValue() + "(" + tmp.getGoodDetections() + "/" + tmp.getBadDetections() + ")");
            w.setImage(x + 1, y, tmp.getImage(jas.getInteger("simplifyFaktor")));
            w.setComponent(x + 3, y, new JButton(new AbstractAction("-" + i) {
                private static final long serialVersionUID = -2348057068938986789L;

                /**
                 * 
                 */

                public void actionPerformed(ActionEvent evt) {
                    jac.removeLetterFromLibrary(Integer.parseInt(((JButton) evt.getSource()).getText().substring(1)));
                    w.destroy();
                    jac.displayLibrary();
                    jac.saveMTHFile();
                }
            }));

            y++;
            if (y > 20) {
                y = 0;
                x += 6;
            }
        }
        w.refreshUI();
    }

    protected void removeLetterFromLibrary(int i) {
        letterDB.remove(i);

    }

    /**
     * Liest den captchaornder aus
     * 
     * @param path
     * @return File Array
     */
    private File[] getImages(String path) {
        File dir = new File(path);

        if (dir == null || !dir.exists()) {
            logger.severe("Image dir nicht gefunden " + path);

        }

        File[] entries = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                // logger.info(pathname.getName());
                if (pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

                    return true;
                } else {
                    return false;
                }
            }

        });
        return entries;

    }


    /**
     * @return the imageType
     */
    public String getImageType() {
        return imageType;
    }

    /**
     * @param imageType
     *            the imageType to set
     */
    public void setImageType(String imageType) {
        logger.finer("SET PARAMETER: [imageType] = " + imageType);
        this.imageType = imageType;
    }

    /**
     * @return the letterNum
     */
    public int getLetterNum() {
        return letterNum;
    }

    /**
     * @param letterNum
     *            the letterNum to set
     */
    public void setLetterNum(int letterNum) {
        logger.finer("SET PARAMETER: [letterNum] = " + letterNum);
        this.letterNum = letterNum;
    }

    /**
     * @return the method
     */
    public String getMethodDir() {
        return methodDir;
    }

    /**
     * @param methodPath
     * @param methodName
     */
    public void setMethod(String methodPath, String methodName) {
        logger.finer("SET PARAMETER: [method] = " + methodDir);

        this.methodDir = methodName;

    }

    /**
     * @return the methodAuthor
     */
    public String getMethodAuthor() {
        return methodAuthor;
    }

    /**
     * @param methodAuthor
     *            the methodAuthor to set
     */
    public void setMethodAuthor(String methodAuthor) {
        logger.finer("SET PARAMETER: [methodAuthor] = " + methodAuthor);
        this.methodAuthor = methodAuthor;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName
     *            the methodName to set
     */
    public void setMethodName(String methodName) {
        logger.finer("SET PARAMETER: [methodName] = " + methodName);
        this.methodName = methodName;
    }

    /**
     * @return the resultFile
     */
    public String getResultFile() {
        return resultFile;
    }

    /**
     * @param resultFile
     *            the resultFile to set
     */
    public void setResultFile(String resultFile) {
        logger.finer("SET PARAMETER: [resultFile] = " + resultFile);
        this.resultFile = resultFile;
    }

    /**
     * @return the sourceImage
     */
    public String getSourceImage() {
        return sourceImage;
    }

    /**
     * @param sourceImage
     *            the sourceImage to set
     */
    public void setSourceImage(String sourceImage) {
        logger.finer("SET PARAMETER: [sourceImage] = " + sourceImage);
        this.sourceImage = sourceImage;
    }

    /**
     * Führt diverse Tests durch. file dient als testcaptcha
     * 
     * @param file
     */
    public void runTestMode(File file) {
        Image img = UTILITIES.loadImage(file);
        Captcha captcha = createCaptcha(img);

        BasicWindow.showImage(captcha.getImage(2), "Original bild");
        captcha.testColor();
        BasicWindow.showImage(captcha.getImage(2), "Farbtester. Bild sollte Identisch sein");

        BasicWindow.showImage(captcha.getImage(2));
        // jas.setBackgroundSampleCleanContrast(0.15);
        // captcha.crop(80, 0, 0, 14);
        captcha.cleanBackgroundByColor(14408167);
        // captcha.reduceWhiteNoise(1);
        // captcha.toBlackAndWhite(0.9);

        BasicWindow.showImage(captcha.getImage(2));

        Vector<PixelObject> letters = captcha.getBiggestObjects(4, 200, 0.7, 0.8);
        for (int i = 0; i < letters.size(); i++) {
            PixelObject obj = letters.elementAt(i);
            // BasicWindow.showImage(obj.toLetter().getImage(3));

            Letter l = obj.toLetter();
            l.removeSmallObjects(0.3, 0.5);

            l = l.align(0.5, -45, +45);

            // BasicWindow.showImage(l.getImage(3));
            l.reduceWhiteNoise(2);
            l.toBlackAndWhite(0.6);
            BasicWindow.showImage(l.getImage(1));

        }

        // captcha.crop(13,8,33,8);
        // BasicWindow.showImage(captcha.getImage(3));
        // captcha.blurIt(5);

        //
        // executePrepareCommands(captcha);

        // BasicWindow.showImage(captcha.getImage(3),"With prepare Code");

    }

    /**
     * @return JACscript Instanz
     */
    public JACScript getJas() {
        return jas;
    }

    /**
     * Entfernt Buchstaben mit einem schlechetb Bad/Good verhältniss
     */
    public void removeBadLetters() {
        Letter tmp;
        logger.info("aktuelle DB Size: " + letterDB.size());
        for (int i = letterDB.size() - 1; i >= 0; i--) {
            tmp = letterDB.elementAt(i);
            if ((tmp.getGoodDetections() == 0 && tmp.getBadDetections() > 0) || ((double) tmp.getBadDetections() / (double) tmp.getGoodDetections()) >= jas.getDouble("findBadLettersRatio")) {
                logger.info("bad Letter entfernt: " + tmp.getDecodedValue() + " (" + tmp.getBadDetections() + "/" + tmp.getGoodDetections() + ")");
                letterDB.removeElementAt(i);
            }

        }
        logger.info("neue DB Size: " + letterDB.size());

        sortLetterDB();
        saveMTHFile();

    }

    /**
     * @return gibt die Lettermap als String zurück
     */
    private String getLetterMapString() {
        StringBuffer ret = new StringBuffer();
        int i = 0;
        for (int x = 0; x < letterMap.length; x++) {
            ret.append("|");
            i++;
            for (int y = 0; y < letterMap[0].length; y++) {

                ret.append(letterMap[x][y]);
                i++;
                ret.append(",");
                i++;
            }
            ret.deleteCharAt(ret.length() - 1);
            logger.fine("Db String: " + ((x * 100) / letterDB.size()) + "%");
        }
        ret.deleteCharAt(0);
        return ret.toString();

    }

    /**
     * @return the showDebugGui
     */
    public boolean isShowDebugGui() {
        return showDebugGui;
    }

    /**
     * @param showDebugGui
     *            the showDebugGui to set
     */
    public void setShowDebugGui(boolean showDebugGui) {
        this.showDebugGui = showDebugGui;
    }

    /**
     * @param path
     * @return Gibt die Pfade zu allen methoden zurück
     */
    public static File[] getMethods(String path) {
      File dir = new File(path);
        
              if (dir == null || !dir.exists()) {
                  logger.severe("Resource dir nicht gefunden: "+path);
        
              }
        
              File[] entries = dir.listFiles(new FileFilter() {
                  public boolean accept(File pathname) {
                      // logger.info(pathname.getName());
                      if (pathname.isDirectory() && new File(pathname.getAbsoluteFile()+UTILITIES.FS+"jacinfo.xml").exists()) {
        
                          return true;
                      } else {
                          return false;
                      }
                  }
        
              });
              return entries;
    }
/**
 * Führt einen testlauf mit den übergebenen methoden durch
 * @param methods
 */
    public static void testMethods(File[] methods) {
        for(int i=0; i<methods.length;i++){
            testMethod(methods[i]);
        }
        
    }
/**
 * Testet die Angegebene Methode. dabei werden analysebilder erstellt.
 * @param file
 */
    public static void testMethod(File file) {
        int checkCaptchas=20;
        String code;
        String inputCode;
        int totalLetters=0;
        int correctLetters=0;
        File captchaFile;
        Image img;
        String methodsPath=file.getParentFile().getAbsolutePath();
        String methodName=file.getName();
        File captchaDir=UTILITIES.getFullFile(new String[]{file.getAbsolutePath(),"captchas"});
        logger.info("Test Method: "+methodName+" at "+methodsPath);
        JAntiCaptcha jac= new JAntiCaptcha(methodsPath,methodName);
        File[] entries = captchaDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                // logger.info(pathname.getName());
                if (pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".png") || pathname.getName().endsWith(".gif")) {

                    return true;
                } else {
                    return false;
                }
            }
  
        });
        ScrollPaneWindow w=new ScrollPaneWindow(jac);
        w.setTitle(" Test Captchas: " + file.getAbsolutePath());
        w.resizeWindow(100);
        logger.info("Found Testcaptchas: "+entries.length);
        int testNum=Math.min(checkCaptchas, entries.length);
        logger.info("Test "+testNum+" Captchas");
        int i=0;
        for(i=0; i<testNum;i++){
            captchaFile= entries[(int)(Math.random()*entries.length)];
            img=UTILITIES.loadImage(captchaFile);
            w.setText(0, i, captchaFile.getName());
            w.setImage(1, i, img);
           
            w.repack();
            
          
           
           jac = new JAntiCaptcha(methodsPath, methodName);
           // BasicWindow.showImage(img);
           Captcha cap = jac.createCaptcha(img);
           if(cap==null){
              logger.severe("Captcha Bild konnte nicht eingelesen werden");
            continue;
           }
           
           w.setImage(2, i, cap.getImage());
           // BasicWindow.showImage(cap.getImageWithGaps(2));
           code = jac.checkCaptcha(cap);
           w.setImage(3, i, cap.getImage());
       
           w.setText(4, i, "JAC:"+code);
           
           w.repack();
           
           
           inputCode = UTILITIES.prompt("Bitte Captcha Code eingeben", code);
         
           w.setText(5, i, "User:"+inputCode);
           w.repack();
           if(code==null)code="";
           if(inputCode==null)inputCode="";
           code=code.toLowerCase();
           inputCode=inputCode.toLowerCase();
           for( int x=0; x<inputCode.length();x++){
               totalLetters++;
               if(inputCode.charAt(x)==code.charAt(x)){
                   correctLetters++;
               }
           }
           logger.info("Erkennung: "+correctLetters+"/"+totalLetters+" = "+UTILITIES.getPercent(correctLetters,totalLetters)+"%");
        }
        w.setText(0, i+1, "Erkennung: "+UTILITIES.getPercent(correctLetters,totalLetters)+"%");
        w.setText(4, i+1, "Richtig: "+correctLetters);
        w.setText(5, i+1, "Falsch: "+(totalLetters-correctLetters));
        UTILITIES.showMessage("Erkennung: "+correctLetters+"/"+totalLetters+" = "+UTILITIES.getPercent(correctLetters,totalLetters)+"%");
    }

 
    
//  
//  private static String[] getMethods() {

//
//  }
}