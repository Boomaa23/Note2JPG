package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Args;
import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Box;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.*;
import com.boomaa.note2jpg.create.Shape;
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
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main extends NFields {
    private static final String CURRENT_VERSION_TAG = "v0.5.5";

    static {
        @SuppressWarnings("unchecked") List<Logger> loggers =
            Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
        System.out.println(
            "    _   __      __      ___       ______  ______\n" +
            "   / | / /___  / /____ |__ \\     / / __ \\/ ____/\n" +
            "  /  |/ / __ \\/ __/ _ \\__/ /__  / / /_/ / / __  \n" +
            " / /|  / /_/ / /_/  __/ __// /_/ / ____/ /_/ /  \n" +
            "/_/ |_/\\____/\\__/\\___/____/\\____/_/    \\____/   \n" +
            "Note2JPG: A .note to .jpg converter\n" +
            "Developed by Nikhil for AP Physics\n" +
            "github.com/Boomaa23/Note2JPG\n" +
            "Copyright 2020. All Rights Reserved.\n" +
            "---------------------------------------\n" +
            "NOTE: Note2JPG cannot parse positions of text boxes\n");
    }

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        checkForUpdates();
        argsList = Arrays.asList(args);
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
                    }
                }
                try {
                    unzipNote(filename, noExtFilename);
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

                List<Shape> shapes = new ArrayList<>();
                if (sessionObjects.length >= 14 && sessionObjects[14] instanceof NSData) {
                    NSObject[] shapesMain = ((NSArray) ((NSDictionary) PropertyListParser.parse(((NSData) sessionObjects[14]).bytes())).get("shapes")).getArray();
                    for (NSObject obj : shapesMain) {
                        NSDictionary dict = (NSDictionary) obj;
                        NSDictionary appearance = (NSDictionary) dict.get("appearance");
                        NSObject[] shapeColorRGBA = ((NSArray) ((NSDictionary) appearance.get("strokeColor")).get("rgba")).getArray();
                        Color strokeColor = Decode.getShapeStrokeColor(shapeColorRGBA);
                        int scale = Parameter.ImageScaleFactor.getValueInt();
                        double strokeWidth = ((NSNumber) appearance.get("strokeWidth")).doubleValue() * scale;

                        if (dict.containsKey("points")) {
                            NSObject[] parsePoints = ((NSArray) dict.get("points")).getArray();
                            Point[] polyPoints = new Point[parsePoints.length];
                            for (int i = 0; i < parsePoints.length; i++) {
                                Point tempPt = Decode.parseShapePoint(((NSArray) parsePoints[i]).getArray());
                                tempPt.setX((tempPt.getXDbl() + leftOffset) * scale);
                                tempPt.setY(tempPt.getYDbl() * scale);
                                polyPoints[i] = tempPt;
                            }
                            shapes.add(new Shape.NPolygon(strokeColor, strokeWidth, ((NSNumber) dict.get("isClosed")).boolValue(), polyPoints));
                        } else {
                            Point firstPoint;
                            Point secondPoint;
                            Shape.Type shapeType;
                            if (dict.containsKey("startPt")) {
                                shapeType = Shape.Type.LINE;
                                firstPoint = Decode.parseShapePoint(((NSArray) dict.get("startPt")).getArray());
                                secondPoint = Decode.parseShapePoint(((NSArray) dict.get("endPt")).getArray());
                            } else if (dict.containsKey("rotatedRect")) {
                                shapeType = Shape.Type.CIRCLE;
                                NSObject[] points = ((NSArray) ((NSDictionary) dict.get("rotatedRect")).get("corners")).getArray();
                                firstPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                                secondPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
                                for (NSObject loopPoint : points) {
                                    Point currPoint = Decode.parseShapePoint(((NSArray) loopPoint).getArray());
                                    if (currPoint.getXInt() >= secondPoint.getXInt() && currPoint.getYInt() >= secondPoint.getYInt()) {
                                        secondPoint = currPoint;
                                    }
                                    if (currPoint.getXInt() <= firstPoint.getXInt() && currPoint.getYInt() <= firstPoint.getYInt()) {
                                        firstPoint = currPoint;
                                    }
                                }
                            } else {
                                continue;
                            }
                            firstPoint.setX((firstPoint.getXDbl() + leftOffset) * scale);
                            firstPoint.setY(firstPoint.getYDbl() * scale);
                            secondPoint.setX((secondPoint.getXDbl() + leftOffset) * scale);
                            secondPoint.setY(secondPoint.getYDbl() * scale);
                            shapes.add(new Shape(shapeType, strokeColor, strokeWidth, firstPoint, secondPoint));
                        }
                    }
                }

                System.gc();
                while (true) {
                    try {
                        Point[] points = Decode.getPoints(curvespoints);
                        Curve[] curves = Decode.pointsToCurves(points, colors, curvesnumpoints, curveswidth);
                        scaledWidth = Decode.getNumberFromDict(sessionDict, "pageWidthInDocumentCoordsKey") * Parameter.ImageScaleFactor.getValueInt();
                        if (pdfState != PDFState.NONE) {
                            if (pdfState == PDFState.PLIST) {
                                NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(noExtFilename + "/NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
                                String[] pdfLocs = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers"))).allKeys();
                                for (String pdfLoc : pdfLocs) {
                                    pdfs.addAll(ImageUtil.getPdfImages(noExtFilename, pdfLoc));
                                }
                                //TODO implement by-page setting for pdfs in the plist
                            } else if (pdfState == PDFState.FILE_ONLY) {
                                File pdfDir = new File(noExtFilename + "/PDFs/");
                                for (File pdf : pdfDir.listFiles()) {
                                    pdfs.addAll(ImageUtil.getPdfImages(noExtFilename, pdf.getName()));
                                }
                            }
                            pages = pdfs.size();
                        } else {
                            //TODO actually calculate this for non-pdfs
                            pages = 1;
                        }
                        if (Parameter.PageCount.inEither()) {
                            pages = Parameter.PageCount.getValueInt();
                        }
                        scaledHeight = (int) (scaledWidth * pages * 11 / 8.5);
                        bounds = Decode.getBounds(points);
                        setupCurves(curves, shapes.toArray(new Shape[0]));
                        ImageUtil.populateUnscaledAll(ImageUtil.getPdfCanvas(pdfs));
                        if (hasImages && !Parameter.NoEmbedImages.inEither()) {
                            ImageUtil.fillEmbedImageList(noExtFilename);
                            if (imageList.size() > 0) {
                                setupFrame(notename);
                                displayFrame();
                                frame.getContentPane().addMouseListener(new PointTrigger(Point.class));
                                while (imageBounds.size() != imageList.size()) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                                frame.setVisible(false);
                            }
                            ImageUtil.populateEmbedImages();
                        }
                        if (!Parameter.NoTextBoxes.inEither()) {
                            textBoxContents = Decode.getTextBoxes(sessionDict);
                            if (textBoxContents.size() > 0) {
                                setupFrame(notename);
                                displayFrame();
                                frame.getContentPane().addMouseListener(new PointTrigger(Box.class));
                                System.out.print("\rPositioning: " + textBoxContents.get(0) + " (1 / " + textBoxContents.size() + ") on " + PointTrigger.selectState);
                                while (textBoxBounds.size() != textBoxContents.size()
                                    || textBoxBounds.get(textBoxBounds.size() - 1).getCorner(Corner.BOTTOM_RIGHT) == null) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                                frame.setVisible(false);
                                ImageUtil.populateTextBoxes(textBoxContents);
                            }
                        }
                        break;
                    } catch (OutOfMemoryError | NegativeArraySizeException e) {
                        circles = null;
                        pdfs = new ArrayList<>();
                        Parameter.ImageScaleFactor.setLinkedField(Parameter.ImageScaleFactor.getValueInt() - 2);
                        System.gc();
                        System.err.println("Memory limit exceeded with scale " + (Parameter.ImageScaleFactor.getValueInt() + 2));
                    }
                }

                if (Parameter.DisplayConverted.inEither()) {
                    setupFrame(notename);
                    displayFrame();
                } else if (frame != null) {
                    frame.dispose();
                }

                if (!Parameter.NoFileOutput.inEither()) {
                    saveToFile(Parameter.OutputDirectory.getValue() + noExtFilename);
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
                    imageUrls = Arrays.asList(Connections.getAwsExecutor().uploadFile(noExtFilename + ".jpg", Parameter.OutputDirectory.getValue(), Parameter.NewNEOFilename.inEither()));
                    System.out.println("\nImage uploaded to: \n " + imageUrls.get(0) + "\n" + imageUrls.get(1) + "\n");
                }

                if (imageUrls != null) {
                    if (!Parameter.NEONoLink.inEither()) {
                        String assignName;
                        if (Parameter.NEOAssignment.inEither()) {
                            assignName = Parameter.NEOAssignment.getValue();
                        } else {
                            System.out.println("Select the associated NEO assignment");
                            assignName = Args.filenameSelector(neoExecutor.getAssignments().getNames());
                        }
                        //autoselect NEO-format link instead of AWS (b/c of lms_auth server auto-add to img link)
                        String assignmentUrl = neoExecutor.push(assignName, imageUrls.get(1));
                        System.out.println("Posted to the NEO assignment at " + assignmentUrl);
                    }
                } else {
                    System.err.println("No AWS/NEO upload target specified");
                }
            }

            cleanupFiles(new File(noExtFilename + "/"));
            cleanupFiles(new File(noExtFilename));
            if (!notename.equals(notenames.get(notenames.size() - 1))) {
                System.out.println();
            }
            System.exit(0);
        }
    }

    public static void setupCurves(Curve[] curves, Shape[] shapes) {
        circles = new Circles(curves, shapes);
        circles.setSize(new Dimension(scaledWidth, scaledHeight));
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
        try {
            ImageIO.write(ImageUtil.scaleBufferedImage(upscaledAll, iPadWidth, heightFinal), "jpg", new File(filename + ".jpg"));
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
                    if (!hasImages) {
                        hasImages = true;
                    }
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
            // eliminate all non-alphanumeric and non-ascii numbers
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
                    if (!remoteVer.equals(CURRENT_VERSION_TAG)) {
                        System.err.println("A new version " + remoteVer + " is available! Download via the updater or from " + baseUrl + remoteVer + "\n");
                    }
                    break;
            }
        } catch (IOException ignored) {
        }
    }
}
