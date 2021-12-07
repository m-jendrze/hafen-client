package haven;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class GobIconSettings extends WindowX {
    public static final String FILTER_DEFAULT = "Start typing to filter";
    public static final Coord FILTER_C = UI.scale(445, 0);
    public final GobIcon.Settings conf;
    private final Runnable save;
    private static GobCategory lastCategory = GobCategory.ALL;
    
    private static final boolean DBG = false;
    private static final Text.Foundry elf = CharWnd.attrf;
    private static final Text.Foundry elf2 = new Text.Foundry(Text.fraktur, 12).aa(true);
    private static final int elh = elf.height() + (DBG ? elf2.height() : 0) + UI.scale(2);
    private static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    private final IconList list;
    private final CheckBox checkAll;
    private final Label filter;
    private GobCategory category = lastCategory;
    
    
    public class IconList extends FilteredListBox<GobIcon.SettingsWindow.Icon> {
	private Coord showc;
	private Map<String, GobIcon.Setting> cur = null;
	private boolean reorder = false;
	
	private IconList(int w, int h) {
	    super(w, h, elh);
	    this.showc = showc();
	    bgcolor = new Color(0, 0, 0, 84);
	    showFilterText = false;
	}
	
	private Coord showc() {
	    return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
		((elh - CheckBox.sbox.sz().y) / 2)));
	}
	
	public void tick(double dt) {
	    super.tick(dt);
	    Map<String, GobIcon.Setting> cur = this.cur;
	    if(cur != conf.settings) {
		cur = conf.settings;
		ArrayList<GobIcon.SettingsWindow.Icon> ordered = new ArrayList<>();
		for (GobIcon.Setting set : cur.values())
		    ordered.add(new GobIcon.SettingsWindow.Icon(set));
		this.cur = cur;
		this.setItems(ordered);
		reorder = true;
	    }
	    if(reorder) {
		reorder = false;
		for (GobIcon.SettingsWindow.Icon icon : filtered) {
		    if(icon.name == null) {
			try {
			    Resource.Tooltip name = icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip);
			    icon.name = elf.render((name == null) ? "???" : name.t);
			    icon.resnm = elf2.render(icon.conf.res.name);
			} catch (Loading l) {
			    reorder = true;
			}
		    }
		}
		filtered.sort((a, b) -> {
		    if((a.name == null) && (b.name == null))
			return (0);
		    if(a.name == null)
			return (1);
		    if(b.name == null)
			return (-1);
		    return (a.name.text.compareTo(b.name.text));
		});
	    }
	}
	
	@Override
	protected void filter() {
	    super.filter();
	    reorder = true;
	    updateFilter(filter.line());
	    updateAllCheckbox();
	}
    
	private void updateAllCheckbox() {
	    checkAll.a = !filtered.isEmpty() && filtered.stream().allMatch(icon -> icon.conf.show);
	}
    
	@Override
	protected boolean match(GobIcon.SettingsWindow.Icon item, String filter) {
	    if(category != GobCategory.ALL && category != GobCategory.categorize(item)) {
		return false;
	    }
	    if(filter.isEmpty()) {
		return true;
	    }
	    if(item.name == null)
		return (false);
	    return (item.name.text.toLowerCase().contains(filter.toLowerCase()));
	}
	
	public void draw(GOut g) {
	    this.showc = showc();
	    super.draw(g);
	}
	
	public void drawitem(GOut g, GobIcon.SettingsWindow.Icon icon, int idx) {
	    g.chcolor(((idx % 2) == 0) ? every : other);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    try {
		g.aimage(icon.img(), new Coord(0, elh / 2), 0.0, 0.5);
	    } catch (Loading ignored) {}
	    if(DBG) {
		if(icon.name != null)
		    g.aimage(icon.name.tex(), new Coord(elh + UI.scale(5), 0), 0.0, 0);
		if(icon.resnm != null)
		    g.aimage(icon.resnm.tex(), new Coord(elh + UI.scale(5), elh), 0.0, 1);
	    } else {
		if(icon.name != null)
		    g.aimage(icon.name.tex(), new Coord(elh + UI.scale(5), elh / 2), 0.0, 0.5);
	    }
	    g.image(CheckBox.sbox, showc);
	    if(icon.conf.show)
		g.image(CheckBox.smark, showc);
	}
	
	public boolean mousedown(Coord c, int button) {
	    int idx = idxat(c);
	    if((idx >= 0) && (idx < listitems())) {
		Coord ic = c.sub(idxc(idx));
		GobIcon.SettingsWindow.Icon icon = listitem(idx);
		if(ic.x < showc.x + CheckBox.sbox.sz().x) {
		    icon.conf.show = !icon.conf.show;
		    if(save != null)
			save.run();
		    updateAllCheckbox();
		    return (true);
		}
	    }
	    return (super.mousedown(c, button));
	}
	
	public boolean keydown(java.awt.event.KeyEvent ev) {
	    if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		if(sel != null) {
		    sel.conf.show = !sel.conf.show;
		    if(save != null)
			save.run();
		    updateAllCheckbox();
		}
		return (true);
	    }
	    return (super.keydown(ev));
	}
    }
    
    public GobIconSettings(GobIcon.Settings conf, Runnable save) {
	super(Coord.z, "Icon settings");
	this.conf = conf;
	this.save = save;
    
	int h = add(new Label("Categories: "), Coord.z).sz.y;
	checkAll = add(new CheckBox("Select All") {
	    @Override
	    public void changed(boolean val) {
		list.filtered.forEach(icon -> icon.conf.show = val);
		if(save != null)
		    save.run();
	    }
	}, UI.scale(210, 0));
	filter = adda(new Label(FILTER_DEFAULT), FILTER_C, 1, 0);
	h += UI.scale(5);
    
	CategoryList categories = add(new CategoryList(UI.scale(200), 8, elh), 0, h);
	list = add(new IconList(UI.scale(250), UI.unscale(DBG ? 13 : 26)), UI.scale(210), h);
	add(new CheckBox("Notification on newly seen icons") {
	    {this.a = conf.notify;}
	
	    public void changed(boolean val) {
		conf.notify = val;
		if(save != null)
		    save.run();
	    }
	}, categories.pos("bl").adds(5, 5));
	
	categories.change(category);
	pack();
	setfocus(list);
    }
    
    private void updateFilter(String text) {
	filter.settext((text == null || text.isEmpty()) ? FILTER_DEFAULT : text);
	filter.c = FILTER_C.sub( filter.sz.x, 0);
    }
    
    private class CategoryList extends Listbox<GobCategory> {
	private final Coord showc;
	
	public CategoryList(int w, int h, int itemh) {
	    super(w, h, itemh);
	    bgcolor = new Color(0, 0, 0, 84);
	    showc = showc();
	}
	
	private Coord showc() {
	    return (new Coord(sz.x - (sb.vis() ? sb.sz.x : 0) - ((elh - CheckBox.sbox.sz().y) / 2) - CheckBox.sbox.sz().x,
		((elh - CheckBox.sbox.sz().y) / 2)));
	}
	
	@Override
	protected GobCategory listitem(int i) {
	    return GobCategory.values()[i];
	}
	
	@Override
	protected int listitems() {
	    return GobCategory.values().length;
	}
	
	@Override
	protected void drawitem(GOut g, GobCategory cat, int idx) {
	    g.chcolor(((idx % 2) == 0) ? every : other);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    try {
		GobIcon.SettingsWindow.Icon icon = cat.icon();
		g.aimage(icon.img(), new Coord(0, elh / 2), 0.0, 0.5);
		if(icon.name != null) {
		    g.aimage(icon.name.tex(), new Coord(elh + UI.scale(5), elh / 2), 0.0, 0.5);
		}
		if(cat != GobCategory.ALL) {
		    g.image(CheckBox.sbox, showc);
		    if(cat.enabled()) {
			g.image(CheckBox.smark, showc);
		    }
		}
	    } catch (Loading ignored) {}
	}
	
	@Override
	public void change(GobCategory item) {
	    super.change(item);
	    if(category != item) {
		lastCategory = category = item;
		list.needfilter();
	    }
	}
	
	public boolean mousedown(Coord c, int button) {
	    int idx = idxat(c);
	    if((idx >= 0) && (idx < listitems())) {
		GobCategory cat = listitem(idx);
		if(cat != GobCategory.ALL) {
		    Coord ic = c.sub(idxc(idx));
		    if(ic.isect(showc, CheckBox.sbox.sz())) {
			cat.toggle();
			return true;
		    }
		}
	    }
	    return (super.mousedown(c, button));
	}
    }
    
    enum GobCategory {
	ALL("all"),
	ANIMALS("kritters"),
	HERBS("herbs"),
	ORES("ores"),
	ROCKS("rocks"),
	TREE("trees"),
	BUSHES("bushes"),
	OTHER("other");
	
	private final String resname;
	private final CFG<Boolean> cfg;
	private GobIcon.SettingsWindow.Icon icon;
	
	private static final String[] ANIMAL_PATHS = {
	    "/kritter/",
	    "/invobjs/bunny",
	    "/invobjs/bogturtle",
	    "/invobjs/cavecentipede",
	    "/invobjs/cavemoth",
	    "/invobjs/dragonfly",
	    "/invobjs/forestlizard",
	    "/invobjs/forestsnail",
	    "/invobjs/frog",
	    "/invobjs/grasshopper",
	    "/invobjs/grub",
	    "/invobjs/crab",
	    "/invobjs/firefly",
	    "/invobjs/hen",
	    "/invobjs/jellyfish",
	    "/invobjs/ladybug",
	    "/invobjs/magpie",
	    "/invobjs/mallard",
	    "/invobjs/mole",
	    "/invobjs/monarchbutterfly",
	    "/invobjs/moonmoth",
	    "/invobjs/ptarmigan",
	    "/invobjs/quail",
	    "/invobjs/rabbit",
	    "/invobjs/rat",
	    "/invobjs/rockdove",
	    "/invobjs/rooster",
	    "/invobjs/sandflea",
	    "/invobjs/seagull",
	    "/invobjs/silkmoth",
	    "/invobjs/squirrel",
	    "/invobjs/stagbeetle",
	    "/invobjs/swan",
	    "/invobjs/toad",
	    "/invobjs/waterstrider",
	    "/invobjs/woodgrouse",
	};
	
	private static final String[] HERB_PATHS = {
	    "/invobjs/herbs/",
	    "/invobjs/small/bladderwrack",
	    "/invobjs/small/snapdragon",
	    "/invobjs/small/thornythistle",
	};
	
	private static final String[] ORE_PATHS = {
	    "gfx/invobjs/argentite",
	    "gfx/invobjs/blackcoal",
	    "gfx/invobjs/cassiterite",
	    "gfx/invobjs/chalcopyrite",
	    "gfx/invobjs/cinnabar",
	    "gfx/invobjs/coal",
	    "gfx/invobjs/galena",
	    "gfx/invobjs/hematite",
	    "gfx/invobjs/hornsilver",
	    "gfx/invobjs/ilmenite",
	    "gfx/invobjs/leadglance",
	    "gfx/invobjs/limonite",
	    "gfx/invobjs/magnetite",
	    "gfx/invobjs/malachite",
	    "gfx/invobjs/nagyagite",
	    "gfx/invobjs/petzite",
	    "gfx/invobjs/sylvanite",
	};
	private static final String[] ROCK_PATHS = {
	    "gfx/invobjs/alabaster",
	    "gfx/invobjs/apatite",
	    "gfx/invobjs/arkose",
	    "gfx/invobjs/basalt",
	    "gfx/invobjs/breccia",
	    "gfx/invobjs/corund",
	    "gfx/invobjs/diabase",
	    "gfx/invobjs/diorite",
	    "gfx/invobjs/dolomite",
	    "gfx/invobjs/eclogite",
	    "gfx/invobjs/feldspar",
	    "gfx/invobjs/flint",
	    "gfx/invobjs/fluorospar",
	    "gfx/invobjs/gabbro",
	    "gfx/invobjs/gneiss",
	    "gfx/invobjs/granite",
	    "gfx/invobjs/greenschist",
	    "gfx/invobjs/hornblende",
	    "gfx/invobjs/jasper",
	    "gfx/invobjs/kyanite",
	    "gfx/invobjs/limestone",
	    "gfx/invobjs/marble",
	    "gfx/invobjs/mica",
	    "gfx/invobjs/microlite",
	    "gfx/invobjs/olivine",
	    "gfx/invobjs/orthoclase",
	    "gfx/invobjs/pegmatite",
	    "gfx/invobjs/porphyry",
	    "gfx/invobjs/pumice",
	    "gfx/invobjs/quartz",
	    "gfx/invobjs/rhyolite",
	    "gfx/invobjs/sandstone",
	    "gfx/invobjs/schist",
	    "gfx/invobjs/slate",
	    "gfx/invobjs/soapstone",
	    "gfx/invobjs/sodalite",
	    "gfx/invobjs/zincspar",
	};
	
	GobCategory(String category) {
	    resname = "gfx/hud/mmap/categories/" + category;
	    cfg = new CFG<>("mmap.categories." + category, true);
	}
	
	public GobIcon.SettingsWindow.Icon icon() {
	    if(icon == null) {
		Resource.Spec spec = new Resource.Spec(null, resname);
		Resource res = spec.loadsaved(Resource.local());
		
		icon = new GobIcon.SettingsWindow.Icon(new GobIcon.Setting(spec));
		Resource.Tooltip name = res.layer(Resource.tooltip);
		icon.name = elf.render((name == null) ? "???" : name.t);
	    }
	    return icon;
	}
	
	public static GobCategory categorize(GobIcon.SettingsWindow.Icon icon) {
	    return categorize(icon.conf);
	}
	
	public static GobCategory categorize(GobIcon.Setting conf) {
	    String name = conf.res.name;
	    if(name.contains("mm/trees/")) {
		return GobCategory.TREE;
	    } else if(Arrays.stream(ANIMAL_PATHS).anyMatch(name::contains)) {
		return GobCategory.ANIMALS;
	    } else if(Arrays.stream(ROCK_PATHS).anyMatch(name::contains)) {
		return GobCategory.ROCKS;
	    } else if(Arrays.stream(ORE_PATHS).anyMatch(name::contains)) {
		return GobCategory.ORES;
	    } else if(Arrays.stream(HERB_PATHS).anyMatch(name::contains)) {
		return GobCategory.HERBS;
	    } else if(name.contains("mm/bushes/")) {
		return GobCategory.BUSHES;
	    }
	    return GobCategory.OTHER;
	}
	
	public boolean enabled() {
	    return cfg.get();
	}
	
	public void toggle() {
	    cfg.set(!cfg.get());
	}
    }
}
