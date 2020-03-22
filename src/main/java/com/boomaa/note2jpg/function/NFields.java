package com.boomaa.note2jpg.function;

import com.boomaa.note2jpg.create.Box;
import com.boomaa.note2jpg.create.Circles;
import com.boomaa.note2jpg.create.FilenameSource;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.integration.NEOExecutor;

import javax.swing.JFrame;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class NFields {
    protected static JFrame frame;
    protected static Circles circles;
    protected static List<String> filenames = new ArrayList<>();
    protected static Point bounds;
    protected static BufferedImage upscaledAll;
    protected static List<String> argsList;
    protected static List<Box> textBoxBounds = new ArrayList<>();
    protected static List<String> textBoxContents = new ArrayList<>();
    protected static NEOExecutor neoExecutor;
    protected static FilenameSource fnSource;
    protected static int iPadWidth = 1536;
    protected static int leftOffset = 14;
    protected static int heightFinal;
    protected static int scaledWidth;
    protected static int scaledHeight;
    protected static int scaleFactor;
    protected static double originalHeight;
    protected static double displayedHeight;
    protected static double displayedWidth;
    protected static int pdfRes;
    protected static int pages;
    protected static boolean noPdf = false;
    protected static long startTime;
}
