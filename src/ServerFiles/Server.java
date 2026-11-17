package ServerFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 3000;
        int numThreads = 10;
        ExecutorService es = Executors.newFixedThreadPool(numThreads);


        try (ServerSocket listenSocket = new ServerSocket(port)){ // Try-With-Resource
            es.submit(new keyboardMonitor(listenSocket));
            while (true) {
                //WAIT FOR CLIENT TO CONNECT
                Socket clientSocket = listenSocket.accept();
                es.submit(new ClientHandler(clientSocket));
                // Read Keyboard
            } // End While Loop
        }catch (SocketException e){
            es.shutdown();
        }
    } // End of Main

    public static class ClientHandler implements Runnable{
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

            System.out.println("Client Connected: " + clientSocket.getInetAddress());
            try {
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    byte[] reply = message.toUpperCase().getBytes();
                    out.write(reply);
                    System.out.println(reply);
                } // End of ByteRead Loop

                clientSocket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        } //End of Run
    }

    public static class keyboardMonitor implements Runnable{
        private ServerSocket litsenSocket;

        public keyboardMonitor(ServerSocket litsenSocket) {
            this.litsenSocket = litsenSocket;
        }

        @Override
        public void run() {
            Scanner keyboard = new Scanner(System.in);
            while (true){
                String input = keyboard.nextLine();
                if (input.equalsIgnoreCase("Q")){
                    try {
                        litsenSocket.close();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } //End of If
            } // End of Loop
        } // End of Run
    }

} // End of Class