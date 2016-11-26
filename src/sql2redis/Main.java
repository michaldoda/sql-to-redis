/*
SQL to Redis (sql2redis)
Created for my tests and development reasons. Sql2redis can be used to export and transform data 
from sql database to redis. Redis keys are stored as JSON.
*/
package sql2redis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.DriverManager;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sql2redis.fxml"));
        primaryStage.setTitle("SQL2Redis - import data from SQL to Redis as JSON");
        primaryStage.setScene(new Scene(root, 1024, 754));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (Exception e) {

        }
        launch(args);
    }
}
