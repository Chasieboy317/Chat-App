import java.util.*;
import java.io.*;

/**
 * This class contains the logic for sending and receiving message objects from Server to Client and vice-versa.
 */
public class MessageHandler{


	/**
	 * Checks that a message is not corrupted and then sends it according to MessageType.
	 * @param message - message object to be sent.
	 */

	//sendMessage, break up into type
	public static void sendMessage(Message message, ObjectOutputStream objectOutputStream){

		//Data Transfer messages
		if (message.getHeader()==null||message.getBody()==null||message.getHeader().getMessageType()==null||message.getHeader().getMode()==null){
			System.out.println("Message corrupted. Not passing message on.");
		}
		else if(message.getHeader().getMessageType().equals("control")){
			System.out.println("Sending control message");
			send(message,objectOutputStream);
		}
		else if (message.getHeader().getMessageType().equals("command")){
			System.out.println("Sending command message");
			send(message,objectOutputStream);
		}
		else{
			System.out.println("Sending data transfer message");
			send(message,objectOutputStream);
		}
	}
	/**
	 * Takes in a Message object and objectOutputStream
	 * then based on type, mode and code the messages are sent on appropriately to the Client or Server.
	 * @param message - message object to be sent.
	 */

	public static void send(Message message, ObjectOutputStream objectOutputStream){

		try{
			objectOutputStream.writeObject(message);
			objectOutputStream.flush();

		} catch(IOException e){
			System.out.println(e);
		}

	}

	/**
	 * Returns the message code of a recieved message so that the or Client knows how to handle the incoming message.
	 * @param message - message object being receieved.
	 * @return the message code of the message object.
	 */
	public static int  handleMessage(Message message){

		if (message.getHeader()==null||message.getBody()==null||message.getHeader().getMessageType()==null||message.getHeader().getMode()==null){
			System.out.println("Message corrupted");
			return MessageConstants.CORRUPTED_MESSAGE;
		}
		else if (message.getHeader().getMessageType().equals("control")){
			System.out.println("Control message received");
			return  message.getHeader().getCode();
		}
		else if (message.getHeader().getMessageType().equals("command")){
			System.out.println("Command message received");
			return message.getHeader().getCode();
		}
		else {
			System.out.println("Data transfer message received");
			return message.getHeader().getCode();

		}

	}





}
