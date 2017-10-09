package sql2redis.tasks;


import javafx.concurrent.Task;
import redis.clients.jedis.Jedis;

import java.sql.*;

public class ImportTask extends Task<Integer> {
    private final String sqlUrl, sqlUser, sqlPassword, redisHost, redisPort, redisKeySuffixSchema, redisKeyBodySchema, tableToImport, jsonSchema, redisAuth;
    private final Boolean isAutoIncrementSuffix;



    public ImportTask(String sqlUrl, String sqlUser, String sqlPassword, String redisHost, String redisPort, String tableToImport, String jsonSchema, Boolean isAutoIncrementSuffix, String redisKeySuffixSchema, String redisKeyBodySchema, String redisAuth) {
        this.sqlUrl = sqlUrl;
        this.sqlUser = sqlUser;
        this.sqlPassword = sqlPassword;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisAuth = redisAuth;
        this.jsonSchema = jsonSchema;
        this.tableToImport = tableToImport;
        this.isAutoIncrementSuffix = isAutoIncrementSuffix;
        this.redisKeySuffixSchema = redisKeySuffixSchema;
        this.redisKeyBodySchema = redisKeyBodySchema;
        System.out.println("Task, constructor");
    }

    @Override protected Integer call() throws Exception {
        int counter = 0;
        try {
            System.out.println("Task, call method");
            Connection connection = DriverManager.getConnection(this.sqlUrl, this.sqlUser, this.sqlPassword);
            Jedis jedis = new Jedis(this.redisHost, Integer.parseInt(this.redisPort));
            if (!this.redisAuth.equals("")) {
                jedis.auth(this.redisAuth);
            }
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
                    if (rs.getString(name) == null) {
                        jsonRow = jsonRow.replace("$$"+name+"$$", "");
                    } else {
                        jsonRow = jsonRow.replace("$$"+name+"$$", rs.getString(name).replace("\"", "'"));
                    }
                }
                String out = jsonRow.replaceAll("\\s+","").replace("\n", "").replace("\r", "").replace("\\/", "/").trim();
                out = out.replaceAll("\\\\/", "/").replace("\\", "").replaceAll ("\\\\", "").replace('\\','/');
                if (this.isAutoIncrementSuffix) {
                    jedis.set(this.redisKeyBodySchema+"_"+counter, out);
                } else {
                    jedis.set(this.redisKeyBodySchema+"_"+rs.getString(this.redisKeySuffixSchema), out);
                }

            }
            connection.close();
            jedis.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return counter;
    }
}