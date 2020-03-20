package com.boomaa.note2jpg.create;

public class Box {
    private Point upperLeft;
    private Point bottomRight;

    public Box(Point upperLeft, Point bottomRight) {
        this.upperLeft = upperLeft;
        this.bottomRight = bottomRight;
    }

    public Box(Point upperLeft) {
        this.upperLeft = upperLeft;
    }

    public boolean setCorner(Corner corner, Point set) {
        boolean success = validate(corner, set);
        if (success) {
            switch (corner) {
                case UPPER_LEFT:
                    this.upperLeft = set;
                    break;
                case BOTTOM_RIGHT:
                    this.bottomRight = set;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid box corner");
            }
        } else {
            System.err.println("Corner position is invalid.");
            System.out.println();
        }
        return success;
    }

    public Point getCorner(Corner corner) {
        switch (corner) {
            case UPPER_LEFT:
                return upperLeft;
            case BOTTOM_RIGHT:
                return bottomRight;
            default:
                throw new IllegalArgumentException("Invalid box corner");
        }
    }

    public boolean validate(Corner corner, Point set) {
        Point ulTemp;
        Point brTemp;
        switch (corner) {
            case UPPER_LEFT:
                ulTemp = set;
                brTemp = bottomRight;
                break;
            case BOTTOM_RIGHT:
                ulTemp = upperLeft;
                brTemp = set;
                break;
            default:
                throw new IllegalArgumentException("Invalid box corner");
        }
        return ulTemp != null && brTemp != null
            && brTemp.getY() > ulTemp.getY() && brTemp.getX() > ulTemp.getX()
            && ulTemp.distance(brTemp) > 0;
    }
}
