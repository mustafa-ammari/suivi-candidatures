package app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;


public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main_view.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 800);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/light-theme.css")).toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Suivi des candidatures");
        stage.show();
        Platform.runLater(stage::centerOnScreen);

    }
//    @Override
//    public void start(Stage stage) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        stage.setTitle("History Events");
//        stage.initStyle(StageStyle.UNDECORATED);
//        stage.setScene(scene);
//        stage.setMinWidth(320);
//        stage.setMinHeight(406);
//        stage.show();
//
//        DraggableService.makeDraggable(stage);
//
//        Label labelSize = (Label) stage.getScene().getRoot().lookup("#labelSize");
//        System.out.println(labelSize.getText());
//        labelSize.textProperty().bind(Bindings.format("Windows %1$.0fx%2$.0f",stage.widthProperty(), stage.heightProperty()));
//    }

    public static void main(String[] args) {
        launch();
    }
}