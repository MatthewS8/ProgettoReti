package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static Commons.Word_Quizzle_Consts.k_parole;

public class GameController {
    private Client client;
    private int curParola = 1;
    private SocketChannel socketChannel;

    ByteBuffer rBuffer, wBuffer;

    @FXML
    private TextField parolaText;

    @FXML
    private TextField wordText;

    @FXML
    private Button nextBut;

    @FXML
    private Label progress;
    @FXML
    private TextArea textArea;

    public void initializeConnection(Client client, SocketChannel socketChannel){
        rBuffer = ByteBuffer.allocate(1024);
        wBuffer = ByteBuffer.allocate(1024);
        this.client = client;
        this.socketChannel = socketChannel;
        readWord();
    }

    @FXML
    void next(ActionEvent event) {
        if(!nextBut.getText().equals("Home")) {
            try {
                if (curParola++ == 1) {
                    String s = client.username + "-" + wordText.getText();
                    wBuffer = ByteBuffer.wrap(s.getBytes());
                } else {
                    wBuffer = ByteBuffer.wrap((wordText.getText()).getBytes());
                }
                socketChannel.write(wBuffer);
                wordText.clear();
                wBuffer.clear();
                readWord();

            } catch (IOException e) {
                //provo a leggere i risultati
                readWord();
            }
        }else{
            client.launchMain();
        }
    }

    @FXML
    private void readWord() {
        int bytesread = 0;
        StringBuilder s = new StringBuilder();
        do {
            try {
                bytesread = socketChannel.read(rBuffer);
                rBuffer.flip();
                s.append(StandardCharsets.UTF_8.decode(rBuffer).toString());
                rBuffer.flip();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }while(bytesread == 1024);

        rBuffer.clear();

        if(s.toString().contains("ENDGAME")){
            textArea.setText(s.toString());
            textArea.setVisible(true);
            wordText.setVisible(false);
            nextBut.setText("Home");
            nextBut.setDisable(false);
        }else{
            progress.setText(curParola + "/" + k_parole);
            parolaText.setText(s.toString());
        }
    }



}
