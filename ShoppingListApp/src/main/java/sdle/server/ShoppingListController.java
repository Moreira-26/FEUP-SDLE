package sdle.server;

import sdle.crdt.ORMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ShoppingListController {
    DBService dbService;
    String nodeId;

    public ShoppingListController(String nodeId){
        this.dbService = new DBService();
        this.nodeId = nodeId;
        this.dbService.connect(nodeId);
    }

    public ORMap get(String listId){
        byte[] shoppingListByteArray = this.dbService.getShoppingList(listId);
        ORMap shoppingListCRDT = null;
        if(shoppingListByteArray != null){
            shoppingListCRDT = this.deserializeObject(shoppingListByteArray);
        }else{
            System.out.println("Shopping list " + listId + " doesn't exist");
        }

        return shoppingListCRDT;
    }

    public ORMap pull(String listId){

        byte[] shoppingListArray = this.dbService.getShoppingList(listId);
        if(shoppingListArray == null){
            return null;
        }else{
            return deserializeObject(shoppingListArray);
        }
    }

    public void push(String listId, ORMap shoppingListReceived){
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
    }

    public ArrayList<String> getShoppingListIds(){
        if(this.dbService.conn == null){
            return new ArrayList<>();
        }
        ArrayList<String> listIds = this.dbService.getShoppingListIds();
        return listIds;
    }

    public boolean upsert(String listId, ORMap shoppingListCRDT){
        byte[] shoppingListByteArray = this.serializeObject(shoppingListCRDT);
        if(shoppingListByteArray == null){
            return false;
        }
        Boolean stored = this.dbService.upsertShoppingList(listId,shoppingListByteArray);
        return stored;
    }

    public boolean delete(String listId){

        Boolean removed = this.dbService.deleteShoppingList(listId);
        return removed;
    }



    private byte[] serializeObject(ORMap shoppingList) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(shoppingList);
            return bos.toByteArray();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private ORMap deserializeObject(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            // Cast the deserialized object to ShoppingListCRDT
            return (ORMap) ois.readObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        /*ORMap node1 = new ORMap("node1", "shop1");
        node1.inc("bananas", 3);
        node1.dec("bananas", 2);
        node1.inc("bolachas", 2);
        node1.inc("cereais", 2);

        ORMap shop2 = new ORMap("node1", "shop2");
        shop2.inc("tomates",3);
        ShoppingListController controller = new ShoppingListController("node1");
        controller.upsert("shop1",shop2);
        ORMap orMap = controller.get("shop1");
        System.out.println(orMap.toStringPretty());*/
    }
}
