package sdle.server;

import sdle.message.Message;
import sdle.message.MessageType;
import sdle.message.payloads.GossipPayload;
import sdle.message.payloads.ShoppingListPayload;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimerTask;

public class ServerStatus extends TimerTask {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Server server;

    public ServerStatus(Server server){
        this.server = server;
    }
    @Override
    public void run() {
        Long currentTimeMillis = System.currentTimeMillis();
        System.out.println("--------------------------------------------------------");
        System.out.println("Current time: " + formatTime(currentTimeMillis));
        printConfig();
        printRing();
        printShoppingListIds();
        printIncomingMessages();
        printSendingMessages();
    }

    private void printShoppingListIds() {
        System.out.println("---------------- Shopping Lists Stored -------------------");
        System.out.println(this.server.shoppingListController.getShoppingListIds());
    }

    private void printIncomingMessages(){
        System.out.println("---------------- Incoming message -------------------");
        Message message = this.server.messageReceiverService.lastReceived;
        if(message == null){
            return;
        }
        System.out.println(message);
        if(message.getType().equals(MessageType.SHOPPING_LIST_OP)){
            ShoppingListPayload payload = (ShoppingListPayload) message.getPayload();
            System.out.println(" " + payload.operation + " on shoppingList: " + payload.shoppingListId);
        }else if(message.getType().equals(MessageType.GOSSIP)){
            GossipPayload gossipPayload = (GossipPayload) message.getPayload();
            System.out.println(" " + gossipPayload.getRing());
        }
    }

    private void printSendingMessages(){
        System.out.println("---------------- Sending message -------------------");
        Message message = this.server.messageSenderService.lastSent;
        if(message == null){
            return;
        }
        System.out.println(message);
    }

    private void printConfig(){
        System.out.println(this.server.config);
    }

    private void printRing(){
        System.out.println(this.server.ringState);
    }

    private String formatTime(Long currentTimeMillis) {
        if(currentTimeMillis == null) return "";
        LocalDateTime currentTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
        return currentTime.format(TIME_FORMATTER);
    }
}
