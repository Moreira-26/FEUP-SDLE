    package sdle.server;

    import sdle.message.Message;
    import sdle.message.MessageType;
    import sdle.message.payloads.Payload;

    import java.io.IOException;
    import java.net.InetSocketAddress;
    import java.nio.channels.SocketChannel;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.Map;

    public class MessageSenderService {

        Config serverConfig;
        Message lastSent;

        public MessageSenderService(Config serverConfig){
            this.serverConfig = serverConfig;
        }

        public boolean sendUnicastMessage(NodeState receiverState, Payload payload, MessageType messageType){
            Message message = this.createMessage(receiverState.id,receiverState.ip,payload,messageType);
            this.lastSent = message;
            try{
                this.sendMessage(receiverState.ip,message);
            }catch (IOException e){
                return false;
            }
            return true;
        }

        public boolean sendUnicastMessage(NodeState receiverState, Message message){
            this.lastSent = message;
            try{
                this.sendMessage(receiverState.ip,message);
            }catch (IOException e){
                return false;
            }
            return true;
        }

        public Map<String, ArrayList<NodeState>> sendMultiCastMessage(ArrayList<NodeState> receiversState, Payload payload, MessageType messageType){
            Map<String, ArrayList<NodeState>> result = new HashMap<>();
            ArrayList<NodeState> nodesThatReceived = new ArrayList<>();
            ArrayList<NodeState> nodesThatDidntReceived = new ArrayList<>();
            for(NodeState receiverNode: receiversState){
                if(receiverNode.id.equals(this.serverConfig.id)){
                    continue;
                }
                Message messageToSend = this.createMessage(receiverNode.id,receiverNode.ip,payload,messageType);
                //this.lastSent = messageToSend;
                try{
                    this.sendMessage(receiverNode.ip,messageToSend);
                }catch (IOException e){
                    //System.out.println(e.getMessage());
                    nodesThatDidntReceived.add(receiverNode);
                    continue;
                }
                nodesThatReceived.add(receiverNode);
            }
            result.put("received", nodesThatReceived);
            result.put("notReceived", nodesThatDidntReceived);
            return result;
        }

        private void sendMessage(String receiverNodeIp,Message message) throws IOException {
            int port = Integer.parseInt(receiverNodeIp.split(":")[1]);
            SocketChannel clientChannel = null;

            clientChannel = SocketChannel.open();
            try {
                clientChannel.connect(new InetSocketAddress("localhost", port));
                clientChannel.configureBlocking(false);
                message.send(clientChannel);
            } catch (IOException e) {
                throw e;
            }finally {
                clientChannel.close();
            }
        }

        private Message createMessage(String receiverNodeId,String receiverNodeIp,Payload payload,MessageType messageType){
            return new Message(
                            this.serverConfig.id,
                            this.serverConfig.ip,
                            receiverNodeId,
                            receiverNodeIp,
                            payload,
                            messageType
            );
        }
    }
