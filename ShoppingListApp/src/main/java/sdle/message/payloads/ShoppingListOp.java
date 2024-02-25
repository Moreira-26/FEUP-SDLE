package sdle.message.payloads;

public enum ShoppingListOp {
    PUSH,
    RESPONSE_PUSH_SUCCESS,
    PULL,
    RESPONSE_PULL,
    RESPONSE_PULL_NOT_FOUND,
    REPLICATE,
    REMOVE_REPLICATE
}
