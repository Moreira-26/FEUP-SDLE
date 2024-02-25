package sdle.server;

import sdle.crdt.ORMap;
import sdle.message.Message;
import sdle.message.MessageType;
import sdle.message.payloads.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MessageReceiverService {
    Server server;
    Message lastReceived;

    public MessageReceiverService(Server server){
        this.server = server;
    }
    public void processMessage(Message message){
        switch (message.getType()){
            case MEMBERSHIP_CHANGE -> dealMessageMembershipChange(message);
            case GOSSIP -> dealMessageGossip(message);
            case SHOPPING_LIST_OP -> dealShoppingListOP(message);
        }
    }

    private void dealShoppingListOP(Message message) {
        this.lastReceived = message;
        ShoppingListPayload payload = (ShoppingListPayload) message.getPayload();
        //Check if server knows about the shoppingList
        ShoppingListState shoppingListState = this.server.ringState.getShoppingList(payload.shoppingListId);
        if(shoppingListState == null){
            //If it doesn't know create it, calculate the shoppingList's preference node list
            shoppingListState = this.server.ringState.addShoppingList(new ShoppingListState(payload.shoppingListId));
        }

        if(payload.operation.equals(ShoppingListOp.REPLICATE)){
            boolean upsert = this.server.shoppingListController.upsert(payload.shoppingListId,payload.shoppingList);
            if(upsert){
                System.out.println("Replica of shopping list: " + payload.shoppingListId + " saved");
            }else{
                System.out.println("Error saving replica of shopping list: " + payload.shoppingListId);
            }
            return;
        }

        if(payload.operation.equals(ShoppingListOp.REMOVE_REPLICATE)){
            boolean upsert = this.server.shoppingListController.delete(payload.shoppingListId);
            if(upsert){
                System.out.println("Replica of shopping list: " + payload.shoppingListId + " saved");
            }else{
                System.out.println("Error saving replica of shopping list: " + payload.shoppingListId);
            }
            return;
        }

        //Get which node is coordinator
        NodeState coordinator = shoppingListState.preferenceList.get(0);
        ShoppingListPayload payloadToSend = null;
        //Create node state client
        NodeState nodeState = new NodeState(message.getSenderId(), message.getSenderIp(), NodeHealth.ALIVE);
        //If current node is coordinator or request is a redirect
        if(payload.isRedirect || Objects.equals(coordinator.id, this.server.config.id)){
            //PUSH OP
            if(payload.operation.equals(ShoppingListOp.PUSH)){
                //Execute push
                this.server.shoppingListController.push(payload.shoppingListId, payload.shoppingList);
                //Payload to send to client
                payloadToSend = new ShoppingListPayload(null,null,ShoppingListOp.RESPONSE_PUSH_SUCCESS,false);
                //Payload to send to replicas
                ShoppingListPayload payloadRedirect = new ShoppingListPayload(payload.shoppingListId,payload.shoppingList,ShoppingListOp.REPLICATE,false);
                this.server.messageSenderService.sendMultiCastMessage(shoppingListState.preferenceList,payloadRedirect,MessageType.SHOPPING_LIST_OP);
                System.out.println("Push complete");
            }
            //PULL OP
            else if(payload.operation.equals(ShoppingListOp.PULL)){
                //Get ORMap
                ORMap orMapPulled = this.server.shoppingListController.pull(payload.shoppingListId);
                //If ORMap not found in db
                if(orMapPulled == null){
                    //Send not found to client
                    payloadToSend = new ShoppingListPayload(payload.shoppingListId, null,ShoppingListOp.RESPONSE_PULL_NOT_FOUND,false);
                }else{
                    //Send shopping List to client
                    payloadToSend = new ShoppingListPayload(orMapPulled.getShoppingListId(),orMapPulled,ShoppingListOp.RESPONSE_PULL,false);
                }
            }
            //Send
            this.server.messageSenderService.sendUnicastMessage(nodeState,payloadToSend, MessageType.SHOPPING_LIST_OP);
        }else{
            //Redirect
            payload.setRedirect(true);
            boolean sent;
            int i = 0;
            do{
                NodeState nodeToSend = shoppingListState.preferenceList.get(i);
                message.setReceiverId(nodeToSend.id);
                message.setReceiverIp(nodeToSend.ip);
                sent = this.server.messageSenderService.sendUnicastMessage(nodeToSend,message);
            }while (!sent && (i != shoppingListState.preferenceList.size() - 1));
        }
    }

    private void dealMessageMembershipChange(Message message){
        MembershipChangePayload payload = (MembershipChangePayload) message.getPayload();
        NodeState nodeStateReceived = new NodeState(payload.targetId,payload.targetIp,payload.state);
        Map<String, RingState.NodeChanges> coordinatorChanges = null;
        if(payload.getOperation().equals(MembershipChangeOp.ADD)){
            coordinatorChanges = this.server.ringState.addServer(nodeStateReceived);
        }else if(payload.getOperation().equals(MembershipChangeOp.REMOVE)){
            coordinatorChanges = this.server.ringState.removeServer(nodeStateReceived);
        }

        if(coordinatorChanges == null){
            return;
        }

        this.dealCoordinatorChange(coordinatorChanges);
    }

    public void dealCoordinatorChange(Map<String, RingState.NodeChanges> coordinatorChanges){
        if(coordinatorChanges == null || coordinatorChanges.isEmpty()){
            return;
        }
        ShoppingListPayload payloadReplicaAdd;
        ShoppingListPayload payloadReplicaRemove;
        for(Map.Entry<String,RingState.NodeChanges> changesEntry: coordinatorChanges.entrySet()){
            ORMap shoppingList = this.server.shoppingListController.get(changesEntry.getKey());

            payloadReplicaAdd = new ShoppingListPayload(changesEntry.getKey(),shoppingList,ShoppingListOp.REPLICATE,false);
            this.server.messageSenderService.sendMultiCastMessage(changesEntry.getValue().addedNodes,payloadReplicaAdd,MessageType.SHOPPING_LIST_OP);

            payloadReplicaRemove = new ShoppingListPayload(changesEntry.getKey(),null,ShoppingListOp.REMOVE_REPLICATE,false);
            this.server.messageSenderService.sendMultiCastMessage(changesEntry.getValue().removedNodes,payloadReplicaRemove,MessageType.SHOPPING_LIST_OP);
        }
    }

    private  void dealMessageGossip(Message message){
        GossipPayload payload = (GossipPayload) message.getPayload();
        //this.lastReceived = message;
        Map<String, RingState.NodeChanges> coordinatorChanges = this.server.ringState.join(payload.getRing());

        dealCoordinatorChange(coordinatorChanges);
    }
}
