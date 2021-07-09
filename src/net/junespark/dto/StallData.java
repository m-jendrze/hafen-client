package net.junespark.dto;

import java.util.List;

public class StallData {
    private final List<StallRow> rows;
    private final CoordVector coord;
    private final String market;
    private final long timestamp;
    
    public StallData(CoordVector coord, String market) {
        this.rows = null;
        this.coord = coord;
        this.market = market;
        this.timestamp = 0;
    }
    
    public StallData(List<StallRow> rows, CoordVector coord, String market) {
        this.rows = rows;
        this.coord = coord;
        this.market = market;
        this.timestamp = System.currentTimeMillis();
    }
    
    public List<StallRow> getRows() {
        return rows;
    }
    
    public CoordVector getCoord() {
        return coord;
    }
    
    public String getMarket() {
        return market;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
}
