package sdle.message.payloads;

import sdle.crdt.ORMap;

public class ShoppingListPayload implements Payload {
    public String shoppingListId;
    public ORMap shoppingList;
    public ShoppingListOp operation;
    public boolean isRedirect;

    public ShoppingListPayload(String shoppingListId,ORMap shoppingList,ShoppingListOp operation, boolean isRedirect){
        this.shoppingListId = shoppingListId;
        this.shoppingList = shoppingList;
        this.operation = operation;
        this.isRedirect = isRedirect;
    }

    public void setRedirect(boolean redirect){
        this.isRedirect = redirect;
    }
}
