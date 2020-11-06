package Server;

import Commons.UsrSignUpRMI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

import static Commons.Word_Quizzle_Consts.*;

public class Server {
    public static void main(String[] args){
        Executor exe;

        try {
            Database data = new Database();

            //RMI
            UsrSignUpRMI stub = (UsrSignUpRMI) UnicastRemoteObject.exportObject(data,0);
            LocateRegistry.createRegistry(SRMIport);
            Registry reg = LocateRegistry.getRegistry(SRMIport);
            reg.rebind(RMI_NS,stub);

            exe = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            System.out.printf("Server started on port %d\nSignUp service started on RMI port %d",STCPport,SRMIport);
            ServerSocket ssocket = new ServerSocket(STCPport);
            while(true){
                Socket socket = ssocket.accept();
                UserThread ut = new UserThread(data, socket);
                exe.execute(ut);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
