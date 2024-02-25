package sdle.lb;

import sdle.server.NodeState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Config {

    String configFile;
    Map<String,String> nodes;
    int port;
    String ip;

    public Config(String configFile, int port){
        this.configFile = configFile;
        this.port = port;
        this.ip = "localhost:" + port;
        this.read();
    }

    public void read(){
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            String peersString = prop.getProperty("NODES");
            ArrayList<String> nodesInfo = new ArrayList<>(Arrays.asList(peersString.substring(1, peersString.length() - 1).split(",")));
            this.nodes = new TreeMap<>();
            for(String nodeInfo: nodesInfo){
                String nodeId = nodeInfo.split("-")[0];
                String nodeIp = nodeInfo.split("-")[1];
                this.nodes.put(nodeId,nodeIp);
            }
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
        sb.append(" Nodes: ").append(this.nodes).append("\n");

        return sb.toString();
    }


}
