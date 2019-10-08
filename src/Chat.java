import java.io.*;
import java.net.*;

public class Chat {
	String id;
	User user1;
	User user2;
	
	//Constructor
	public Chat(String id, User user1, User user2) {
		this.id = id;
		this.user1=user1;
		this.user2=user2;
	}
	//End Constructor

	/** 
	 * Notify both users that a new chat has been created
	 */
	public void startChat() {
		Message msg = new Message();
		msg.setHeader(new MessageHeader("Data Transfer"));
		msg.getHeader().setMode("unicast");
		msg.getHeader().setCode(MessageConstants.PRIVATE_CHAT_REQUEST);
		msg.setBody(new MessageBody("startGUI"));
		MessageHandler.sendMessage(msg, user1.getObjectOutputStream());
		MessageHandler.sendMessage(msg, user2.getObjectOutputStream());
	}

	/** 
	 * Send the message to both users
	 @param message - messaeg that is to be sent to both users
	 */
	public void sendToUsers(Message message) {
		message.getBody().setMessage(message.getHeader().getSourceUser()+": "+message.getBody().getMessage());
		MessageHandler.sendMessage(message, user1.getObjectOutputStream());
		MessageHandler.sendMessage(message, user2.getObjectOutputStream());
	}

	public User getUser1() {
		return user1;
	}

	public User getUser2() {
		return user2;
	}
}
