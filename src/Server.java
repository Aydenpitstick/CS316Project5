
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static void main(String[] args) throws IOException {

        int port = 3000;
        int numThreads = 10;

        ExecutorService es = Executors.newFixedThreadPool(numThreads);

        File serverFolder = new File("ServerFiles");

        // Create ServerFiles if it does not exist
        if (!serverFolder.exists()) {
            serverFolder.mkdir();
        }

        try (ServerSocket listenSocket = new ServerSocket(port)) {

            System.out.println("Server is running on port " + port);

            es.submit(new KeyboardMonitor(listenSocket));

            while (true) {

                // Wait for client to connect
                Socket clientSocket = listenSocket.accept();

                // Submit a new task for each client
                es.submit(new ClientHandler(clientSocket, serverFolder));
            }

        } catch (SocketException e) {

            System.out.println("Server is shutting down.");

        } finally {

            es.shutdown();
        }
    }


    public static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private File serverFolder;

        public ClientHandler(Socket clientSocket, File serverFolder) {
            this.clientSocket = clientSocket;
            this.serverFolder = serverFolder;
        }

        @Override
        public void run() {

            System.out.println("Client Connected: "
                    + clientSocket.getInetAddress());

            try (Socket socket = clientSocket) {

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();


                int command = in.read();

                switch (command) {

                    case 'L':
                        listFiles(out);
                        break;

                    case 'D':
                        deleteFile(in, out);
                        break;

                    case 'R':
                        renameFile(in, out);
                        break;

                    case 'G':
                        downloadFile(in, out);
                        break;

                    case 'U':
                        uploadFile(in, out);
                        break;

                    default:
                        out.write('F');
                        break;
                }

                out.flush();

            } catch (IOException e) {

                System.out.println("Client error: " + e.getMessage());
            }
        }


        private void listFiles(OutputStream out) throws IOException {

            File[] files = serverFolder.listFiles();

            if (files != null) {

                for (File file : files) {

                    if (file.isFile()) {

                        out.write((file.getName() + "\n").getBytes());
                    }
                }
            }
        }


        private void deleteFile(InputStream in,
                                OutputStream out) throws IOException {

            String fileName = readLine(in);

            File file = getFile(fileName);

            if (file != null && file.exists() && file.isFile()) {

                if (file.delete()) {
                    out.write('S');
                } else {
                    out.write('F');
                }

            } else {

                out.write('F');
            }
        }


        private void renameFile(InputStream in,
                                OutputStream out) throws IOException {

            String oldName = readLine(in);
            String newName = readLine(in);

            File oldFile = getFile(oldName);
            File newFile = getFile(newName);

            if (oldFile != null && newFile != null
                    && oldFile.exists()
                    && oldFile.isFile()
                    && !newFile.exists()) {

                if (oldFile.renameTo(newFile)) {
                    out.write('S');
                } else {
                    out.write('F');
                }

            } else {

                out.write('F');
            }
        }


        private void downloadFile(InputStream in,
                                  OutputStream out) throws IOException {

            String fileName = readLine(in);

            File file = getFile(fileName);

            if (file == null || !file.exists() || !file.isFile()) {

                // File cannot be downloaded
                out.write('F');
                return;
            }


            out.write('S');
            out.flush();


            try (FileInputStream fileIn = new FileInputStream(file)) {

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {

                    out.write(buffer, 0, bytesRead);
                }
            }

            out.flush();
        }


        private void uploadFile(InputStream in,
                                OutputStream out) throws IOException {

            String fileName = readLine(in);

            File file = getFile(fileName);

            if (file == null) {
                out.write('F');
                return;
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {

                byte[] buffer = new byte[1024];
                int bytesRead;


                while ((bytesRead = in.read(buffer)) != -1) {

                    fileOut.write(buffer, 0, bytesRead);
                }

                fileOut.flush();

                // File was received successfully
                out.write('S');

            } catch (IOException e) {


                file.delete();

                out.write('F');
            }
        }


        private String readLine(InputStream in) throws IOException {

            ByteArrayOutputStream data = new ByteArrayOutputStream();

            int character;

            while ((character = in.read()) != -1 && character != '\n') {

                if (character != '\r') {
                    data.write(character);
                }
            }

            if (character == -1 && data.size() == 0) {
                return null;
            }

            return data.toString();
        }


        private File getFile(String fileName) {

            if (fileName == null || fileName.trim().isEmpty()
                    || fileName.contains("/")
                    || fileName.contains("\\")
                    || fileName.equals(".")
                    || fileName.equals("..")) {

                return null;
            }

            return new File(serverFolder, fileName);
        }
    }


    public static class KeyboardMonitor implements Runnable {

        private ServerSocket listenSocket;

        public KeyboardMonitor(ServerSocket listenSocket) {
            this.listenSocket = listenSocket;
        }

        @Override
        public void run() {

            Scanner keyboard = new Scanner(System.in);

            while (true) {

                String input = keyboard.nextLine();

                if (input.equalsIgnoreCase("Q")) {

                    try {

                        listenSocket.close();

                        return;

                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
