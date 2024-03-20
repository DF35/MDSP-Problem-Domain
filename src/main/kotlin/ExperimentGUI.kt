import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.stage.Stage

class ExperimentGUI: Application() {
    override fun start(primaryStage: Stage) {
        val pane = FXMLLoader.load<ScrollPane>(javaClass.classLoader.getResource("fxml/ExperimentGUI.fxml"))
        val scene = Scene(pane)

        primaryStage.title = "Test"
        primaryStage.scene = scene
        primaryStage.show()
    }
}