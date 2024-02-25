package sdle.message.payloads;

import sdle.server.NodeHealth;

public class MembershipChangePayload implements Payload {
    public String targetId;
    public String targetIp;
    public NodeHealth state;
    MembershipChangeOp operation;

    public MembershipChangePayload(String targetId, String targetIp,NodeHealth state, MembershipChangeOp operation){
        this.targetId = targetId;
        this.targetIp = targetIp;
        this.state = state;
        this.operation = operation;
    }

    public MembershipChangeOp getOperation(){
        return  this.operation;
    }

    @Override
    public String toString(){
        String result = "Operation: " + this.operation + "Node: " + this.targetId;
        return result;
    }
}
