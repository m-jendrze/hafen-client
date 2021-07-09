package net.junespark.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class StallItem {
    private final String name;
    private final String gfx;
    private final Double quality;
    private final Integer amount;
    private final Map<String, Object> additionalInfo;
    
    public StallItem(String name, String gfx, Double quality, Integer amount) {
        this.name = name;
        this.gfx = gfx;
        this.quality = quality;
        this.amount = amount;
        additionalInfo = new LinkedHashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getGfx() {
        return gfx;
    }
    
    public Double getQuality() {
        return quality;
    }
    
    public Integer getAmount() {
        return amount;
    }
    
    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }
}
