import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("ui/Login.fxml"));
            Scene scene = new Scene(root, 520, 550);
            primaryStage.setTitle("SAPCIS - Smart Academic & Campus Information System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading Login screen. Ensure ui/Login.fxml and ui/styles.css exist in classpath.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Booting up SAPCIS...");
        launch(args);
    }
}
