package ClientFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        if (args.length != 2){
            System.out.println("Need 2 Arguments: ServerIP & ServerPort");
            return;
        } // End of If Statement
        Scanner keyboard = new Scanner(System.in);
        String input = keyboard.nextLine();
        byte[] message = input.getBytes();

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        // Trigger a connect request to the server and establish connection
        Socket mySocket = new Socket(serverIP, serverPort);
        InputStream in = mySocket.getInputStream();
        OutputStream out = mySocket.getOutputStream();
        out.write(message);
        // Trigger special signal to the server
        // So that the server can get out of the loop
        mySocket.shutdownOutput();

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = in.read(buffer))!=-1){
            String reply = new String(buffer, 0, bytesRead);
            System.out.println(reply);
        } // End of While Loop
        mySocket.close();
    } //End of Main
} //End of Class
