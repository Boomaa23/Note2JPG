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
import java.util.Collections;
import java.util.List;
import java.util.MissingFormatArgumentException;

public class Main {
    public static JFrame frame;
    public static Circles circles;
    public static String filename;
    public static Point bounds;
    public static BufferedImage upscaledAll;
    public static int scaledWidth;
    public static int iPadWidth = 1536;
    public static int leftOffset = 14;
    public static int scaleFactor;
    public static int pdfRes;
    public static boolean noPdf = false;
    public static long startTime;

    static {
        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
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
        determineArgs(args);
        unzipNote();
        NSDictionary sessionMain = (NSDictionary) PropertyListParser.parse(new File(filename + "Session.plist"));
        NSDictionary[] sessionDict = DecodeUtil.isolateDictionary(((NSArray) (sessionMain.getHashMap().get("$objects"))).getArray());
        List<Image> pdfs = new ArrayList<>();
        if (!noPdf) {
            NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
            String[] pdfLocs = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers"))).allKeys();
            for (String pdfLoc : pdfLocs) {
                pdfs.addAll(ImageUtil.getPdfImages(pdfLoc));
            }
        }
        cleanupFiles(new File(filename));

        float[] curvespoints = DecodeUtil.getNumberB64String(NumberType.FLOAT, DecodeUtil.getDataFromDict(sessionDict, "curvespoints"));
        float[] curvesnumpoints = DecodeUtil.getNumberB64String(NumberType.INTEGER, DecodeUtil.getDataFromDict(sessionDict, "curvesnumpoints"));
        float[] curveswidth = DecodeUtil.getNumberB64String(NumberType.FLOAT, DecodeUtil.getDataFromDict(sessionDict, "curveswidth"));
        int[] curvescolors = DecodeUtil.parseB64Colors(DecodeUtil.getDataFromDict(sessionDict, "curvescolors"));

        Color[] colors = DecodeUtil.getColorsFromInts(curvescolors);
        Point[] points = DecodeUtil.getPoints(curvespoints);
        Curve[] curves = DecodeUtil.pointsToCurves(points, colors, curvesnumpoints, curveswidth);
        scaledWidth = DecodeUtil.getNumberFromDict(sessionDict, "pageWidthInDocumentCoordsKey") * scaleFactor;
        bounds = DecodeUtil.getBounds(points);

        setupCurves(curves);
        ImageUtil.populateUnscaledAll(ImageUtil.getPdfCanvas(pdfs));
        if (args.length == 4 && args[3].equals("--display")) {
            setupFrame();
            displayFrame();
        }
        saveToFile();
    }

    public static void determineArgs(String[] args) {
        if (args.length >= 1) {
            filename = args[0] + "/";
            if (args[1] != null) {
                scaleFactor = Integer.parseInt(args[1]);
            } else {
                scaleFactor = 4;
            }
            if (args[2] != null) {
                pdfRes = Integer.parseInt(args[2]) * 100;
            } else {
                pdfRes = 300;
            }
            System.out.println("Params: name=\"" + args[0] + "\" scale=" + scaleFactor + " pdfScale=" + args[2]);
        } else {
            throw new MissingFormatArgumentException("Not enough parameters passed");
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
        System.out.println("Completed in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
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

    public static boolean cleanupFiles(File fn) {
        File[] allContents = fn.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                cleanupFiles(file);
            }
        }
        return fn.delete();
    }
}
