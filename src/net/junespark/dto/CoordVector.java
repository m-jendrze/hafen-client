package net.junespark.dto;

import haven.Coord2d;

public class CoordVector {
    
    private final double x;
    private final double y;
    
    public CoordVector(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public static CoordVector coordToVector(Coord2d c1, Coord2d c2) {
        Coord2d vect = c1.sub(c2);
        return new CoordVector(vect.x, vect.y);
    }
}
