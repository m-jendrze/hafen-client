package net.junespark;

import haven.*;
import haven.render.*;
import haven.render.sl.FragData;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class HeadlessUI implements UIPanel, UI.Context, Console.Directory {
    private UI ui;
    
    @Override
    public UI newui(UI.Runner fun) {
        if(ui != null) {
            synchronized (ui) {
                ui.destroy();
            }
        }
        ui = new UI(this, new Coord(0, 0), fun);
        ui.cons.add(this);
        return (ui);
    }
    
    @Override
    public void background(boolean bg) {
    
    }
    
    @Override
    public void setSize(int w, int h) {
    
    }
    
    @Override
    public Dimension getSize() {
        return null;
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    this.wait(100);
                }
                UI ui = this.ui;
                if(ui.sess != null)
                    ui.sess.glob.ctick();
                ui.tick();
                ui.draw(new GOut(new FakeRender(), new BufPipe(), new Coord(0, 0)));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void setmousepos(Coord c) {
    
    }
    
    @Override
    public Map<String, Console.Command> findcmds() {
        return Collections.emptyMap();
    }
    
    public static class FakeRender implements Render {
        @Override
        public void dispose() {}
        
        @Override
        public Environment env() {
            return new FakeEnv(this);
        }
        
        @Override
        public void submit(Render sub) {}
        
        @Override
        public void draw(Pipe pipe, Model data) {}
        
        @Override
        public void clear(Pipe pipe, FragData buf, FColor val) {}
        
        @Override
        public void clear(Pipe pipe, double val) {}
        
        @Override
        public <T extends DataBuffer> void update(T buf, DataBuffer.PartFiller<? super T> data, int from, int to) {}
        
        @Override
        public <T extends DataBuffer> void update(T buf, DataBuffer.Filler<? super T> data) {}
        
        @Override
        public void pget(Pipe pipe, FragData buf, Area area, VectorFormat fmt, Consumer<ByteBuffer> callback) {}
        
        @Override
        public void pget(Texture.Image img, VectorFormat fmt, Consumer<ByteBuffer> callback) {}
        
        @Override
        public void timestamp(Consumer<Long> callback) {}
        
        @Override
        public void fence(Runnable callback) {}
    }
    
    public static class FakeEnv implements Environment {
        private final FakeRender fakeRender;
        
        public FakeEnv(FakeRender fakeRender) {
            this.fakeRender = fakeRender;
        }
        
        @Override
        public void dispose() {}
        
        @Override
        public Render render() {
            return fakeRender;
        }
        
        @Override
        public FillBuffer fillbuf(DataBuffer target, int from, int to) {
            return new FakeFillBuffer();
        }
        
        @Override
        public DrawList drawlist() {
            return new FakeDrawList();
        }
        
        @Override
        public void submit(Render cmd) {}
        
        @Override
        public Caps caps() {
            return new FakeCaps();
        }
    }
    
    public static class FakeFillBuffer implements FillBuffer {
        @Override
        public void dispose() {}
        
        @Override
        public int size() {return 0;}
        
        @Override
        public boolean compatible(Environment env) {return true;}
        
        @Override
        public ByteBuffer push() {return ByteBuffer.allocate(0);}
        
        @Override
        public void pull(ByteBuffer buf) {}
    }
    
    public static class FakeDrawList implements DrawList {
        @Override
        public void dispose() {}
        
        @Override
        public void draw(Render out) {}
        
        @Override
        public boolean compatible(Environment env) {
            return true;
        }
        
        @Override
        public void add(Slot<? extends Rendered> slot) {}
        
        @Override
        public void remove(Slot<? extends Rendered> slot) {}
        
        @Override
        public void update(Slot<? extends Rendered> slot) {}
        
        @Override
        public void update(Pipe group, int[] statemask) {}
    }
    
    
    public static class FakeCaps implements Environment.Caps {
        @Override
        public String vendor() {return "fakeVendor";}
        
        @Override
        public String driver() {return "fakeDriver";}
        
        @Override
        public String device() {return "fakeDevice";}
    }
    
}
