package sdle.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import sdle.crdt.ORMap;
import sdle.message.Message;
import sdle.message.MessageType;
import sdle.message.payloads.ShoppingListOp;
import sdle.message.payloads.ShoppingListPayload;

import java.sql.SQLException;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;

public class Controller {

    DBService dbService;
    Config config;
    String username;
    public Controller() {
        this.dbService = new DBService();
        this.config = new Config("src/main/java/sdle/client/conf");

    }

    public ORMap getShoppingList(String listId, String username) {

        byte[] shoppingListBytes = null;
        ORMap shoppingList = null;

        try (Connection conn = this.dbService.connect(username)) {
            shoppingListBytes = this.dbService.getShoppingList(listId);

            if(shoppingListBytes != null){
                shoppingList = deserializeObject(shoppingListBytes);
            }
            else {
                return null;
            }

        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getMessage());
        } finally {
            this.dbService.closeConnection();
        }

        return shoppingList;
    }

    public boolean createShoppingList(String listId, String username) {

        boolean created = false;
        ORMap shoppingList = null;

        try (Connection conn = this.dbService.connect(username)) {
            // Check if the shopping list with the specified id already exists
            if (shoppingListExists(listId)) {
                return false;
            } else {

                shoppingList = new ORMap(username,listId);

                byte[] shoppingListBytes = serializeObject(shoppingList);

                created = this.dbService.upsertShoppingList(listId, shoppingListBytes);

            }

        } catch (SQLException ex) {
            return false;
        } finally {
            this.dbService.closeConnection();
        }

        return created;
    }

    public Boolean addItemToList(String listId, String item, int quantity, String username) {

        Boolean added = false;
        ORMap shoppingList = null;

        try (Connection conn = this.dbService.connect(username)) {

            byte[] shoppingListBytes = this.dbService.getShoppingList(listId);

            // Check if the shopping list with the specified id exists
            if(shoppingListBytes != null){
                shoppingList = deserializeObject(shoppingListBytes);

                shoppingList.inc(item,quantity);

                byte[] newShoppingListBytes = serializeObject(shoppingList);

                added = this.dbService.upsertShoppingList(listId, newShoppingListBytes);
            }
            else {
                return false;
            }

        } catch (SQLException ex) {
            return false;
        } finally {
            this.dbService.closeConnection();
        }

        return added;
    }

    public Boolean removeItemFromList(String listId, String item, int quantity, String username) {

        Boolean removed;
        ORMap shoppingList = null;

        try (Connection conn = this.dbService.connect(username)) {

            byte[] shoppingListBytes = this.dbService.getShoppingList(listId);

            // Check if the shopping list with the specified id exists
            if(shoppingListBytes != null){
                shoppingList = deserializeObject(shoppingListBytes);

                shoppingList.dec(item,quantity);

                byte[] newShoppingListBytes = serializeObject(shoppingList);

                removed = this.dbService.upsertShoppingList(listId, newShoppingListBytes);
            }
            else {
                return false;
            }

        } catch (SQLException ex) {
            return false;
        } finally {
            this.dbService.closeConnection();
        }

        return removed;
    }

    public ArrayList<String> getShoppingListIds(String username){
        this.dbService.connect(username);
        ArrayList<String> listIds = this.dbService.getShoppingListIds();
        this.dbService.closeConnection();
        return listIds;
    }

    private Boolean shoppingListExists(String listId) throws SQLException {
        // Check if the shopping list with the specified id exists
        byte[] shoppingListBytes = this.dbService.getShoppingList(listId);

        return shoppingListBytes != null;
    }

    private byte[] serializeObject(ORMap shoppingList) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(shoppingList);
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ORMap deserializeObject(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            // Cast the deserialized object to ShoppingListCRDT
            return (ORMap) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void pullShoppingList(String listId, String username){
        this.dbService.connect(username);
        String loadbalancerIp = this.config.lbs.get("lb1");
        System.out.println(loadbalancerIp);

        ShoppingListPayload payload = new ShoppingListPayload(listId, null, ShoppingListOp.PULL,false);
        Message message = new Message(username,this.config.ip,"lb1", loadbalancerIp ,payload,MessageType.SHOPPING_LIST_OP);
        boolean sent = this.sendMessageWaitForResponse(message, loadbalancerIp, config.port);
        if(!sent){
            loadbalancerIp = this.config.lbs.get("lb2");
            message.setReceiverIp(loadbalancerIp);
            message.setReceiverId("lb2");
            this.sendMessageWaitForResponse(message, loadbalancerIp, config.port);
        }
        this.dbService.closeConnection();
    }

    public void pushShoppingList(String listId, String username){
        this.dbService.connect(username);
        try {
            if(!this.shoppingListExists(listId)){
                return;
            }
        } catch (SQLException e) {
            return;
        }
        ORMap shoppingList = this.getShoppingList(listId,username);
        String loadbalancerIp = this.config.lbs.get("lb1");
        ShoppingListPayload payload = new ShoppingListPayload(listId,shoppingList,ShoppingListOp.PUSH,false);
        Message message = new Message(username,this.config.ip, "lb1", loadbalancerIp, payload,MessageType.SHOPPING_LIST_OP);
        boolean sent = this.sendMessageWaitForResponse(message, loadbalancerIp, config.port);
        if(!sent){
            loadbalancerIp = this.config.lbs.get("lb2");
            message.setReceiverId("lb2");
            message.setReceiverIp(loadbalancerIp);
            this.sendMessageWaitForResponse(message, loadbalancerIp, config.port);
        }

        this.dbService.closeConnection();
    }


    private void sendMessage(Message message, String receiverIp) throws IOException {
        int port = Integer.parseInt(receiverIp.split(":")[1]);
        SocketChannel clientChannel = null;

        clientChannel = SocketChannel.open();
        try {
            System.out.println(message);
            clientChannel.connect(new InetSocketAddress("localhost", port));
            message.send(clientChannel);
        } catch (IOException e) {
            throw e;
        }finally {
            clientChannel.close();
        }
    }

    public boolean sendMessageWaitForResponse(Message message, String receiverIp, int port) {
        try {
            this.sendMessage(message,receiverIp);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        try{
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

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
                        accept(selector,serverChannel);

                    }else if(key.isReadable()){
                        read(key);
                        return true;
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void processMessage(Message message){
        System.out.println("Message Received:");
        System.out.println(message);
        if (message.getType().equals(MessageType.SHOPPING_LIST_OP)){
            ShoppingListPayload shoppingListPayload = (ShoppingListPayload) message.getPayload();
            System.out.println(shoppingListPayload.operation);
            switch (shoppingListPayload.operation){
                case RESPONSE_PULL -> this.processPull(shoppingListPayload);
                case RESPONSE_PULL_NOT_FOUND -> this.processPullNotFound(shoppingListPayload);
                case RESPONSE_PUSH_SUCCESS -> this.processPushSuccess(shoppingListPayload);
            }
        }

    }

    private void processPullNotFound(ShoppingListPayload shoppingListPayload) {
        System.out.println("Shopping list " + shoppingListPayload.shoppingListId + " not found");
    }

    private void processPushSuccess(ShoppingListPayload shoppingListPayload) {
        System.out.println("Push success");
    }

    private void processPull(ShoppingListPayload payload){
        ORMap shoppingListReceived = payload.shoppingList;
        shoppingListReceived.setReplicaId(this.username);
        String listId = payload.shoppingListId;
        byte[] shoppingListArray = this.dbService.getShoppingList(listId);
        //If shoppingList received doesn't exist
        if(shoppingListArray == null){
            //Save it in db
            this.dbService.upsertShoppingList(listId,this.serializeObject(shoppingListReceived));
        }else{
            //Get the ORMap in DB
            ORMap shoppingListDB =  deserializeObject(shoppingListArray);
            //Join with received
            shoppingListDB.join(shoppingListReceived);
            //Save it in DB
            byte[] shoppingListArrayJoin = this.serializeObject(shoppingListDB);
            this.dbService.upsertShoppingList(listId,shoppingListArrayJoin);
        }
        System.out.println("Shopping list " + listId + " pulled");
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

        this.processMessage(message);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private void accept(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        //Accept client
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);

        //Register client for reading
        clientChannel.register(selector, SelectionKey.OP_READ);
    }


}