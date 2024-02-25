package sdle.lb;

import sdle.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class LoadBalancer {
    Config config;
    int port;
    ServerSocketChannel serverChannel;
    String lbId;

    public LoadBalancer(int port, String lbId){
        this.lbId = lbId;
        this.port = port;
        this.config = new Config("src/main/java/sdle/lb/conf",port);
    }
    public void run() {
        try{
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            this.serverChannel = serverChannel;

            System.out.println("Starting server at port " + this.port);

            Selector selector = Selector.open();
            this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);


            while (true){
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if(!key.isValid()){
                        continue;
                    }
                    if(key.isAcceptable()){
                        accept(selector,this.serverChannel);

                    }else if(key.isReadable()){
                        read(key);
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Message message;
        try {
            message = Message.read(clientChannel);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            // Handle connection reset
            System.out.println("Client disconnected: " + clientChannel);
            clientChannel.close();
            return;
        }

        if(message == null){
            System.out.println("Received null message");
            return;
        }
        System.out.println("MESSAGE RECEIVED:");
        System.out.println(message);

        this.processMessage(message);
    }

    private void accept(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        //Accept client
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);

        //Register client for reading
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private boolean processMessage(Message message){
        //Choose a random node,change the receiverId/receiverIp and send it
        String nodeId = chooseNode();
        String nodeIp = this.config.nodes.get(nodeId);
        try {
            message.setReceiverId(nodeId);
            message.setReceiverIp(nodeIp);
            this.sendMessage(nodeIp,message);
        } catch (IOException e) {
            return false;
        }

        return true;

    }

    private String chooseNode(){

        // Get the size of the nodes map
        int size = this.config.nodes.size();

        // If there are no nodes, return null or throw an exception based on your requirements
        if (size == 0) {
            return null; // or throw new IllegalStateException("No nodes available");
        }

        // Generate a random index
        int randomIndex = new Random().nextInt(size);

        // Get a random node by iterating through the map using an index
        int currentIndex = 0;
        for (Map.Entry<String, String> entry : this.config.nodes.entrySet()) {
            if (currentIndex == randomIndex) {
                return entry.getKey(); // Return the nodeId of the randomly chosen node
            }
            currentIndex++;
        }
        return  null;
    }

    private void sendMessage(String receiverNodeIp,Message message) throws IOException {
        int port = Integer.parseInt(receiverNodeIp.split(":")[1]);
        SocketChannel clientChannel = null;

        clientChannel = SocketChannel.open();
        try {
            System.out.println("REDIRECTING MESSAGE:");
            System.out.println(message);

            clientChannel.connect(new InetSocketAddress("localhost", port));
            clientChannel.configureBlocking(false);
            message.send(clientChannel);
        } catch (IOException e) {
            throw e;
        }finally {
            clientChannel.close();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String lbId = args[1];
        LoadBalancer lb = new LoadBalancer(port,lbId);
        System.out.println(lb.config);
        lb.run();

    }
}
