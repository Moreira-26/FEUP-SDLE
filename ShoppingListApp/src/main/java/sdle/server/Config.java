package sdle.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Config {

    String configFile;
    ArrayList<NodeState> peers;
    int port;
    String ip;
    String id;
    Boolean isPeer;

    public Config(String configFile, int port, String id){
        this.configFile = configFile;
        this.port = port;
        this.ip = "localhost:" + port;
        this.id = id;
        this.read();
    }

    public void read(){
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            String peersString = prop.getProperty("PEERS");
            ArrayList<String> peersInfo = new ArrayList<>(Arrays.asList(peersString.substring(1, peersString.length() - 1).split(",")));
            ArrayList<String> peersIPs = new ArrayList<>();
            this.peers = new ArrayList<>();
            for(String peerInfo: peersInfo){
                String peerId = peerInfo.split("-")[0];
                String peerIp = peerInfo.split("-")[1];
                peersIPs.add(peerIp);
                NodeState peerState = new NodeState(peerId,peerIp, NodeHealth.ALIVE);
                this.peers.add(peerState);
            }

            this.isPeer = peersIPs.contains(this.ip);


        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found");
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration:\n");
        sb.append("  configFile: ").append(configFile).append("\n");
        sb.append("  port: ").append(port).append("\n");
        sb.append("  ip: ").append(ip).append("\n");
        sb.append("  serverId: ").append(id).append("\n");
        sb.append("  isPeer: ").append(isPeer).append("\n");
        sb.append("  peers: ").append(peers).append("\n");

        return sb.toString();
    }


}
