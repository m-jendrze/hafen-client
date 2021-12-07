package net.junespark;

import haven.*;
import net.junespark.dto.BotPathing;
import net.junespark.dto.JTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static haven.OCache.*;

public class JBot {
    
    private UI ui;
    private static JBot jbot;
    private long WALK_TIMEOUT_MULT = 50;
    private long WALK_TIMEOUT_MIN = 2000;
    private long TIMEOUT_DELAY = 100;
    private static final Object waiter = new Object();
    private static Map<String, Integer> octMap = octMap();
    
    private JBot(UI ui) {
        this.ui = ui;
        initCmds(ui);
    }
    
    public static JBot createJBot(UI ui) {
        if(jbot == null) {
            jbot = new JBot(ui);
        } else {
            jbot.ui = ui;
            jbot.initCmds(ui);
        }
        return jbot;
    }
    
    private void initCmds(UI ui) {
        ui.cons.setcmd("J.hearth", (cons, args) -> Defer.later((BotAction) () -> {
            hearth();
            return null;
        }));
        ui.cons.setcmd("J.k", (cons, args) -> Defer.later((BotAction) () -> {
            play("F2....G2#/D3#....E2#..G2#/D3#..F2..A2#/D3.");
            return null;
        }));
        ui.cons.setcmd("J.goto", (cons, args) -> Defer.later((BotAction) () -> {
            if(args.length > 2) {
                goTo(playerCoord().sub(new Coord2d(
                    Double.parseDouble(args[1]),
                    Double.parseDouble(args[2])
                )));
            }
            return null;
        }));
    }
    
    public void goTo(Coord2d mc) throws InterruptedException {
        long timeout = walkingTimeout(mc);
        ui.gui.map.wdgmsg("click", Coord.of(0, 0), mc.floor(posres), 1, 0);
        long time = System.currentTimeMillis();
        while (distanceToPlayer(mc) > 1) {
            pause(50);
            if(System.currentTimeMillis() - time > timeout) {
                System.out.println("walk timeout");
                throw new InterruptedException("walk timeout");
            }
        }
        pause(50);
        System.out.println("beep");
    }
    
