package chat.server;

import chat.network.TCPConnection;
import chat.network.TCPConnectionlistener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Scanner;


public class ChatServer implements TCPConnectionlistener {

    private static int port;
    private static int number;

    public static void main(String[] args){
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter port: ");
        port = myObj.nextInt();
        System.out.println("Enter number of people: ");
        number = myObj.nextInt();
        new ChatServer(port, number);
    }

    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    public ChatServer(int port, int number){

        System.out.println("Server running...");
        try(ServerSocket serverSocket = new ServerSocket(port)){
            while (true) {
                if (connections.size() < number) {
                    try {
                        new TCPConnection(this, serverSocket.accept());
                    } catch (IOException e) {
                        System.out.println("TCPConnection " + e);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
          } catch(IOException e){
              throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void onConnetionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        sendToAllConnections("     Client connected: " + tcpConnection + "/");
    }

    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String string) {
        sendToAllConnections(string);
    }

    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        sendToAllConnections("     Client disconnected: " + tcpConnection + "/");
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection: " + e);
    }

    private void sendToAllConnections(String string){
        int cnt = connections.size();
        for(int i=0; i<cnt; i++){
            connections.get(i).sendString(string);
        }
    }
}
