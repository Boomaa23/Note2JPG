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
import org.apache.commons.logging.Log;
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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
        if (args.length >= 3) {
            filename = args[0] + "/";
            scaleFactor = Integer.parseInt(args[1]);
            pdfRes = Integer.parseInt(args[2]) * 100;
            System.out.println("Params: filename=\"" + args[0] + "\" scaleFactor=" + scaleFactor + " pdfScaleFactor=" + args[2]);
        } else {
            throw new MissingFormatArgumentException("Not enough parameters passed");
        }
        unzipNote();
        NSDictionary sessionMain = (NSDictionary) PropertyListParser.parse(new File(filename + filename + "Session.plist"));
        NSDictionary[] sessionDict = DecodeUtil.isolateDictionary(((NSArray) (sessionMain.getHashMap().get("$objects"))).getArray());
        List<Image> pdfs = new ArrayList<>();
        if (!noPdf) {
            NSDictionary pdfMain = (NSDictionary) PropertyListParser.parse(new File(filename + filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist"));
            String[] pdfLocs = ((NSDictionary) (pdfMain.getHashMap().get("pageNumbers"))).allKeys();
            for (String pdfLoc : pdfLocs) {
                pdfs.addAll(ImageUtil.getPDFImages(pdfLoc));
            }
        }

        float[] curvespoints = DecodeUtil.getNumberB64String(NumberType.FLOAT, DecodeUtil.getDataFromDict(sessionDict, "curvespoints"));
        float[] curvesnumpoints = DecodeUtil.getNumberB64String(NumberType.INTEGER, DecodeUtil.getDataFromDict(sessionDict, "curvesnumpoints"));
        float[] curveswidth = DecodeUtil.getNumberB64String(NumberType.FLOAT, DecodeUtil.getDataFromDict(sessionDict, "curveswidth"));
        float[] curvescolors = makePositive(DecodeUtil.getNumberB64String(NumberType.INTEGER, DecodeUtil.getDataFromDict(sessionDict, "curvescolors")));

        Color[] colors = DecodeUtil.getColorsFromFloats(curvescolors);
        Point[] points = DecodeUtil.getPoints(curvespoints);
        Curve[] curves = DecodeUtil.pointsToCurves(points, colors, curvesnumpoints, curveswidth);
        scaledWidth = (int) (DecodeUtil.getNumberFromDict(sessionDict, "pageWidthInDocumentCoordsKey") * scaleFactor);
        bounds = DecodeUtil.getBounds(points);

        setupFrame();
        setupCurves(curves);
        if (args.length == 4 && args[3].equals("--display")) {
            displayFrame();
        }
        saveToFile(pdfs);
    }

    public static void setupCurves(Curve[] curves) {
        circles = new Circles(curves);
        circles.setSize(new Dimension(scaledWidth, bounds.getY()));
    }
    public static void setupFrame() {
        frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.setForeground(Color.WHITE);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(scaledWidth, bounds.getY()));
        frame.setSize(new Dimension(scaledWidth, bounds.getY()));
    }

    public static void displayFrame() {
        Image img = ImageUtil.scaleImageScreen(ImageUtil.getCirclesBuffImg());
        JLabel imgTemp = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(imgTemp);
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
    }

    public static float[] makePositive(float[] input) {
        for (int i = 0;i < input.length;i++) {
            if (input[i] < 0) {
                input[i] *= -1;
            }
        }
        return input;
    }

    public static void saveToFile(List<Image> pdfs) {
        try {
            int scaledHeight = (int) (scaledWidth * 11 / 8.5);
            int overallHeight = noPdf ? circles.getHeight() : scaledHeight * pdfs.size();
            BufferedImage canvas = new BufferedImage(scaledWidth, overallHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = (Graphics2D) canvas.getGraphics();
            int lastBottom = 0;
            for (int i = 0;i < pdfs.size();i++) {
                Image pdf = pdfs.get(i).getScaledInstance(canvas.getWidth(), scaledHeight, Image.SCALE_SMOOTH);
                g2.drawImage(pdf, 0, lastBottom, null);
                System.out.print("\r" + "PDF: " + (i + 1) + " / " + pdfs.size());
                lastBottom += pdf.getHeight(null);
            }
            System.out.println();
            BufferedImage draw = ImageUtil.getCirclesBuffImg();
            if (!noPdf) {
                g2.drawImage(ImageUtil.makeColorTransparent(draw, Color.WHITE), 0, 0, null);
            } else {
                g2.drawImage(draw, 0, 0, null);
            }
            g2.dispose();

            int heightFinal = (int) (((double) iPadWidth / canvas.getWidth()) * canvas.getHeight());
            Image scaledFinal = canvas.getScaledInstance(iPadWidth, heightFinal, Image.SCALE_SMOOTH);
            BufferedImage bufferScaledFinal = new BufferedImage(scaledFinal.getWidth(null), scaledFinal.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2f = (Graphics2D) bufferScaledFinal.getGraphics();
            g2f.drawImage(scaledFinal, 0, 0, null);
            g2f.dispose();

            ImageIO.write(bufferScaledFinal, "jpg", new File(filename.substring(0, filename.length() - 1) + ".jpg"));
            System.out.println("Write completed in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fileToString(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static void unzipNote() {
        try {
            ZipFile zipFile = new ZipFile(filename.substring(0, filename.length() - 1) + ".note");
            zipFile.extractFile(filename + "Session.plist", filename);
            try {
                zipFile.extractFile(filename + "NBPDFIndex/NoteDocumentPDFMetadataIndex.plist", filename);
            } catch (ZipException e) {
                noPdf = true;
            }
            List<FileHeader> files = zipFile.getFileHeaders();
            for (FileHeader file : files) {
                if (file.getFileName().contains("PDFs")) {
                    zipFile.extractFile(file, filename);
                }
            }
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }
}
