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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.MissingFormatArgumentException;

public class Main {
    private static JFrame frame;
    private static Circles circles;
    private static String filename;
    private static Point bounds;
    private static double scaleFactor = 8;

    public static void main(String[] args) throws IOException, PropertyListFormatException, ParseException, SAXException, ParserConfigurationException {
        if (args.length > 0) {
            filename = args[0] + "/";
        } else {
            throw new MissingFormatArgumentException("Note filename not passed or not found");
        }
        unzipNote();
        NSDictionary parser = (NSDictionary) PropertyListParser.parse(new File(filename + filename + "Session.plist"));
        NSDictionary[] dict = isolateDictionary(((NSArray) (parser.getHashMap().get("$objects"))).getArray());

        float[] curvespoints = getNumberB64String(NumberType.FLOAT, getKeyFromDict(dict, "curvespoints"));
        float[] curvesnumpoints = getNumberB64String(NumberType.INTEGER, getKeyFromDict(dict, "curvesnumpoints"));
        float[] curveswidth = getNumberB64String(NumberType.FLOAT, getKeyFromDict(dict, "curveswidth"));
        float[] curvescolors = makePositive(getNumberB64String(NumberType.INTEGER, getKeyFromDict(dict, "curvescolors")));

        Color[] colors = getColorsFromFloats(curvescolors);
        Point[] points = getPoints(curvespoints);
        Curve[] curves = pointsToCurves(points, colors, curvesnumpoints, curveswidth);
        bounds = getMaximumBounds(points);

        setupFrame();
        setupCurves(curves);
        if (args.length > 1 && args[1].equals("--display")) {
            displayFrame();
        }
        saveToFile();
    }

    private static void setupFrame() {
        frame = new JFrame();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(bounds.getX(), bounds.getY()));
        frame.setSize(new Dimension(bounds.getX(), bounds.getY()));
    }

    private static void setupCurves(Curve[] curves) {
        circles = new Circles(curves);
        circles.setSize(new Dimension(bounds.getX(), bounds.getY()));
    }

    private static void displayFrame() {
        Image img = scaleImageScreen(getCirclesBuffImg());
        JLabel imgTemp = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(imgTemp);
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
    }

    private static Curve[] pointsToCurves(Point[] points, Color[] colors, float[] numPoints, float[] widths) {
        Curve[] curves = new Curve[numPoints.length];
        int done = 0;
        for (int i = 0;i < numPoints.length;i++) {
            int len = (int) numPoints[i];
            Point[] temp = new Point[len];
            System.arraycopy(points, done, temp, 0, len);
            curves[i] = new Curve(temp, colors[i], widths[i] * scaleFactor);
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
            colors[i] = new Color(((int) raw[i]), false);
        }
        return colors;
    }

    private static Point[] getPoints(float[] coords) {
        Point[] points = new Point[coords.length / 2];
        int reps = 0;
        for (int i = 0;i < coords.length - 1;i += 2) {
            points[i - reps] = new Point(coords[i] * scaleFactor, coords[i + 1] * scaleFactor);
            reps++;
        }
        return points;
    }

    private static Point getMaximumBounds(Point[] points) {
        int maxX = 0, maxY = 0;
        for (Point point : points) {
            if (point.getX() > maxX) {
                maxX = point.getX();
            }
            if (point.getY() > maxY) {
                maxY = point.getY();
            }
        }
        return new Point(maxX, maxY + 50);
    }

    private static float[] makePositive(float[] input) {
        for (int i = 0;i < input.length;i++) {
            if (input[i] < 0) {
                input[i] *= -1;
            }
        }
        return input;
    }

    public static Image scaleImageScreen(Image image) {
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        if (height >= screen.getHeight()) {
            width /= (height / screen.getHeight());
            height = screen.getHeight();
        }

        return image.getScaledInstance((int) (width), (int) (height) - 50, Image.SCALE_SMOOTH);
    }

    private static BufferedImage getCirclesBuffImg() {
        BufferedImage img = new BufferedImage(bounds.getX(), bounds.getY(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        circles.print(g2d);
        g2d.setBackground(Color.WHITE);
        g2d.dispose();
        return img;
    }

    public static NSDictionary[] isolateDictionary(NSObject[] objects) {
        List<NSDictionary> dict = new ArrayList<>();
        for (NSObject obj : objects) {
            if (obj instanceof NSDictionary) {
                dict.add((NSDictionary) obj);
            }
        }
        NSDictionary[] out = new NSDictionary[dict.size()];
        return dict.toArray(out);
    }

    public static String getKeyFromDict(NSDictionary[] dict, String key) {
        for (int i = 0;i < dict.length;i++) {
            NSData obj = (NSData) dict[i].get(key);
            if (obj != null) {
                return obj.getBase64EncodedData();
            }
        }
        return "";
    }

    private static void saveToFile() {
        try {
            BufferedImage circleImg = getCirclesBuffImg();
            int ipadWidth = 1536;
            Image rimg = circleImg.getScaledInstance(ipadWidth, ipadWidth * circleImg.getHeight() / circleImg.getWidth(), Image.SCALE_SMOOTH);
            BufferedImage img = new BufferedImage(rimg.getWidth(null), rimg.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();
            g2d.drawImage(rimg, 0, 0, null);
            g2d.dispose();
            ImageIO.write(img, "jpg", new File(filename.substring(0, filename.length() - 1) + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
