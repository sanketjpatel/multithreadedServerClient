package MultithreadedServerClient;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.*;
import java.net.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author sanket
 */
public class SocketStream {

    protected Socket socket;
    protected DataInputStream input;
    protected DataOutputStream output;

    SocketStream(InetAddress acceptorHost, int acceptorPort)
            throws SocketException, IOException {
        socket = new Socket(acceptorHost, acceptorPort);
        setStreams();
    }

    SocketStream(Socket socket) throws IOException {
        this.socket = socket;
        setStreams();
    }

    private void setStreams() throws IOException {
        // get an input stream for reading from the data socket
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
    }

    public void sendMessage(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    } // end sendMessage

    public String receiveMessage() throws IOException {
        String message = input.readUTF();
        return message;
    } // end receiveMessage

    public void close() throws IOException {
        socket.close();
    }

    public void sendFile(File file) throws IOException {
        FileInputStream fileIn = new FileInputStream(file);
        byte[] buf = new byte[Short.MAX_VALUE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buf)) != -1) {
            output.writeShort(bytesRead);
            output.write(buf, 0, bytesRead);
        }
        output.writeShort(-1);
        fileIn.close();
    }

    public void receiveFile(File file) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        byte[] buf = new byte[Short.MAX_VALUE];
        int bytesSent;
        while ((bytesSent = input.readShort()) != -1) {
            input.readFully(buf, 0, bytesSent);
            fileOut.write(buf, 0, bytesSent);
        }
        fileOut.close();
    }

    public void sendReadFile(File file) throws IOException {
        BufferedReader breader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        line = breader.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = breader.readLine();
        }

        String everything = sb.toString();
        sendMessage(everything);

        breader.close();
    }

    public boolean readLock(File file, String locktype) {
        switch (locktype) {
            case "set":
                return true;
            case "release":
                return false;
            default:
                return false;
        }
    }

    public boolean writeLock(File file, String locktype) {
        switch (locktype) {
            case "set":
                return true;
            case "release":
                return false;
            default:
                return false;
        }
    }

} // end class
