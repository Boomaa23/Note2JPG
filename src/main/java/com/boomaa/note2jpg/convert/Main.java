package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Args;
import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.uxutil.SwingConsole;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.*;
import com.boomaa.note2jpg.create.Shape;
import com.boomaa.note2jpg.uxutil.SwingInputField;
import com.boomaa.note2jpg.integration.s3upload.Connections;
import com.boomaa.note2jpg.state.FilenameSource;
import com.boomaa.note2jpg.state.NumberType;
import com.boomaa.note2jpg.state.PDFState;
import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class Main extends NFields {
    private static final String CURRENT_RELEASE_TAG = "v0.6.0";

    static {
        @SuppressWarnings("unchecked") List<Logger> loggers =
                Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
    }

    private static void printWelcome() {
        System.out.println(
            "    _   __      __      ___       ______  ______\n" +
            "   / | / /___  / /____ |__ \\     / / __ \\/ ____/\n" +
            "  /  |/ / __ \\/ __/ _ \\__/ /__  / / /_/ / / __  \n" +
            " / /|  / /_/ / /_/  __/ __// /_/ / ____/ /_/ /  \n" +
            "/_/ |_/\\____/\\__/\\___/____/\\____/_/    \\____/   \n" +
            "Note2JPG: A .note to .jpg converter\n" +
            "Developed by Nikhil for AP Physics\n" +
            "github.com/Boomaa23/Note2JPG\n" +
            "Copyright 2020-2021. All Rights Reserved.\n" +
            "---------------------------------------\n" +
            "NOTE: Note2JPG cannot parse z-layers\n");
    }

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        argsList = Arrays.asList(args);
        setupConsoleGUI();
        printWelcome();
        checkForUpdates();
        Args.parse();
        Args.logic();
        Args.check();

        for (String notename : notenames) {
            String filename = validateFilename(notename) + ".note";
            String noExtFilename = filename.substring(0, filename.lastIndexOf('.'));
            if (!Parameter.WipeUploaded.inEither()) {
                startTime = System.currentTimeMillis();
                if (Parameter.OutputDirectory.inEither()) {
                    try {
                        unzipNote(Parameter.OutputDirectory.getValue() + filename, noExtFilename);
                    } catch (ZipException ignored) {
                        // No source note in output directory, move on without error
                    }
                }
                try {
                    unzipNote(filename, noExtFilename);
                    if (Parameter.ForceDriveDownload.inEither()) {
                        throw new ZipException("Thrown forcefully to download from remote.");
                    }
                } catch (ZipException e) {
                    if (Parameter.ConfigVars.FILENAME_SOURCE == FilenameSource.NEO) {
                        System.err.println("Could not find local .note file for NEO assignment \"" + notename + "\"");
                    }
                    if (Parameter.UseDriveDownload.inEither() && Connections.getGoogleUtils().isNoteMatch(notename)) {
                        Connections.getGoogleUtils().downloadNote(notename, Parameter.OutputDirectory.getValue() + filename);
                        unzipNote(Parameter.OutputDirectory.getValue() + filename, noExtFilename);
                    } else {
                        System.err.println("Note file matching \"" + notename + "\" could not be found");
                        continue;
                    }
                }

                System.out.println("Args: name=\"" + notename + "\" scale=" + Parameter.ImageScaleFactor.getValue() + " pdfScale=" + Parameter.PDFScaleFactor.getValueInt());
                NSDictionary sessionMain = (NSDictionary) PropertyListParser.parse(new File(noExtFilename + "/Session.plist"));
                NSObject[] sessionObjects = ((NSArray) (sessionMain.getHashMap().get("$objects"))).getArray();
                NSDictionary[] sessionDict = Decode.isolateDictionary(sessionObjects);
                List<Image> pdfs = new ArrayList<>();

                float[] curvespoints = Decode.parseB64Numbers(NumberType.FLOAT, Decode.getDataFromDict(sessionDict, "curvespoints"));
                float[] curvesnumpoints = Decode.parseB64Numbers(NumberType.INTEGER, Decode.getDataFromDict(sessionDict, "curvesnumpoints"));
                float[] curveswidth = Decode.parseB64Numbers(NumberType.FLOAT, Decode.getDataFromDict(sessionDict, "curveswidth"));
                Color[] colors = Decode.parseB64Colors(Decode.getDataFromDict(sessionDict, "curvescolors"));

                List<EmbedImage> images = Decode.getEmbedImages(sessionObjects);
                List<TextBox> textBoxes = Decode.getTextBoxes(sessionObjects);
                List<Shape> shapes = new ArrayList<>();
                for (NSObject sessionObj : sessionObjects) {
                    if (sessionObj instanceof NSData) {
                        try {
                            NSObject shapesPrimary = ((NSDictionary) PropertyListParser.parse(((NSData) sessionObj).bytes())).get("shapes");
                            if (shapesPrimary != null) {
                                shapes = Decode.getShapes(((NSArray) shapesPrimary).getArray());
                                break;
                            }
                        } catch (PropertyListFormatException ignored) {
                            // Shape plist is invalid if the parser throws an exception, continue without error
                        }
                    }
                }

                System.gc();
                while (true) {
                    try {
                        Point[] points = Decode.getScaledPoints(curvespoints);
                        Curve[] curves = Decode.pointsToCurves(points, colors, curvesnumpoints, curveswidth);
                        scaledWidth = Math.max(defWidth, Decode.getNumberFromDict(sessionDict, "pageWidthInDocumentCoordsKey")) * Parameter.ImageScaleFactor.getValueInt();
                        bounds = Decode.getBounds(points, shapes.toArray(new Shape[0]));
                        if (pdfState != PDFState.NONE) {
                            if (pdfState == PDFState.PLIST) {
                                NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(noExtFilename + "/NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
                                NSDictionary pdfInfo = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers")));
                                for (String pdfLoc : pdfInfo.allKeys()) {
                                    List<Image> cPdfAll = ImageUtil.getPdfImages(noExtFilename, pdfLoc);
                                    NSObject[] pdfPgMap = ((NSArray) pdfInfo.get(pdfLoc)).getArray();
                                    int pgCtr = -1;
                                    for (Image cPdfPage : cPdfAll) {
                                        pgCtr += 2;
                                        int pgIdx = ((NSNumber) pdfPgMap[pgCtr]).intValue();
                                        if (pgIdx > pdfs.size()) {
                                            for (int i = 0; i <= pgIdx; i++) {
                                                try {
                                                    pdfs.get(i);
                                                } catch (IndexOutOfBoundsException e) {
                                                    pdfs.add(null);
                                                }
                                            }
                                        }
                                        pdfs.set(pgIdx - 1, cPdfPage);
                                    }
                                    for (int i = pdfs.size() - 1; i >= 0; i--) {
                                        if (pdfs.get(i) == null) {
                                            pdfs.remove(i);
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            } else if (pdfState == PDFState.FILE_ONLY) {
                                File pdfDir = new File(noExtFilename + "/PDFs/");
                                for (File pdf : Objects.requireNonNull(pdfDir.listFiles())) {
                                    pdfs.addAll(ImageUtil.getPdfImages(noExtFilename, pdf.getName()));
                                }
                            }
                            pages = pdfs.size();
                        }
                        if (Parameter.PageCountOut.inEither()) {
                            pages = Parameter.PageCountOut.getValueInt();
                        } else {
                            double tempPages = bounds.getYDbl() / (scaledWidth * 11 / 8.5);
                            int ceilPages = (int) Math.ceil(tempPages);
                            tempPages = Math.max(1, Parameter.FitExactHeight.inEither() ?
                                    (ceilPages != 1 ? (tempPages + 0.1) : tempPages) : ceilPages);
                            if (tempPages > pages || pdfState == PDFState.NONE) {
                                pages = tempPages;
                            }
                        }
                        boolean landscapePdfs = pdfs.size() > 0 && pdfs.get(0).getWidth(null) > pdfs.get(0).getHeight(null);
                        scaledHeight = (int) (scaledWidth * pages * (landscapePdfs ? 8.5 / 11 : 11 / 8.5));
                        upscaledAll = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);

                        ImageUtil.drawPdfImages(ImageUtil.getPdfCanvas(pdfs));
                        ImageUtil.drawEmbedImages(images, noExtFilename);
                        ImageUtil.drawTextBoxes(textBoxes);

                        drawRenderer = new DrawRenderer(curves, shapes.toArray(new Shape[0]));
                        drawRenderer.setSize(new Dimension(scaledWidth, scaledHeight));
                        drawRenderer.print(upscaledAll.createGraphics());
                        break;
                    } catch (OutOfMemoryError | NegativeArraySizeException e) {
                        drawRenderer = null;
                        pdfs = new ArrayList<>();
                        Parameter.ImageScaleFactor.setLinkedField(Parameter.ImageScaleFactor.getValueInt() - 2);
                        System.gc();
                        System.err.println("Memory limit exceeded with scale " + (Parameter.ImageScaleFactor.getValueInt() + 2));
                    }
                }

                if ((!Parameter.NoPagePrompt.inEither() || Parameter.PageSelectionIn.inEither()) && Math.ceil(pages) != 1) {
                    int numSelPages = 0;
                    List<Integer> allowedPages = new LinkedList<>();
                    String[] allNotesSel = Args.getPageSelection().split("/");
                    for (String noteSel : allNotesSel) {
                        String[] allPgsSel = noteSel.trim().split(",");
                        for (String pgsSel : allPgsSel) {
                            pgsSel = pgsSel.trim();
                            int idxDash = pgsSel.indexOf("-");
                            if (idxDash == -1) {
                                allowedPages.add(Integer.parseInt(pgsSel));
                            } else {
                                int endComma = 1;
                                while (idxDash != -1) {
                                    int start = Integer.parseInt(pgsSel.substring(endComma - 1, idxDash));
                                    endComma = pgsSel.indexOf(",");
                                    if (endComma == -1) {
                                        endComma = pgsSel.length();
                                    }
                                    int end = Integer.parseInt(pgsSel.substring(idxDash + 1, endComma));
                                    for (int pg = start; pg <= end; pg++) {
                                        allowedPages.add(pg);
                                    }
                                    idxDash = pgsSel.indexOf("-", idxDash + 1);
                                }
                            }
                        }
                    }
                    Collections.sort(allowedPages);
                    ImageUtil.filterValidPages(allowedPages);
                    pages = numSelPages;
                }

                if (Parameter.DisplayConverted.inEither()) {
                    setupFrame(notename);
                    displayFrame();
                } else if (frame != null) {
                    frame.dispose();
                }

                if (!Parameter.NoFileOutput.inEither()) {
                    saveToFile(Parameter.OutputDirectory.getValue() + noExtFilename);
                    savedNotes.add(filename);
                }
            }

            if (Parameter.NEOUsername.inEither() && Parameter.NEOPassword.inEither()) {
                if (Parameter.WipeUploaded.inEither()) {
                    System.out.println("Wiping " + notename + " from upload sources specified");
                    System.out.println("The application will exit after this takes place.");
                    if (Parameter.UseDriveDownload.inEither() || Parameter.UseAWS.inEither()) {
                        if (Parameter.UseAWS.inEither()) {
                            System.out.println("Please note AWS does not remove the file's record entirely, and instead overwrites the file data with nulls");
                            Connections.getAwsExecutor().remove(notename + ".jpg");
                        }
                        if (Parameter.UseDriveUpload.inEither()) {
                            Connections.getGoogleUtils().deleteAllMatchingImages(notename);
                        }
                        System.exit(0);
                    } else {
                        throw new IllegalArgumentException("No upload source specified to wipe from");
                    }
                }

                List<String> imageUrls = null;
                if (Parameter.UseAWS.inEither()) {
                    imageUrls = Arrays.asList(Connections.getAwsExecutor().uploadFile(noExtFilename + ".jpg",
                            Parameter.OutputDirectory.getValue(), Parameter.NewNEOFilename.inEither()));
                    System.out.println("\nImage uploaded to: \n" + imageUrls.get(0) + "\n" + imageUrls.get(1) + "\n");
                }

                if (imageUrls != null) {
                    if (!Parameter.NEONoLink.inEither()) {
                        String assignName;
                        if (Parameter.NEOAssignment.inEither()) {
                            assignName = Parameter.NEOAssignment.getValue();
                        } else {
                            System.out.println("Select the associated NEO assignment");
                            assignName = Args.filenameSelector(neoExecutor.getAssignments().getNames(), "");
                        }
                        // Autoselect NEO-format link instead of AWS (b/c of lms_auth server auto-add to img link)
                        String assignmentUrl = neoExecutor.push(assignName, imageUrls.get(1));
                        System.out.println("Posted to the NEO assignment at " + assignmentUrl);
                    }
                } else {
                    System.err.println("No AWS/NEO upload target specified");
                }
            }

            cleanupFiles(new File(noExtFilename + "/"));
            cleanupFiles(new File(noExtFilename));
        }

        if (savedNotes.size() > 1 && Parameter.Concatenate.inEither()) {
            int heightCtr = 0;
            BufferedImage canvas = new BufferedImage(iPadWidth, concatHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) canvas.getGraphics();
            // Re-loading the images is slower but saves memory
            for (String notename : savedNotes) {
                File imgFile = new File(notename + ".jpg");
                Image saved = ImageIO.read(imgFile);
                g2.drawImage(saved, 0, heightCtr, null);
                heightCtr += saved.getHeight(null);
                cleanupFiles(imgFile);
            }
            g2.dispose();

            // Concatenated files get a different naming scheme
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String concatFilename = "concat_" + df.format(new Date()) + ".jpg";
            try {
                ImageIO.write(canvas, "jpg", new File(concatFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Concatenated all converted notes into \"" + concatFilename + "\"");
        }

        System.out.println();
        if (!Parameter.ConsoleOnly.inEither()) {
            System.out.println("Press any key to quit...");
            outputDone = true;
        } else {
            System.exit(0);
        }
    }

    public static void setupFrame(String filename) {
        frame = new JFrame("Note2JPG | " + filename);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = (int) (screen.getWidth() / 2.5);
        frame.setSize(new Dimension(width, (int) (width * (11 / 8.5))));
    }

    public static void displayFrame() {
        Image img = ImageUtil.scaleImageFrame(upscaledAll);
        displayedWidth = img.getWidth(null);
        JLabel imgTemp = new JLabel(new ImageIcon(img));
        JPanel container = new JPanel();
        container.add(imgTemp);
        JScrollPane scrPane = new JScrollPane(container);
        scrPane.getVerticalScrollBar().setUnitIncrement(20);
        scrPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.setResizable(false);
        frame.add(scrPane);
        frame.setContentPane(scrPane);
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
    }

    public static void saveToFile(String filename) {
        heightFinal = (int) (((double) iPadWidth / upscaledAll.getWidth()) * upscaledAll.getHeight());
        concatHeight += heightFinal;
        try {
            ImageIO.write(ImageUtil.convertColorspace(ImageUtil.scaleBufferedImage(upscaledAll, iPadWidth, heightFinal),
                    BufferedImage.TYPE_INT_RGB), "jpg", new File(filename + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Completed in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds with scale=" + Parameter.ImageScaleFactor.getValueInt());
    }

    public static void unzipNote(String filename, String noExtNoteName) throws ZipException {
        ZipFile zipFile = new ZipFile(filename);
        String innerFolder = "";
        try {
            innerFolder = zipFile.getFileHeaders().get(0).getFileName();
        } catch (IndexOutOfBoundsException ignored) {
            throw new ZipException("Throwing exception to illicit no-zip response");
        }
        innerFolder = innerFolder.substring(0, innerFolder.lastIndexOf("/"));
        zipFile.extractFile(innerFolder + "/Session.plist", noExtNoteName, "/Session.plist");
        try {
            try {
                zipFile.extractFile(innerFolder + "/NBPDFIndex/NoteDocumentPDFMetadataIndex.plist", noExtNoteName,
                        "/NBPDFIndex/NoteDocumentPDFMetadataIndex.plist");
                pdfState = PDFState.PLIST;
            } catch (ZipException e) {
                pdfState = PDFState.NONE;
            }
            List<FileHeader> files = zipFile.getFileHeaders();
            for (FileHeader file : files) {
                if (file.getFileName().contains("PDFs")) {
                    String actualFileName =  file.getFileName().substring(file.getFileName().lastIndexOf('/') + 1);
                    zipFile.extractFile(innerFolder + "/PDFs/" + actualFileName, noExtNoteName,
                            "/PDFs/" + actualFileName);
                    if (pdfState == PDFState.NONE) {
                        pdfState = PDFState.FILE_ONLY;
                    }
                }
                if (file.getFileName().contains("Images")) {
                    String actualFileName =  file.getFileName().substring(file.getFileName().lastIndexOf('/') + 1);
                    zipFile.extractFile(innerFolder + "/Images/" + actualFileName, noExtNoteName,
                            "/Images/" + actualFileName);
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void cleanupFiles(File fn) {
        File[] allContents = fn.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                cleanupFiles(file);
            }
        }
        fn.delete();
    }

    public static String validateFilename(String filename) {
        StringBuilder sb = new StringBuilder();
        char[] fn = filename.toCharArray();
        for (char c : fn) {
            // Eliminate all non-alphanumeric and non-ascii numbers
            // Not technically required, but known to always be the same as the NEO registration
            if (inRange(c, 48, 57) || inRange(c, 65, 90) || inRange(c, 97, 122) || c == 45) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private static void checkForUpdates() {
        try {
            String baseUrl = "https://github.com/Boomaa23/Note2JPG/releases/";
            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "latest").openConnection();
            connection.setConnectTimeout(1000);
            connection.setInstanceFollowRedirects(false);
            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                    String redirect = connection.getHeaderField("Location");
                    String remoteVer = redirect.substring(redirect.lastIndexOf("/") + 1);
                    if (!remoteVer.equals(CURRENT_RELEASE_TAG)) {
                        System.err.println("A new version " + remoteVer + " is available! Download via the updater or from " + baseUrl + remoteVer + "\n");
                    }
                    break;
            }
        } catch (IOException ignored) {
            // This is a non-critical error, ignore
        }
    }

    private static void setupConsoleGUI() {
        if (!Parameter.ConsoleOnly.inEither()) {
            consoleFrame = new JFrame("Note2JPG");
            try {
                consoleFrame.setIconImage(ImageIO.read(new File("lib/note2jpg-icon.png")));
            } catch (IOException ignored) {
                // This is a non-critical error, ignore
            }
            consoleFrame.setSize(640, 480);
            consoleFrame.setResizable(false);
            consoleFrame.setLocationRelativeTo(null);
            consoleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            KeyAdapter keyCloser = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (outputDone) {
                        consoleFrame.dispose();
                        System.exit(0);
                    }
                }
            };

            JTextArea outArea = new JTextArea();
            outArea.setEditable(false);
            outArea.setAutoscrolls(true);
            outArea.addKeyListener(keyCloser);
            outArea.setRows(24);
            outArea.setFont(new Font("monospaced", Font.PLAIN, 12));
            JScrollPane scr = new JScrollPane(outArea);
            scr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JTextField inField = new JTextField();
            inField.addKeyListener(keyCloser);
            SwingInputField userIn = new SwingInputField(inField);
            inField.addActionListener(userIn);
            System.setIn(userIn);

            PrintStream pos = new PrintStream(new SwingConsole(outArea));
            System.setOut(pos);
            System.setErr(pos);
            JPanel content = (JPanel) consoleFrame.getContentPane();
            content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            content.setLayout(new BorderLayout());
            content.add(scr, BorderLayout.NORTH);
            content.add(inField, BorderLayout.SOUTH);
            consoleFrame.setVisible(true);
        }
    }
}
