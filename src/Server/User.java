package Server;

import Commons.Exceptions.UserAlreadyExistsException;

import java.util.*;


public class User {
    private final String uname;
    private final String passw;
    private int score = 0;
    private Set<String> friends;
    private transient boolean online = false;
    private transient boolean playing = false;
    private transient int UDPport;

    public User(String usr, String password){
        this.uname = usr;
        this.passw = password;
        this.friends = new HashSet<>();
    }

    public String getUsername() {
        return uname;
    }

    public boolean checkPassw(String passw) {
        return this.passw.equals(passw);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points){
        score += points;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public void addFriend(String fname) throws UserAlreadyExistsException {
        if(friends.contains(fname)) throw new UserAlreadyExistsException("Friend already present");
        friends.add(fname);
    }
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean state){
        this.online = state;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public int getUDPport() {
        return UDPport;
    }

    public void setUDPport(int UDPport) {
        this.UDPport = UDPport;
    }

    public static boolean isValid(String s){
        return s != null && !s.equals("") && !s.equals(" ");
    }

    public static boolean isValid(String usr, String passw){
        return isValid(usr) && isValid(passw);
    }

    @Override
    public boolean equals(Object obj){
        return this.uname.equals(((User) obj).getUsername());
    }
}

