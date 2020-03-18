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
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Main extends NFields {
    static {
        @SuppressWarnings("unchecked") List<Logger> loggers =
            Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
        System.out.println(
            "---------------------------------------" + "\n" +
            "Note2JPG: A .note to .jpg converter" + "\n" +
            "Developed by Nikhil for AP Physics" + "\n" +
            "github.com/Boomaa23/Note2JPG" + "\n" +
            "Copyright 2020. All Rights Reserved." + "\n" +
            "---------------------------------------" + "\n" +
            "NOTE: Note2JPG cannot parse entered text or created shapes" + "\n");
    }

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        startTime = System.currentTimeMillis();
        argsList = Arrays.asList(args);
        Args.determineArgs();
        unzipNote();
        NSDictionary sessionMain = (NSDictionary) PropertyListParser.parse(new File(filename + "Session.plist"));
        NSDictionary[] sessionDict = Decode.isolateDictionary(((NSArray) (sessionMain.getHashMap().get("$objects"))).getArray());
        List<Image> pdfs = new ArrayList<>();
        if (!noPdf) {
            NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
            String[] pdfLocs = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers"))).allKeys();
            for (String pdfLoc : pdfLocs) {
                pdfs.addAll(ImageUtil.getPdfImages(pdfLoc));
            }
        }
//        cleanupFiles(new File(filename));

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
                bounds = Decode.getBounds(points);
                setupCurves(curves);
                ImageUtil.populateUnscaledAll(ImageUtil.getPdfCanvas(pdfs));
                break;
            } catch (OutOfMemoryError e) {
                circles = null;
                scaleFactor -= 2;
                System.gc();
                System.err.println("Memory limit exceeded with scale " + (scaleFactor + 2));
            }
        }

        if (argsList.contains("--display")) {
            setupFrame();
            displayFrame();
        }
        if (!argsList.contains("--nofile")) {
            saveToFile();
        }
    }

    public static void setupCurves(Curve[] curves) {
        circles = new Circles(curves);
        circles.setSize(new Dimension(scaledWidth, bounds.getY()));
    }

    public static void setupFrame() {
        frame = new JFrame("Note2JPG | " + filename.substring(0, filename.length() - 1));
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = (int) (screen.getWidth() / 2.5);
        frame.setSize(new Dimension(width, (int) ((bounds.getY() / scaleFactor) * 1.5)));
    }

    public static void displayFrame() {
        Image img = ImageUtil.scaleImageFrame(upscaledAll);
        JLabel imgTemp = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(imgTemp);
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
    }

    public static BufferedImage scaleImage(BufferedImage canvas, int width, int height) {
        Image scaled = canvas.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferScaled = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2f = (Graphics2D) bufferScaled.getGraphics();
        g2f.drawImage(scaled, 0, 0, null);
        g2f.dispose();
        return bufferScaled;
    }

    public static void saveToFile() {
        int heightFinal = (int) (((double) iPadWidth / upscaledAll.getWidth()) * upscaledAll.getHeight());
        try {
            ImageIO.write(scaleImage(upscaledAll, iPadWidth, heightFinal), "jpg", new File(filename.substring(0, filename.length() - 1) + ".jpg"));
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
