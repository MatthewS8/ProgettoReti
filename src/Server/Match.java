package Server;

import Commons.Exceptions.UserDoesNotExistsException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static Commons.Word_Quizzle_Consts.*;

public class Match extends Thread{
    private Database data;
    private String user1, user2;
    private int port;
    private ArrayList<String> parole;  //list di parole scelte a caso dal dizionario [ita]
    private ArrayList<String>  words;  //list di parole tradotte [eng]

    private boolean end_game = false;

    public Match(Database db, String username, String friendU, int port){
        data = db;
        user1 = username;
        user2 = friendU;
        this.port = port;
        parole = new ArrayList<>(k_parole);
        words = new ArrayList<>(k_parole);
    }


    public void run(){
        int endkey = 0;
        ArrayList<GameStats> gamers = new ArrayList<GameStats>(2);
        gamers.add(new GameStats());
        gamers.add(new GameStats());
        ServerSocketChannel ssc;
        Selector selector = null;

        //scegliamo le k parole dal dizionario
        for (int i=0; i < k_parole; i++) {
            try {
                String s = Files.readAllLines(Paths.get(Dizionario)).get((int) (Math.random() * Diz_lines));
                if(!parole.contains(s))
                    parole.add(s);
                else i--;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            int accepted_user = 0;

            while (!end_game || endkey != 2) {
                try {
                    selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        System.out.println("A key is acceptable");
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_WRITE, gamers.get(accepted_user));
                        gamers.get(accepted_user++).sock = client;

                        //accetto prima entrambi gli utenti poi avvio il timer
                        //mandando la prima parola a entrambi
                        if (accepted_user == 2) {
                            //entrambi sono connessi
                            //traduco le parole, imposto il timer
                            translateWords();
                            Timer t = new Timer();
                            t.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    end_game = true;
                                }
                            }, challenge_time);
                        }

                    } else if (accepted_user == 2) {

                        //entrambi sono connessi posso cominciare la sfida
                        if (key.isWritable()) {
                            //devo scrivere una parola
                            System.out.println("A key is writable");
                            SocketChannel client = (SocketChannel) key.channel();
                            GameStats gs = (GameStats) key.attachment();

                            //se non è finito il tempo e se non ho finito le parole
                            if (!end_game && gs.curr_word < k_parole) {
                                String parola = parole.get(gs.curr_word);
                                ByteBuffer to_write = ByteBuffer.wrap(parola.getBytes());
                                client.write(to_write);
                                //ho scritto tutta la parola -- attendo la risposta in lettura
                                gs.sock.register(selector, SelectionKey.OP_READ, gs);
                            } else {
                                //il client ha finito
                                sendResult(gs, client);

                                endkey++;
                                if (endkey == 2) end_game = true;
                                client.close();
                            }
                        } else if (key.isReadable()) {
                            //DEVO LEGGERE UNA RISPOSTA
                            System.out.println("A key is readable");
                            SocketChannel client = (SocketChannel) key.channel();
                            GameStats gs = (GameStats) key.attachment();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            String answer = "";
                            int read = client.read(buffer);
                            if (end_game) {
                                //ho consumato il buffer di dati su cui si era sospeso il client e
                                //imposto l-operazione di scrittura per mandare il risultato al prossimo ciclo
                                gs.sock.register(selector, SelectionKey.OP_WRITE, gs);
                            } else {
                                buffer.flip();
                                if (read == 1024) {
                                    //se leggo tutto il buffer potrebbe esserci altro da leggere

                                    if (gs.user.equals("")) {
                                        //prima lettura contiene username
                                        //<username>-<risposta>
                                        String risposta = StandardCharsets.UTF_8.decode(buffer).toString();
                                        String[] token = risposta.split("-");
                                        gs.user = token[0];
                                        gs.partial_read = token[1];
                                    } else {
                                        //attendo la lettura successiva
                                        if (gs.partial_read.equals("")) {
                                            gs.partial_read = StandardCharsets.UTF_8.decode(buffer).toString();
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            gs.partial_read = sb.append(gs.partial_read).append(StandardCharsets.UTF_8.decode(buffer).toString()).toString();
                                        }
                                    }
                                } else if (read == -1) {
                                    //client disconesso
                                    key.cancel();
                                    if (++endkey == 2) end_game = true;

                                } else if (read < 1024) {
                                    if (gs.user.equals("")) {
                                        //prima lettura contiene username
                                        String risposta = StandardCharsets.UTF_8.decode(buffer).toString();
                                        String[] token = risposta.split("-");
                                        gs.user = token[0];
                                        if (!gs.partial_read.equals("")) {
                                            answer = gs.partial_read + token[1];
                                            gs.partial_read = "";
                                        } else answer = token[1];
                                    } else {
                                        if (!gs.partial_read.equals("")) {
                                            answer = gs.partial_read + StandardCharsets.UTF_8.decode(buffer).toString();
                                            gs.partial_read = "";
                                        } else answer = StandardCharsets.UTF_8.decode(buffer).toString();
                                    }
                                    if (answer.toLowerCase().equals(words.get(gs.curr_word).toLowerCase())) {
                                        // traduzione corretta
                                        gs.paole_corrette++;
                                    } else gs.parole_errate++;
                                    gs.curr_word++;
                                    gs.sock.register(selector, SelectionKey.OP_WRITE, gs);
                                }
                            }
                        }
                    }

                }
            }
        } catch (IOException e) {
            System.out.println("Connection problem");
            e.printStackTrace();
        }

        try{

        //calcolo chi ha vinto e aggiorno i punti sul database
        if(gamers.get(0).totalpoints > gamers.get(1).totalpoints) {
            gamers.get(0).totalpoints += bonus_vittoria;
        }else if(gamers.get(0).totalpoints < gamers.get(1).totalpoints){
            gamers.get(1).totalpoints += bonus_vittoria;
        }

            data.setPoints(gamers.get(0).user,gamers.get(0).totalpoints);
            data.setPoints(gamers.get(1).user, gamers.get(1).totalpoints);
        } catch (UserDoesNotExistsException e) {
            System.out.println(e.getMessage());
        }
        try {
            if(selector != null)
                selector.close();
            gamers.get(0).sock.close();
            gamers.get(1).sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void translateWords() throws IOException {
        //traduzione k parole
        for (int i = 0; i < parole.size(); i++) {
            URL url = new URL("https://api.mymemory.translated.net/get?q=" + parole.get(i) + "&langpair=it|en");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200){
                throw new RuntimeException("Failed, error code " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output = br.readLine();
            try {
                JSONObject ans = (JSONObject) ((JSONObject)(new JSONParser().parse(output))).get("responseData");
                words.add(i, ans.get("translatedText").toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            conn.disconnect();
        }
    }

    private void totalPoints(GameStats g){
        g.totalpoints = (g.paole_corrette * risp_corretta) - (g.parole_errate * risp_errata);
    }

    private void sendResult(GameStats g, SocketChannel sock){
        totalPoints(g);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder().append("ENDGAME ").append(g.user).append(" scored ").append(g.totalpoints);
        buffer = ByteBuffer.wrap(sb.toString().getBytes());
        try {
            sock.write(buffer);
        }catch(IOException e){
            System.out.println(g.user + " is not connected");
        }
        try {
            data.setPlaying(g.user, false);
        } catch (UserDoesNotExistsException e) {
            e.printStackTrace();
        }
    }
}



class GameStats{
    int curr_word = 0;
    int paole_corrette = 0;
    int parole_errate = 0;
    int totalpoints = 0;
    String partial_read = "";
    String user = "";
    SocketChannel sock;


}