import java.net.Socket;
import java.util.*;
import javax.swing.*;
import java.io.*;

/**
 * This class represents a client that has connected to the server via it's own socket.
 */
public class Client{
    // variables used for class
    static ClientGUI gui;
    static privateChatGUI chatGui;
    static Socket client;
    static String username;
    static String filename = "";
    public static String onlineUsers;
    static boolean loggedOut=false;
    static ArrayList<String> blockedUsers = new ArrayList<String>();
    static int packetCount = 0;
    public static ObjectOutputStream objectOutputStream;
    public static ObjectInputStream objectInputStream;
    public static byte[] receivedFile = new byte[10000000];

    /**
     * Method to get the sockets objectOutputStream
     * @return ObjectOutputStream
     */
    public static ObjectOutputStream getObjectOutputStream(){
        return objectOutputStream;
    }
    /**
     * Method to get the sockets objectInputStream
     * @return ObjectInputStream
     */
    public static ObjectInputStream getObjectInputStream(){
        return objectInputStream;
    }

    /**
     * Establishes a connection with the server and instantiates the input/output streams.
     * Starts login GUI and retrieves client username.
     * Displays the GUI and sends/listens for messages.
     */
    public static void main (String[] args) {


        try {
            client = new Socket("Christinas-MacBook-Pro.local", 1337);
			
            //objectOutputStream should always be created before objectInputStream and flushed
            objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(client.getInputStream());
            username = usernameGUI();
            sendUserNameToServer();

            displayMenu();
            chatGui = null; //private chat gui initiliazed before the while loop
            while (!loggedOut){

                getMessage();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Starts the login GUI for the client
     * @return the client's username
     */

    public static String usernameGUI(){
        userGUI gui = new userGUI();
        gui.main(null);
        // waits until the username has been retrieved
        while(!gui.usernameRetrieved.get()){

        }
        return gui.username;
    }

    /**
     * Called in ClientGUI, sends a broadcast/group chat message to the server
     * @param message - the message object to send to server
     */
    static void sendMessage(String message){

        Message messageObj = new Message();
        messageObj.setBody(new MessageBody(message));
        messageObj.setHeader(new MessageHeader("Data Transfer"));
        messageObj.getHeader().setDirection("ToUser");
        messageObj.getHeader().setMode("broadcast");
        messageObj.getHeader().setCode(MessageConstants.GROUP_CHAT);
        messageObj.getHeader().setSourceUser(username);
        MessageHandler.sendMessage(messageObj, getObjectOutputStream());

    }

    /**
     * Displays the menu (main) GUI
     */
    public static void displayMenu(){
        gui = new ClientGUI();
        gui.startGui();

    }

    /**
     * This method is constantly called inside the while (true) loop in Client's main method.
     * It listens for incoming messages and calls MessageHandler to determine what to do with the message.
     */
    public static void getMessage(){

        try {
            if(!loggedOut) {
                Message msgObj = (Message) objectInputStream.readObject();
                int messageCode = MessageHandler.handleMessage(msgObj);

                switch (messageCode) {
                    case MessageConstants.GET_ONLINE_USERS:
                        getOnlineUsers(msgObj);
                        break;
                    case MessageConstants.GROUP_CHAT:
                        updateChat(msgObj);
                        break;
                    case MessageConstants.SEND_FILE_REQUEST:
                        if (msgObj.getHeader().getDestinationUser().equals(username)) {requestReceiveFileAtDestinationUser(msgObj);}
                        break;
                    case MessageConstants.FILE_REQUEST_DENIED:
                        if (msgObj.getHeader().getSourceUser().equals(username)) {notifySourceUserTransferDenied();}
                        break;
                    case MessageConstants.FILE_REQUEST_ACCEPTED:
                        notifySourceUserAndInitiateFileSending(msgObj);
                        break;
                    case MessageConstants.SEND_FILE_PACKET:
                        if (msgObj.getHeader().getDestinationUser().equals(username)) {promptDestUserForFilenameAndReceiveTransfer(msgObj);}
                        break;
                    case MessageConstants.PRIVATE_CHAT_REQUEST:
                        if (msgObj.getBody().getMessage().equals("startGUI")) { //receive a private chat request from the server and start the private chat gui
                            chatGui = new privateChatGUI();
                            chatGui.startGui();}
                        break;
                    case MessageConstants.PRIVATE_CHAT:
                        chatGui.ChatDisplay.append(msgObj.getBody().getMessage()+'\n'); //print the private chat message to the private chat gui
                        break;

                    case MessageConstants.FILE_TRANSFER_FAILED:
                        if (msgObj.getHeader().getSourceUser().equals(username)){
                            JOptionPane.showMessageDialog(null, "File transfer failed.");
                        }
                        break;
                    case MessageConstants.FILE_TRANSFER_SUCCEEDED:
                        if (msgObj.getHeader().getSourceUser().equals(username)){
                            JOptionPane.showMessageDialog(null, "File transfer succeeded.");
                        }
                        break;
                }
            }
        } catch(Exception e){
            System.out.println("User successfully logged out.");
        }

    }
    static Socket getClient(){
        return client;
    }

    /**
     * Retrieves a list of online users from the server
     * @param message - message object containing the online users
     */
    public static void getOnlineUsers(Message message){ gui.OnlineUsers.setText(message.getBody().getMessage());}

    /**
     * Updates the group chat with the message specified in msgObj's body.
     * @param msgObj - the message object containing the message string to be appended to the chat text area.
     */
    public static void updateChat(Message msgObj){
        boolean userisblocked = false;
        // if user is blocked display message from blocked user
        for (String user: blockedUsers) {
            if (user.equals(msgObj.getHeader().getSourceUser())) {
                gui.ChatDisplay.append("***message from blocked user***"+'\n');
                userisblocked = true;
            }
        }
        // if user is not blocked display their message
        if (!userisblocked) gui.ChatDisplay.append(msgObj.getBody().getMessage());
    }

    /**
     * Prompts the destination user of a file transfer request to accept or deny the file.
     * @param message - message object containing source user and filename
     */

    public static void requestReceiveFileAtDestinationUser(Message message){
        int option = JOptionPane.showConfirmDialog(null, "Receive file: " + message.getBody().getMessage() + " from user: " + message.getHeader().getSourceUser(), "File Transfer Request", JOptionPane.YES_NO_OPTION);
        if (option==1){
            message.getHeader().setCode(MessageConstants.FILE_REQUEST_DENIED);
            MessageHandler.sendMessage(message,getObjectOutputStream());
        }
        else{
            message.getHeader().setCode(MessageConstants.FILE_REQUEST_ACCEPTED);
            MessageHandler.sendMessage(message, getObjectOutputStream());
        }
    }

    /**
     * Display a JOptionPane to notify the source user of a file transfer request that the transfer has been denied by destination user.
     */
    public static void notifySourceUserTransferDenied(){ JOptionPane.showMessageDialog(null, "Recipient denied file transfer request."); }

    /**
     * Display a JOptionPane to notify the source user of a file transfer request that the transfer request has been accepted.
     * Begin file transfer.
     * @param message - message object containing destUser
     */
    public static void notifySourceUserAndInitiateFileSending(Message message){
        JOptionPane.showMessageDialog(null, "File transfer initiated.");
        sendFile(message.getBody().getMessage(), message.getHeader().getDestinationUser());
    }

    /**
     * If the file transfer is being initiated{
     * Display a JOptionPane to request a filename for file to be saved at the destination user.}
     * Receive file packet.
     * @param message - message object containing file packets to be downloaded
     */

    public static void promptDestUserForFilenameAndReceiveTransfer(Message message){
        if (filename.equals("")) {
            filename = JOptionPane.showInputDialog(null, "Enter filename to save file.");
            File file = new File(filename);
        }
        receiveFile(message);
    }

    /**
     * Send a file to the destination user via server, packet by packet.
     * @param filename - file to be sent.
     * @param destUser - user to send file to.
     */
    public static void sendFile(String filename, String destUser){
        try{
            File file = new File(filename);
            // creates bufferedInputsStream to read in file bytes
            BufferedInputStream fileIN = new BufferedInputStream(new FileInputStream(file));
            int sizeOfFile = (int)file.length();
            int sizePerChunk=sizeOfFile/4;
            byte[] fileBytes = new byte[sizeOfFile];
            // read file bytes into byte array
            fileIN.read(fileBytes, 0, sizeOfFile);
            fileIN.close();
            int startByte = 0;
            int endByte = sizePerChunk;
            // send the file sequentially in 4 packets
            for (int i=1; i <=4 ; i++){
                // create message object containing the file packet
                Message message = new Message();
                message.setHeader(new MessageHeader("Data Transfer"));
                message.getHeader().setMode("unicast");
                message.getHeader().setCode(MessageConstants.SEND_FILE_PACKET);
                message.getHeader().setPacketSequenceNumber(i);
                message.setBody(new MessageBody(fileBytes,startByte,sizePerChunk));
                message.getHeader().setDestinationUser(destUser);
                message.getHeader().setSourceUser(username);
                message.getHeader().setFileSize(sizeOfFile);
                // send file packet to server
                MessageHandler.sendMessage(message, getObjectOutputStream());
                startByte = endByte;
                endByte +=sizePerChunk;
            }

        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    /**
     * Send a request to server to send a file to a destination user.
     * @param usernameRecipient - destination user to recieve file.
     * @param filename - filename to send.
     */

    public static void sendFileRequest(String usernameRecipient, String filename){
        Message message = new Message();
        message.setHeader(new MessageHeader("control"));
        message.getHeader().setCode(MessageConstants.SEND_FILE_REQUEST);
        message.getHeader().setMode("unicast");
        message.getHeader().setDestinationUser(usernameRecipient);
        message.getHeader().setSourceUser(username);
        message.setBody(new MessageBody(filename));

        MessageHandler.sendMessage(message, getObjectOutputStream());
    }

    /**
     * Receive a file packet and store it in a byte array which represents the file.
     * If the last packet is received based on packetsequencenumber and the total number of packets recieved matches
     * then save final file and notify user that transfer was successful (via server).
     * else
     * notify user that file transfer was not successful (via server).
     * @param message - message object containing the file packet.
     */

    public static void receiveFile(Message message){
        try{
            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(filename));
            int fileSize = message.getHeader().getFileSize();
            int endByte = message.getBody().getStartByte() + message.getBody().getSizeOfPacket();
            for (int i = message.getBody().getStartByte() ; i < endByte ; i++){
                receivedFile[i] = message.getBody().getFilePacket()[i];
            }
            // each time a packet is received update counter
            packetCount++;
            Message messageAck = new Message();
            messageAck.setHeader(new MessageHeader("control"));
            messageAck.getHeader().setMode("unicast");
            messageAck.getHeader().setSourceUser(message.getHeader().getSourceUser());
            messageAck.setBody(new MessageBody("file transfer"));
            // if final packet received&&all other packets recieved
            // display success message and notify src user
            if (message.getHeader().getPacketSequenceNumber()==4&&packetCount==4){
                outStream.write(receivedFile,0,fileSize);
                outStream.flush();
                JOptionPane.showMessageDialog(null, "File downloaded successfully.");
                filename="";
                messageAck.getHeader().setCode(MessageConstants.FILE_TRANSFER_SUCCEEDED);
                MessageHandler.sendMessage(messageAck, getObjectOutputStream());
                packetCount=0;

            }
            // else display failure message
            // and notify src user file transfer failed
            else if (message.getHeader().getPacketSequenceNumber()==4&&packetCount!=4){
                JOptionPane.showMessageDialog(null, "File downloaded failed.");
                filename="";
                messageAck.getHeader().setCode(MessageConstants.FILE_TRANSFER_FAILED);
                MessageHandler.sendMessage(messageAck, getObjectOutputStream());
                packetCount=0;
            }

        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    /**
     * Send username of logged on user to server
     */

    static void sendUserNameToServer(){
        Message msgObj = new Message();
        msgObj.setHeader((new MessageHeader("Data Transfer")));
        msgObj.getHeader().setCode(MessageConstants.NEW_USER);
        msgObj.getHeader().setMode("unicast");
        msgObj.setBody(new MessageBody(username));
        MessageHandler.sendMessage(msgObj,getObjectOutputStream());
    }
	
	/**
     * Send a private chat request to a server thread
     * @param usernameRecipient - the client who is to be the other chat partner
     */
    public static void privateChatRequest(String usernameRecipient) {
        Message message = new Message();
        message.setHeader(new MessageHeader("command"));
        message.getHeader().setCode(MessageConstants.PRIVATE_CHAT_REQUEST);
        message.getHeader().setMode("unicast");
        message.getHeader().setDestinationUser(usernameRecipient);
        message.getHeader().setSourceUser(username);
        message.setBody(new MessageBody(usernameRecipient));
        MessageHandler.sendMessage(message, getObjectOutputStream());
    }

	/**
     * Send a private chat message to a server thread
     * @param msg - the message that is to be sent to the server thread
     */
    public static void privateChat(String msg) {
        Message message = new Message();
        message.setHeader(new MessageHeader("Data Transfer"));
        message.getHeader().setMode("unicast");
        message.getHeader().setCode(MessageConstants.PRIVATE_CHAT);
        message.getHeader().setSourceUser(username);
        message.setBody(new MessageBody(msg));
        MessageHandler.sendMessage(message, getObjectOutputStream());
    }

	/**
	 * Block a user
	 * Add the user to the client's internal blockedUsers list
	 * Send the name of the user to be blocked to a server thread
	 * @param user - the name of the user that is to be blocked
	 */
    public static void blockUser(String user) {
        blockedUsers.add(user);
        Message message = new Message();
        message.setHeader(new MessageHeader("control"));
        message.getHeader().setMode("unicast");
        message.getHeader().setCode(MessageConstants.BLOCK_USER);
        message.getHeader().setSourceUser(username);
        message.setBody(new MessageBody(user));
        MessageHandler.sendMessage(message, getObjectOutputStream());
    }

    /**
     * Log a user out of the server.
     * First notify server of logoff.
     * Then close output/input streams and finally close socket.
     */

    static void logOut(){
        Message message = new Message();
        message.setHeader(new MessageHeader("command"));
        message.getHeader().setCode(MessageConstants.LOGOUT);
        message.getHeader().setMode("unicast");
        message.setBody(new MessageBody(""));
        MessageHandler.sendMessage(message,getObjectOutputStream());
        loggedOut=true;
        try{
            getObjectOutputStream().close();
            getObjectInputStream().close();
            client.close();
        } catch (IOException e){System.out.println(e);}
    }

    public void dummyMessageTest(){
        Message corruptedMessage = new Message();
        MessageHandler.sendMessage(corruptedMessage, getObjectOutputStream());
    }
}

