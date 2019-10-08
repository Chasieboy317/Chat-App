import java.net.*;
import java.io.*;
import java.util.*;

/**
 * This class acts as the Server for each connected client.
 * The client and user class communicate via their sockets and input/output streams.
 */

public class User implements Runnable{
	String username;
	Socket socket;
	boolean loggedOut = false;
	static ArrayList<User> users;
	ArrayList<Chat> chats;
	ArrayList<User> blockedUsers;

	ObjectOutputStream objectOutputStream;
	ObjectInputStream objectInputStream;


	/**
	 * Constructor for the User class. Sets the socket, connected users and list of private chats
	 * @param socket - the socket for client communication
	 * @param users - list of connected users
	 * @param chats - list of ongoing private chats
	 * @param blockedUsers - list of blocked users
	 */
	//Constructor
	User(Socket socket, ArrayList<User> users, ArrayList<Chat> chats){
		this.socket = socket;
		this.users=users;
		this.chats=chats;
		blockedUsers = new ArrayList<User>();
		try{
			this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectOutputStream.flush();
			this.objectInputStream = new ObjectInputStream(socket.getInputStream());
		}
		catch(IOException e){
			System.out.println(e);
		}
	}
	//End Constructor

	public ObjectOutputStream getObjectOutputStream(){
		return this.objectOutputStream;
	}
	public ObjectInputStream getObjectInputStream(){
		return this.objectInputStream;
	}

	/**
	 * This method is called from Server (thread.run).
	 * It is responsible for sending and recieving all messages from the Client and forwarding them to
	 * the relevant recipients.
	 * Received messages are processed in message handler, and then acted on based on message code.
	 */
	public void run(){

		while (!loggedOut){

			try {

				sendClientOnlineUsers();
				Message msgObj = (Message) objectInputStream.readObject();

				int messageCode = MessageHandler.handleMessage(msgObj);

				if (msgObj != null) {

					switch (messageCode) {
						case MessageConstants.NEW_USER:
							this.username = msgObj.getBody().getMessage();
							break;
						case MessageConstants.SEND_FILE_REQUEST:
							if (msgObj.getHeader().getSourceUser().equals(username)) {
								sendFileTransferRequestFromServer(msgObj);
							}
							break;
						case MessageConstants.FILE_REQUEST_DENIED:
							if (!msgObj.getHeader().getSourceUser().equals(username)) {
								notifyServerFileRequestDenied(msgObj);
							} else {
								notifyClientFileRequestDenied(msgObj);
							}
							break;
						case MessageConstants.FILE_REQUEST_ACCEPTED:
							if (msgObj.getHeader().getDestinationUser().equals(username)) {
								notifyServerFileRequestAccepted(msgObj);
							} else {
								notifyUserFileRequestAccepted(msgObj);
							}
							break;
						case MessageConstants.SEND_FILE_PACKET:
							if (msgObj.getHeader().getSourceUser().equals(username)) {
								sendFilePacketToServer(msgObj);
							} else {
								sendFilePacketToDestinationUser(msgObj);
							}
							break;
						case MessageConstants.GROUP_CHAT:
							broadcastMessage(username + ": " + msgObj.getBody().getMessage());
							break;
						case MessageConstants.BLOCK_USER:
							blockUser(msgObj);
							break;
						case MessageConstants.PRIVATE_CHAT_REQUEST:
							startPrivateChat(msgObj);
							break;
						case MessageConstants.PRIVATE_CHAT:
							sendPrivateMessage(msgObj);
							break;
						case MessageConstants.LOGOUT:
							logOut();
							break;
						case MessageConstants.FILE_TRANSFER_FAILED:
							if (msgObj.getHeader().getSourceUser().equals(username)){
								MessageHandler.sendMessage(msgObj, getObjectOutputStream());
							}
							else{
								for (User u : users){
									if (u.username.equals(msgObj.getHeader().getSourceUser())){
										MessageHandler.sendMessage(msgObj, u.getObjectOutputStream());
									}
								}
							}
							break;
						case MessageConstants.FILE_TRANSFER_SUCCEEDED:
							if (msgObj.getHeader().getSourceUser().equals(username)){
								MessageHandler.sendMessage(msgObj, getObjectOutputStream());
							}
							else{
								for (User u : users){
									if (u.username.equals(msgObj.getHeader().getSourceUser())){
										MessageHandler.sendMessage(msgObj, u.getObjectOutputStream());
									}
								}
							}
							break;
						case MessageConstants.CORRUPTED_MESSAGE:
							System.out.println("Corrupted message being discarded");
							break;
					}
				}

			} catch (Exception e){
				System.out.println(e);
			}

		}

	}

