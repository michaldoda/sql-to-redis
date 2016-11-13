package sql2redis;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import redis.clients.jedis.Jedis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML
    public PasswordField sqlPassword;

    @FXML
    public TextField sqlConnectionString, sqlUsername, redisHostname, redisPort, redisPrefix;

    @FXML
    public ListView tableList;

    @FXML
    public ChoiceBox redisSuffix;

    @FXML
    public Button importFromTable, connect;

    @FXML
    public ListView tokenList;

    @FXML
    private TextArea log;

    private Connection connection = null;

    private String selectedTable;

    private int selectedTableFetchSize = 0;

    private Jedis jedis;

    private List<String> selectedTableColumns = new ArrayList<>();

    @FXML
    private void initialize() {
        this.log.setEditable(false);
    }

    public void connect() {
        this.redisConnect();
        this.sqlConnect();
        this.getTableList();
    }

    private void getTableList() {
        try {
            DatabaseMetaData dmd = this.connection.getMetaData();
            String[] systemName = {"TABLE", "VIEW"};
            ResultSet rs = dmd.getTables("%", "%", "%", systemName);
            ArrayList tmpTables = new ArrayList();
            while (rs.next()) {
                tmpTables.add(rs.getString(3));
            }
            this.tableList.setItems(FXCollections.observableArrayList(tmpTables));
        } catch (Exception e) {
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
        this.getTableInfo(tableName);
        this.redisSuffix.setItems(FXCollections.observableArrayList(this.selectedTableColumns));
        this.setTokenlist();
        this.redisSuffix.setValue("Auto-increment id");
        this.log("info", "Table: "+this.selectedTable+ " contains: " + this.selectedTableFetchSize + " rows");
    }

    private void log(String type, String text) {
        this.log.appendText("[" +type.toUpperCase()+ "] "+ text +"\n");
    }

    private void getTableInfo(String tableName) {
        try {
            DatabaseMetaData dmd = this.connection.getMetaData();
            ResultSet resultSet = dmd.getColumns(null, null, tableName, "%");
            List<String> columns = new ArrayList<>();
            while (resultSet.next()) {
                columns.add(resultSet.getString(4));
            }
            this.selectedTableColumns = columns;

            String sql = "SELECT count(*) as total FROM $tableName".replace("$tableName", tableName);
            Statement stmt = this.connection.createStatement();
            ResultSet resultSetCount = stmt.executeQuery(sql);
            while (resultSetCount.next()) {
                this.selectedTableFetchSize = resultSetCount.getInt("total");
            }
        } catch (SQLException e) {
            this.log("error", e.getMessage());
        }
        this.selectedTableColumns.add("Auto-increment id");
    }

    private void sqlConnect() {
        try {
            String connectionString = this.sqlConnectionString.getText();
            String username = this.sqlUsername.getText();
            String password = this.sqlPassword.getText();
            this.connection = DriverManager.getConnection(connectionString, username, password);
            this.connect.setText("Reconnect");
            this.log("info", "connected to SQL Database!");
        } catch (Exception e) {
            this.log("error", e.getMessage());
        }
    }

    private void redisConnect() {
        try {
            String hostname = this.redisHostname.getText();
            int port = Integer.parseInt(this.redisPort.getText());
            this.jedis = new Jedis(hostname, port);
            this.log("info", "connected to Reds!");
        } catch (Exception e) {
            this.log("error", e.getMessage());
        }
    }

    public void tableToRedisImport() {
        try {
            String sql = "SELECT * FROM $tableName".replace("$tableName", this.selectedTable);
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            double counter = 1;
            while (rs.next()) {
                counter++;
                this.jedis.set(""+counter+"", "pattern");
            }
        } catch (Exception e) {
            this.log("error", "Import not completed, "+e.getMessage());
        }
    }

    private void setTokenlist() {
        this.tokenList.setItems(FXCollections.observableArrayList(this.selectedTableColumns));
    }
}
