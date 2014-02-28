package MultiThreadedServerClient;

import java.io.*;
import java.net.InetAddress;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author sanket
 */
public class Client {

    private static SocketStream socket = null;

    public static void main(String[] args) {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);

        try {
            System.out.println("Welcome to the client.\n"
                    + "What is the server hostname?");
            String hostName = br.readLine();
            if (hostName.length() == 0) // if user did not enter a name
            {
                hostName = "localhost"; // use the default host name
            }
            System.out.println("What is the port number of the server host?");
            String portNum = br.readLine();
            if (portNum.length() == 0) {
                portNum = "29456"; // default port number
            }
            socket = new SocketStream(
                    InetAddress.getByName(hostName), Integer.parseInt(portNum));
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host ");
        }

        if (socket != null) {

            try {
                boolean done = false;
                String echo;
                
                String initialMsg = socket.receiveMessage();
                if(initialMsg.equals("Server too busy. Try later.")) {
                    System.out.println(initialMsg);
                    socket.close();
                    return;
                }
                
                System.out.println(initialMsg);
                
                String name = br.readLine();
                socket.sendMessage(name);
                        

                while (!done) {

                    System.out.println("Enter Code: upload = Upload a file, read = Read a file, write = Write a file, delete = Delete, logout = Exit");
                    String message = br.readLine();
                    boolean messageOK = false;

                    if (message.equals("upload")) {
                        messageOK = true;
                        socket.sendMessage("upload");
                        System.out.println("\nEnter the path of the file you want to upload");
                        String filePath = br.readLine();
                        File file = new File(filePath);

                        while (!file.exists()) {
                            System.out.println("No such file found. Try a different path or type 'exit' ");
                            filePath = br.readLine();
                            if (filePath.equals("exit")) {
                                break;
                            }
                            file = new File(filePath);
                        }
                        if (filePath.equals("exit")) {
                            continue;
                        }
                        socket.sendMessage(filePath);
                        socket.sendFile(file);
                    }

                    if (message.equals("read")) {
                        messageOK = true;
                        socket.sendMessage("read");
                        System.out.println("\nEnter the path of the file you want to read");
                        String filePath = br.readLine();
                        socket.sendMessage(filePath);
                        System.out.println("\nStarting to read file: " + filePath + "\n\n");
                        System.out.println(socket.receiveMessage());
                        System.out.println("\n\nCompleted reading file: " + filePath + "\n");
                        continue;
                    }

                    if (message.equals("write")) {
                        messageOK = true;
                        socket.sendMessage("write");
                        System.out.println("\nEnter the path of the file you want to write to");
                        String filePath = br.readLine();
                        socket.sendMessage(filePath);
                        System.out.println("\nStarting to write file: " + filePath + "\n\n");

                        System.out.println(socket.receiveMessage());

                        Writer writer = null;
                        try {
                            writer = new BufferedWriter(new OutputStreamWriter(
                                    new FileOutputStream(filePath + "-edits"), "utf-8"));
                            writer.write(br.readLine());
                        } catch (IOException ex) {
                        } finally {
                            try {
                                writer.close();
                            } catch (Exception ex) {
                            }
                        }

                        File outputFile = new File(filePath + "-edits");
                        socket.sendFile(outputFile);
                        outputFile.delete();
                        continue;
                    }

                    if (message.equals("delete")) {
                        messageOK = true;
                        socket.sendMessage("delete");
                        System.out.println("Enter the path of the file you want to delete or type 'exit' ");
                        String filePath = br.readLine();
                        if (filePath.equals("exit")) {
                            continue;
                        }
                        socket.sendMessage(filePath);
                        continue;
                    }

                    if (message.equals("logout")) {
                        messageOK = true;
                        System.out.println("Logged Out");
                        done = true;
                        socket.sendMessage("logout");
                        socket.close();
                        break;
                    }

                    if (messageOK) {
                        System.out.println("Invalid input");
                        continue;
                    }

                    // get reply from server
                    echo = socket.receiveMessage();
                    System.out.println(echo);
                } // end while
            } // end try
            catch (IOException | NumberFormatException e) {
                System.err.println("IOException:  " + e);
            } // end catch

        }	//end-if(socket!=null)

    } // end main
} // end class
