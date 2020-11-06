package Commons;

public class Word_Quizzle_Consts {

    /**
     * Classe di constanti
     */

    //server
    public static final int STCPport = 60152;
    public static final int SRMIport = 55010;
    public static final String RMI_NS = "UsrSignUp";
    public static final int BufferSize = 1024*1024; //1 MB

    public static final String JsonFile = "./out/production/WordQuizzle/Server/Files/database.json";
    public static final String Dizionario = "./out/production/WordQuizzle/Server/Files/dizionario.txt";
    public static final int Diz_lines = 1160;

    //sfida
    public static final int k_parole = 10;
    public static final int risp_corretta = 2;
    public static final int risp_errata = 1;
    public static final int bonus_vittoria = 3;
    public static final int challenge_time = 1000 * 60 * 2; // 1 minutes

}
