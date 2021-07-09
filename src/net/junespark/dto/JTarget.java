package net.junespark.dto;

import haven.Coord2d;
import haven.Gob;

public class JTarget {
    
    private final CoordVector coord;
    private transient boolean done;
    public transient Gob gob;
    
    public JTarget(CoordVector coord) {
        this.coord = coord;
    }
    
    public CoordVector getCoord() {
        return coord;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public void setDone(boolean done) {
        this.done = done;
    }
    
    public Coord2d getGameCoord() {
        return gob.rc;
    }
    
    public boolean initialize(Gob gob) {
        if (gob != null) {
            this.gob = gob;
        }
        return gob != null;
    }
}
