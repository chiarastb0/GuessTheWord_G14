/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.model;

/**
 *
 * @author angel
 */
public class Partita {
   private int idPartita;
   private String dataOra;
   private String parolaNascosta;
   
   public Partita(){}
   
   public Partita(int idPartita, String dataOra, String parolaNascosta){
       this.idPartita = idPartita;
       this.dataOra = dataOra;
       this.parolaNascosta = parolaNascosta;
   }
   
   public Partita(String dataOra, String parolaNascosta){
       this.dataOra = dataOra;
       this.parolaNascosta = parolaNascosta;
   }

    public int getIdPartita() {
        return idPartita;
    }

    public void setIdPartita(int id_partita) {
        this.idPartita = id_partita;
    }

    public String getDataOra() {
        return dataOra;
    }

    public void setDataOra(String dataOra) {
        this.dataOra = dataOra;
    }

    public String getParolaNascosta() {
        return parolaNascosta;
    }

    public void setParolaNascosta(String parolaNascosta) {
        this.parolaNascosta = parolaNascosta;
    }
   
    @Override
    public String toString() {
        return "Partita{" + "id=" + idPartita + ", data='" + dataOra + "', parola='" + parolaNascosta + "'}";
    }
   
}

