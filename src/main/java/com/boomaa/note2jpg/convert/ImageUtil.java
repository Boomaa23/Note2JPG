package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.config.Parameter;
import com.boomaa.note2jpg.create.EmbedImage;
import com.boomaa.note2jpg.create.Point;
import com.boomaa.note2jpg.create.TextBox;
import com.boomaa.note2jpg.state.PDFState;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
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

    public static BufferedImage scaleBufferedImage(BufferedImage canvas, int width, int height) {
        Image scaled = canvas.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferScaled = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2f = (Graphics2D) bufferScaled.getGraphics();
        g2f.drawImage(scaled, 0, 0, null);
        g2f.dispose();
        return bufferScaled;
    }

    public static void drawTextBoxes(List<TextBox> textBoxes) {
        BufferedImage img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg2 = img.createGraphics();
        cg2.setBackground(Color.WHITE);
        cg2.setColor(Color.WHITE);
        cg2.fillRect(0, 0, img.getWidth(), img.getHeight());
        cg2.setColor(Color.BLACK);

        for (int i = 0; i < textBoxes.size(); i++) {
            TextBox textBox = textBoxes.get(i);
            int x = textBox.getUpperLeft().getXInt();

            int lastOverflow = 0;
            int lastSpc = 0;
            for (int j = 0; j < textBox.getText().length(); j++) {
                int currChar = Math.min(255, textBox.getText().charAt(j));
                if (currChar == '\n') {
                    continue;
                }
                if (currChar == ' ') {
                    lastSpc = j;
                }
                cg2.setFont(new Font("Arial", Font.PLAIN, (int) (textBox.rangeOfIndex(j).getFontSize() * Parameter.ImageScaleFactor.getValueInt())));
                if (((j - lastOverflow + 2) * cg2.getFontMetrics().getWidths()[currChar]) > textBox.getBottomRight().getXInt()) {
                    textBox.setText(textBox.getText().substring(0, lastSpc) + "\n" + textBox.getText().substring(lastSpc + 1));
                    j = lastSpc;
                    lastOverflow = j - 2;
                }
            }

            StringReader sr = new StringReader(textBox.getText());
            int leadingLineBreaks = 0;
            try {
                while (sr.read() == '\n') {
                    leadingLineBreaks++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            int y = textBox.getUpperLeft().getYInt() + (cg2.getFontMetrics().getHeight() * leadingLineBreaks);

            AffineTransform fontTransform = new AffineTransform();
            fontTransform.rotate(textBox.getRotationRadians(), 0, 0);
            String[] splitTextBoxes = textBox.getText().split("\n");
            int ctr = 0;
            for (String box : splitTextBoxes) {
                TextBox.SubRange cRange = textBox.rangeOfIndex(ctr);
                cg2.setFont(new Font("Arial", Font.PLAIN, (int) (cRange.getFontSize() * Parameter.ImageScaleFactor.getValueInt())).deriveFont(fontTransform));
                cg2.setColor(cRange.getColor());
                if (!cRange.equals(textBox.rangeOfIndex(ctr + box.length() - 1))) {
                    Map<Integer, TextBox.SubRange> subRanges = textBox.getSubRanges();
                    int normX = x;
                    List<Integer> srKeys = new ArrayList<>(subRanges.keySet());
                    Collections.sort(srKeys);
                    int lastIdx = 0;
                    for (int sk : srKeys) {
                        String partialStr = box.substring(lastIdx, sk);
                        lastIdx = sk;
                        int[] widths = cg2.getFontMetrics().getWidths();
                        int strWidth = 0;
                        for (char c : partialStr.toCharArray()) {
                            strWidth += widths[c];
                        }
                        cg2.drawString(partialStr, x, y);
                        x += strWidth;
                    }
                    ctr += textBox.getText().length();
                    x = normX;
                } else {
                    cg2.drawString(box, x, y += cg2.getFontMetrics().getHeight());
                    ctr += box.length();
                }
            }
            System.out.print("\r" + "Text: " + (i + 1) + " / " + textBoxes.size());
        }

        cg2.dispose();
        Graphics2D g2 = (Graphics2D) upscaledAll.getGraphics();
        g2.drawImage(makeColorTransparent(img, Color.WHITE), 0, 0, null);
        System.out.println(textBoxes.size() == 0 ? "Text: None" : "");
    }

    public static void drawEmbedImages(List<EmbedImage> images, String noExtFilename) {
        Graphics2D g2 = (Graphics2D) upscaledAll.getGraphics();
        AffineTransform normal = g2.getTransform();
        for (int i = 0; i < images.size(); i++) {
            EmbedImage image = images.get(i);
            Point pos = image.getPosUpperLeft();
            Point scaleDim = image.getScaleDim();
            Point cropUL = image.getCropUpperLeft();
            Point cropDim = image.getCropBottomRight().add(cropUL.negate());
            Image drawImg = image.getImage(noExtFilename).getSubimage(cropUL.getXInt(), cropUL.getYInt(), cropDim.getXInt(), cropDim.getYInt())
                    .getScaledInstance(scaleDim.getXInt(), scaleDim.getYInt(), Image.SCALE_SMOOTH);
            g2.setTransform(AffineTransform.getRotateInstance(image.getRotationRadians(),
                    pos.getXInt() + drawImg.getWidth(null) / 2.0,
                    pos.getYInt() + drawImg.getHeight(null) / 2.0));
            if (image.hasRoundCorners()) {
                drawImg = applyRoundedCorner(drawImg);
            }
            g2.drawImage(drawImg, pos.getXInt(), pos.getYInt(), null);
            System.out.print("\r" + "Image: " + (i + 1) + " / " + images.size());
        }
        System.out.println((images.size() == 0 ? "Image: None" : "") + "\n");
        g2.setTransform(normal);
        g2.dispose();
        //TODO captions on everything (conditional)
    }

    private static BufferedImage applyRoundedCorner(Image image) {
        int cornerRadius = 10 * Parameter.ImageScaleFactor.getValueInt();
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();
        return output;
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
        System.out.println();
        return canvas;
    }

    public static void filterValidPages(List<Integer> validPages) {
        int pageHeight = (int) (scaledHeight / pages);
        BufferedImage cutCanvas = new BufferedImage(scaledWidth, validPages.size() * pageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2cc = (Graphics2D) cutCanvas.getGraphics();
        for (int i = 0; i < validPages.size(); i++) {
            Image img = upscaledAll.getSubimage(0, (validPages.get(i) - 1) * pageHeight, scaledWidth, pageHeight);
            g2cc.drawImage(img, 0, i * pageHeight, null);
        }
        g2cc.dispose();
        upscaledAll = cutCanvas;
    }
}
