package net.junespark;

import auto.Bot;
import haven.*;

public class RemoteInterface implements Runnable {
    
    private UI ui;
    private JBot jBot;
    private static RemoteInterface ri;
    private Integer counter = 0;
    private final Object waiter = new Object();
    private boolean started = false;
    
    private RemoteInterface() {}
    
    public static RemoteInterface createOrUpdate(UI ui) {
        if(ri == null) {
            ri = new RemoteInterface();
        }
        ri.ui = ui;
        ri.jBot = new JBot(ui);
        ui.forcePswd = true;
        return ri;
    }
    
    @Override
    public void run() {
        if(started) return;
        started = true;
        System.out.println("started remote interface");
        JBot.pause(3000);
        jBot.login("Kerath", "y25vpqp5");
        JBot.pause(3000);
        jBot.selectCharacter("Kerath Finloch");
        JBot.pause(1000);
        jBot.findWindowByTitle("Reset")
            .ifPresent(e -> ui.wdgmsg(e.lchild.prev, "activate"));
        JBot.pause(10000);
        Bot.test(ui.gui);
        synchronized (waiter) {
            while (true) {
                try {
                    System.out.println(counter);
                    counter++;
                    waiter.wait(1000);
                    //ui.wdgmsg(ui.getwidget(1), "login", creds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
