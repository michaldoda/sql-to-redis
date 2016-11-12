package sql2redis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML
    public PasswordField sqlPassword;

    @FXML
    public TextField sqlConnectionString, sqlSchema, sqlUsername, redisHostname, redisPort, redisPrefix;

    @FXML
    public ListView tableList;

    @FXML
    public ChoiceBox redisSuffix;

    @FXML
    public ProgressBar tableListProgressbar;

    @FXML
    public Button importFromTable, connect;

    @FXML
    private TextArea log;

    private Connection connection = null;

    private String selectedTable;

    private Jedis jedis;

    @FXML
    private void initialize() {
        this.log.setEditable(false);
    }

    public void connect() {
        this.redisConnect();
        this.sqlConnect();
        this.getTableList();
    }

    public void getTableList() {
        try {
            DatabaseMetaData dmd = this.connection.getMetaData();
            String[] systemName = {"TABLE"};
            ResultSet rs = dmd.getTables("%", "%", "%", systemName);
            ArrayList tmpTables = new ArrayList();
            while (rs.next()) {
                tmpTables.add(rs.getString(3));
            }
            this.tableList.setItems(FXCollections.observableArrayList(tmpTables));
        } catch (SQLException e) {
            this.log("error", e.getMessage());
        }
    }

    public void selectTableList() {
        if (tableList.getItems().size() == 0) {
            return;
        }
        String tableName = tableList.getSelectionModel().getSelectedItem().toString();
        this.selectedTable = tableName;
        this.redisPrefix.setText(tableName);
        this.redisSuffix.setItems(FXCollections.observableArrayList(this.getTableColumns(tableName)));
        this.redisSuffix.setValue("Auto-increment id");
        this.log("info", "Table: "+this.selectedTable+ " contains: " + this.getTableFetchSize() + " rows");
    }

    public void log(String type, String text) {
        this.log.appendText("[" +type.toUpperCase()+ "] "+ text +"\n");
    }

    private List getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        try {
            DatabaseMetaData dmd = this.connection.getMetaData();
            ResultSet rs = dmd.getColumns(null, null, tableName, "%");
            while (rs.next()) {
                columns.add(rs.getString(4));
            }
        } catch (SQLException e) {
            this.log("error", e.getMessage());
        }
        columns.add("Auto-increment id");
        return columns;
    }

    public void sqlConnect() {
        try {
            String connectionString = this.sqlConnectionString.getText();
            String username = this.sqlUsername.getText();
            String password = this.sqlPassword.getText();
            this.connection = DriverManager.getConnection(connectionString, username, password);
            this.connect.setText("Reconnect");
            this.log("info", "connected to SQL Database!");
        } catch (SQLException e) {
            this.log("error", e.getMessage());
        }
    }

    private void redisConnect() {
        try {
            String hostname = this.redisHostname.getText();
            int port = Integer.parseInt(this.redisPort.getText());
            this.jedis = new Jedis(hostname, port);
            this.log("info", "connected to Reds!");
        } catch (JedisException e) {
            this.log("error", e.getMessage());
        } catch (Exception e) {
            this.log("error", e.getMessage());
        }
        this.tableToRedisImport();
    }

    public void tableToRedisImport() {
        try {
            String sql = "SELECT * FROM $tableName".replace("$tableName", this.selectedTable);
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            double size = rs.getFetchSize();
            double counter = 1;
            while (rs.next()) {
                counter++;
                this.jedis.set(""+counter+"", "pattern");
                double currentProgress = this.tableListProgressbar.getProgress();
                double newProgress = currentProgress + counter/size;
                this.tableListProgressbar.setProgress(newProgress);
            }
        } catch (Exception e) {
            this.log("error", "Import not completed, "+e.getMessage());
        }
    }

    private int getTableFetchSize() {
        int result = 0;
        try {
            String sql = "SELECT * FROM $tableName".replace("$tableName", this.selectedTable);
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            result = rs.getFetchSize();
        } catch (Exception e) {
            this.log("error", "Import not completed, "+e.getMessage());
        }
        return result;
    }
}
