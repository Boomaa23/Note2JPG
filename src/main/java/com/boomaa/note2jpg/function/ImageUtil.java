package com.boomaa.note2jpg.function;

import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil extends NFields {
    public static Image scaleImageFrame(Image image) {
        double width = image.getWidth(null);
        double height = image.getHeight(null);

        if (width >= frame.getWidth()) {
            height /= (width / frame.getWidth());
            width = frame.getWidth();
        }

        return image.getScaledInstance((int) (width), (int) (height), Image.SCALE_SMOOTH);
    }

    public static void populateUnscaledAll(BufferedImage pdfs) {
        Graphics2D g2 = (Graphics2D) pdfs.getGraphics();
        if (noPdf) {
            circles.print(g2);
        } else {
            System.out.println();
            BufferedImage img = new BufferedImage(scaledWidth, bounds.getY(), BufferedImage.TYPE_INT_RGB);
            Graphics2D cg2 = img.createGraphics();
            circles.print(cg2);
            cg2.dispose();
            g2.drawImage(ImageUtil.makeColorTransparent(img, Color.WHITE), 0, 0, null);
        }
        g2.dispose();
        upscaledAll = pdfs;
    }

    public static Image makeColorTransparent(Image im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    return 0x00FFFFFF & rgb;
                } else {
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    public static List<Image> getPdfImages(String pdf) throws OutOfMemoryError {
        try {
            PDFDocument document = new PDFDocument();
            document.load(new File(filename + "PDFs/" + pdf));

            SimpleRenderer renderer = new SimpleRenderer();
            renderer.setResolution(pdfRes);
            return renderer.render(document);
        } catch (IOException | RendererException | DocumentException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static BufferedImage getPdfCanvas(List<Image> pdfs) throws OutOfMemoryError {
        int scaledHeight = (int) (scaledWidth * 11 / 8.5);
        int overallHeight = noPdf ? circles.getHeight() : scaledHeight * pdfs.size();
        BufferedImage canvas = new BufferedImage(scaledWidth, overallHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) canvas.getGraphics();
        int lastBottom = 0;
        for (int i = 0;i < pdfs.size();i++) {
            Image pdf = pdfs.get(i).getScaledInstance(canvas.getWidth(), scaledHeight, Image.SCALE_SMOOTH);
            g2.drawImage(pdf, 0, lastBottom, null);
            System.out.print("\r" + "PDF: " + (i + 1) + " / " + pdfs.size());
            lastBottom += pdf.getHeight(null);
        }
        return canvas;
    }
}
