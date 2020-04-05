package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Corner;
import com.boomaa.note2jpg.state.PDFState;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil extends NFields {
    static {
        System.load(System.getProperty("user.dir") + "/lib/gsdll64.dll");
    }

    public static Image scaleImageFrame(Image image) {
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        originalHeight = height;

        if (width < frame.getWidth()) {
            height /= (width / frame.getWidth());
            width = frame.getWidth();
        }
        if (width > frame.getWidth()) {
            height /= (width / frame.getWidth());
            width = frame.getWidth();
        }
        displayedHeight = height;

        return image.getScaledInstance((int) (width), (int) (height), Image.SCALE_SMOOTH);
    }

    public static BufferedImage scaleImage(BufferedImage canvas, int width, int height) {
        Image scaled = canvas.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferScaled = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2f = (Graphics2D) bufferScaled.getGraphics();
        g2f.drawImage(scaled, 0, 0, null);
        g2f.dispose();
        return bufferScaled;
    }

    public static void populateUnscaledAll(BufferedImage pdfs) {
        Graphics2D g2 = (Graphics2D) pdfs.getGraphics();
        if (pdfState == PDFState.NONE) {
            circles.print(g2);
        } else {
            System.out.println();
            BufferedImage img = new BufferedImage(scaledWidth, (int) (scaledWidth * pages * 11 / 8.5), BufferedImage.TYPE_INT_RGB);
            Graphics2D cg2 = img.createGraphics();
            circles.print(cg2);
            cg2.dispose();
            g2.drawImage(ImageUtil.makeColorTransparent(img, Color.WHITE), 0, 0, null);
        }
        g2.dispose();
        upscaledAll = pdfs;
    }

    public static void populateTextBoxes(List<String> textBoxes) {
        BufferedImage img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D cg2 = img.createGraphics();
        cg2.setBackground(Color.WHITE);
        cg2.setColor(Color.WHITE);
        cg2.fillRect(0, 0, img.getWidth(), img.getHeight());
        cg2.setColor(Color.BLACK);
        for (int i = 0;i < textBoxes.size();i++) {
            cg2.setFont(new Font("Arial", Font.PLAIN, 12 * Parameter.ImageScaleFactor.getValueInt()));
            int x = textBoxBounds.get(i).getCorner(Corner.UPPER_LEFT).getX();
            int lastOverflow = 0;
            for (int j = 0;j < textBoxes.get(i).length();j++) {
                int currChar = Math.min(255, textBoxes.get(i).charAt(j));
                if (((j - lastOverflow + 2) * cg2.getFontMetrics().getWidths()[currChar]) > textBoxBounds.get(i).getCorner(Corner.BOTTOM_RIGHT).getX()) {
                    textBoxes.set(i, textBoxes.get(i).substring(0, j) + "\n" + textBoxes.get(i).substring(j));
                    lastOverflow = j - 2;
                }
            }
            int y = textBoxBounds.get(i).getCorner(Corner.UPPER_LEFT).getY() - cg2.getFontMetrics().getHeight();
            for (String line : textBoxes.get(i).split("\n")) {
                cg2.drawString(line, x, y += cg2.getFontMetrics().getHeight());
            }
        }
        cg2.dispose();
        Graphics2D g2 = (Graphics2D) upscaledAll.getGraphics();
        g2.drawImage(makeColorTransparent(img, Color.WHITE), 0, 0, null);
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

    public static List<Image> getPdfImages(String filename, String pdf) throws OutOfMemoryError {
        try {
            PDFDocument document = new PDFDocument();
            document.load(new File(filename + "/PDFs/" + pdf));

            SimpleRenderer renderer = new SimpleRenderer();
            renderer.setResolution(Parameter.PDFScaleFactor.getValueInt() * 100);
            return renderer.render(document);
        } catch (UnsatisfiedLinkError e) {
            System.err.println();
            System.err.println("No PDF reader library found. Try downloading dependencies again OR");
            System.err.println("Download this and place in lib/ https://s3.amazonaws.com/s3.edu20.org/files/2796766/gsdll64.dll");
            System.exit(1);
        } catch (IOException | RendererException | DocumentException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static BufferedImage getPdfCanvas(List<Image> pdfs) throws OutOfMemoryError, NegativeArraySizeException {
        BufferedImage canvas = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        if (pdfState != PDFState.NONE) {
            Graphics2D g2 = (Graphics2D) canvas.getGraphics();
            int lastBottom = 0;
            for (int i = 0; i < pdfs.size(); i++) {
                Image pdf = pdfs.get(i).getScaledInstance(canvas.getWidth(), canvas.getHeight() / pages, Image.SCALE_SMOOTH);
                g2.drawImage(pdf, 0, lastBottom, null);
                System.out.print("\r" + "PDF: " + (i + 1) + " / " + pdfs.size());
                lastBottom += pdf.getHeight(null);
            }
        } else {
            System.out.println("PDF: None");
        }
        return canvas;
    }

    public static InputStream imageToInputStream(BufferedImage img) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "jpg", os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(os.toByteArray());
    }
}
