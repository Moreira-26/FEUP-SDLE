package sdle.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Config {
    String configFile;
    Map<String,String> lbs;
    int port;
    String ip;

    public Config(String configFile){
        this.configFile = configFile;
        this.read();
    }

    public void read(){
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            String peersString = prop.getProperty("LOAD_BALANCERS");
            ArrayList<String> nodesInfo = new ArrayList<>(Arrays.asList(peersString.substring(1, peersString.length() - 1).split(",")));
            this.lbs = new TreeMap<>();
            for(String nodeInfo: nodesInfo){
                String nodeId = nodeInfo.split("-")[0];
                String nodeIp = nodeInfo.split("-")[1];
                this.lbs.put(nodeId,nodeIp);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found");
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void setPort(int port){
        this.port = port;
        this.ip = "localhost:"+port;
    }


}
