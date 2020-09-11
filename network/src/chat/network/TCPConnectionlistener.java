package chat.network;

public interface TCPConnectionlistener {

    void onConnetionReady(TCPConnection tcpConnection);
    void onReceiveString(TCPConnection tcpConnection, String string);
    void onDisconnect(TCPConnection tcpConnection);
    void onException(TCPConnection tcpConnection, Exception e);

}
