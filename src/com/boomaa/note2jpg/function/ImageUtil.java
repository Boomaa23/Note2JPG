package com.boomaa.note2jpg.function;

import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
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

public class ImageUtil extends Main {
    public static Image scaleImageScreen(Image image) {
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        if (height >= screen.getHeight()) {
            width /= (height / screen.getHeight());
            height = screen.getHeight();
        }

        return image.getScaledInstance((int) (width), (int) (height), Image.SCALE_SMOOTH);
    }

    public static BufferedImage getCirclesBuffImg() {
        BufferedImage img = new BufferedImage(scaledWidth, bounds.getY(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        circles.print(g2d);
        g2d.setBackground(Color.WHITE);
        g2d.dispose();
        return img;
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

    public static List<Image> getPDFImages(String pdf) {
        try {
            PDFDocument document = new PDFDocument();
            document.load(new File(filename + filename + "PDFs/" + pdf));

            SimpleRenderer renderer = new SimpleRenderer();
            renderer.setResolution(pdfRes);
            return renderer.render(document);
        } catch (IOException | RendererException | DocumentException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
