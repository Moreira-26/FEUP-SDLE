package sdle.server;

import java.util.TimerTask;

public class GossipTask  extends TimerTask {

    private final Server server;
    public GossipTask(Server server){
        super();
        this.server = server;
    }

    @Override
    public void run() {
        this.server.sendGossipMessage();
    }
}
