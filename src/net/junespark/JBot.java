package net.junespark;

import haven.*;
import net.junespark.dto.BotPathing;
import net.junespark.dto.JTarget;

import java.util.Optional;
import java.util.function.Consumer;

import static haven.OCache.*;

public class JBot {
    
    private UI ui;
    private long WALK_TIMEOUT_MULT = 50;
    private long TIMEOUT_DELAY = 100;
    private static final Object waiter = new Object();
    
    public JBot(UI ui) {
        this.ui = ui;
    }
    
    public void goTo(Coord2d mc) throws InterruptedException {
        long timeout = walkingTimeout(mc);
        ui.gui.map.wdgmsg("click", Coord.of(0, 0), mc.floor(posres), 1, 0);
        long time = System.currentTimeMillis();
        while (distanceToPlayer(mc) > 1) {
            pause(30);
            if(System.currentTimeMillis() - time > timeout)
                throw new InterruptedException();
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
        return TIMEOUT_DELAY + (long) (WALK_TIMEOUT_MULT * distanceToPlayer(mc));
    }
    
    public static void pause(long ms) {
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
        if (loginScreen == null) {
            System.out.println("Already logged in");
            return;
        }
        Object[] creds = new Object[] {new AuthClient.NativeCred(login, password), false};
        ui.wdgmsg(loginScreen, "login", creds);
    }
    
    public void selectCharacter(String characterName) {
        ui.wdgmsg(ui.getSingleWidget(Charlist.class), "play", characterName);
    }
    
    public Optional<Window> findWindowByTitle(String title) {
        return ui.getWidget(Window.class)
            .map(e-> (Window) e)
            .filter(e-> "Restart".equals(e.caption()))
            .findFirst();
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
            p.getStalls().stream()
                .peek(s -> s.initialize(
                    gobByCoord(reference.rc.sub(s.getCoord().getX(), s.getCoord().getY()))
                        .orElse(null))
                ).forEach(s -> {
                    System.out.printf("going to check stall x: %s, y: %s%n",s.getCoord().getX(), s.getCoord().getY());
                    act.act(s);
                try {
                    goTo(goTo);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
