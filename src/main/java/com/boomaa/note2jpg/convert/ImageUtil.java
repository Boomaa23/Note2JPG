package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.TextBox;
import com.boomaa.note2jpg.state.PDFState;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static Image scaleImage(Image canvas, double scale) {
        return canvas.getScaledInstance((int) (scale * canvas.getWidth(null)), (int) (scale * canvas.getHeight(null)), Image.SCALE_SMOOTH);
    }

    public static BufferedImage scaleBufferedImage(BufferedImage canvas, int width, int height) {
        Image scaled = canvas.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferScaled = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2f = (Graphics2D) bufferScaled.getGraphics();
        g2f.drawImage(scaled, 0, 0, null);
        g2f.dispose();
        return bufferScaled;
    }

    public static void populateTextBoxes(List<TextBox> textBoxes) {
        BufferedImage img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg2 = img.createGraphics();
        cg2.setBackground(Color.WHITE);
        cg2.setColor(Color.WHITE);
        cg2.fillRect(0, 0, img.getWidth(), img.getHeight());
        cg2.setColor(Color.BLACK);
        for (TextBox textBox : textBoxes) {
            int x = textBox.getUpperLeft().getXInt();
            int lastOverflow = 0;
            for (int j = 0; j < textBox.getText().length(); j++) {
                int currChar = Math.min(255, textBox.getText().charAt(j));
                cg2.setFont(new Font("Arial", Font.PLAIN, (int) (textBox.rangeOfIndex(j).getFontSize() * Parameter.ImageScaleFactor.getValueInt())));
                if (((j - lastOverflow + 2) * cg2.getFontMetrics().getWidths()[currChar]) > textBox.getBottomRight().getXInt()) {
                    textBox.setText(textBox.getText().substring(0, j) + "\n" + textBox.getText().substring(j));
                    lastOverflow = j - 2;
                }
            }
            int y = textBox.getUpperLeft().getYInt() - cg2.getFontMetrics().getHeight();
            String[] splitTextBoxes = textBox.getText().split("\n");
            int ctr = 0;
            for (String box : splitTextBoxes) {
                TextBox.SubRange cRange = textBox.rangeOfIndex(ctr);
                cg2.setFont(new Font("Arial", Font.PLAIN, (int) (cRange.getFontSize() * Parameter.ImageScaleFactor.getValueInt())));
                cg2.setColor(cRange.getColor());
                if (!cRange.equals(textBox.rangeOfIndex(ctr + box.length() - 1))) {
                    Map<Integer, TextBox.SubRange> subRanges = textBox.getSubRanges();
                    int normX = x;
                    for (Map.Entry<Integer, TextBox.SubRange> entry : subRanges.entrySet()) {
                        int endIdx = Math.min(box.length(), ctr + entry.getKey());
                        String partialStr = box.substring(ctr, endIdx);
                        int strWidth = (cg2.getFontMetrics().getWidths()[partialStr.charAt(0)]) * partialStr.length();
                        cg2.drawString(partialStr, x += strWidth, y);
                    }
                    x = normX;
                } else {
                    cg2.drawString(box, x, y += cg2.getFontMetrics().getHeight());
                    ctr += box.length();
                }
            }
        }
        cg2.dispose();
        Graphics2D g2 = (Graphics2D) upscaledAll.getGraphics();
        g2.drawImage(makeColorTransparent(img, Color.WHITE), 0, 0, null);
    }

    public static Image makeColorTransparent(Image im, Color color) {
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

    public static BufferedImage convertColorspace(BufferedImage image, int newType) {
        BufferedImage rawImage = image;
        image = new BufferedImage(rawImage.getWidth(), rawImage.getHeight(), newType);
        ColorConvertOp convForm = new ColorConvertOp(null);
        convForm.filter(rawImage, image);
        return image;
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
        BufferedImage canvas = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        if (pdfState != PDFState.NONE) {
            Graphics2D g2 = (Graphics2D) canvas.getGraphics();
            int lastBottom = 0;
            for (int i = 0; i < pdfs.size(); i++) {
                Image pdf = pdfs.get(i).getScaledInstance(canvas.getWidth(), (int) (canvas.getHeight() / pages), Image.SCALE_SMOOTH);
                g2.drawImage(pdf, 0, lastBottom, null);
                System.out.print("\r" + "PDF: " + (i + 1) + " / " + pdfs.size());
                lastBottom += pdf.getHeight(null);
            }
        } else {
            System.out.print("PDF: None");
        }
        return canvas;
    }

    public static void fillEmbedImageList(String foldername) {
        File files = new File(foldername + "/Images/");
        for (File f : files.listFiles()) {
            try {
                imageList.add(ImageIO.read(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void populateEmbedImages() {
        Graphics2D g2 = (Graphics2D) upscaledAll.getGraphics();
        for (int i = 0; i < imageList.size(); i++) {
            Point pt = imageBounds.get(i);
            BufferedImage img = imageList.get(i);
            g2.drawImage(scaleImage(makeColorTransparent(img, Color.WHITE), (scaledWidth / displayedWidth) / Parameter.PDFScaleFactor.getValueInt()), pt.getXInt(), pt.getYInt(), null);
        }
        g2.dispose();
    }
}
