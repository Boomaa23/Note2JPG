package com.boomaa.note2jpg.function;

import com.boomaa.note2jpg.create.Circles;
import com.boomaa.note2jpg.create.Curve;
import com.boomaa.note2jpg.create.NumberType;
import com.boomaa.note2jpg.create.Point;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
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
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main extends NFields {
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
            "Note2JPG: A .note to .jpg converter" + "\n" +
            "Developed by Nikhil for AP Physics" + "\n" +
            "github.com/Boomaa23/Note2JPG" + "\n" +
            "Copyright 2020. All Rights Reserved." + "\n" +
            "---------------------------------------" + "\n" +
            "NOTE: Note2JPG cannot parse shapes or positions of text boxes" + "\n");
    }

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        startTime = System.currentTimeMillis();
        argsList = Arrays.asList(args);
        Args.determineArgs();
        unzipNote();
        NSDictionary sessionMain = (NSDictionary) PropertyListParser.parse(new File(filename + "Session.plist"));
        NSDictionary[] sessionDict = Decode.isolateDictionary(((NSArray) (sessionMain.getHashMap().get("$objects"))).getArray());
        List<Image> pdfs = new ArrayList<>();

        float[] curvespoints = Decode.parseB64Numbers(NumberType.FLOAT, Decode.getDataFromDict(sessionDict, "curvespoints"));
        float[] curvesnumpoints = Decode.parseB64Numbers(NumberType.INTEGER, Decode.getDataFromDict(sessionDict, "curvesnumpoints"));
        float[] curveswidth = Decode.parseB64Numbers(NumberType.FLOAT, Decode.getDataFromDict(sessionDict, "curveswidth"));
        int[] curvescolors = Decode.parseB64Colors(Decode.getDataFromDict(sessionDict, "curvescolors"));

        Color[] colors = Decode.getColorsFromInts(curvescolors);

        System.gc();
        while (true) {
            if (scaleFactor <= 0) {
                throw new ArithmeticException("Cannot have a scale factor of zero");
            }
            try {
                Point[] points = Decode.getPoints(curvespoints);
                Curve[] curves = Decode.pointsToCurves(points, colors, curvesnumpoints, curveswidth);
                scaledWidth = Decode.getNumberFromDict(sessionDict, "pageWidthInDocumentCoordsKey") * scaleFactor;
                if (!noPdf) {
                    NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
                    String[] pdfLocs = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers"))).allKeys();
                    for (String pdfLoc : pdfLocs) {
                        pdfs.addAll(ImageUtil.getPdfImages(pdfLoc));
                    }
                    pages = pdfs.size();
                } else {
                    pages = 1;
                }
                scaledHeight = (int) (scaledWidth * pages * 11 / 8.5);
                bounds = Decode.getBounds(points);
                setupCurves(curves);
                ImageUtil.populateUnscaledAll(ImageUtil.getPdfCanvas(pdfs));
                if (!argsList.contains("--notextboxes")) {
                    textBoxes = Decode.getTextBoxes(sessionDict);
                    if (textBoxes.size() > 0) {
                        setupFrame();
                        displayFrame();
                        System.out.print("\rPositioning: " + textBoxes.get(0) + " (1 / " + textBoxes.size() + ")");
                        frame.getContentPane().addMouseListener(new PointTrigger());
                        while (tbClicked < textBoxes.size()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                        frame.setVisible(false);
                        ImageUtil.populateTextBoxes(textBoxes);
                        System.out.println(textBoxPoints);
                    }
                }
                break;
            } catch (OutOfMemoryError e) {
                circles = null;
                pdfs = new ArrayList<>();
                scaleFactor -= 2;
                System.gc();
                System.err.println("Memory limit exceeded with scale " + (scaleFactor + 2));
            }
        }

        if (argsList.contains("--display")) {
            setupFrame();
            displayFrame();
        } else if (frame != null) {
            frame.dispose();
        }
        if (!argsList.contains("--nofile")) {
            saveToFile();
        }
        cleanupFiles(new File(filename));
    }

    public static void setupCurves(Curve[] curves) {
        circles = new Circles(curves);
        circles.setSize(new Dimension(scaledWidth, scaledHeight));
    }

    public static void setupFrame() {
        frame = new JFrame("Note2JPG | " + filename.substring(0, filename.length() - 1));
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = (int) (screen.getWidth() / 2.5);
        frame.setSize(new Dimension(width, (int) (width * (11 / 8.5))));
    }

    public static void displayFrame() {
        Image img = ImageUtil.scaleImageFrame(upscaledAll);
        JLabel imgTemp = new JLabel(new ImageIcon(img));
        JPanel container = new JPanel();
        container.add(imgTemp);
        JScrollPane scrPane = new JScrollPane(container);
        scrPane.getVerticalScrollBar().setUnitIncrement(20);
        scrPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrPane);
        frame.setContentPane(scrPane);
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
    }

    public static void saveToFile() {
        int heightFinal = (int) (((double) iPadWidth / upscaledAll.getWidth()) * upscaledAll.getHeight());
        try {
            ImageIO.write(ImageUtil.scaleImage(upscaledAll, iPadWidth, heightFinal), "jpg", new File(filename.substring(0, filename.length() - 1) + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Completed in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds with scale=" + scaleFactor);
    }

    public static void unzipNote() {
        try {
            ZipFile zipFile = new ZipFile(filename.substring(0, filename.length() - 1) + ".note");
            zipFile.extractFile(filename + "Session.plist", ".");
            try {
                zipFile.extractFile(filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist", ".");
            } catch (ZipException e) {
                noPdf = true;
            }
            List<FileHeader> files = zipFile.getFileHeaders();
            for (FileHeader file : files) {
                if (file.getFileName().contains("PDFs")) {
                    zipFile.extractFile(file, ".");
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
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
}