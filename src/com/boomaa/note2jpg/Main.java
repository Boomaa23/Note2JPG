package com.boomaa.note2jpg;

import javax.swing.JFrame;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

public class Main {
    private static JFrame frame;

    public static void main(String[] args) {
        String b64 = fileToString("template/reverse/curvespoints2");
        float[] coordinatePairs = getCoords(b64);
        Point[] points = getPoints(coordinatePairs);
        setupFrame();
        displayPoints(points);
    }

    private static float[] getCoords(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
        float[] coords = new float[bytes.length / 4];
        for (int i = 0;i < bytes.length;i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(bytes, i, temp, 0, temp.length);
            coords[i / 4] = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return coords;
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

    private static void displayPoints(Point[] points) {
        frame.getContentPane().add(new Circles(points));
        frame.repaint();
        frame.revalidate();
        frame.setVisible(true);
        System.out.println(frame.getContentPane().getComponent(0));
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
}
