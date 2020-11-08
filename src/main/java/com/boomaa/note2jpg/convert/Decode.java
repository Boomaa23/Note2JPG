package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Curve;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.Shape;
import com.boomaa.note2jpg.state.NumberType;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;

import java.awt.Color;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public class Decode extends NFields {
    public static Curve[] pointsToCurves(Point[] points, Color[] colors, float[] numPoints, float[] widths) {
        Curve[] curves = new Curve[numPoints.length];
        int done = 0;
        for (int i = 0; i < numPoints.length; i++) {
            int len = (int) numPoints[i];
            Point[] temp = new Point[len];
            System.arraycopy(points, done, temp, 0, len);
            curves[i] = new Curve(temp, colors[i], widths[i] * Parameter.ImageScaleFactor.getValueInt());
            done += len;
        }
        return curves;
    }

    public static float[] parseB64Numbers(NumberType numberType, String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
        float[] output = new float[bytes.length / 4];
        for (int i = 0; i < bytes.length; i += 4) {
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

    public static Color[] parseB64Colors(String b64) {
        byte[] decoded = Base64.getDecoder().decode(b64.getBytes());
        String hexFull = String.format("%040x", new BigInteger(1, decoded));
        Color[] colors = new Color[(hexFull.length() / 8)];
        int parsed = 0;
        for (int i = 0; i < colors.length; i++) {
            int rgb = Integer.parseInt(hexFull.substring(parsed, parsed + 6), 16);
            int alpha = Integer.parseInt(hexFull.substring(parsed + 6, parsed + 8), 16);
            colors[i] = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
            parsed += 8;
        }
        return colors;
    }

    public static Point parseShapePoint(NSObject[] objects) {
        return new Point(nsObjFloatVal(objects[0]), nsObjFloatVal(objects[1]));
    }

    public static Color getShapeStrokeColor(NSObject[] objects) {
        return new Color(nsObjFloatVal(objects[0]),
                nsObjFloatVal(objects[1]),
                nsObjFloatVal(objects[2]),
                nsObjFloatVal(objects[3]));
    }

    private static float nsObjFloatVal(NSObject obj) {
        return ((NSNumber) obj).floatValue();
    }

    public static Point[] getScaledPoints(float[] coords) {
        Point[] points = new Point[coords.length / 2];
        int reps = 0;
        int scale = Parameter.ImageScaleFactor.getValueInt();
        for (int i = 0; i < coords.length - 1; i += 2) {
            points[i - reps] = new Point((coords[i] + leftOffset) * scale, coords[i + 1] * scale);
            reps++;
        }
        return points;
    }

    public static Point getBounds(Point[] points, Shape[] shapes) {
        int maxY = 0;
        for (Point point : points) {
            if (point.getYInt() > maxY) {
                maxY = point.getYInt();
            }
        }
        for (Shape shape : shapes) {
            if (shape instanceof Shape.NPolygon) {
                for (Point point : ((Shape.NPolygon) shape).getPoints()) {
                    if (point.getYInt() > maxY) {
                        maxY = point.getYInt();
                    }
                }
            } else {
                if (shape.getEndPoint().getYInt() > maxY) {
                    maxY = shape.getEndPoint().getYInt();
                }
                if (shape.getBeginPoint().getYInt() > maxY) {
                    maxY = shape.getBeginPoint().getYInt();
                }
            }
        }
        return new Point(scaledWidth, maxY);
    }

    public static List<String> getTextBoxes(NSDictionary[] sessionDict) {
        List<String> textBoxes = new ArrayList<>();
        Pattern filter = Pattern.compile("^[a-zA-Z0-9_]{8}-[a-zA-Z0-9_]{4}-[a-zA-Z0-9_]{4}-[a-zA-Z0-9_]{4}-[a-zA-Z0-9_]{12}$");
        for (NSDictionary dict : sessionDict) {
            String st = Arrays.toString(dict.allKeys());
            if (st.contains("NS.bytes")) {
                String text = new String(Base64.getDecoder().decode(((NSData) (dict.get("NS.bytes"))).getBase64EncodedData().getBytes())).trim();
                if (!filter.matcher(text).matches() && !textBoxes.contains(text) && !text.trim().equals("")) {
                    textBoxes.add(text);
                }
            }
        }
        return textBoxes;
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

    public static String getDataFromDict(NSDictionary[] dict, String key) {
        for (NSDictionary loopDict : dict) {
            NSData obj = (NSData) loopDict.get(key);
            if (obj != null && !obj.getBase64EncodedData().trim().equals("")) {
                return obj.getBase64EncodedData();
            }
        }
        return "";
    }

    public static int getNumberFromDict(NSDictionary[] dict, String key) {
        for (NSDictionary loopDict : dict) {
            NSNumber obj = (NSNumber) loopDict.get(key);
            if (obj != null) {
                return (int) obj.floatValue();
            }
        }
        return -1;
    }
}