	/**
	 * Iterate through the list of users and find both users associated with the msgObj
	 * If the users are found, iterate through Server.connectedChats and if an existing chat containing the users already exists, remove it
	 * If the users haven't blocked each other, create a new chat and add it to Server.connectedChats
	 * notify both users of the new private chat
	 * @param msgObj - message object that is used to create the chats. It contains the command to create a new chat, as well as both usernames
	 */
	void startPrivateChat(Message msgObj) { 
		String otherUsername = msgObj.getBody().getMessage();
		User user1=null;
		User user2=null;
		for (User u: users) {
			if (u.getUsername().equals(otherUsername)) {
				user2 = u;
			}
			else if (u.getUsername().equals(msgObj.getHeader().getSourceUser())) {
				user1 = u;
			}
		}
		if (user1==null||user2==null) {}
		else {
			for (Chat c: Server.connectedChats) {
				if (c.getUser1().equals(user1)||c.getUser1().equals(user2)||c.getUser2().equals(user2)||c.getUser2().equals(user1)) {Server.connectedChats.remove(c); break;} //check to see if there is an existing chat containing user1 and user2
			}
			if (!(user1.checkIfBlocked(user2)||user2.checkIfBlocked(user1))) { //check to see if either user has blocked each other
				Chat chat = new Chat((Server.id++)+"", user1, user2);
				Server.connectedChats.add(chat);
				chat.startChat(); //notify the users of a new private chat
			}
		}
	}

	/** 
	 * Send a private message to both users
	 * Iterate through the list of chats in Server.connectedChats and find the users with the associated SourceUser strings
	 * Send the message to both users
	 * @param msgObj - Message object containing the message that is to be sent to both users, as well as the name of the user it is to be sent to
	 */
	void sendPrivateMessage(Message msgObj) {
		for (Chat c: Server.connectedChats) {
			if (c.getUser1().getUsername().equals(msgObj.getHeader().getSourceUser()) || c.getUser2().getUsername().equals(msgObj.getHeader().getSourceUser())) {
				c.sendToUsers(msgObj);
			}
		}
	}

	/**
	 * Block a user
	 * For all users in Server.connectedSockets, if the name given in msgObj matches one of the users add them to the user's block list
	 * param msgObj - Message object containing the SourceUser name and the name of the user who is to be blocked
	 */
	void blockUser(Message msgObj) {
		for (User user: Server.connectedSockets) {
			if (user.getUsername().equals(msgObj.getBody().getMessage())) {
				this.blockedUsers.add(user);
			}
		}
	}

	/**
	 * Send client a list of users that are connected to the server.
	 */
	void sendClientOnlineUsers(){
		String onlineUsers ="";
		for (User u : Server.connectedSockets){
			onlineUsers+=u.username+"\n";
		}
		Message sendOnline = new Message();
		sendOnline.setHeader(new MessageHeader("Data Transfer"));
		sendOnline.getHeader().setCode(MessageConstants.GET_ONLINE_USERS);
		sendOnline.getHeader().setMode("unicast");
		sendOnline.setBody(new MessageBody(onlineUsers));
		for (User u: Server.connectedSockets) {
			MessageHandler.send(sendOnline, u.getObjectOutputStream());
		}
	}

	/**
	 * Send a broadcasted message to all connected users
	 * @param message
	 */
	void broadcastMessage(String message){

		Message msgObject = new Message();
		msgObject.setBody(new MessageBody(message));
		msgObject.setHeader(new MessageHeader("Data Transfer"));
		msgObject.getHeader().setDirection("ToServer");
		msgObject.getHeader().setMode("broadcast");
		msgObject.getHeader().setSourceUser(username);
		msgObject.getHeader().setCode(MessageConstants.GROUP_CHAT);

		try{

			for(User u:users){
				MessageHandler.sendMessage(msgObject,u.getObjectOutputStream());
			}
		}catch(Exception e){
			System.out.println(e);
			System.out.println("Couldn't send to all Users");
		}

	}

