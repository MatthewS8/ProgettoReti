package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;


public class MainController {

    private Client client;

    @FXML
    Button NGBut;

    @FXML
    Button friendsbut;

    @FXML
    Button leaderboardBut;

    @FXML
    ListView<String> listView;

    @FXML
    Button logOutBut;

    @FXML
    Button addBut;

    @FXML
    TextField addFText;

    @FXML
    Label errLa;

    @FXML
    Label usernameLab;

    @FXML
    Label scoreLabel;


    @FXML
    private Label challengeLab;

    @FXML
    private ChoiceBox<String> choiceBox;

    @FXML
    private Button sendBut;



    @FXML
    public void NewGame(ActionEvent e){
        usernameLab.setVisible(false);
        scoreLabel.setVisible(false);

        addFText.setVisible(false);
        addBut.setVisible(false);
        listView.setVisible(false);

        errLa.setVisible(false);

        challengeLab.setVisible(true);

        ArrayList<String> friendsList = client.getFriendListHandler();

        choiceBox.getItems().clear();
        if(friendsList != null && friendsList.size()>0) {
            for (String s : friendsList)
                choiceBox.getItems().add(s);
            choiceBox.setValue(friendsList.get(0));
        }

        choiceBox.setVisible(true);
        sendBut.setVisible(true);

    }


    @FXML
    public void sendRequest(ActionEvent e){
        errLa.setText("Invite sent. Waiting for a response");
        errLa.setVisible(true);

        String choice = choiceBox.getValue();

        sendBut.setDisable(true);
        if(choice != null && client.newGameHandler(choice)){
            //sfida accettata
            sendBut.setDisable(false);
            client.launchGame();
        }
        sendBut.setDisable(false);
    }




    @FXML
    public void addFriend(ActionEvent e){
        String s = client.addFriendHandler(addFText.getText());
        errLa.setVisible(true);
        if(s == null){
            errLa.setText("An error occured. Please try later");
        }else{
            errLa.setText(s);
        }
    }


    @FXML
    public void showFriends(ActionEvent e){

        choiceBox.setVisible(false);
        sendBut.setVisible(false);
        challengeLab.setVisible(false);

        usernameLab.setVisible(false);
        scoreLabel.setVisible(false);

        addFText.setVisible(true);
        addBut.setVisible(true);

        ArrayList<String> friendsList = client.getFriendListHandler();
        listView.getItems().clear();
        if(friendsList != null) {
            for(String s : friendsList)
                listView.getItems().add(s);
            listView.setVisible(true);
        }else{
            usernameLab.setVisible(true);
            scoreLabel.setVisible(true);
        }
        listView.setVisible(true);
    }


    public void showScore(){

        usernameLab.setText(client.username);
        usernameLab.setTextAlignment(TextAlignment.CENTER);
        usernameLab.setAlignment(Pos.CENTER);

        //potrebbe mostrare un risultato di errore
        scoreLabel.setText(client.getPointsHandler());
        scoreLabel.setTextAlignment(TextAlignment.CENTER);
        scoreLabel.setAlignment(Pos.CENTER);

        usernameLab.setVisible(true);
        scoreLabel.setVisible(true);
    }

    @FXML
    public void showLeaderboard(ActionEvent e){

        choiceBox.setVisible(false);
        sendBut.setVisible(false);
        challengeLab.setVisible(false);

        usernameLab.setVisible(false);
        scoreLabel.setVisible(false);

        addFText.setVisible(false);
        addBut.setVisible(false);

        ArrayList<String> leaderList = client.getLeaderboardHandler();
        listView.getItems().clear();
        if(leaderList != null) {
            listView.getItems().addAll(leaderList);
            listView.setVisible(true);
        }
    }

    @FXML
    public void logOut(ActionEvent e){
        if(client.logOutHandler()){
            client.launchLogIn();
        }

    }

    @FXML
    public void setErrorLabel(String message){
        errLa.setText(message);
        errLa.setVisible(true);
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
