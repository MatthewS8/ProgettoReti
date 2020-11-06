package Client;

import static Commons.Word_Quizzle_Consts.*;
import Commons.Exceptions.UserAlreadyExistsException;
import Commons.UsrSignUpRMI;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.ArrayList;


public class Client extends Application {
    private static FXMLLoader loader;
    private static Stage stage;
    private Parent root;
    private Socket clientSock;
    private int udpPort;
    UDPListener listener;
    BufferedWriter writer_Tcp;
    BufferedReader reader_Tcp;
    String username;

    LogInController logController;
    MainController mainController;


    private int gamePort;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setOnCloseRequest(e -> {
            if(username != null){
                logOutHandler();
            }
            stage.close();
            System.exit(0);
        });

        stage.setTitle("WORD QUIZZLE");
        stage.setResizable(false);
        launchLogIn();
        stage.show();
    }

    public void launchLogIn(){
        try{
            loader = new FXMLLoader(getClass().getClassLoader().getResource("./Client/pageviews/LogInPage.fxml"));
            root = loader.load();
            logController = loader.getController();
            logController.setClient(this);
            Scene scene = root.getScene();
            if(scene == null){
                stage.setScene(new Scene(root));
            }else{
                stage.getScene().setRoot(root);
            }
            stage.setResizable(false);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void launchMain() {
        try {
            loader = new FXMLLoader(getClass().getClassLoader().getResource("./Client/pageviews/MainPage.fxml"));
            root = loader.load();
            mainController = loader.getController();
            mainController.setClient(this);
            mainController.showScore(); //todo check this out
            Scene scene = root.getScene();
            if(scene == null){
                stage.setScene(new Scene(root));
            }else{
                stage.getScene().setRoot(root);
            }
            stage.setResizable(false);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void launchGame(){
        SocketChannel gameSocket = null;
        try {
            gameSocket = SocketChannel.open();
            gameSocket.connect(new InetSocketAddress("localhost", gamePort));
        } catch (IOException e) {
            //something went wrong
            Alert al = new Alert(Alert.AlertType.ERROR,"An error occured connecting to host", ButtonType.OK);
            al.setTitle("Impossibile to launch the game");
            al.showAndWait().filter(resp -> resp == ButtonType.OK).ifPresent(resp ->{
                al.close();
            });
            launchMain();
        }

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("./Client/pageviews/GamePage.fxml"));
            Parent root = loader.load();

            GameController gameController = loader.getController();
            gameController.initializeConnection(this, gameSocket);

            stage.setScene(new Scene(root));
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean signUpHandler(String username, String passw){ //todo check this
        try {
            UsrSignUpRMI serverObj;
            Registry r = LocateRegistry.getRegistry(SRMIport);
            Remote remoteObj = r.lookup(RMI_NS);
            serverObj = (UsrSignUpRMI) remoteObj;

            if (serverObj.signUpUsr(username, passw)) {
                //registrazione avvenuta con successo
                //eseguo il log in Automatico
                return logInHandler(username, passw);
            }
        }catch (RemoteException | NotBoundException e){
            logController.setErrorLabel("Connection error. PLease try again");
            e.printStackTrace();
        } catch ( UserAlreadyExistsException | NullPointerException e) {
            // la registrazione è fallita
            logController.setErrorLabel(e.getMessage());
            //e.printStackTrace();
        }
        return false;
    }

    public boolean logInHandler (String username, String passw){
        try {
            this.username = username;
            clientSock = new Socket("localhost", STCPport);
            udpPort = (int) (Math.random() *1000 + 48152);
            reader_Tcp = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
            writer_Tcp = new BufferedWriter(new OutputStreamWriter(clientSock.getOutputStream()));
            //RICHIESTA DI LOGIN: LOGIN <USER> <PASS> <UDP port>
            writer_Tcp.write("LOGIN " + username + " " + passw + " " + udpPort);
            writer_Tcp.newLine();
            writer_Tcp.flush();

            //dare il messaggio di errore
            String ans = reader_Tcp.readLine();
            if(ans.equals("5 login ok")){
                DatagramSocket udp = new DatagramSocket(udpPort);
                listener = new UDPListener(username, udp);
                listener.setClient(this);
                Thread t = new Thread(listener);
                t.start();
                return true;
            }else {
                logController.setErrorLabel(ans);
            }

        } catch (IOException e) {
            logController.setErrorLabel("Connection error. Please try again");
            e.printStackTrace();
        }
        return false;

    }

    public String addFriendHandler(String friend){
        //RICHIESTA DI AGGIUNTA AMICO: ADDFRIEND <User> <Friend_Name>
        try {
            writer_Tcp.write("ADDFRIEND " + username + " " + friend);
            writer_Tcp.newLine();
            writer_Tcp.flush();
            String ans = reader_Tcp.readLine();
            String[] tokm = ans.split("\\s+");
            StringBuilder ret = new StringBuilder();
            for(int i =1; i < tokm.length; i++)
                ret.append(tokm[i]);
            return ret.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public String getPointsHandler(){
        //Richiesta punti: GETPOINTS <User>
        try {
            writer_Tcp.write("GETPOINTS " + username );
            writer_Tcp.newLine();
            writer_Tcp.flush();
            String ans = reader_Tcp.readLine();
            if(ans.contains("POINTS"))
                return ans;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "POINTS: 0";
    }

    public ArrayList<String> getFriendListHandler(){
        //FRIENDSLIST <usr>
        try{
            writer_Tcp.write("FRIENDSLIST " + username );
            writer_Tcp.newLine();
            writer_Tcp.flush();
            String ans = reader_Tcp.readLine();
            if(ans.equals("10 Username not valid") || ans.equals("11 User not registered")){
                mainController.setErrorLabel(ans);
            }else {
                JSONArray jsonArray = (JSONArray) new JSONParser().parse(ans);
                ArrayList<String> friendsList = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++)
                    friendsList.add(jsonArray.get(i).toString());
                return friendsList;
            }
        } catch (IOException | ParseException e) {
            mainController.setErrorLabel("Connection error. Please try again.");
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getLeaderboardHandler(){
        //LEADERBOARD <usr>
        try{
            writer_Tcp.write("LEADERBOARD " + username);
            writer_Tcp.newLine();
            writer_Tcp.flush();
            String ans = reader_Tcp.readLine();
            if(ans.equals("10 Username not valid") || ans.equals("11 User not registered")){
                mainController.setErrorLabel(ans);
            }else {
                JSONArray jsonArray = (JSONArray) new JSONParser().parse(ans);
                ArrayList<String> leadeboardlist = new ArrayList<>();

                for(int i = 0; i<jsonArray.size(); i++) {
                    JSONObject usr = (JSONObject) jsonArray.get(i);
                    leadeboardlist.add(usr.get("Username") + "\t\t\t\t\tScore:" + usr.get("Score"));
                }
                return leadeboardlist;
            }
        } catch (IOException | ParseException e) {
            mainController.setErrorLabel("Connection error. Please try again.");
            e.printStackTrace();
        }
        return null;
    }

    public boolean newGameHandler(String friend){
        //PLAY <User> <Friend_Name>
        try{
            writer_Tcp.write("PLAY "  + username + " " + friend );
            writer_Tcp.newLine();
            writer_Tcp.flush();
            String ans = reader_Tcp.readLine();
            String [] tok = ans.split("\\s+");
            if(tok[0].equals(friend)){
                gamePort = Integer.parseInt(tok[1]);
                return true;
            }else if(ans.equals("CONNECTION REFUSED")){
                mainController.setErrorLabel(friend + " declined your invite");
            }else if(ans.equals("15 User not online") || ans.equals("16 User is playing another match")){
                mainController.setErrorLabel(friend + " is busy or not online");
            }else
                mainController.setErrorLabel("Connection error. Please try again.");
        }catch(IOException e){
            mainController.setErrorLabel("Connection error. Please try again.");
            e.printStackTrace();
        }
        return false;
    }


    public boolean logOutHandler (){
        //LOGOUT <username>
        try {
            writer_Tcp.write("LOGOUT " + username);
            writer_Tcp.newLine();
            writer_Tcp.flush();
            if(listener.isAlive()) {
                listener.setFinished(true);
            }
            String ans = reader_Tcp.readLine();
            if(ans.equals("6 Logout ok")){
                clientSock.close();
                username = null;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void setGamePort(int port){
        this.gamePort = port;
    }

}
