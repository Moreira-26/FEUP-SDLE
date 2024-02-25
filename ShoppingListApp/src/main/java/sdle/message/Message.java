package sdle.message;

import sdle.message.payloads.Payload;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Message implements Serializable {
    String senderId;
    String senderIp;
    String receiverId;
    String receiverIp;
    Payload payload;
    MessageType type;

    public Message(String senderId, String senderIp, String receiverId, String receiverIp, Payload payload, MessageType type){
        this.senderId = senderId;
        this.senderIp = senderIp;
        this.receiverId = receiverId;
        this.receiverIp = receiverIp;
        this.payload = payload;
        this.type = type;
    }

    /*
    * Sends Message to the received socketChannel
    */
    public boolean send(SocketChannel channel) throws IOException {
        //Create byte stream
        ByteArrayOutputStream byteOUTStream = new ByteArrayOutputStream();
        //Create object stream
        ObjectOutputStream objectOUTStream = new ObjectOutputStream(byteOUTStream);
        //Write message to stream
        objectOUTStream.writeObject(this);
        //Convert message to byte array
        byte[] messageBytes = byteOUTStream.toByteArray();
        //Get message size
        int messageSize = messageBytes.length;
        //Create buffer to hold message size and message content
        ByteBuffer buffer = ByteBuffer.allocate(4 + messageSize);
        buffer.putInt(messageSize);
        //Write byte stream to buffer
        buffer.put(messageBytes);
        //Flip buffer
        buffer.flip();
        while(buffer.hasRemaining()){
            channel.write(buffer);
        }
        //Write buffer to channel
        return true;
    }

    public static Message read(SocketChannel channel) throws IOException, ClassNotFoundException {
        //Create  byte buffer to hold object
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
        //Read message size bytes from channel into buffer
        int bytesRead = channel.read(sizeBuffer);
        if(bytesRead == -1){
            return null;
        }

        if (sizeBuffer.remaining() > 0) {
            // The object size has not been fully read yet, so return null for now
            return null;
        }


        sizeBuffer.flip();
        int messageSize = sizeBuffer.getInt();

        // Create buffer to hold message content
        ByteBuffer contentBuffer = ByteBuffer.allocate(messageSize);
        // Read message content from channel into content buffer
        bytesRead = channel.read(contentBuffer);

        if (bytesRead == -1) {
            // Channel has been closed
            return null;
        }

        if (contentBuffer.remaining() > 0) {
            // The message content has not been fully read yet, so return null for now
            return null;
        }

        contentBuffer.flip();
        byte[] messageBytes = new byte[messageSize];
        contentBuffer.get(messageBytes);

        // Create object input stream with byte array input stream
        ObjectInputStream objectInStream = new ObjectInputStream(new ByteArrayInputStream(messageBytes));
        // Read object from stream
        Message messageReceived =(Message) objectInStream.readObject();
        channel.close();
        return messageReceived;
    }

    public void setSenderId(String senderId){
        this.senderId = senderId;
    }

    public void setSenderIp(String senderIp){
        this.senderIp = senderIp;
    }

    public void setReceiverId(String receiverId){
        this.receiverId = receiverId;
    }

    public void setReceiverIp(String receiverIp){
        this.receiverIp = receiverIp;
    }

    public String getSenderId(){
        return this.senderId;
    }

    public String getSenderIp(){
        return this.senderIp;
    }

    public Object getPayload() {
        return this.payload;
    }

    public MessageType getType(){return this.type;}

    @Override
    public String toString() {
        return "Message:\n" +
                " FROM: " + senderId + "-" + senderIp + "\n" +
                " TO: " + receiverId + "-" + receiverIp + "\n" +
                " TYPE: " + type;
    }

}
