import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DaoFactory { // creates, manages, and makes sure there is just one of each DAO
    static Connection connect = null;
    static Statement statement = null;
    static PreparedStatement prep = null;
    static ResultSet rs = null;
    static Scanner scan = null;
    
    private static Connection getConnection(){
        if(connect == null) {
            // try {
            //     ResourceBundle reBundle = ResourceBundle.getBundle(
            //         "src/CathyNguyenProject0/dbConfig" // unused if commented out
            //     ); // At this point,what's the point?
            //     String dbString = reBundle.getString("DatabaseLocation");
            //     connect = DriverManager.getConnection(dbString);
            // } catch (SQLException sqlE) {
            //     sqlE.printStackTrace();
            // } catch (Exception e1) {
            //     System.out.println("Check database config file");
            //     e1.printStackTrace();
            try {
                connect = DriverManager.getConnection("jdbc:sqlite:src/CathyNguyenProject0/Project0.db");
            } catch (SQLException e2) {
                System.out.print("[Unable to get Connection]: ");
                e2.printStackTrace();
            }
            // }
        }
        return connect;
    }
    private static void closeConnection() {
        if(connect == null) {
            System.out.println("[Cannot close a Connection that is null]");
        }
        else {
            try {
                connect.close();
            } catch (SQLException e) {
                System.out.print("[Cannot closeConnection]: ");
                e.printStackTrace();
            }
            connect = null;
        }
    }
    
    private static Statement getStatement() {
        if(statement == null) {
            try {
                statement = getConnection().createStatement();
            } catch (SQLException e) {
                System.out.print("[Cannot createStatement]: ");
                e.printStackTrace();
            }
        }
        return statement;
    }
    private static void closeStatement() {
        if(statement == null) {
            System.out.println("[Cannot close a Statement that is null]");
        }
        else {
            try {
                statement.close();
                closeConnection();
            } catch (SQLException e) {
                System.out.print("[Cannot closeStatement]: ");
                e.printStackTrace();
            }
            statement = null;
        }
    }
    
    static PreparedStatement getPreparedStatement() {
        if(prep == null) {
            System.out.println("[getPrepareStatement should pass a string]");
            System.exit(0);
        }
        return prep;
    }
    static PreparedStatement getPreparedStatement(String str) {
        if(prep == null) {
            try {
                prep = getConnection().prepareStatement(str);
            } catch (SQLException e) {
                System.out.print("[Cannot create PrepareStatement]: ");
                e.printStackTrace();
            }
        }
        else {
            closePreparedStatement();
            prep = getPreparedStatement(str);
        }
        return prep;
    }
    static void closePreparedStatement() {
        if(prep == null) {
            System.out.println("[Cannot close a PreparedStatement that is null]");
        }
        else {
            try {
                prep.close();
                closeConnection();
            } catch (SQLException e) {
                System.out.print("[Cannot closePreparedStatement]: ");
                e.printStackTrace();
            }
            prep = null;
        }
    }
    
    static ResultSet getResultSet(String sqlString) {
        if(rs == null) {
            try {
                rs = getStatement().executeQuery(sqlString);   
            } catch (SQLException e) {
                System.out.println("This error again! :D");
                System.out.print("[Cannot create ResultSet]: ");
                e.printStackTrace();
            }
        }
        return rs;
    }
    static void closeResultSet() {
        if(rs == null) {
            System.out.println("[Cannot close a ResultSet that is null]");
        }
        else {
            try {
                rs.close();
                closeStatement();
            } catch (SQLException e) {
                System.out.print("[Cannot closeResultSet]: ");
                e.printStackTrace();
            }
            rs = null;
        }
    }
    
    static Scanner getScanner() {
        if(scan == null) {
            scan = new Scanner(System.in);
        }
        return scan;
    }
    static void closeScanner() { // closing the scanner causes issues. Save 'til the end?
        if(scan == null) {
            System.out.println("[Cannot close a Scanner that is null]");
        }
        else {
            scan.close();
            scan = null;
        }
    }
}