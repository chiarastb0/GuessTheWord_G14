/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.db;

import java.sql.SQLException;

/**
 *
 * @author admin
 */
public class DBException extends RuntimeException {

    public DBException(String msg) {
        super(msg);
    }
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
