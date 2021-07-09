package net.junespark.dto;

public class Price {
    
    private final String name;
    private final String gfx;
    private final Integer amount;
    private final Integer quality;
    
    public Price(String name, String gfx, Integer amount, Integer quality) {
        this.name = name;
        this.gfx = gfx;
        this.amount = amount;
        this.quality = quality;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGfx() {
        return gfx;
    }
    
    public Integer getAmount() {
        return amount;
    }
    
    public Integer getQuality() {
        return quality;
    }
}
