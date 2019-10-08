import java.net.Socket;
import java.io.*;
import javax.swing.JTextArea;
import java.util.*;

public class MessageHeader implements Serializable {

    private String messageType; //command, data transfer, control
    private int packetSequenceNumber; //can be randomly generated?
    private int fileSize;
    private boolean redelivery; //used to indicate if message was previously (unsuccessfully) delivered

    //Data transfer message. Simply left blank for messages of other types
    private String sourceUser;
    private String destinationUser;

    private String mode; //options = Broadcast, Unicast

    //From Christina's message constants class
    private int code;

    private String direction; //ToServer or ToUser

    private ArrayList<User> users;//For groupchat

    /*
        private User user;


        public User getUser(){
            return this.user;
        }

        public void setUser(User user){
            this.user = user;
        }
    */
    public ArrayList<User> getUsers(){
        return this.users;
    }

    public void setUsers(ArrayList<User> users){
        this.users=users;
    }

    public String getDirection(){
        return this.direction;
    }

    public void setDirection(String direction){
        this.direction = direction;
    }

    public MessageHeader(String messageType){

        this.messageType=messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getPacketSequenceNumber() {
        return packetSequenceNumber;
    }

    public void setPacketSequenceNumber(int packetSequenceNumber) {
        this.packetSequenceNumber = packetSequenceNumber;
    }

    public boolean isRedelivery() {
        return redelivery;
    }

    public void setRedelivery(boolean redelivery) {
        this.redelivery = redelivery;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getDestinationUser() {
        return destinationUser;
    }

    public void setDestinationUser(String destinationUser) {
        this.destinationUser = destinationUser;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }


    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
}