    public void playerGoTo(Double x, Double y) {
        try {
            goTo(playerCoord().sub(new Coord2d(x, y)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void hearth() {
        ui.gui.wdgmsg("act", "travel", "hearth", 0);
        waitForNewMap();
    }
    
    public void waitForNewMap() {
        Coord3f cc = getcc();
        boolean noMap = cc == null;
        while (cc == null) {
            cc = getcc();
            pause(50);
        }
        if(noMap) {
            return;
        }
        while (getcc() != null && cc.equals(getcc())) {
            pause(50);
        }
    }
    
    private Coord3f getcc() {
        try {
            return ui.gui.map.getcc();
        } catch (Loading | NullPointerException e) {
            return null;
        }
    }
    
    public Optional<Gob> gobByCoord(Coord2d coord) {
        return ui.sess.glob.oc.stream()
            .filter(gob -> gob.rc.dist(coord) <= 1)
            .findFirst();
    }
    
    public double distanceToPlayer(Gob gob) {
        Gob p = gob.glob.oc.getgob(gob.glob.sess.ui.gui.plid);
        return p.rc.dist(gob.rc);
    }
    
    public double distanceToPlayer(Coord2d mc) {
        Gob p = ui.sess.glob.oc.getgob(ui.gui.plid);
        return p.rc.dist(mc);
    }
    
    public Coord2d playerCoord() {
        return ui.sess.glob.oc.getgob(ui.gui.plid).rc;
    }
    
    private long walkingTimeout(Coord2d mc) {
        return Long.max(TIMEOUT_DELAY + (long) (WALK_TIMEOUT_MULT * distanceToPlayer(mc)), WALK_TIMEOUT_MIN);
    }
    
    public static void pause(long ms) {
        if (ms < 1)
            return;
        synchronized (waiter) {
            try {
                waiter.wait(ms);
            } catch (InterruptedException ignore) {
            }
        }
    }
    
    public void remotePathing(Gob reference, JBotAction act) {
        BotPathing pathing = Integration.bot();
        pathing.getPath().forEach(n -> {
            System.out.println(n);
            pathing.getNodes().stream()
                .filter(p -> n.equals(p.getId()))
                .findFirst()
                .ifPresent(iGoTo(reference, act));
        });
    }
    
    public void login(String login, String password) {
        Widget loginScreen = ui.getwidget(1);
        if(loginScreen == null) {
            System.out.println("Already logged in");
            return;
        }
        Object[] creds = new Object[]{new AuthClient.NativeCred(login, password), false};
        ui.wdgmsg(loginScreen, "login", creds);
        while (ui.getSingleWidget(Charlist.class) == null) {
            Widget wdg = ui.getSingleWidget(Charlist.class);
            pause(100);
        }
        System.out.println("logged in");
    }
    
    public void logout() {
        ui.wdgmsg(ui.gui, "act", "lo");
        while (ui.getwidget(1) == null) {
            pause(100);
        }
        System.out.println("logged out");
    }
    
    public void selectCharacter(String characterName) {
        ui.wdgmsg(ui.getSingleWidget(Charlist.class), "play", characterName);
        pause(300);
        findWindowByTitle("Restart")
            .ifPresent(e -> ui.wdgmsg(e.lchild.prev, "activate"));
        waitForNewMap();
        System.out.println("in game");
    }
    
    public Optional<Window> findWindowByTitle(String title) {
        return ui.getWidget(Window.class)
            .map(e -> (Window) e)
            .filter(e -> title.equals(e.caption()))
            .findFirst();
    }
    
    public Optional<Window> waitForWindow(String title) {
        Window window = null;
        int pause = 20;
        int count = 0;
        int timeout = 2000;
        while (window == null && count * pause < timeout) {
            window = ui.getWidget(Window.class)
                .map(e -> (Window) e)
                .filter(e -> title.equals(e.caption()))
                .findFirst()
                .orElse(null);
            pause(pause);
            count++;
        }
        return Optional.of(window);
    }
    
    private static Map<String, Integer> octMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("C1", 0);
        map.put("C1#", 1);
        map.put("D1b", 1);
        map.put("D1", 2);
        map.put("D1#", 3);
        map.put("E1b", 3);
        map.put("E1", 4);
        map.put("F1", 5);
        map.put("E1#", 5);
        map.put("F1#", 6);
        map.put("G1b", 6);
        map.put("G1", 7);
        map.put("G1#", 8);
        map.put("A1b", 8);
        map.put("A1", 9);
        map.put("A1#", 10);
        map.put("B1b", 10);
        map.put("B1", 11);
        
        map.put("C2", 12);
        map.put("C2#", 13);
        map.put("D2b", 13);
        map.put("D2", 14);
        map.put("D2#", 15);
        map.put("E2b", 15);
        map.put("E2", 16);
        map.put("F2", 17);
        map.put("E2#", 17);
        map.put("F2#", 18);
        map.put("G2b", 18);
        map.put("G2", 19);
        map.put("G2#", 20);
        map.put("A2b", 20);
        map.put("A2", 21);
        map.put("A2#", 22);
        map.put("B2b", 22);
        map.put("B2", 23);
        
        map.put("C3", 24);
        map.put("C3#", 25);
        map.put("D3b", 25);
        map.put("D3", 26);
        map.put("D3#", 27);
        map.put("E3b", 27);
        map.put("E3", 28);
        map.put("F3", 29);
        map.put("E3#", 29);
        map.put("F3#", 30);
        map.put("G3b", 30);
        map.put("G3", 31);
        map.put("G3#", 32);
        map.put("A3b", 32);
        map.put("A3", 33);
        map.put("A3#", 34);
        map.put("B3b", 34);
        map.put("B3", 35);
        
        return map;
    }
    
    public void play(String notes) {
        float start = 1;
        float ms = 0.2f;
        float ms_s = 0.01f;
        int lastTime = 0;
        int lastNote = -1;
        String[] _16 = notes.split("\\.");
        for (int i = 0; i < _16.length; i++) {
            String[] sn = _16[i].split("/");
            for (int j = 0; j < sn.length; j++)
                if(octMap.containsKey(sn[j])) {
                    float v = start + ms * i + ms_s * j;
                    int current = Math.round(v * 1000);
                    int note = octMap.get(sn[j]);
                    pause(current - lastTime);
                    playNote(note, v, lastNote);
                    lastNote = note;
                    lastTime = current;
                }
        }
    }
    
    private void playNote(int note, float t, int lastNote) {
        float delay = 0.005f;
        findWindowByTitle("Lute")
            .ifPresent(luteWdg -> {
                if (lastNote >= 0)
                    ui.wdgmsg(luteWdg, "stop", lastNote, t);
                ui.wdgmsg(luteWdg, "play", note, t + delay);
            });
    }
    
    public interface JBotAction {
        void act(JTarget target);
    }
    
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }
    
    static <T> Consumer<T> interrupted(
        ThrowingConsumer<T, Exception> throwingConsumer) {
        
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
    
    Consumer<BotPathing.Node> iGoTo(Gob reference, JBotAction act) {
        return interrupted((p) -> {
            Coord2d goTo = reference.rc.sub(p.getCoord().getX(), p.getCoord().getY());
            goTo(goTo);
            List<Double> y = p.getStalls().stream().map(e -> e.getCoord().getY()).collect(Collectors.toList());
            List<Double> x = p.getStalls().stream().map(e -> e.getCoord().getX()).collect(Collectors.toList());
            p.getStalls().stream()
                .peek(s -> s.initialize(
                    gobByCoord(reference.rc.sub(s.getCoord().getX(), s.getCoord().getY()))
                        .orElse(null))
                ).forEach(s -> {
                System.out.printf("going to check stall x: %s, y: %s%n", s.getCoord().getX(), s.getCoord().getY());
                try {
                    goTo(s.gob.rc.add(new Coord2d(Math.cos(s.gob.a) * 15, Math.sin(s.gob.a) * 15)));
                    act.act(s);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
    }
    
    private interface BotAction extends Defer.Callable<Void> {
        @Override
        Void call() throws InterruptedException;
    }
}
