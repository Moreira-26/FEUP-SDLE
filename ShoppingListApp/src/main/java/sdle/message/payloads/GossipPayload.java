package sdle.message.payloads;

import sdle.server.NodeState;

import java.util.SortedMap;

public class GossipPayload implements Payload {

    SortedMap<Long, NodeState> ring;

    public GossipPayload(SortedMap<Long, NodeState> ring){
        this.ring = ring;
    }

    public SortedMap<Long, NodeState> getRing(){
        return this.ring;
    }




}
