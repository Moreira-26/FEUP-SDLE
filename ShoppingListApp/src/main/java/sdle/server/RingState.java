package sdle.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RingState {
    // Make it thread-safe
    private SortedMap<Long, NodeState> ringNodes;
    public ReentrantReadWriteLock lockRingNodes;
    private SortedMap<Long, ShoppingListState> ringShoppingLists;//ShoppingList Position -> ShoppingListState
    private final MessageDigest md;
    private final String serverId;

    public RingState(String serverId, ReentrantReadWriteLock lockRingNodes){
        this.serverId = serverId;
        this.ringNodes = new TreeMap<>();
        this.ringShoppingLists = new TreeMap<>();
        this.lockRingNodes = lockRingNodes;

        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, NodeChanges> addServer(NodeState server){
        this.lockRingNodes.writeLock().lock();
        SortedMap<Long, ShoppingListState> oldShoppingLists = deepCopy(this.ringShoppingLists);

        long hash = this.generateHash(server.id);
        server.setLastUpdate(Instant.now());
        //Add Server from ring
        this.ringNodes.put(hash,server);
        //Recalculate preference list for all shopping lists
        this.calculatePreferenceListsAll();
        //Calculate coordinator changes for this server
        Map<String, NodeChanges> coordinatorChanges = this.getCoordinatorChanges(oldShoppingLists);

        this.lockRingNodes.writeLock().unlock();
        return coordinatorChanges;
    }

    private Map<String, NodeChanges> getCoordinatorChanges(SortedMap<Long, ShoppingListState> oldShoppingLists){
        Map<String, NodeChanges> allChanges = calculatePreferenceListDifferences(oldShoppingLists,this.ringShoppingLists);

        Map<String, NodeChanges> coordinatorChanges = new TreeMap<>();
        for(Map.Entry<String,NodeChanges> change: allChanges.entrySet()){
            if(isCoordinator(change.getKey())){
                coordinatorChanges.put(change.getKey(),change.getValue());
            }
        }
        return coordinatorChanges;
    }

    public Map<String,NodeChanges> removeServer(NodeState server){
        this.lockRingNodes.writeLock().lock();
        SortedMap<Long, ShoppingListState> oldShoppingLists = deepCopy(this.ringShoppingLists);

        long hash = this.generateHash(server.id);
        //Remove Server from ring
        this.ringNodes.get(hash).state = NodeHealth.DOWN;
        this.ringNodes.get(hash).setLastUpdate(Instant.now());
        //this.ringNodes.remove(hash); TODO: WORKING REMOVE=DOWN
        //Recalculate preference list for all shopping lists
        this.calculatePreferenceListsAll();
        //Get coordinator changes for this server
        Map<String, NodeChanges> coordinatorChanges = this.getCoordinatorChanges(oldShoppingLists);

        this.lockRingNodes.writeLock().unlock();
        return coordinatorChanges;
    }

    public ShoppingListState getShoppingList(String shoppingListId){
        long hash = this.generateHash(shoppingListId);
        return this.ringShoppingLists.get(hash);
    }

    private SortedMap<Long, ShoppingListState> deepCopy(SortedMap<Long, ShoppingListState> originalMap) {
        SortedMap<Long, ShoppingListState> copiedMap = new TreeMap<>();

        for (Long key : originalMap.keySet()) {
            ShoppingListState originalShoppingListState = originalMap.get(key);
            ShoppingListState copiedShoppingListState = new ShoppingListState(originalShoppingListState);

            copiedMap.put(key, copiedShoppingListState);
        }

        return copiedMap;
    }

    private boolean isCoordinator(String shoppingListId){
        long hash = this.generateHash(shoppingListId);
        return this.ringShoppingLists.get(hash).preferenceList.getFirst().id.equals(this.serverId);
    }

    private void calculatePreferenceListsAll(){
        for(Map.Entry<Long, ShoppingListState> entry: this.ringShoppingLists.entrySet()){
            ShoppingListState shoppingList = entry.getValue();
            shoppingList.setPreferenceList(this.calculatePreferenceList(shoppingList.id));
            this.ringShoppingLists.put(entry.getKey(),shoppingList);
        }
    }
    public ArrayList<NodeState> calculatePreferenceList(String shoppingListId){
        Long shoppingListPosition = this.generateHash(shoppingListId);

        // Get the 3 closest nodes in a clockwise order
        ArrayList<NodeState> preferenceList = new ArrayList<>();
        Iterator<Map.Entry<Long, NodeState>> iterator = this.ringNodes.entrySet().iterator();
        Map.Entry<Long, NodeState> entry;
        boolean firstPass = true;
        while (preferenceList.size() < 3){
            if(iterator.hasNext()){
                entry = iterator.next();
                if(entry.getValue().state.equals(NodeHealth.DOWN)){
                    continue;
                }
                if(entry.getKey() >= shoppingListPosition || !firstPass){
                    preferenceList.add(entry.getValue());
                }
            }else{
                //Iterator to the begin
                iterator = this.ringNodes.entrySet().iterator();
                entry = iterator.next();
                if(entry.getValue().state.equals(NodeHealth.DOWN)){
                    continue;
                }
                preferenceList.add(entry.getValue());
                firstPass = false;
            }
        }

        return preferenceList;

    }

    public static class NodeChanges {
        ArrayList<NodeState> addedNodes = new ArrayList<>();
        ArrayList<NodeState> removedNodes = new ArrayList<>();
    }

    private NodeChanges comparePreferenceLists(ArrayList<NodeState> oldList, ArrayList<NodeState> newList){
        NodeChanges changes = new NodeChanges();

        Set<String> oldIds = new HashSet<>();
        Set<String> newIds = new HashSet<>();

        // Populate sets with node IDs
        for (NodeState node : oldList) {
            oldIds.add(node.id);
        }
        for (NodeState node : newList) {
            newIds.add(node.id);
        }

        // Find added nodes
        for (NodeState newNode : newList) {
            if (!oldIds.contains(newNode.id)) {
                changes.addedNodes.add(newNode);
            }
        }

        // Find removed nodes
        for (NodeState oldNode : oldList) {
            if (!newIds.contains(oldNode.id)) {
                changes.removedNodes.add(oldNode);
            }
        }
        return changes;
    }

    private Map<String,NodeChanges>  calculatePreferenceListDifferences(SortedMap<Long, ShoppingListState> oldLists, SortedMap<Long, ShoppingListState> newLists) {
        Map<String,NodeChanges> shoppingListPreferenceChanges = new TreeMap<>();

        for (Map.Entry<Long, ShoppingListState> entry : oldLists.entrySet()) {
            long shoppingListPos = entry.getKey();
            //Get old shoppingList state
            ShoppingListState oldShoppingList = entry.getValue();
            //Get new shoppingList state
            ShoppingListState newShoppingList = newLists.get(shoppingListPos);

            // Compare old and new preference lists
            ArrayList<NodeState> oldPreferenceList = oldShoppingList.preferenceList;
            ArrayList<NodeState> newPreferenceList = newShoppingList.preferenceList;

            NodeChanges changes = this.comparePreferenceLists(oldPreferenceList,newPreferenceList);

            shoppingListPreferenceChanges.put(oldShoppingList.id,changes);
        }
        return  shoppingListPreferenceChanges;
    }

    public ShoppingListState addShoppingList(ShoppingListState shoppingList){
        this.lockRingNodes.readLock().lock();
        long hash = this.generateHash(shoppingList.id);
        shoppingList.setPreferenceList(this.calculatePreferenceList(shoppingList.id));
        this.ringShoppingLists.put(hash,shoppingList);
        this.lockRingNodes.readLock().unlock();
        return shoppingList;
    }

    public ArrayList<Map<String, NodeChanges>> setNodesAsDown(ArrayList<NodeState> downNodes){
        ArrayList<Map<String,NodeChanges>> result = new ArrayList<>();
        for(NodeState node: downNodes){
            result.add(this.removeServer(node));
        }
        return result;
    }

    private long generateHash(String key) {
        this.md.reset();
        this.md.update(key.getBytes());
        byte[] digest = this.md.digest();
        return ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
    }

    //TODO: RECALCULATE SHOPPING LIST PREFERENCE LIST ON JOIN, GET THE CHANGES WHEN COORDINATOR AND SEND MESSAGES
    public Map<String,NodeChanges> join(SortedMap<Long, NodeState> ringStateReceived){
        this.lockRingNodes.writeLock().lock();

        SortedMap<Long, NodeState> joinedRing = new TreeMap<>();

        for (var entry : this.ringNodes.entrySet()) {
            Long key = entry.getKey();
            NodeState nodeState = entry.getValue();

            // Check if the second map also contains the key
            if (ringStateReceived.containsKey(key)) {
                // Compare timestamps to determine which NodeState to include
                NodeState otherNodeState = ringStateReceived.get(key);
                if (otherNodeState.getLastUpdate().isAfter(nodeState.getLastUpdate())) {
                    joinedRing.put(key, otherNodeState);
                } else {
                    joinedRing.put(key, nodeState);
                }
            } else {
                joinedRing.put(key, nodeState);
            }
        }
        for (var entry : ringStateReceived.entrySet()) {
            Long key = entry.getKey();
            NodeState nodeState = entry.getValue();

            // Check if the first map already contains the key
            if (!this.ringNodes.containsKey(key)) {
                joinedRing.put(key, nodeState);
            }
        }
        Map<String,NodeChanges> coordinatorChanges = null;
        if(!this.ringNodes.equals(joinedRing)){
            SortedMap<Long, ShoppingListState> oldShoppingLists = deepCopy(this.ringShoppingLists);
            this.ringNodes = joinedRing;
            this.calculatePreferenceListsAll();
            coordinatorChanges = this.getCoordinatorChanges(oldShoppingLists);
        }



        this.lockRingNodes.writeLock().unlock();
        return coordinatorChanges;
    }

    public SortedMap<Long, NodeState> getRingNodes(){
        return this.ringNodes;
    }

    public SortedMap<Long, ShoppingListState> getRingShoppingLists(){
        return this.ringShoppingLists;
    }

    @Override
    public String toString(){
        List<Map.Entry<Long, ?>> mergedList = new ArrayList<>();
        mergedList.addAll(ringNodes.entrySet());
        mergedList.addAll(ringShoppingLists.entrySet());

        mergedList.sort(Comparator.comparing(Map.Entry::getKey));

        StringBuilder result = new StringBuilder("Ring:\n");
        for(Map.Entry<Long, ?> entry: mergedList){
            Long position = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof NodeState) {
               result.append(((NodeState) value).id).append("[").append(((NodeState) value).state).append("]");
            } else if (value instanceof ShoppingListState) {
                result.append(((ShoppingListState) value).id);
            }
            result.append("->");
        }
        return result.toString();
    }

    public void printNodeStates(){
        for(Map.Entry<Long,NodeState> node : this.ringNodes.entrySet()){
            System.out.println(node.getValue().id + node.getValue().state + " " + node.getValue().getLastUpdate() + "->");
        }
    }

    public static void main(String[] args) {

        ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
        ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock();

        RingState ringState1 = new RingState("node1", lock1);
        RingState ringState2 = new RingState("node2", lock2);

        NodeState nodeState1 = new NodeState("peer1", "localhost:5000", NodeHealth.ALIVE);


        NodeState nodeState2 = new NodeState("peer2", "localhost:5001", NodeHealth.ALIVE);

        NodeState nodeState3 = new NodeState("peer1", "localhost:5002", NodeHealth.DOWN);

        NodeState nodeState4 = new NodeState("peer2", "localhost:5003", NodeHealth.DOWN);

        /*NodeState nodeState5 = new NodeState("node3", "localhost:5004", NodeHealth.ALIVE);
        NodeState nodeState6 = new NodeState("testDADADADSDA", "localhost:5004", NodeHealth.ALIVE);*/


        ringState1.addServer(nodeState1);
        ringState1.addServer(nodeState2);
        ringState2.addServer(nodeState3);
        ringState2.addServer(nodeState4);

        ringState1.join(ringState2.ringNodes);

        ringState1.printNodeStates();


        /*System.out.println(ringState);


        System.out.println("----------------------------");
        for(Map.Entry<Long, ShoppingListState> entry: ringState.ringShoppingLists.entrySet()){
            System.out.println(entry.getValue().id);
            System.out.println(entry.getValue().preferenceList);
            System.out.println("--------------------");
        }*/
        /*ShoppingListState result = ringState.addShoppingList(shoppingListState1);
        System.out.println(result.preferenceList);

        /*ShoppingListState shoppingListState2 = new ShoppingListState("hugo/shoppingList-1234");
        ringState.addShoppingList(shoppingListState2);*/

    }
}
