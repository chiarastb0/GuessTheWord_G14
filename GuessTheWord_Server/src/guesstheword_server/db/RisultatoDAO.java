/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.db;

/**
 *
 * @author letiz
 */

import java.sql.*;
import java.util.*;
import guesstheword_server.model.Risultato;

public class RisultatoDAO implements DAO<Risultato> {

    @Override
    public Optional<Risultato> selectById(long id) {
        Optional<Risultato> result = Optional.empty();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM RISULTATO WHERE id_risultato = " + id)) {

            Risultato r = null;

            if (rs.next()) {
                int idRisultato = rs.getInt("id_risultato");
                int idPartita = rs.getInt("id_partita");
                int idUtente = rs.getInt("id_utente");
                String esito = rs.getString("esito");
                int tempo = rs.getInt("tempo_risposta_ms");

                r = new Risultato(idRisultato, idPartita, idUtente, esito, tempo);
            }

            result = Optional.ofNullable(r);

        } catch (SQLException e) {
            throw new DBException("Errore selectById Risultato", e);
        }

        return result;
    }

    @Override
    public List<Risultato> selectAll() {
        List<Risultato> lista = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM RISULTATO")) {

            while (rs.next()) {
                Risultato r = new Risultato(
                        rs.getInt("id_risultato"),
                        rs.getInt("id_partita"),
                        rs.getInt("id_utente"),
                        rs.getString("esito"),
                        rs.getInt("tempo_risposta_ms")
                );

                lista.add(r);
            }

        } catch (SQLException e) {
            throw new DBException("Errore selectAll Risultato", e);
        }

        return lista;
    }

    @Override
    public void insert(Risultato r) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "INSERT INTO RISULTATO (id_partita, id_utente, esito, tempo_risposta_ms) VALUES ("
                    + r.getIdPartita() + ", "
                    + r.getIdUtente() + ", '"
                    + r.getEsito() + "', "
                    + r.getTempoRispostaMs()
                    + ")";

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new DBException("Errore insert Risultato", e);
        }
    }

    @Override
    public void update(Risultato r) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "UPDATE RISULTATO SET "
                    + "id_partita = " + r.getIdPartita() + ", "
                    + "id_utente = " + r.getIdUtente() + ", "
                    + "esito = '" + r.getEsito() + "', "
                    + "tempo_risposta_ms = " + r.getTempoRispostaMs()
                    + " WHERE id_risultato = " + r.getIdRisultato();

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new DBException("Errore update Risultato", e);
        }
    }

    @Override
    public void delete(Risultato r) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "DELETE FROM RISULTATO WHERE id_risultato = " + r.getIdRisultato();

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new DBException("Errore delete Risultato", e);
        }
    }

    public int getNumeroVittorie(int idUtente) {
        int count = 0;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM RISULTATO WHERE id_utente = " + idUtente
                             + " AND esito = 'VITTORIA'")) {

            if (rs.next()) {
                count = rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new DBException("Errore conteggio vittorie", e);
        }

        return count;
    }
}
