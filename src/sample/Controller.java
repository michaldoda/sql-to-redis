package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;

public class Controller {

    @FXML
    private ChoiceBox databaseDriver;

    @FXML
    private TextArea log;

    @FXML
    private Button connect;

    ObservableList<String> databaseDriverItems = FXCollections.observableArrayList("MySQL", "Oracle", "PostgreSQL");

    @FXML
    private void initialize() {
        this.log.setEditable(false);
        this.databaseDriver.setItems(databaseDriverItems);
    }

    public void connect() throws InterruptedException {
        this.log("info", "Connected!");
        this.connect.setDisable(true);
        this.connect.setText("Disconnect");
    }

    public void disconnect() {

    }

    public void log(String type, String text) {
        this.log.appendText("[" +type.toUpperCase()+ "] "+ text +"\n");
    }
}
