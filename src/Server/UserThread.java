package Server;

import Commons.Exceptions.*;

import java.io.*;
import java.net.*;
import java.util.Random;


public class UserThread extends Thread {
    private Database db;
    private Socket socketClient;

    public UserThread(Database database, Socket client) {
        this.socketClient = client;
        this.db = database;
    }


    /*  il thread gestisce un utente
    *   attraverso delle keyword come LOGIN, ADDFRIEND etc.
    *   sempre poste come prima parola di un messaggio ben organizzato
    *   l-intero ciclo di vita del thread i.e. la gestione per tutto il tempo della connessione
    *   di un client consiste nel mettersi in ascolto con una read
    *   e tokenizzare il messaggio per inoltrare la richiesta al database condiviso da tutti i threads
     */
    @Override
    public void run() {
        String username = "";
        try (BufferedReader sockRead = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
             BufferedWriter sockWrite = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream()));) {
            String fromClient;
            String[] messageTok;

            do {
                fromClient = sockRead.readLine();
                messageTok = fromClient.split("\\s+"); //token ad ogni spazio
                if (messageTok[0].equals("LOGIN")) {
                    System.out.println("Message from client " + fromClient);
                    //RICHIESTA DI LOGIN: LOGIN <USER> <PASS> <UDP port>
                    try {
                        if (db.login(messageTok[1], messageTok[2], Integer.parseInt(messageTok[3]))) {
                            //login ok
                            sockWrite.write("5 login ok");
                            sockWrite.newLine();
                            sockWrite.flush();
                            username = messageTok[1];
                        } else {

                        }
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username or password not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (WrongPasswordException e) {
                        sockWrite.write("12 Wrong Password");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserAlreadyLoggedInException e) {
                        sockWrite.write("13 Your account has already logged in");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("ADDFRIEND")) {
                    System.out.println("Message from client " + fromClient);
                    //RICHIESTA DI AGGIUNTA AMICO: ADDFRIEND <User> <Friend_Name>
                    try {
                        if (db.addFriend(messageTok[1], messageTok[2])) {
                            sockWrite.write("6 Friend added");
                            sockWrite.newLine();
                            sockWrite.flush();
                        }
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserAlreadyExistsException e) {
                        sockWrite.write("14 You are already friends");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("GETPOINTS")) {
                    System.out.println("Message from client " + fromClient);
                    //Richiesta punti: GETPOINTS <User>
                    try {
                        sockWrite.write("POINTS " + db.getPoints(messageTok[1]));
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("FRIENDSLIST")) {
                    System.out.println("Message from client " + fromClient);
                    //FRIENDSLIST <user>
                    try {
                        sockWrite.write(db.friendList(messageTok[1]).toJSONString());
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("LEADERBOARD")) {
                    System.out.println("Message from client " + fromClient);
                    //LEADERBOARD <user>
                    try {
                        sockWrite.write(db.getLeaderboad(messageTok[1]).toJSONString());
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("PLAY")) {
                    System.out.println("Message from client " + fromClient);
                    //PLAY <User> <Friend_Name>
                    try {
                        int udpPort = db.match_challenge(messageTok[1], messageTok[2]);
                        try {
                            DatagramSocket datagramSocket = new DatagramSocket();
                            StringBuilder sb = new StringBuilder();
                            int matchPort = (new Random().nextInt(100)) + 60152;
                            //todo check this
                            byte[] buffer = sb.append("Match ").append(messageTok[1]).append(" ").append(matchPort).append("\n").toString().getBytes();
                            datagramSocket.send(new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), udpPort));
                            datagramSocket.setSoTimeout(1000 * 10);
                            byte[] inBuffer = new byte[1024];
                            DatagramPacket inPacket = new DatagramPacket(inBuffer, 1024);
                            datagramSocket.receive(inPacket);
                            String[] messReceived = new String(inPacket.getData()).split("\\s+");
                            if (messReceived[0].equals("ACCEPTED")) {
                                //ACCEPTED <frienduser> <user>
                                sockWrite.write("" + messReceived[1] + " " + matchPort);
                                sockWrite.newLine();
                                sockWrite.flush();
                                Match match = new Match(db, messReceived[2], messReceived[1], matchPort);
                                //li metto in modalit' gioco
                                db.setPlaying(messReceived[2], true);
                                db.setPlaying(messReceived[1], true);
                                // questa si blocchera poi sulla read in caso di chiusura
                                // da parte del client eseguo il logout
                                match.start();

                            } else {// offerta rifiutata
                                sockWrite.write("CONNECTION REFUSED");
                                sockWrite.newLine();
                                sockWrite.flush();
                            }
                        } catch (SocketException e) {
                            sockWrite.write("PROBLEM OCCURRED");
                            sockWrite.newLine();
                            sockWrite.flush();
                        } catch (SocketTimeoutException e) {
                            //richiesta scaduta
                            sockWrite.write("CONNECTION TIME OUT");
                            sockWrite.newLine();
                            sockWrite.flush();

                        }
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserNotOnlineException e) {
                        sockWrite.write("15 User not online");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserBusyException e) {
                        sockWrite.write("16 User is playing another match");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                } else if (messageTok[0].equals("LOGOUT")) {
                    //LOGOUT <username>
                    try {
                        db.logout(messageTok[1]);
                        sockWrite.write("6 Logout ok");
                        sockWrite.newLine();
                        sockWrite.flush();
                        socketClient.close();
                    } catch (NullPointerException e) {
                        sockWrite.write("10 Username not valid");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserDoesNotExistsException e) {
                        sockWrite.write("11 User not registered");
                        sockWrite.newLine();
                        sockWrite.flush();
                    } catch (UserNotOnlineException e) {
                        sockWrite.write("15 User not online");
                        sockWrite.newLine();
                        sockWrite.flush();
                    }
                }

            } while (!messageTok[0].equals("LOGOUT"));
        }catch(SocketException e ){
            System.out.println("User has closed connection");
            try {
                db.logout(username);
            } catch (UserDoesNotExistsException | UserNotOnlineException e1) {
                e1.printStackTrace();}
        }catch (IOException e) {
            System.out.println("Something went wrong");
        }
    }
}