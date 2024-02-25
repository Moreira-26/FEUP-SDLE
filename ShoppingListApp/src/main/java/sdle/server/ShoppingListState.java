package sdle.server;

import javafx.collections.transformation.SortedList;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

public class ShoppingListState {
    String id;
    ArrayList<NodeState> preferenceList;

    public ShoppingListState(String id){
        this.id = id;
    }


    // Copy constructor for deep copy
    public ShoppingListState(ShoppingListState original) {
        this.id = original.id;
        this.preferenceList = new ArrayList<>();

        for (NodeState originalNodeState : original.preferenceList) {
            NodeState copiedNodeState = new NodeState(originalNodeState);
            this.preferenceList.add(copiedNodeState);
        }
    }

    public void setPreferenceList(ArrayList<NodeState> preferenceList) {
        this.preferenceList = preferenceList;
    }
}
