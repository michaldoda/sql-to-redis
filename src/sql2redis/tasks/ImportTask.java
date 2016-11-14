package sql2redis.tasks;


import javafx.concurrent.Task;
import redis.clients.jedis.Jedis;

import java.sql.*;

public class ImportTask extends Task<Integer> {

    private final String sqlUrl;

    private final String sqlUser;

    private final String sqlPassword;

    private final String redisHost;

    private final int redisPort;

    private final String jsonSchema;

    private final String tableToImport;

    public ImportTask(String sqlUrl, String sqlUser, String sqlPassword, String redisHost, int redisPort, String tableToImport, String jsonSchema) {
        this.sqlUrl = sqlUrl;
        this.sqlUser = sqlUser;
        this.sqlPassword = sqlPassword;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.jsonSchema = jsonSchema;
        this.tableToImport = tableToImport;
        System.out.println("Task, constructor");
    }

    @Override protected Integer call() throws Exception {
        int counter = 0;
        try {
            System.out.println("Task, call method");
            Connection connection = DriverManager.getConnection(this.sqlUrl, this.sqlUser, this.sqlPassword);
            Jedis jedis = new Jedis(this.redisHost, this.redisPort);
            String sql = "SELECT * FROM $tableName".replace("$tableName", this.tableToImport);
            Statement stmt = connection.createStatement();
            connection.setAutoCommit(false);
            stmt.setFetchSize(40000);
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            while (rs.next()) {
                String jsonRow = jsonSchema;
                counter++;
                for (int i = 1; i <= columnCount; i++ ) {
                    String name = rsmd.getColumnName(i);
                    if (rs.getString(name) != null) {
                        jsonRow = jsonRow.replace("$"+name, rs.getString(name));
                    } else {
                        jsonRow = jsonRow.replace("$"+name, "");
                    }
                }
                System.out.println(jsonRow);
                jedis.set(""+counter+"", jsonRow);
            }
            connection.close();
            jedis.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return counter;
    }
}