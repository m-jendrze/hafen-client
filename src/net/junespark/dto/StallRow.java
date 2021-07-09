package net.junespark.dto;

public class StallRow {
    private final StallItem item;
    private final Price price;
    private final String left;
    
    public StallRow(StallItem item, Price price, String left) {
        this.item = item;
        this.price = price;
        this.left = left;
    }
    
    public StallItem getItem() {
        return item;
    }
    
    public Price getPrice() {
        return price;
    }
    
    public String getLeft() {
        return left;
    }
}
