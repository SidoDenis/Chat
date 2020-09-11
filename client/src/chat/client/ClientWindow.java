package chat.client;

import chat.network.TCPConnection;
import chat.network.TCPConnectionlistener;
import sun.security.provider.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class ClientWindow extends JFrame implements ActionListener, TCPConnectionlistener {

    private static String IP_ADDR = null;
    static {
        try {
            IP_ADDR = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    private static int PORT;
    private static int NUMBER;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static Cipher cipher;
    private static Cipher decryptcipher;
    private static KeyPairGenerator pairgen;
    private static SecureRandom random;
    private static KeyPair pair;
    private static Key publicKey;
    private static Key privateKey;
    private static String url = "jdbc:mysql://localhost:3306/keylist";
    private static String user = "root";
    private static String password = "33gjgeufq";

    public static void main(String[] args) throws Exception {
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter port: ");
        PORT = myObj.nextInt();
        System.out.println("Enter number: ");
        NUMBER = myObj.nextInt();

        cipher = Cipher.getInstance("RSA");

        //insertkeys();
        getKeys();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow();
            }
        });
    }

    private final JTextArea log = new JTextArea();
    private final JTextField fieldNickName = new JTextField("alex");
    private final JTextField fieldInput = new JTextField();
    private String number;
    private TCPConnection connection;

    private ClientWindow(){
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        log.setEditable(false);
        log.setLineWrap(true);
        add(log, BorderLayout.CENTER);
        fieldInput.addActionListener(this);
        add(fieldInput, BorderLayout.SOUTH);
        add(fieldNickName, BorderLayout.NORTH);
        setVisible(true);
        try {
            connection = new TCPConnection(this, IP_ADDR, PORT);
        } catch (IOException e) {
            printMsg("Connection exception: " + e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)  {
        byte[] bytes = new byte[256];
        String msg = fieldInput.getText();
        String string = "";
        if(msg.equals("")) return;
        fieldInput.setText(null);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            bytes = cipher.doFinal((fieldNickName.getText() + ": " + msg).getBytes("UTF-8"));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        for(byte b : bytes){
            string = string + b + " ";
        }
        connection.sendString(string);
    }

    @Override
    public void onConnetionReady(TCPConnection tcpConnection) {
        printMsg("Connection ready...");
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        if (value.startsWith("     ") && value.endsWith("/")){
            printMsg(value);
        }
        else {
            String line = Decript(value);
            printMsg(line);
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMsg("Connection closed...");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("Connection exception: " + e);
    }

    private synchronized void printMsg(String msg){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private String Decript(String value){
        try {
            decryptcipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] bytes = new byte[256];
        byte[] msg = null;
        String[] strings = value.split(" ");
        String line = "";
        int i = 0;
        for(int j =0; j < strings.length; j++){
            bytes[i] = Byte.parseByte(strings[j]);
            i++;
        }
        try {
            decryptcipher.init(Cipher.DECRYPT_MODE, privateKey);
            msg = decryptcipher.doFinal(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(byte b : msg){
            line = line + (char)b;
        }
        return line;
    }

    public static Connection getConnection(){
        try {
            Connection con = DriverManager.getConnection(url, user, password);
            return con;
        }
        catch(Exception e){System.out.println(e);}
        return null;
    }

    public static void getKeys() {
        try {
            String pub = null, priv = null;
            byte [] bytes1 = new byte[3000];
            byte [] bytes2 = new byte[3000];
            int num = NUMBER - 1;
            Connection con = getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT * FROM keyslist LIMIT "+num+",1");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                pub = rs.getString("key1");
                priv = rs.getString("key2");
            }
            String[] strings1 = pub.split(" ");
            String[] strings2 = priv.split(" ");
            int i = 0;
            for(int j =0; j < strings1.length; j++){
                bytes1[i] = Byte.parseByte(strings1[j]);
                i++;
            }
            i = 0;
            for(int j =0; j < strings2.length; j++){
                bytes2[i] = Byte.parseByte(strings2[j]);
                i++;
            }
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes1));
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes2));

        }catch (Exception e){
            System.out.println(e);
        }
    }

    public static void insertkeys(){
        try{
            for(int i = 0; i <10; i++) {
                Connection con = getConnection();
                pairgen = KeyPairGenerator.getInstance("RSA");
                pair = pairgen.generateKeyPair();
                publicKey = pair.getPublic();
                privateKey = pair.getPrivate();

                String line1 = "";
                byte[] encodedPubKey = publicKey.getEncoded();
                for (byte b : encodedPubKey) {
                    line1 = line1 + b + " ";
                }
                String line2 = "";
                byte[] encodedPrivKey = privateKey.getEncoded();
                for (byte b : encodedPrivKey) {
                    line2 = line2 + b + " ";
                }

                PreparedStatement line = con.prepareStatement("INSERT INTO keyslist (key1, key2) VALUES ('" + line1 + "','" + line2 + "')");
                line.executeUpdate();
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }
}
