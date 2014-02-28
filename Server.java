package MultiThreadedServerClient;

import java.io.*;
import java.net.*;

public class Server {

    private static ServerSocket serverSocket = null;
    private static SocketStream clientSocket = null;

    static final String loginMessage = "Logged In";
    static final String logoutMessage = "Logged Out";
    private static final int maxClients = 2;
    private static final clientThread[] threads = new clientThread[maxClients];

    public static void main(String[] args) {
        int serverPort = 29456; // default port
        //String message;

        if (args.length < 1) {
            System.out.println("Usage: java Server <portNumber>\n"
                    + "Now using port number=" + serverPort);
        } else {
            //serverPort = Integer.valueOf(args[0]).intValue();
            serverPort = Integer.parseInt(args[0]);
        }
        /*
         try {
         ServerSocket serverSocket = new ServerSocket(serverPort);
         System.out.println("Server ready.");
         } catch (IOException e) {
         System.out.println(e);
         }
         */
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server ready");
        } catch (IOException e) {
            System.out.println(e);
        }

        // instantiates a stream socket for accepting connections
        //ServerSocket serverSocket = new ServerSocket(serverPort);
        //System.out.println("Daytime server ready.");
        while (true) { // forever loop

            try {

                // wait to accept a connection
                System.out.println("Waiting for a connection..");
                clientSocket = new SocketStream(serverSocket.accept());
                //clientSocket = serverSocket.accept();
                //System.out.println("Connection accepted..");
                
                //boolean done = false;

                int i=0;

                //code to handle new clients. Make a new thread if it does not exist
                for (i = 0; i < maxClients; i++) {
                    if (threads[i] == null) {
                        //System.out.println("Waiting for a connection..");
                        //clientSocket = new SocketStream(serverSocket.accept());
                        //System.out.println("Connection accepted..");
                        
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        System.out.println("Connection accepted..");
                        System.out.println("Number of active threads = "+(i+1));
                        break;
                    }
                }

                //Code to handle client limit
                if (i == maxClients) {
                    //DataOutputStream os = new DataOutputStream(clientSocket.output);
                    //os.writeChars("Server too busy. Try later.");
                    clientSocket.sendMessage("Server too busy. Try later.");
                    //os.close();
                    clientSocket.close();
                }

            } catch (IOException e) {
                System.out.println(e);
            }
        }	//end while-true		
    }	//main
}	//Server	

//class clientThread implements Runnable {
class clientThread extends Thread {

    private String clientName = null;
    private DataInputStream is = null;
    //private PrintStream os = null;
    private DataOutputStream os = null;
    private SocketStream clientSocket = null;
    private final clientThread[] threads;
    private int maxClients;

    public clientThread(SocketStream clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClients = threads.length;
    }

