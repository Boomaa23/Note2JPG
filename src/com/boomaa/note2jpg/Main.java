package com.boomaa.note2jpg;

import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {
    private static JFrame frame;
    private static String filename;

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        filename = "8 | 0 Linearizing Video Notes/";
        unzipNote();
        NSDictionary parser = (NSDictionary) PropertyListParser.parse(new File(filename + filename + "Session.plist"));
        NSDictionary[] dict = isolateDictionary(((NSArray) (parser.getHashMap().get("$objects"))).getArray());

        float[] curvespoints = getNumberB64String(NumberType.FLOAT, getKeyFromDict(dict, "curvespoints"));
        float[] curvesnumpoints = getNumberB64String(NumberType.INTEGER, getKeyFromDict(dict, "curvesnumpoints"));
        float[] curveswidth = getNumberB64String(NumberType.FLOAT, getKeyFromDict(dict, "curveswidth"));
        float[] rawColors = makePositive(getNumberB64String(NumberType.INTEGER, getKeyFromDict(dict, "curvescolors")));

        Color[] colors = getColorsFromFloats(rawColors);
        Point[] points = getPoints(curvespoints);
        Curve[] curves = pointsToCurves(points, colors, curvesnumpoints, curveswidth);

        setupFrame();
        displayCurves(curves);
        saveToFile();
    }

    private static Curve[] pointsToCurves(Point[] points, Color[] colors, float[] numPoints, float[] widths) {
        Curve[] curves = new Curve[numPoints.length];
        int done = 0;
        for (int i = 0;i < numPoints.length;i++) {
            int len = (int) numPoints[i];
            Point[] temp = new Point[len];
            System.arraycopy(points, done, temp, 0, len);
            curves[i] = new Curve(temp, colors[i], (int) widths[i]);
            done += len;
        }
        return curves;
    }

    private static float[] getNumberB64String(NumberType numberType, String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
        float[] output = new float[bytes.length / 4];
        for (int i = 0;i < bytes.length;i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(bytes, i, temp, 0, temp.length);
            ByteBuffer buffer = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
            switch (numberType) {
                case FLOAT:
                    output[i / 4] = buffer.getFloat();
                    break;
                case INTEGER:
                    output[i / 4] = buffer.getInt();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid datatype");
            }
        }
        return output;
    }

    private static Color[] getColorsFromFloats(float[] raw) {
        Color[] colors = new Color[raw.length];
        for (int i = 0;i < raw.length;i++) {
            colors[i] = new Color((int) raw[i], false);
        }
        return colors;
    }

    private static Point[] getPoints(float[] coords) {
        Point[] points = new Point[coords.length / 2];
        int reps = 0;
        for (int i = 0;i < coords.length - 1;i += 2) {
            points[i - reps] = new Point(coords[i], coords[i + 1], i - reps);
            reps++;
        }
        return points;
    }

    private static void setupFrame() {
        frame = new JFrame();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
    }

    private static void displayCurves(Curve[] curves) {
        frame.getContentPane().add(new Circles(curves));
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
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

    private static void saveToFile() {
        BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        frame.printAll(g2d);
        g2d.dispose();
        try {
            ImageIO.write(img, "jpg", new File(filename.substring(0, filename.length() - 1) + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float[] makePositive(float[] input) {
        for (int i = 0;i < input.length;i++) {
            if (input[i] < 0) {
                input[i] *= -1;
            }
        }
        return input;
    }

    private static NSDictionary[] isolateDictionary(NSObject[] objects) {
        List<NSDictionary> dict = new ArrayList<>();
        for (NSObject obj : objects) {
            if (obj instanceof NSDictionary) {
                dict.add((NSDictionary) obj);
            }
        }
        NSDictionary[] out = new NSDictionary[dict.size()];
        return dict.toArray(out);
    }

    private static String getKeyFromDict(NSDictionary[] dict, String key) {
        for (int i = 0;i < dict.length;i++) {
            NSData obj = (NSData) dict[i].get(key);
            if (obj != null) {
                return obj.getBase64EncodedData();
            }
        }
        return "";
    }

    private static void unzipNote() {
        try {
            ZipFile zipFile = new ZipFile(filename.substring(0, filename.length() - 1) + ".note");
            zipFile.extractFile(filename + "Session.plist", filename);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }
}
