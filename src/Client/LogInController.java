package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class LogInController {
    Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    Label errorLab;

    @FXML
    Button signUpBut;

    @FXML
    Button logInBut;

    @FXML
    TextField usernameText;

    @FXML
    PasswordField passText;

    @FXML
    public void signUp(ActionEvent e){
        String user = usernameText.getText();
        String pass = passText.getText();

        if(client.signUpHandler(user,pass)){
            client.launchMain();
        }else{
            passText.clear();
        }
    }

    @FXML
    public void logIn(ActionEvent e){
        String user = usernameText.getText();
        String pass = passText.getText();

        if(client.logInHandler(user,pass)){
            client.launchMain();
        }else{
            passText.clear();
        }
    }


    public void setErrorLabel(String message){
        errorLab.setText(message);
        errorLab.setVisible(true);
    }


}
