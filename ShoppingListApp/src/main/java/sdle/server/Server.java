package sdle.server;

import sdle.message.Message;
import sdle.message.MessageType;
import sdle.message.payloads.GossipPayload;
import sdle.message.payloads.MembershipChangeOp;
import sdle.message.payloads.MembershipChangePayload;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


public class Server {

    int port;
    ServerSocketChannel serverChannel;
    Config config;
    RingState ringState;
    MessageReceiverService messageReceiverService;
    MessageSenderService messageSenderService;
    ShoppingListController shoppingListController;
    ReentrantReadWriteLock lockRingNodes;

    public Server(String serverId, int port){
        this.port = port;
        this.config = new Config("src/main/java/sdle/server/conf", port, serverId);
        this.lockRingNodes = new ReentrantReadWriteLock();
        this.ringState = new RingState(serverId, lockRingNodes);
        this.messageReceiverService = new MessageReceiverService(this);
        this.messageSenderService = new MessageSenderService(this.config);
        this.shoppingListController = new ShoppingListController(this.config.id);
    }

    public void run() {
        try{
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new ServerStatus(this), 0, 1000);


            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            this.serverChannel = serverChannel;

            System.out.println("Starting server at port " + this.port);

            Selector selector = Selector.open();
            this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            if(!this.config.isPeer){
                MembershipChangePayload payload = new MembershipChangePayload(this.config.id,this.config.ip,NodeHealth.ALIVE, MembershipChangeOp.ADD);
                this.messageSenderService.sendMultiCastMessage(this.config.peers, payload, MessageType.MEMBERSHIP_CHANGE);
            }else{
                NodeState nodeState = new NodeState(this.config.id,this.config.ip,NodeHealth.ALIVE);
                this.ringState.addServer(nodeState);
            }


            this.startGossipProtocol();

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

        this.messageReceiverService.processMessage(message);
    }

    private void accept(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        //Accept client
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);

        //Register client for reading
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void startGossipProtocol() {
        GossipTask gossipTask = new GossipTask(this);
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(gossipTask,0,30);
    }

    public void sendGossipMessage(){

        this.lockRingNodes.readLock().lock();

        GossipPayload gossipPayload = new GossipPayload(this.ringState.getRingNodes());

        ArrayList<NodeState> nodes = new ArrayList<>(this.ringState.getRingNodes().values());
        List<NodeState> aliveNodes  = nodes.stream().filter(nodeState -> nodeState.state.equals(NodeHealth.ALIVE)).toList();

        int subsetSize = Math.min(aliveNodes.size(), 2);
        ArrayList<NodeState> randomSubset = this.getRandomSubset(aliveNodes, subsetSize);

        Map<String, ArrayList<NodeState>> result = this.messageSenderService.sendMultiCastMessage(randomSubset,gossipPayload,MessageType.GOSSIP);
        this.lockRingNodes.readLock().unlock();
        if(!result.get("notReceived").isEmpty()){
            ArrayList<Map<String, RingState.NodeChanges>> coordinatorChanges = this.ringState.setNodesAsDown(result.get("notReceived"));
            for(Map<String, RingState.NodeChanges> coordinatorChange: coordinatorChanges){
                this.messageReceiverService.dealCoordinatorChange(coordinatorChange);
            }
        }
    }

    private ArrayList<NodeState> getRandomSubset(List<NodeState> nodes, int subsetSize) {
        ArrayList<NodeState> randomSubset = new ArrayList<>();
        Random random = new Random();

        while (randomSubset.size() < subsetSize) {
            int randomIndex = random.nextInt(nodes.size());
            NodeState randomNode = nodes.get(randomIndex);

            if (!randomSubset.contains(randomNode)) {
                randomSubset.add(randomNode);
            }
        }

        return randomSubset;
    }

    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Wrong usage of server: arg[0]-port arg[1]-serverId");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String serverId = args[1];
        Server server = new Server(serverId,port);
        server.run();
    }

}
