package Commons;

import Commons.Exceptions.UserAlreadyExistsException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia registrazione utente via RMI
 */
public interface UsrSignUpRMI extends Remote {

    public boolean signUpUsr(String usrname, String passw) throws RemoteException, UserAlreadyExistsException;
}