	/**
	 * Send a user a file transfer request.
	 * @param message - message object containing file transfer request details
	 */
	void sendFileTransferRequestFromServer(Message message){
		String receiver = message.getHeader().getDestinationUser();
		for (User u : users) {
			if (u.username.equals(receiver)) {
				Message newMessage= new Message();
				newMessage.setHeader(new MessageHeader("command"));
				newMessage.setBody(new MessageBody(message.getBody().getMessage()));
				newMessage.getHeader().setCode(MessageConstants.SEND_FILE_REQUEST);
				newMessage.getHeader().setMode("unicast");
				newMessage.getHeader().setSourceUser(message.getHeader().getSourceUser());
				newMessage.getHeader().setDestinationUser(message.getHeader().getDestinationUser());
				MessageHandler.sendMessage(newMessage, u.getObjectOutputStream());
			}
		}
	}

	/**
	 * Notify a client's related User thread (Server) that the file request was denied.
	 * @param message
	 */
	void notifyServerFileRequestDenied(Message message){
		for (User u : users) {
			if (u.username.equals(message.getHeader().getSourceUser())) {
				Message newMessage= new Message();
				newMessage.setHeader(new MessageHeader("control"));
				newMessage.setBody(new MessageBody(message.getBody().getMessage()));
				newMessage.getHeader().setCode(MessageConstants.FILE_REQUEST_DENIED);
				newMessage.getHeader().setMode("unicast");
				newMessage.getHeader().setSourceUser(message.getHeader().getSourceUser());
				newMessage.getHeader().setDestinationUser(message.getHeader().getDestinationUser());
				MessageHandler.sendMessage(newMessage, u.getObjectOutputStream());
			}
		}
	}

	/**
	 * Send a message to the client to notify them that their file transfer request was denied.
	 * @param message
	 */
	void notifyClientFileRequestDenied(Message message){
		Message newMessage= new Message();
		newMessage.setHeader(new MessageHeader("control"));
		newMessage.setBody(new MessageBody(message.getBody().getMessage()));
		newMessage.getHeader().setCode(MessageConstants.FILE_REQUEST_DENIED);
		newMessage.getHeader().setMode("unicast");
		newMessage.getHeader().setSourceUser(message.getHeader().getSourceUser());
		newMessage.getHeader().setDestinationUser(message.getHeader().getDestinationUser());
		MessageHandler.sendMessage(newMessage, getObjectOutputStream());
	}

	/**
	 * Send a message to the server to notify the sending client that the file transfer request was accepted.
	 */
	void notifyServerFileRequestAccepted(Message message){
		for (User u :users){
			if (u.username.equals(message.getHeader().getSourceUser())){
				MessageHandler.sendMessage(message, u.getObjectOutputStream());
			}
		}
	}
	/**
	 * Send a message to the client to notify them that their file transfer request was accepted.
	 * @param message
	 */
	void notifyUserFileRequestAccepted(Message message){
		MessageHandler.sendMessage(message,getObjectOutputStream());
	}

	/**
	 * Send a file packet to the server to be forwarded to destination user
	 * @param message - message containing the file packet
	 */
	void sendFilePacketToServer(Message message){
		for (User u : users) {
			if (u.username.equals(message.getHeader().getDestinationUser())) {
				MessageHandler.sendMessage(message, u.getObjectOutputStream());

			}
		}
	}

	/**
	 * Send a file packet to the client.
	 * @param message - message containing file packet.
	 */
	void sendFilePacketToDestinationUser(Message message){
		MessageHandler.sendMessage(message,getObjectOutputStream());
	}

	/**
	 * Check if a user is blocked
	 * Iterate through the list of blocked users and if a user has been found return true
	 * @param u - the user that must be searched for through the list of blockedUsers
	 */
	public boolean checkIfBlocked(User u) {
		for (User user: this.blockedUsers) {
			if (user.equals(u)) {
				return true;
			}
		}
		return false;
	}

	public void setUsername(String username) {
		this.username=username;
	}

	public String getUsername() {
		return username;
	}

	public Socket getSocket() {
		return socket;
	}

	/**
	 * Recieves a message that a client has logged out.
	 * Removes them from the server's list of online users and then closes input/output streams and the socket.
	 */
	void logOut(){
		try{	loggedOut = true;

			for (User u : users){

				if(u.username.equals(username)){

					Server.connectedSockets.remove(u);
					break;
				}
			}
			getObjectOutputStream().close();
			getObjectInputStream().close();
			socket.close();
			System.out.println("User successfully logged out.");


		} catch (IOException e){
			System.out.println(e);
		}
	}

}
