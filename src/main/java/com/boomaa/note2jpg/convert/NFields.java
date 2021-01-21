package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.create.DrawRenderer;
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
    protected static List<String> savedNotes = new ArrayList<>();
    protected static JFrame consoleFrame;
    protected static JFrame frame;
    protected static DrawRenderer drawRenderer;
    protected static Point bounds;
    public static BufferedImage upscaledAll;
    protected static final int iPadWidth = 1536;
    protected static final int defWidth = 565;
    protected static final int leftOffset = 14;
    protected static int heightFinal;
    protected static int scaledWidth;
    protected static int scaledHeight;
    protected static double originalHeight;
    protected static double displayedHeight;
    protected static double displayedWidth;
    protected static int concatHeight;
    protected static double pages;
    protected static PDFState pdfState = PDFState.NONE;
    protected static long startTime;
    protected static boolean outputDone = false;
}
