package com.boomaa.note2jpg.function;

import com.boomaa.note2jpg.create.Curve;
import com.boomaa.note2jpg.create.NumberType;
import com.boomaa.note2jpg.create.Point;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DecodeUtil extends Main {
    public static Curve[] pointsToCurves(Point[] points, Color[] colors, float[] numPoints, float[] widths) {
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

    public static float[] getNumberB64String(NumberType numberType, String b64) {
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

    public static Color[] getColorsFromFloats(float[] raw) {
        Color[] colors = new Color[raw.length];
        for (int i = 0;i < raw.length;i++) {
            colors[i] = new Color(((int) raw[i]), false);
        }
        return colors;
    }

    public static Point[] getPoints(float[] coords) {
        Point[] points = new Point[coords.length / 2];
        int reps = 0;
        for (int i = 0;i < coords.length - 1;i += 2) {
            points[i - reps] = new Point((coords[i] + leftOffset) * scaleFactor, coords[i + 1] * scaleFactor);
            reps++;
        }
        return points;
    }

    public static Point getBounds(Point[] points) {
        int maxY = 0;
        for (Point point : points) {
            if (point.getY() > maxY) {
                maxY = point.getY();
            }
        }
        return new Point(scaledWidth, maxY);
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
        for (int i = 0;i < dict.length;i++) {
            NSData obj = (NSData) dict[i].get(key);
            if (obj != null && !obj.getBase64EncodedData().trim().equals("")) {
                return obj.getBase64EncodedData();
            }
        }
        return "";
    }

    public static int getNumberFromDict(NSDictionary[] dict, String key) {
        for (int i = 0;i < dict.length;i++) {
            NSNumber obj = (NSNumber) dict[i].get(key);
            if (obj != null) {
                return (int) obj.floatValue();
            }
        }
        return -1;
    }
}
