package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Curve;
import com.boomaa.note2jpg.create.EmbedImage;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.Shape;
import com.boomaa.note2jpg.create.TextBox;
import com.boomaa.note2jpg.state.NumberType;
import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.UID;

import java.awt.Color;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Decode extends NFields {
    public static Curve[] pointsToCurves(Point[] points, Color[] colors, float[] numPoints, float[] widths) {
        Curve[] curves = new Curve[numPoints.length];
        int done = 0;
        for (int i = 0; i < numPoints.length; i++) {
            int len = (int) numPoints[i];
            Point[] temp = new Point[len];
            System.arraycopy(points, done, temp, 0, len);
            curves[i] = new Curve(temp, i < colors.length ? colors[i] : Color.BLACK, widths[i] * Parameter.ImageScaleFactor.getValueInt());
            done += len;
        }
        return curves;
    }

    public static float[] parseB64Numbers(NumberType numberType, String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < bytes.length; i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(bytes, i, temp, 0, temp.length);
            ByteBuffer buffer = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
            switch (numberType) {
                case FLOAT:
                    out[i / 4] = buffer.getFloat();
                    break;
                case INTEGER:
                    out[i / 4] = buffer.getInt();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid datatype");
            }
        }
        return out;
    }

    public static Color[] parseB64Colors(String b64) {
        if (b64.trim().isBlank()) {
            return new Color[0];
        }
        List<Color> colors = new LinkedList<>();
        byte[] decoded = Base64.getDecoder().decode(b64.getBytes());
        String hexFull = String.format("%8x", new BigInteger(1, decoded));
        for (int i = 0; i < hexFull.length(); i += 8) {
            int rgb = Integer.parseInt(hexFull.substring(i, i + 6), 16);
            int alpha = Integer.parseInt(hexFull.substring(i + 6, i + 8), 16);
            colors.add(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha));
        }
        return colors.toArray(new Color[0]);
    }

    public static Point parseShapePoint(NSObject[] objects) {
        return new Point(nsObjFloatVal(objects[0]), nsObjFloatVal(objects[1]));
    }

    public static Color getShapeColor(NSDictionary appearance, String colorTypeSearch) {
        NSObject[] objects = ((NSArray) ((NSDictionary) appearance.get(colorTypeSearch)).get("rgba")).getArray();
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
                for (int y : ((Shape.NPolygon) shape).getYPoints()) {
                    if (y > maxY) {
                        maxY = y;
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

    public static List<Shape> getShapes(NSObject[] shapesMain) {
        List<Shape> shapes = new ArrayList<>();
        for (NSObject obj : shapesMain) {
            NSDictionary dict = (NSDictionary) obj;
            NSDictionary appearance = (NSDictionary) dict.get("appearance");

            Color strokeColor = Decode.getShapeColor(appearance, "strokeColor");
            Color fillColor = appearance.containsKey("fillColor") ?
                    Decode.getShapeColor(appearance, "fillColor") : null;
            int scale = Parameter.ImageScaleFactor.getValueInt();
            double strokeWidth = ((NSNumber) appearance.get("strokeWidth")).doubleValue() * scale;

            if (dict.containsKey("points")) {
                NSObject[] parsePoints = ((NSArray) dict.get("points")).getArray();
                int[] xPts = new int[parsePoints.length];
                int[] yPts = new int[parsePoints.length];
                for (int i = 0; i < parsePoints.length; i++) {
                    Point tempPt = Decode.parseShapePoint(((NSArray) parsePoints[i]).getArray());
                    xPts[i] = (int) ((tempPt.getXDbl() + leftOffset) * scale);
                    yPts[i] = (int) (tempPt.getYDbl() * scale);
                }

                shapes.add(new Shape.NPolygon(strokeColor, strokeWidth, fillColor, xPts, yPts));
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
                    NSObject[] dictPoints = ((NSArray) ((NSDictionary) dict.get("rotatedRect")).get("corners")).getArray();
                    firstPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                    secondPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    for (NSObject loopPoint : dictPoints) {
                        Point currPoint = Decode.parseShapePoint(((NSArray) loopPoint).getArray());
                        if (currPoint.getXInt() > secondPoint.getXInt()) {
                            secondPoint.setX(currPoint.getXInt());
                        }
                        if (currPoint.getYInt() > secondPoint.getYInt()) {
                            secondPoint.setY(currPoint.getYInt());
                        }
                        if (currPoint.getXInt() < firstPoint.getXInt()) {
                            firstPoint.setX(currPoint.getXInt());
                        }
                        if (currPoint.getYInt() < firstPoint.getYInt()) {
                            firstPoint.setY(currPoint.getYInt());
                        }
                    }
                } else {
                    continue;
                }
                firstPoint.setX((firstPoint.getXDbl() + leftOffset) * scale);
                firstPoint.setY(firstPoint.getYDbl() * scale);
                secondPoint.setX((secondPoint.getXDbl() + leftOffset) * scale);
                secondPoint.setY(secondPoint.getYDbl() * scale);
                shapes.add(new Shape(shapeType, strokeColor, strokeWidth, fillColor, firstPoint, secondPoint));
            }
        }
        return shapes;
    }

    public static List<TextBox> getTextBoxes(NSObject[] sessionObjects) {
        List<TextBox> boxes = new ArrayList<>();
        Map<NSDictionary, TextBox> boxClaimed = new HashMap<>();
        for (NSObject obj : sessionObjects) {
            if (obj instanceof NSDictionary) {
                NSDictionary dict = (NSDictionary) obj;
                NSDictionary textMeta = dict;
                double scale = Parameter.ImageScaleFactor.getValueInt();
                Point upperLeft = new Point(leftOffset * scale, 0);
                Point bottomRight = new Point((defWidth - leftOffset) * scale, Integer.MAX_VALUE);
                double rotRad = 0;

                if (dict.containsKey(("textStore"))) {
                    textMeta = (NSDictionary) sessionObjects[fromSUID(dict.get("textStore"))];
                    upperLeft = boxPointFromDict(sessionObjects, dict, "documentOrigin", true);
                    bottomRight = upperLeft.add(boxPointFromDict(sessionObjects, dict, "unscaledContentSize", true));
                    rotRad = ((NSNumber) dict.get("rotationDegrees")).doubleValue();
                } else if (boxClaimed.containsKey(dict)) {
                    continue;
                }

                if (textMeta.containsKey("attributedString")) {
                    NSDictionary innerTextMeta = (NSDictionary) sessionObjects[fromSUID(textMeta.get("attributedString"))];
                    NSObject[] innerMetaObjs = ((NSArray) innerTextMeta.get("NS.objects")).getArray();
                    var textWrapper = sessionObjects[fromSUID(innerMetaObjs[0])];
                    String text = null;
                    if (textWrapper instanceof NSString) {
                        text = ((NSString) textWrapper).getContent();
                    } else if (textWrapper instanceof NSDictionary && ((NSDictionary) textWrapper).containsKey("NS.bytes")) {
                        text = new String(((NSData) ((NSDictionary) textWrapper).get("NS.bytes")).bytes());
                    }
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    TextBox next = new TextBox(upperLeft, bottomRight, text, rotRad);

                    NSObject[] dataSubRanges = ((NSArray) ((NSDictionary) sessionObjects[fromSUID(innerMetaObjs[1])]).get("NS.objects")).getArray();
                    int endIdxCtr = 0;
                    for (NSObject rawRange : dataSubRanges) {
                        if (rawRange instanceof UID) {
                            NSObject[] srData = ((NSArray) ((NSDictionary) sessionObjects[fromSUID(rawRange)]).get("NS.objects")).getArray();
                            String rawColor = ((NSString) sessionObjects[fromSUID(srData[0])]).getContent();
                            int ioFirstComma = rawColor.indexOf(',');
                            int ioSecondComma = rawColor.indexOf(',', ioFirstComma + 1);
                            int ioThirdComma = rawColor.indexOf(',', ioSecondComma + 1);
                            Color rangeColor = new Color(Float.parseFloat(rawColor.substring(0, ioFirstComma)),
                                    Float.parseFloat(rawColor.substring(ioFirstComma + 1, ioSecondComma)),
                                    Float.parseFloat(rawColor.substring(ioSecondComma + 1, ioThirdComma)),
                                    Float.parseFloat(rawColor.substring(ioThirdComma + 1)));
                            String fullRange = ((NSString) sessionObjects[fromSUID(srData[1])]).getContent();
                            endIdxCtr += Integer.parseInt(fullRange.substring(fullRange.indexOf(',') + 2, fullRange.indexOf('}')));
                            double rangeFontSize = ((NSNumber) sessionObjects[fromSUID(((NSArray) ((NSDictionary) sessionObjects[fromSUID(srData[2])]).get("NS.objects")).getArray()[0])]).doubleValue();
                            next.putSubRange(endIdxCtr, new TextBox.SubRange(rangeColor, rangeFontSize));
                        }
                    }

                    boxClaimed.put(textMeta, next);
                    boxes.add(next);
                }
            }
        }
        return boxes;
    }

    private static Point boxPointFromDict(NSObject[] sessionObjects, NSDictionary dict, String search, boolean useOffset) {
        String valuePair = ((NSString) sessionObjects[fromSUID(dict.get(search))]).getContent();
        int idxMidComma = valuePair.indexOf(',');
        double offset = useOffset ? leftOffset : 0;
        int scale = Parameter.ImageScaleFactor.getValueInt();
        return new Point((Double.parseDouble(valuePair.substring(1, idxMidComma)) + offset) * scale,
                (Double.parseDouble(valuePair.substring(idxMidComma + 1, valuePair.length() - 1)) + offset) * scale);
    }

    public static List<EmbedImage> getEmbedImages(NSObject[] sessionObjects) {
        List<EmbedImage> images = new ArrayList<>();
        for (NSObject obj : sessionObjects) {
            if (obj instanceof NSDictionary) {
                NSDictionary dict = (NSDictionary) obj;
                if (dict.containsKey("figure")) {
                    double rotationRadians = ((NSNumber) dict.get("rotationDegrees")).doubleValue();
                    boolean roundCorners = ((NSNumber) dict.get("cornerMode")).doubleValue() == 2.0;
                    Point scaleDim = boxPointFromDict(sessionObjects, dict, "unscaledContentSize", false);
                    Point posUpperLeft = boxPointFromDict(sessionObjects, dict, "documentOrigin", false);

                    NSDictionary figureProperties = (NSDictionary) sessionObjects[fromSUID(dict.get("figure"))];
                    String cropRect = ((NSString) sessionObjects[fromSUID(figureProperties.get("FigureCropRectKey"))]).getContent();
                    int idxHalfComma = cropRect.indexOf(',', cropRect.indexOf(',') + 1);
                    String cropULRaw = cropRect.substring(2, idxHalfComma - 1);
                    String cropBRRaw = cropRect.substring(idxHalfComma + 3, cropRect.length() - 2);
                    int idxQuarterComma = cropULRaw.indexOf(',');
                    // No scale factor for crop b/c it is applied before scale
                    Point cropUL = new Point(Double.parseDouble(cropULRaw.substring(0, idxQuarterComma)),
                            Double.parseDouble(cropULRaw.substring(idxQuarterComma + 2)));
                    idxQuarterComma = cropBRRaw.indexOf(',');
                    Point cropBR = new Point(Double.parseDouble(cropBRRaw.substring(0, idxQuarterComma)),
                            Double.parseDouble(cropBRRaw.substring(idxQuarterComma + 2)));

                    NSObject imageObject = ((NSDictionary) sessionObjects[fromSUID(figureProperties.get("FigureBackgroundObjectKey"))]).get("kImageObjectSnapshotKey");
                    String loc = ((NSString) sessionObjects[fromSUID(((NSDictionary) sessionObjects[fromSUID(imageObject)]).get("relativePath"))]).getContent();

                    images.add(new EmbedImage(loc, posUpperLeft, scaleDim, cropUL, cropBR, rotationRadians, roundCorners));
                }
            }
        }
        return images;
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

    public static int fromSUID(NSObject obj) {
        byte[] b = ((UID) obj).getBytes();
        return b.length == 2 ? Byte.toUnsignedInt(b[0]) * 256 + Byte.toUnsignedInt(b[1]) : Byte.toUnsignedInt(b[0]);
    }
}
