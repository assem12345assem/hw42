import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Handler {
    private String userName;
    private Socket userSocket;
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private static Map<Socket, String> users = new HashMap<>();
    private static List<Socket> clients =  new ArrayList<>();


    public Handler(Socket userSocket) {
        this.userSocket = userSocket;
        userName = String.valueOf(userSocket.getPort());
        users.put(userSocket, userName);
        clients.add(userSocket);
    }

    public String getUserName() {
        return userName;
    }

    public Socket getUserSocket() {
        return userSocket;
    }

    public static void submitThreads(Socket socket) {
        pool.submit(()-> handle(socket));
    }

    private static void handle(Socket socket)  {

        System.out.printf("Подключён клиент: %s%n", socket);
        Handler handler = new Handler(socket);
        try(Scanner reader = getReader(socket);
            PrintWriter writer = getWriter(socket);
            socket){
            sendResponse("Привет " + socket, writer);
            while (true) {
                String message = "<" + users.get(socket) + ">"+ reader.nextLine().strip();
                System.out.printf("Got: %s%n", message);

                sendResposeAllUsers(message, socket, clients);

                if(isEmptyMsg(message) || isQuitMsg(message)) {
                    break;
                }
            }
        } catch (NoSuchElementException e){
            System.out.println("Client dropped the connection!");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.printf("Client disconnected: %s%n", socket);
        }
    }
    private static PrintWriter getWriter(Socket socket) throws IOException {
        OutputStream stream = socket.getOutputStream();
        return new PrintWriter(stream);
    }
    private static Scanner getReader(Socket socket) throws IOException{
        InputStream stream = socket.getInputStream();
        InputStreamReader input = new InputStreamReader(stream, "UTF-8");
        return new Scanner(input);
    }
    private static boolean isQuitMsg(String message) {
        return "bye".equalsIgnoreCase(message);
    }
    private static boolean isEmptyMsg(String message) {
        return message == null || message.isBlank();
    }
    private static void sendResponse(String response, Writer writer) throws IOException{
        writer.write(response);
        writer.write(System.lineSeparator());
        writer.flush();
    }

    private static void sendResposeAllUsers(String response, Socket socket, List<Socket> clients) throws IOException {
        if(!clients.equals(socket)) {
            for(Socket client : clients) {
                sendResponse(response, getWriter(client));
            }
        }
    }
}
