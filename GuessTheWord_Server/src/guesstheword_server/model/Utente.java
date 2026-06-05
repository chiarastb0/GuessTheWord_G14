/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.model;

/**
 *
 * @author angel
 */
public class Utente {
    private long idUtente;
    private String username;
    private String password;
    private String ruolo;
    
    public Utente(String username, String password, String ruolo) {
        this.username=username;
        this.password=password;
        this.ruolo=ruolo;
    }
    
    public Utente(long idUtente, String username, String password, String ruolo) {
        this.idUtente=idUtente;
        this.username=username;
        this.password=password;
        this.ruolo=ruolo;
    }

    public long getIdUtente() {
        return idUtente;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setIdUtente(long idUtente) {
        this.idUtente = idUtente;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }
    
    @Override
    public String toString() {
        return "Utente{" + "idUtente=" + idUtente + ", username=" + username + ", password=" + password + ", ruolo=" + ruolo + '}';
    }
    
}

