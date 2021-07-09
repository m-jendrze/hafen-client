package net.junespark;

import haven.*;
import haven.resutil.Curiosity;
import haven.resutil.FoodInfo;
import net.junespark.dto.*;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StallInspector {
    
    private final Map<Integer, Widget> widgets;
    private boolean finished = true;
    private static final int TIMEOUT = 3000;
    private long lastCall = Long.MAX_VALUE;
    private Coord2d vcCoord;
    private String vcName;
    
    public StallInspector(Map<Integer, Widget> widgets) {
        this.widgets = widgets;
        System.out.println("created stall inspector");
    }
    
    public void start() {
        finished = false;
        lastCall = System.currentTimeMillis();
    }
    
    public boolean initializedWithVC() {
        return vcCoord != null || vcName != null;
    }
    
    public boolean isFinished() {
        if(System.currentTimeMillis() - lastCall > TIMEOUT) {
            finished = true;
        }
        return finished;
    }
    
    public boolean openedStall() {
        Map<Integer, Widget> widgets = new HashMap<>(this.widgets);
        return widgets.values().stream()
            .anyMatch(e -> "Barter Stand".equals(getFieldValueString(e, "title")));
    }
    
    public void analyzeStall(Coord2d stallCoords) {
        Widget wdg = widgets.values().stream()
            .filter(e -> "Barter Stand".equals(getFieldValueString(e, "title")))
            .findFirst()
            .orElse(null);
        if(wdg == null) {
            return;
        }
        Widget child = wdg.child;
        List<Widget> shopsRows = new ArrayList<>();
        do {
            if("haven.res.ui.barterbox.Shopbox".equals(child.getClass().getName())) {
                shopsRows.add(child);
            }
            child = child.next;
        } while (child != null);
        final List<StallRow> stallRows = shopsRows.stream()
            .map(this::analyzeWidget)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Coord2d vect = vcCoord.sub(stallCoords);
        Integration.sendStallData(new StallData(stallRows, new CoordVector(vect.x, vect.y), vcName));
        finished = true;
    }
    
    public StallRow analyzeWidget(Widget wdg) {
        if(!"haven.res.ui.barterbox.Shopbox".equals(wdg.getClass().getName())) {
            return null;
        }
        try {
            final StallItem item = getStallItem(wdg);
            final Price price = getPrice(wdg);
            if(item == null || price == null) {
                return new StallRow(null, null, null);
            }
            final String left = ((Text.Line) wdg.getClass().getDeclaredField("num").get(wdg)).text;
            return new StallRow(item, price, left);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private StallItem getStallItem(Widget wdg) throws NoSuchFieldException, IllegalAccessException {
        List<Object> cinfo = (List<Object>) getFieldValue(wdg, "cinfo");
        ResData resdata = (ResData) getFieldValue(wdg, "res");
        if(resdata == null || cinfo == null) {
            return null;
        }
        Resource res = resdata.res.get();
        final String gfx = res.name;
        StallItem item = new StallItem(getName(cinfo), gfx, getQuality(cinfo), getAmount(cinfo));
        List<Supplier<Map<String, ?>>> extractors = List.of(
            () -> getCoinage(cinfo),
            () -> getWear(cinfo),
            () -> getGast(cinfo),
            () -> getGrevious(cinfo),
            () -> getArmpen(cinfo),
            () -> getWeight(cinfo),
            () -> getDamage(cinfo),
            () -> getArmor(cinfo),
            () -> getIngredients(cinfo),
            () -> getContents(cinfo),
            () -> getFoodInfo(cinfo),
            () -> getCuriosity(cinfo),
            () -> getAttrMods(cinfo),
            () -> getISlots(cinfo),
            () -> getSlotted(cinfo)//
        );
        item.getAdditionalInfo().putAll(
            extractors.stream()
                .map(Supplier::get)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        analyzeCinfo(cinfo);
        return item;
    }
    
    private void analyzeCinfo(List<Object> cinfo) throws IllegalAccessException {
        //TODO elixirs
        
        List<String> knownFields = new ArrayList<>();
        knownFields.add("haven.res.ui.tt.q.quality.Quality");
        knownFields.add("haven.ItemInfo$Name");
        knownFields.add("haven.GItem$Amount");
        knownFields.add("haven.res.ui.tt.wear.Wear");
        knownFields.add("Gast");
        knownFields.add("Grievous");
        knownFields.add("Armpen");
        knownFields.add("Weight");
        knownFields.add("Damage");
        knownFields.add("Armor");
        knownFields.add("Coinage");
        knownFields.add("Level");
        knownFields.add("ISlots");
        knownFields.add("Slotted");
        knownFields.add("Ingredient");
        knownFields.add("haven.ItemInfo$Contents");
        knownFields.add("haven.resutil.FoodInfo");
        knownFields.add("haven.resutil.Curiosity");
        knownFields.add("haven.res.ui.tt.attrmod.AttrMod");
        
        cinfo.stream()
            .filter(e -> !knownFields.contains(e.getClass().getName()))
            .forEach(this::analyzeObject);
    }
    
    private void analyzeObject(Object o) {
        System.out.println("--- ---");
        System.out.println(o.getClass().getName());
        System.out.println("--- fields");
        Field[] fields = o.getClass().getFields();
        try {
            for (Field field : fields) {
                field.setAccessible(true);
                System.out.println('\t' + field.getName() + ": " + field.get(o));
            }
            System.out.println("--- dfields");
            Field[] dfields = o.getClass().getDeclaredFields();
            for (Field field : dfields) {
                field.setAccessible(true);
                System.out.println('\t' + field.getName() + ": " + field.get(o));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private Price getPrice(Widget wdg) throws NoSuchFieldException, IllegalAccessException {
        Object price = wdg.getClass().getDeclaredField("price").get(wdg);
        if(price == null) {
            return null;
        }
        Resource res = ((ResData) getFieldValue(price, "res")).res.get();
        Object info = getFieldValue(price, "info");
        String name = null;
        if(info != null) {
            List<ItemInfo> cinfo = ItemInfo.buildinfo((ItemInfo.Owner) price, (Object[]) info);
            name = getName(Arrays.asList(cinfo.toArray()));
        }
        name = name != null ? name : res.layer(Resource.Tooltip.class).t;
        final String pgfx = res.name;
        if(pgfx.contains("coins")) {
            Object[][] infos = (Object[][]) info;
            if(infos.length > 1 && infos[1].length > 1) {
                name = name.replace("Coins", "")
                    .replace("Coin", "") + infos[1][1];
            }
        }
        Integer num = (Integer) getFieldValue(wdg, "pnum");
        Integer q = (Integer) getFieldValue(wdg, "pq");
        return new Price(name, pgfx, num, q);
    }
    
    private Integer getAmount(List<Object> cinfo) {
        Object quality = cinfo.stream()
            .filter(e -> "haven.GItem$Amount".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(quality != null) {
            return (Integer) getFieldValue(quality, "num");
        }
        return null;
    }
    
    private Double getQuality(List<Object> cinfo) {
        Object quality = cinfo.stream()
            .filter(e -> "haven.res.ui.tt.q.quality.Quality".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(quality != null) {
            return (Double) getFieldValue(quality, "q");
        }
        return null;
    }
    
    private String getName(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.ItemInfo$Name".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            String name = (String) getFieldValue(element, "original");
            name = name.replace("Coin of ", "")
                .replace("coins of ", "");
            return name;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getContents(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.ItemInfo$Contents".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element instanceof ItemInfo.Contents) {
            ItemInfo.Contents contents = (ItemInfo.Contents) element;
            ItemInfo.Contents.Content content = contents.content();
            List<Object> info = (List<Object>) getFieldValue(element, "sub");
            Map<String, Object> elixir = getElixir(info);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", content.name);
            map.put("quality", content.q.single().value);
            map.put("count", content.count);
            map.put("unit", content.unit);
            if(!elixir.isEmpty())
                map.put("elixir", elixir);
            return Collections.singletonMap(
                "contents", map
            );
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getWear(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.res.ui.tt.wear.Wear".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "dur", getFieldValue(element, "d"),
                "maxDur", getFieldValue(element, "m"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getGast(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Gast".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "hungerReduction", getFieldValue(element, "glut"),
                "fepBonus", getFieldValue(element, "fev"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getIngredients(List<Object> cinfo) {
        List<Object> element = cinfo.stream()
            .filter(e -> "Ingredient".equals(e.getClass().getName()))
            .collect(Collectors.toList());
        if(element.size() > 0) {
            return Map.of(
                "ingredients", element.stream()
                    .map(e -> Map.of(
                        "name", getFieldValue(e, "name"),
                        "value", getFieldValue(e, "val")
                    )).collect(Collectors.toList()));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getGrevious(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Grievous".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "grevious", getFieldValue(element, "deg"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getArmpen(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Armpen".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "armPen", getFieldValue(element, "deg"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getCoinage(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Coinage".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "coinage", getFieldValue(element, "nm"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getWeight(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Weight".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            Resource attr = getFieldAsResource(element, "attr");
            if(attr != null) {
                return Collections.singletonMap(
                    "combat", Map.of(
                        "gfx", attr.name,
                        "name", attr.layer(Resource.Tooltip.class).t));
            }
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getDamage(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Damage".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Map.of(
                "damage", getFieldValue(element, "dmg"));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getArmor(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Armor".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Collections.singletonMap(
                "armor", Map.of(
                    "hard", getFieldValue(element, "hard"),
                    "soft", getFieldValue(element, "soft")));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getFoodInfo(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.resutil.FoodInfo".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element instanceof FoodInfo) {
            FoodInfo foodInfo = (FoodInfo) element;
            return Collections.singletonMap(
                "food", Map.of(
                    "energy", foodInfo.end,
                    "hunger", foodInfo.glut,
                    "evs", getFoodEvents(foodInfo.evs)));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, Object> getCuriosity(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.resutil.Curiosity".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element instanceof Curiosity) {
            Curiosity curio = (Curiosity) element;
            return Collections.singletonMap(
                "curio", Map.of(
                    "lp", curio.exp,
                    "weight", curio.mw,
                    "exp", curio.enc,
                    "time", curio.time,
                    "lph", curio.lph));
        }
        return Collections.emptyMap();
    }
    
    private static Map<String, List<Map<String, Object>>> getAttrMods(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "haven.res.ui.tt.attrmod.AttrMod".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            List<?> mods = (List<?>) getFieldValue(element, "mods");
            List<Map<String, Object>> modsList = mods.stream()
                .map(StallInspector::getMod)
                .collect(Collectors.toList());
            return Collections.singletonMap("mods", modsList);
        }
        return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getElixir(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Elixir".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            List<?> effs = (List<?>) getFieldValue(element, "effs");
            List<Map<String, Object>> modsList = effs.stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    Resource res;
                    boolean heal = "HealWound".equals(e.getClass().getName());
                    boolean wound = "AddWound".equals(e.getClass().getName());
                    boolean mod = "haven.res.ui.tt.attrmod.AttrMod".equals(e.getClass().getName());
                    if(heal || wound) {
                        Session.CachedRes.Ref ref = (Session.CachedRes.Ref) getFieldValue(e, "res");
                        res = ref.get();
                        map.put("name", res.layer(Resource.Tooltip.class).t);
                        map.put("gfx", res.name);
                        map.put("mod", getFieldValue(e, "a"));
                        map.put("eff", heal ? "heal" : "wound");
                    }
                    if(mod) {
                        Map<String, List<Map<String, Object>>> mods = getAttrMods(Collections.singletonList(e));
                        if(mods.get("mods") != null && !mods.get("mods").isEmpty()) {
                            Map<String, Object> modV = mods.get("mods").get(0);
                            if(modV.get("attr") != null) {
                                Map<String, Object> attr = (Map<String, Object>) modV.get("attr");
                                map.put("name", attr.get("name"));
                                map.put("gfx", attr.get("gfx"));
                            }
                            map.put("mod", modV.get("mod"));
                            map.put("eff", "mod");
                        }
                    }
                    return map;
                }).collect(Collectors.toList());
            return Map.of(
                "time", getFieldValue(element, "time"),
                "eff", modsList);
        }
        return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getISlots(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "ISlots".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Collections.singletonMap(
                "gilded", Map.of(
                    "min", getFieldValue(element, "pmin"),
                    "max", getFieldValue(element, "pmax"),
                    "slots", getFieldValue(element, "left"),
                    "attr", Arrays.stream(((Resource[]) getFieldValue(element, "attrs")))
                        .map(StallInspector::getAttrFromRes)
                        .collect(Collectors.toList()),
                    "gildings", getGildings((List<Object>) getFieldValue(element, "s")))
            );
        }
        return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getSlotted(List<Object> cinfo) {
        Object element = cinfo.stream()
            .filter(e -> "Slotted".equals(e.getClass().getName()))
            .findFirst()
            .orElse(null);
        if(element != null) {
            return Collections.singletonMap(
                "gilding", Map.of(
                    "min", getFieldValue(element, "pmin"),
                    "max", getFieldValue(element, "pmax"),
                    "attr", Arrays.stream(((Resource[]) getFieldValue(element, "attrs")))
                        .map(StallInspector::getAttrFromRes)
                        .collect(Collectors.toList()),
                    "stats", getAttrMods((List<Object>) getFieldValue(element, "sub"))
                        .get("mods"))
            );
        }
        return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getGildings(List<Object> s) {
        return s.stream()
            .map(e -> {
                Resource res = getFieldAsResource(e, "res");
                Map<String, Object> map = new LinkedHashMap<>();
                if(res != null) {
                    map.put("gfx", res.name);
                }
                map.put("name", getFieldValue(e, "name"));
                map.put("info", getAttrMods((List<Object>) getFieldValue(e, "info"))
                    .get("mods"));
                return map;
            }).collect(Collectors.toList());
    }
    
    private static Map<String, Object> getMod(Object o) {
        Resource attr = getFieldAsResource(o, "attr");
        Integer mod = (Integer) getFieldValue(o, "mod");
        return Map.of(
            "attr", getAttrFromRes(attr),
            "mod", mod
        );
    }
    
    private static List<Map<String, Object>> getFoodEvents(FoodInfo.Event[] events) {
        return Arrays.stream(events)
            .map(e -> Map.of(
                "attr", e.ev.nm,
                "color", awtColorToHexString(e.ev.col),
                "gfx", getFieldValue(e, "res"),
                "value", e.a
            )).collect(Collectors.toList());
    }
    
    private static String awtColorToHexString(Color color) {
        if(color == null) {
            return "undefined";
        } else {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
    }
    
    private static Map<String, Object> getAttrFromRes(Resource res) {
        if(res == null) {
            return Collections.emptyMap();
        } else {
            return Map.of(
                "gfx", res.name,
                "name", res.layer(Resource.Tooltip.class).t
            );
        }
    }
    
    private static Resource getFieldAsResource(Object o, String field) {
        Object attrO = getFieldValue(o, field);
        if(attrO instanceof Resource) {
            return (Resource) attrO;
        }
        return null;
    }
    
    private String getFieldValueString(Object obj, String name) {
        return (String) getFieldValue(obj, name);
    }
    
    private static Object getFieldValue(Object obj, String name) {
        Object v = null;
        try {
            Field f = getField(obj, name);
            v = f.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return v;
    }
    
    private static Field getField(Object obj, String name) throws NoSuchFieldException {
        Class cls = obj.getClass();
        while (true) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
                if(cls == null) {
                    throw e;
                }
            }
            
        }
    }
    
    public void initializeVC(GameUI gui, Coord2d rc) {
        this.vcCoord = rc;
        List<String> names = new ArrayList<>();
        Widget chatElement = gui.chat.child;
        do {
            final String name = getFieldValueString(chatElement, "name");
            if(name != null) {
                names.add(name);
            }
            chatElement = chatElement.next;
        } while (chatElement != null);
        vcName = gui.polowners.values().stream()
            .filter(e -> !names.contains(e))
            .findFirst()
            .orElse(null);
    }
    
    public void initializeStalls(List<Coord2d> stallCoords) {
        Integration.sendMultipleStalls(stallCoords.stream().map(e -> CoordVector.coordToVector(vcCoord, e))
            .map(e -> new StallData(e, vcName))
            .collect(Collectors.toList()));
    }
}
