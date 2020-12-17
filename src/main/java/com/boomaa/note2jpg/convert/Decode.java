package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Curve;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Decode extends NFields {
    private static class RefTag extends LinkedList<Byte> {
    }

    public static void followRefs(NSObject[] objs) {
        Map<RefTag, String> keys = new HashMap<>();
        Map<RefTag, String> values = new HashMap<>();
        for (NSObject obj : objs) {
            if (obj instanceof NSDictionary) {
                NSDictionary dict = (NSDictionary) obj;
                if (dict.containsKey("NS.keys") && dict.containsKey("NS.objects") && ((NSArray) dict.get("NS.keys")).getArray().length != 0) {
                    addRef(objs, dict, keys, new RefTag(), "NS.keys");
                    addRef(objs, dict, values, new RefTag(), "NS.objects");
                }
            }
        }
    }

    private static void addRef(NSObject[] objs, NSDictionary dict, Map<RefTag, String> toAddMap, RefTag prevRefs, String search) {
        NSObject[] nsSelected = ((NSArray) dict.get(search)).getArray();
        for (NSObject loop : nsSelected) {
            if (loop instanceof UID) {
                byte ref = ((UID) loop).getBytes()[0];
                prevRefs.add(ref);
                NSObject innerObj = objs[ref];
                if (innerObj instanceof NSString) {
                    toAddMap.put(prevRefs, ((NSString) innerObj).getContent());
                } else if (innerObj instanceof NSDictionary) {
                    NSDictionary innerDict = (NSDictionary) innerObj;
                    if (innerDict.containsKey("NS.bytes")) {
                        toAddMap.put(prevRefs, new String(Base64.getDecoder().decode(((NSData) innerDict.get("NS.bytes")).getBase64EncodedData())));
                    } else if (innerDict.containsKey("NS.objects")) {
                        addRef(objs, innerDict, toAddMap, prevRefs, "NS.objects");
                    }
                    System.out.println("whelp");
                }
            }
        }
    }

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

    public static float[] parseB64Numbers(NumberType numberType, String[] b64s) {
        List<Float> outList = new LinkedList<>();
        for (String b64 : b64s) {
            byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
            float[] bufferOut = new float[bytes.length / 4];
            for (int i = 0; i < bytes.length; i += 4) {
                byte[] temp = new byte[4];
                System.arraycopy(bytes, i, temp, 0, temp.length);
                ByteBuffer buffer = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
                switch (numberType) {
                    case FLOAT:
                        bufferOut[i / 4] = buffer.getFloat();
                        break;
                    case INTEGER:
                        bufferOut[i / 4] = buffer.getInt();
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid datatype");
                }
            }
            for (float val : bufferOut) {
                outList.add(val);
            }
        }
        float[] outArray = new float[outList.size()];
        for (int i = 0; i < outArray.length; i++) {
            outArray[i] = outList.get(i);
        }
        return outArray;
    }

    public static Color[] parseB64Colors(String[] b64s) {
        List<Color> out = new LinkedList<>();
        for (String b64 : b64s) {
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
            out.addAll(Arrays.asList(colors));
        }
        return out.toArray(new Color[0]);
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

    public static List<Shape> getShapes(NSObject[] shapesMain) {
        List<Shape> shapes = new ArrayList<>();
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
                    NSObject[] dictPoints = ((NSArray) ((NSDictionary) dict.get("rotatedRect")).get("corners")).getArray();
                    firstPoint = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
                    secondPoint = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    for (NSObject loopPoint : dictPoints) {
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
        return shapes;
    }

    public static List<TextBox> getTextBoxes(NSObject[] sessionObjects) {
        List<TextBox> boxes = new ArrayList<>();
        int scale = Parameter.ImageScaleFactor.getValueInt();
        for (NSObject obj : sessionObjects) {
            if (obj instanceof NSDictionary) {
                NSDictionary dict = (NSDictionary) obj;
                String dictKeys = Arrays.toString(dict.allKeys());
                if (dictKeys.contains("textStore")) {
                    NSDictionary textMeta = (NSDictionary) sessionObjects[fromSUID(dict.get("textStore"))];
                    NSDictionary innerTextMeta = (NSDictionary) sessionObjects[fromSUID(textMeta.get("attributedString"))];
                    NSObject[] innerMetaObjs = ((NSArray) innerTextMeta.get("NS.objects")).getArray();
                    String text = ((NSString) sessionObjects[fromSUID(innerMetaObjs[0])]).getContent();

                    Point dimensions = boxPointFromDict(sessionObjects, dict, "unscaledContentSize");
                    Point upperLeft = boxPointFromDict(sessionObjects, dict, "documentOrigin");
                    TextBox next = new TextBox(upperLeft, upperLeft.add(dimensions), text);

                    NSObject[] dataSubRanges = ((NSArray) ((NSDictionary) sessionObjects[fromSUID(innerMetaObjs[1])]).get("NS.objects")).getArray();
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
                            int endingIdx = Integer.parseInt(fullRange.substring(fullRange.indexOf(',') + 2, fullRange.indexOf('}')));
                            double rangeFontSize = ((NSNumber) sessionObjects[fromSUID(((NSArray) ((NSDictionary) sessionObjects[fromSUID(srData[2])]).get("NS.objects")).getArray()[0])]).doubleValue();
                            next.putSubRange(endingIdx, new TextBox.SubRange(rangeColor, rangeFontSize));
                        }
                    }

                    boxes.add(next);
                }
            } else if (obj instanceof NSString) {
                //TODO support inline text
//                String content = ((NSString) obj).getContent();
//                if (!textBoxContents.contains(content)) {
//                    boxes.add(new TextBox(new Point(leftOffset * scale, 0),
//                            new Point(Integer.MAX_VALUE, Integer.MAX_VALUE), content));
//                }
            }
        }
        return boxes;
    }

    private static Point boxPointFromDict(NSObject[] sessionObjects, NSDictionary dict, String search) {
        String valuePair = ((NSString) sessionObjects[fromSUID(dict.get(search))]).getContent();
        int idxMidComma = valuePair.indexOf(',');
        int scale = Parameter.ImageScaleFactor.getValueInt();
        return new Point((Double.parseDouble(valuePair.substring(1, idxMidComma - 1)) + leftOffset) * scale,
                (Double.parseDouble(valuePair.substring(idxMidComma + 1, valuePair.length() - 1)) + leftOffset) * scale);
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

    public static String[] getDataFromDict(NSDictionary[] dict, String key) {
        List<String> out = new LinkedList<>();
        for (NSDictionary loopDict : dict) {
            NSData obj = (NSData) loopDict.get(key);
            if (obj != null && !obj.getBase64EncodedData().trim().equals("")) {
                out.add(obj.getBase64EncodedData());
            }
        }
        return out.toArray(new String[0]);
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

    public static byte fromSUID(NSObject obj) {
        return ((UID) obj).getBytes()[0];
    }
}
