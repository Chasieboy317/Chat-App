import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class represents the Server.
 * It creates a server socket and port to listen on for incoming connections.
 * Then starts a new thread for every connected client.
 */
public class Server {

    public static final int PORT =1337;
    public static ArrayList<User> connectedSockets = new ArrayList<User>();
    public static ArrayList<Chat> connectedChats = new ArrayList<Chat>();
    public static int id = 0;

    public static void main (String[] args) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(PORT);
            System.out.println("Server running");

            while(true){
                User user;
                try {
                    // create a new user for every new socket
                    user = new User(ss.accept(),connectedSockets, connectedChats);
                    user.setUsername(getClientMessage(user.getObjectInputStream()));
                    // add new user to list of connected sockets
                    connectedSockets.add(user);
                    // add list of currently connected users to new user
                    System.out.println("Connection made");
                    // start user thread
                    Thread userThread = new Thread(user);
                    userThread.start();
                }
                catch (Exception e) {
                    System.out.println(e);
                }

            }
        }

        catch (Exception e) {
            System.out.println(e);
        }

    }

    public static String getClientMessage(ObjectInputStream objectInputStream){
        try {
            Message msgObj = (Message) objectInputStream.readObject();

            return msgObj.getBody().getMessage();
        }
        catch (Exception e) {
            System.out.println(e);
        }
        return "message is NULL";
    }

}
