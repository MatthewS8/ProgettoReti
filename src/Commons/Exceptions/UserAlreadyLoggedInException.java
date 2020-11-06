package Commons.Exceptions;

public class UserAlreadyLoggedInException extends Exception {
    public UserAlreadyLoggedInException(){super();}
    public UserAlreadyLoggedInException(String s) {
        super(s);
    }
}
