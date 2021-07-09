package net.junespark.dto;

import java.util.List;

public class BotPathing {
    private final List<Node> nodes;
    private final List<Long> path;
    
    public BotPathing(List<Node> nodes, List<Long> path) {
        this.nodes = nodes;
        this.path = path;
    }
    
    public List<Node> getNodes() {
        return nodes;
    }
    
    public List<Long> getPath() {
        return path;
    }
    
    public static class Node {
        private final CoordVector coord;
        private final long id;
        private final List<JTarget> stalls;
    
        public Node(CoordVector coord, long id, List<JTarget> stalls) {
            this.coord = coord;
            this.id = id;
            this.stalls = stalls;
        }
    
        public CoordVector getCoord() {
            return coord;
        }
    
        public long getId() {
            return id;
        }
    
        public List<JTarget> getStalls() {
            return stalls;
        }
    }
}
