package Client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.net.*;
import java.util.Optional;

public class UDPListener extends Thread{
    private static DatagramSocket UDPSocket;
    private boolean finished = false;
    public String username;
    Client client;
    private int port_connection;

    public UDPListener(String username, DatagramSocket ds){
        UDPSocket = ds;
        this.username = username;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket msgReceived = new DatagramPacket(buffer, buffer.length);
        while(!finished){
            try {
                UDPSocket.receive(msgReceived);
                port_connection = msgReceived.getPort();
            } catch (IOException e) {
                System.exit(-1);
            }
            //Match <user_sfidante> <TCP_Port>
            String s = new String(msgReceived.getData());
            String[] tok = s.split("\\s+");

            if(tok[0].equals("Match")){
                int TcpPort = Integer.parseInt(tok[2]);
                Platform.runLater(() -> {
                    Alert notify = new Alert(Alert.AlertType.CONFIRMATION,
                            tok[1] + " asked you to join a new match. Do you want to play?",ButtonType.YES,ButtonType.NO);
                    notify.setTitle("Challenge request");
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(30000);
                            if (notify.isShowing()) {
                                Platform.runLater(notify::close);
                            }
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                    Optional<ButtonType> result = notify.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        try {
                            acceptChallenge(port_connection, TcpPort, tok[1]);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }else if(result.isPresent() && result.get() == ButtonType.NO){
                        String deny = "DENY";
                        try {
                            DatagramPacket pa = new DatagramPacket(deny.getBytes(),deny.length(),InetAddress.getByName("localhost"), port_connection);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                });

            }
        }
        UDPSocket.close();
    }

    public void setFinished(boolean state){
        finished = state;
    }

    public void acceptChallenge(int connport, int gameport, String friend) throws UnknownHostException {
        String tmp = "ACCEPTED " + username + " " + friend + "\n";
        byte[] buffer=tmp.getBytes();
        DatagramPacket pckt = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), connport);
        try {
            UDPSocket.send(pckt);
            client.setGamePort(gameport);
            client.launchGame();
        } catch (IOException |NullPointerException e) {
            e.printStackTrace();
        }
    }

}