    public void run() {

        int maxClients = this.maxClients;
        clientThread[] threads = this.threads;

        try {
            /*
             * Create input and output streams for this client.
             */
            //is = new DataInputStream(clientSocket.socket.getInputStream());
            //os = new PrintStream(clientSocket.socket.getOutputStream());
            
            is = clientSocket.input;
            os = clientSocket.output;

            String name;
            while (true) {
                clientSocket.sendMessage("Enter your name:");
                //name = is.readLine().trim();
                name = clientSocket.receiveMessage().trim();
                if (name.indexOf('@') == -1) {
                    break;
                } else {
                    clientSocket.sendMessage("The name should not contain '@' character.");
                }
            }

            clientSocket.sendMessage("Welcome " + name
                    + "\nTo leave enter /quit in a new line.");

            synchronized (this) {
                for (int i = 0; i < maxClients; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }
                for (int i = 0; i < maxClients; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        threads[i].clientSocket.sendMessage("*** A new client " + name
                                + " has connected to the server !! ***");
                    }
                }
            }

            /**
             * Start client code handling *
             */
            while (true) {

                String line = is.readLine();
                if (line.startsWith("/quit")) {
                    break;
                }

                /**
                 * ** UPLOAD ***
                 */
                if ((line.trim()).equals("upload")) {
                    String filePath = clientSocket.receiveMessage();
                    if (filePath.equals("exit")) {
                        continue;
                    }
                    //filePath = "Received/"+filePath;
                    boolean create;
                    create = new File(filePath).mkdirs();
                    File outFile = new File(filePath);
                    outFile.delete();

                    clientSocket.receiveFile(outFile);
                    clientSocket.sendMessage("File received " + outFile.length() + " bytes");
                }
                /**
                 * ** READ ***
                 */
                if ((line.trim()).equals("read")) {
                    String filePath = clientSocket.receiveMessage();
                    File file = new File(filePath);
                    if (!file.exists()) {
                        clientSocket.sendMessage("Invalid filePath. Try Again!\n");
                        continue;
                    }
                    clientSocket.readLock(file, "set");
                    System.out.println("Read lock has been set");
                    System.out.println("Client reading file: " + filePath);
                    clientSocket.sendReadFile(file);
                    clientSocket.readLock(file, "release");
                }

                /**
                 * ** WRITE ***
                 */
                if ((line.trim()).equals("write")) {
                    String filePath = clientSocket.receiveMessage();
                    File file = new File(filePath);
                    if (!file.exists()) {
                        clientSocket.sendMessage("Invalid filePath. Try Again!\n");
                        continue;
                    }

                    clientSocket.writeLock(file, "set");
                    System.out.println("Write lock has been set to: ");
                    System.out.println("Client writing to file: " + filePath);

                    clientSocket.sendReadFile(file);
                    File editsToFile = new File(filePath + "-editsreceived");
                    clientSocket.receiveFile(editsToFile);
                    try {
                        try (FileWriter fw = new FileWriter(filePath, true);
                                BufferedReader bready = new BufferedReader(new FileReader(filePath + "-editsreceived"))) {
                            StringBuilder sb = new StringBuilder();
                            String liner = bready.readLine();
                            while (liner != null) {
                                sb.append(liner);
                                sb.append(System.lineSeparator());
                                liner = bready.readLine();
                            }
                            String alledits = sb.toString();
                            fw.write(alledits);
                        }
                    } catch (IOException ioe) {
                        System.err.println("IOException: " + ioe.getMessage());
                    }
                    editsToFile.delete();
                    clientSocket.writeLock(file, "release");
                }

                /**
                 * ** DELETE ***
                 */
                if ((line.trim()).equals("delete")) {
                    // Delete the file
                    String filePath = clientSocket.receiveMessage();
                    if (filePath.equals("exit")) {
                        continue;
                    }
                    File deletionFile = new File(filePath);
                    deletionFile.delete();
                    System.out.println("File deleted");
                }

            }//end-while(true); client handling code

            synchronized (this) {
                for (int i = 0; i < maxClients; i++) {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientName != null) {
                        threads[i].clientSocket.sendMessage("*** The client " + name
                                + " is leaving !! ***");
                    }
                }
            }
            clientSocket.sendMessage("*** Bye " + name + " ***");

            /*
             * Clean up. Set the current thread variable to null so that a new client
             * could be accepted by the server.
             */
            synchronized (this) {
                for (int i = 0; i < maxClients; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }

            /*
             * Close the output stream, close the input stream, close the socket.
             */
            is.close();
            os.close();
            clientSocket.close();

        } //end-try-in-run()
        catch (IOException ex) {
            ex.printStackTrace();
        }

    }	//end run()

}	//end class clientThread

/*
                    
 if( (message.trim()).equals("500") ) {
 // Session over; close the data socket.
 clientSocket.sendMessage(logoutMessage);
 clientSocket.close();
 done = true;
 } // end if

                    

 } // end while !done
 } // end try
 catch (IOException ex) {
 ex.printStackTrace();
 }
 } // end while-forever

 } // end main
 } // end class
 */
