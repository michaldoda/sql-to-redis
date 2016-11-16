package sql2redis;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import redis.clients.jedis.Jedis;
import sql2redis.models.ImportTaskModel;
import sql2redis.tasks.ImportTask;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML
    public PasswordField sqlPassword;

    @FXML
    public TextField sqlConnectionString, sqlUsername, redisHostname, redisPort, redisKeyName;

    @FXML
    public ListView tableList;

    @FXML
    public ChoiceBox redisSuffix;

    @FXML
    public Button importSqlToRedis, connect;

    @FXML
    public ListView tokenList;

    @FXML
    public TextArea jsonSchema;

    @FXML
    public TableView<ImportTaskModel> backgroundThreadsTable;

    @FXML
    private TextArea log;

    private Connection connection = null;

    private String selectedTable;

    private int selectedTableFetchSize = 0;

    private Jedis jedis;

    private boolean isRedisConnected = false;

    private boolean isSqlConnected = false;

    public int importCounter = 0;

    private List<Thread> backgroundThreads = new ArrayList<Thread>();

    private List<String> selectedTableColumns = new ArrayList<>();

    @FXML
    private void initialize() {
        this.log.setEditable(false);
        this.tokenList.setMouseTransparent(true);
        this.tokenList.setFocusTraversable(false);

        Timeline fiveSecondsWonder = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                getThreadsSize();
            }
        }));
        fiveSecondsWonder.setCycleCount(Timeline.INDEFINITE);
        fiveSecondsWonder.play();
    }

    public void connect() {
        this.redisConnect();
        this.sqlConnect();
        if (this.isSqlConnected && this.isRedisConnected) {
            this.getTableList();
        }
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
        this.redisKeyName.setText(tableName);
        this.getTableInfo(tableName);
        this.tokenList.setVisible(true);
        this.setTokenlist();
        this.prepareJsonSchema();
        this.selectedTableColumns.add("Auto-increment id");
        this.redisSuffix.setItems(FXCollections.observableArrayList(this.selectedTableColumns));
        this.redisSuffix.setValue("Auto-increment id");
        this.importSqlToRedis.setDisable(false);
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
    }

    private void sqlConnect() {
        try {
            String connectionString = this.sqlConnectionString.getText();
            String username = this.sqlUsername.getText();
            String password = this.sqlPassword.getText();
            this.connection = DriverManager.getConnection(connectionString, username, password);
            this.connect.setText("Reconnect");
            this.isSqlConnected = true;
            this.log("info", "Successfully connected to sql database");
        } catch (Exception e) {
            this.log("error", e.getMessage());
        }
    }

    private void redisConnect() {
        try {
            String hostname = this.redisHostname.getText();
            int port = Integer.parseInt(this.redisPort.getText());
            this.jedis = new Jedis(hostname, port);
            this.jedis.connect();
            if (this.jedis.isConnected()) {
                this.isRedisConnected = true;
                this.log("info", "Successfully connected to redis");
            } else {
                this.log("error", "The connection to the redis has failed");
            }
        } catch (Exception e) {
            this.log("error", e.getMessage());
        }
    }

    final public boolean test() {
        return true;
    }

    final public void getThreadsSize() {
        for (int i = 0, j = this.backgroundThreads.size(); i < j; i++) {
            Thread tmpThread = this.backgroundThreads.get(i);
            this.backgroundThreadsTable.getItems().forEach(item -> {
                if (item.getName().equals(tmpThread.getName())) {
                    item.setStatus(tmpThread.getState().toString());
                }
            });
            ObservableList t = this.backgroundThreadsTable.getItems();
            FXCollections.copy(this.backgroundThreadsTable.getItems(), t);
        }
    }

    public void importSqlToRedis() {
        String sqlUrl = this.sqlConnectionString.getText();
        String sqlUser = this.sqlUsername.getText();
        String sqlPassword = this.sqlPassword.getText();
        String redisHost = this.redisHostname.getText();
        int redisPort = Integer.parseInt(this.redisPort.getText());
        String tableToImport = this.selectedTable;
        String jsonSchema = this.jsonSchema.getText();
        ImportTask importTask = new ImportTask(sqlUrl, sqlUser, sqlPassword, redisHost, redisPort, tableToImport, jsonSchema);
        Thread th = new Thread(importTask);
        th.setName("Task" + this.importCounter);
        this.importCounter++;
        this.backgroundThreads.add(th);
        th.setDaemon(true);
        th.start();
        this.log("info", "Started");

        ImportTaskModel newTask = new ImportTaskModel(th.getName(), tableToImport, ImportTaskModel.translateState(th.getState().toString()));
        final ObservableList<ImportTaskModel> row = FXCollections.observableArrayList(newTask);
        this.backgroundThreadsTable.getItems().addAll(row);
//
//        importTask.setOnSucceeded(new EventHandler() {
//            @Override
//            public void handle(Event t) {
//                if (importTask.isDone()){
//                    test();
//                    System.out.println("ok");
//                } else {
//                    System.out.println("nope");
//                }
//            }
//        });
    }

    private void setTokenlist() {
        this.tokenList.setItems(FXCollections.observableArrayList(this.selectedTableColumns));
    }

    private void prepareJsonSchema() {
        StringBuilder jsonSchemaText = new StringBuilder();
        jsonSchemaText.append("{\n");
        for (int i = 0; i < this.selectedTableColumns.size(); i++) {
            String tmpName = this.selectedTableColumns.get(i);
            jsonSchemaText.append("    \""+tmpName+"\": " + "$"+tmpName+"\",\n");
        }
        jsonSchemaText.append("}");
        this.jsonSchema.setText(jsonSchemaText.toString());
    }
}
