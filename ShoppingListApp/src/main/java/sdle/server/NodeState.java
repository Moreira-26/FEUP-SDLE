package sdle.server;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NodeState implements Serializable {
    public String id;
    public String ip;
    public NodeHealth state;

    private Instant lastUpdate;

    public NodeState(String id, String ip, NodeHealth state){
        this.id = id;
        this.ip = ip;
        this.state = state;
        this.lastUpdate = Instant.now();

    }

    public Instant getLastUpdate(){
        return this.lastUpdate;
    }

    public void setLastUpdate(Instant instant){
        this.lastUpdate = instant;
    }

    // Copy constructor for deep copy
    public NodeState(NodeState original) {
        this.id = original.id;
        this.ip = original.ip;
        this.state = original.state;
        this.lastUpdate = original.lastUpdate;
    }

    @Override
    public String toString(){
        return  "Node:" + this.id + " State:" + this.state + "\n";
    }

}
