package sql2redis.core.db.sql;


import java.sql.*;

public class SqlDataAccess {

    private java.sql.Connection connection = null;

    public static Connection getDb(String driver, String user, String password, String schema) {
        try {
            return DriverManager.getConnection("");
        } catch (SQLException e) {

        }

        return null;
    }
}
