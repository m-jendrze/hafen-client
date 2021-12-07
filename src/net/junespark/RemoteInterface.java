package net.junespark;

import auto.Bot;
import haven.UI;

public class RemoteInterface implements Runnable {
    
    private UI ui;
    private JBot jBot;
    private static RemoteInterface ri;
    private Integer counter = 0;
    private final Object waiter = new Object();
    private boolean started = false;
    private boolean enabled = true;
    
    private RemoteInterface() {}
    
    public static RemoteInterface createOrUpdate(UI ui) {
        if(ri == null) {
            ri = new RemoteInterface();
        }
        ri.ui = ui;
        ri.jBot = JBot.createJBot(ui);
        ui.forcePswd = true;
        return ri;
    }
    
    @Override
    public void run() {
        if(!enabled || started) return;
        /*
            33 0
            0 -33
            99 0
            0 495
         */
        started = true;
        System.out.println("started remote interface");
        JBot.pause(3000);
        jBot.login("your_login", "your_password");
        jBot.selectCharacter("character_name");
        jBot.hearth();
        //pathing from hearth fire to spot with visible village claim and clear path to middle of street
        //currently set for finloch market
        jBot.playerGoTo(33d, 0d);//3 tiles east
        jBot.playerGoTo(0d, -33d);//3 tiles south
        jBot.playerGoTo(99d, 0d);//9 tiles east
        jBot.playerGoTo(0d, 495d);//north
        Bot.test(ui.gui);
        synchronized (waiter) {
            while (true) {
                try {
                    //forgot what i wanted to do here lol
                    counter++;
                    waiter.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
