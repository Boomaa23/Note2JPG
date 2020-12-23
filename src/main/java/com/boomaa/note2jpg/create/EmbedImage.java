package com.boomaa.note2jpg.create;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EmbedImage {
    private final String loc;
    private final Point posUpperLeft;
    private final Point scaleDim;
    private final Point cropUpperLeft;
    private final Point cropBottomRight;
    private final double rotationRadians;
    private final boolean roundCorners;

    public EmbedImage(String loc, Point posUpperLeft, Point scaleDim,
                      Point cropUpperLeft, Point cropBottomRight,
                      double rotationRadians, boolean roundCorners) {
        this.loc = loc;
        this.posUpperLeft = posUpperLeft;
        this.scaleDim = scaleDim;
        this.cropUpperLeft = cropUpperLeft;
        this.cropBottomRight = cropBottomRight;
        this.rotationRadians = rotationRadians;
        this.roundCorners = roundCorners;
    }

    public BufferedImage getImage(String noExtFilename) {
        try {
            return ImageIO.read(new File(noExtFilename + "/" + loc));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Point getPosUpperLeft() {
        return posUpperLeft;
    }

    public Point getScaleDim() {
        return scaleDim;
    }

    public Point getCropUpperLeft() {
        return cropUpperLeft;
    }

    public Point getCropBottomRight() {
        return cropBottomRight;
    }

    public double getRotationRadians() {
        return rotationRadians;
    }

    public boolean hasRoundCorners() {
        return roundCorners;
    }
}
