package Server;

import Commons.Exceptions.*;
import Commons.UsrSignUpRMI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.json.simple.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.rmi.RemoteException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static Commons.Word_Quizzle_Consts.BufferSize;
import static Commons.Word_Quizzle_Consts.JsonFile;

public class Database implements UsrSignUpRMI {
    private ConcurrentHashMap<String, User> users; // Hashmap degli utenti, lo username vale come chiave di ricerca


    public Database() throws IOException {
        users = new ConcurrentHashMap<String, User>();
        //se il file esiste recupero gli utenti registrati
        if(Files.exists(Paths.get(JsonFile)))
            users = new Gson().fromJson(getStrJs(),new TypeToken<ConcurrentHashMap<String,User>>(){}.getType());
        else {
            Path file = Paths.get(JsonFile);
            try {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
            } catch (IOException e) {
                System.err.format("createFile error: %s%n", e);
            }
        }
    }

    @Override
    public boolean signUpUsr(String usrname, String passw) throws RemoteException, UserAlreadyExistsException {
        if(!User.isValid(usrname, passw)) throw new NullPointerException("User not valid");

        if(users.put(usrname, new User(usrname, passw)) != null) {
            throw new UserAlreadyExistsException("This username is already registered. Please choose another one.");
        }

        updateJs();
        return true;
    }

    public boolean login(String usrname, String passw, int UDPport)
            throws UserDoesNotExistsException, UserAlreadyLoggedInException, WrongPasswordException {
        if(!User.isValid(usrname, passw)) throw new NullPointerException("User not valid");
        //controllo che sia presente
        User u = users.get(usrname);
        if(u == null) throw new UserDoesNotExistsException();
        if(!u.checkPassw(passw)) throw new WrongPasswordException("Password " + passw + " is wrong");
        if(u.isOnline()) throw new UserAlreadyLoggedInException(usrname);
        u.setOnline(true);
        u.setPlaying(false);
        u.setUDPport(UDPport);
        return true;
    }

    public void logout(String username) throws UserDoesNotExistsException, UserNotOnlineException {
        if(!User.isValid(username)) throw new NullPointerException("User not valid");
        //controllo che sia presente
        User u = users.get(username);
        if(u == null) throw new UserDoesNotExistsException();
        if(u.isOnline()) u.setOnline(false);
        else throw new UserNotOnlineException();
    }

    public boolean addFriend(String user, String ufriend) throws UserAlreadyExistsException, UserDoesNotExistsException {
        if(!User.isValid(user,ufriend)) throw new NullPointerException();
        //verifico che ci siano entrambi in users
        if(users.containsKey(user) && users.containsKey(ufriend)){
            users.get(user).addFriend(ufriend);
            users.get(ufriend).addFriend(user);
            updateJs();
        }else throw new UserDoesNotExistsException();
        return true;
    }

    public JSONArray friendList(String user) throws UserDoesNotExistsException {
        if(!User.isValid(user)) throw new NullPointerException();
        if(users.containsKey(user)){
            JSONArray ja = new JSONArray();
            for (String s : users.get(user).getFriends()) ja.add(s);
            return ja;
        }else throw new UserDoesNotExistsException();
    }

    public int getPoints(String user) throws UserDoesNotExistsException {
        if(!User.isValid(user)) throw new NullPointerException();
        if(users.containsKey(user)){
            return users.get(user).getScore();
        }else throw new UserDoesNotExistsException();
    }

    public  void setPoints(String user, int points) throws UserDoesNotExistsException {
        if(!User.isValid(user)) throw new NullPointerException();
        if(users.containsKey(user)){
            users.get(user).addScore(points);
        }else throw new UserDoesNotExistsException();
        updateJs();
    }


    public JSONArray getLeaderboad(String user) throws UserDoesNotExistsException {
        if(!User.isValid(user)) throw new NullPointerException();
        if(users.containsKey(user)){
            class Element{String name; int score; Element(String n, int s){name = n; score = s;}}
            ArrayList<Element> ar = new ArrayList<>();
            ar.add(new Element(user,users.get(user).getScore()));
            for (String curr_friend : users.get(user).getFriends()) {
                ar.add(new Element(curr_friend, users.get(curr_friend).getScore()));
            }
            ar.sort((e, e1) -> e1.score - e.score);
            JSONArray jsonArray = new JSONArray();
            for(Element e : ar) {
                JSONObject obj = new JSONObject();
                obj.put("Username", e.name);
                obj.put("Score", e.score);
                jsonArray.add(obj);
            }
            return jsonArray;
        }else throw new UserDoesNotExistsException();
    }

    /**
     *
     * @param user     username
     * @param ufriend  friend username
     * @return  UDP_Port di friend se rispetta tutti i controlli
     * @throws UserDoesNotExistsException
     * @throws UserNotOnlineException
     * @throws UserBusyException
     * @throws NullPointerException
     */
    public int match_challenge(String user, String ufriend) throws UserDoesNotExistsException, UserNotOnlineException, UserBusyException{
        if(!User.isValid(user,ufriend)) throw new NullPointerException();
        User us = users.get(user);
        if(us != null && us.isOnline()) {
            User friend = users.get(ufriend);
            if (friend != null && friend.isOnline()) {
                if(us.getFriends().contains(ufriend)){ // ufriend è nella sua lista amici
                    if(!us.isPlaying() || !friend.isPlaying()) {
                        return friend.getUDPport();
                    }else throw new UserBusyException();
                }else throw new UserNotOnlineException();
            }else throw new UserNotOnlineException();
        }else throw new UserDoesNotExistsException();
    }


    public  void setPlaying(String user, boolean playing) throws UserDoesNotExistsException {
        if(!User.isValid(user)) throw new NullPointerException();
        if(users.containsKey(user)){
            users.get(user).setPlaying(playing);
        }else throw new UserDoesNotExistsException();
    }

    //carica dal file json
    public static String getStrJs()throws IOException{
        FileChannel fc = FileChannel.open(Paths.get(JsonFile), StandardOpenOption.READ);
        ByteBuffer buf = ByteBuffer.allocate(BufferSize);
        StringBuilder str = new StringBuilder();
        while(fc.read(buf) != -1){
            buf.flip();
            while (buf.hasRemaining()){
                str.append(StandardCharsets.UTF_8.decode(buf).toString());
            }
            buf.flip();
        }

        return str.toString();
    }

    //Aggiorna il file json
    public synchronized void updateJs(){
        try(FileChannel fc = FileChannel.open(Paths.get(JsonFile),StandardOpenOption.WRITE)){
            ByteBuffer bf = ByteBuffer.wrap(new Gson().toJson(users).getBytes(StandardCharsets.UTF_8));
            while(bf.hasRemaining())
                fc.write(bf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
