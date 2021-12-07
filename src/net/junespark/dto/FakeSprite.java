package net.junespark.dto;

import haven.*;

import java.util.Random;

public class FakeSprite extends GSprite {
    
    private final Coord sz = new Coord(1,1);
    
    public FakeSprite(Owner owner) {
        super(owner);
    }
    
    @Override
    public void draw(GOut g) {}
    
    @Override
    public Coord sz() {
        return sz;
    }
    
    public static class FakeOwner implements Owner {
    
        private final Resource res;
        
        public FakeOwner(Resource res) {
            this.res = res;
        }
    
        @Override
        public <T> T context(Class<T> cl) {
            return null;
        }
    
        @Override
        public Random mkrandoom() {
            return new Random();
        }
    
        @Override
        public Resource getres() {
            return res;
        }
    }
}
