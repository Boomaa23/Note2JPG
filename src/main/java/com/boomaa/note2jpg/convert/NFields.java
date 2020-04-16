package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.create.Box;
import com.boomaa.note2jpg.create.Circles;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.integration.NEOExecutor;
import com.boomaa.note2jpg.state.PDFState;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class NFields {
    public static NEOExecutor neoExecutor;
    public static List<String> argsList;
    protected static List<String> notenames = new ArrayList<>();
    protected static JFrame frame;
    protected static Circles circles;
    protected static Point bounds;
    public static BufferedImage upscaledAll;
    protected static List<Box> textBoxBounds = new ArrayList<>();
    protected static List<String> textBoxContents = new ArrayList<>();
    protected static List<Point> imageBounds = new ArrayList<>();
    protected static List<BufferedImage> imageList = new ArrayList<>();
    protected static int iPadWidth = 1536;
    protected static int leftOffset = 14;
    protected static int heightFinal;
    protected static int scaledWidth;
    protected static int scaledHeight;
    protected static double originalHeight;
    protected static double displayedHeight;
    protected static double displayedWidth;
    protected static int pages;
    protected static PDFState pdfState = PDFState.NONE;
    protected static long startTime;
    protected static boolean hasImages = false;
}
