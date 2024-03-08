import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import java.net.URL
import java.util.*

class InstanceGUI: Application() {
    override fun start(primaryStage: Stage) {
        val pane = FXMLLoader.load<AnchorPane>(javaClass.getResource("InstanceGUI.fxml"))
        val scene = Scene(pane)

        primaryStage.title = "Test"
        primaryStage.scene = scene
        primaryStage.show()
    }

    /*private var editIndex = -1 // Initialize to -1
    override fun start(primaryStage: Stage) {
        primaryStage.title = "To-Do List"
        val items: ObservableList<String> = FXCollections.observableArrayList()
        val listView = ListView(items)
        val newItemField = TextField()
        val addButton = Button("Add")
        val removeButton = Button("Remove")
        val editButton = Button("Edit")
        addButton.onAction = EventHandler { _: ActionEvent? ->
            val newItem = newItemField.text
            if (!newItem.isEmpty()) {
                if (editIndex != -1) {
                    items.set(editIndex, newItem) // Edit existing item
                    editIndex = -1 // Reset edit index
                } else {
                    items.add(newItem) // Add new item
                }
                newItemField.clear()
            }
        }
        removeButton.onAction = EventHandler { _: ActionEvent? ->
            val selectedIndex = listView.selectionModel.selectedIndex
            if (selectedIndex >= 0) {
                items.removeAt(selectedIndex)
            }
        }
        editButton.onAction = EventHandler { _: ActionEvent? ->
            val selectedIndex = listView.selectionModel.selectedIndex
            if (selectedIndex >= 0) {
                newItemField.text = items.get(selectedIndex)
                editIndex = selectedIndex // Set edit index
            }
        }
        val inputBox = HBox(10.0)
        inputBox.children.addAll(newItemField, addButton, removeButton, editButton)
        val root = VBox(10.0)
        root.children.addAll(inputBox, listView)
        val scene = Scene(root, 400.0, 300.0)
        primaryStage.setScene(scene)
        primaryStage.show()
    } */
}

class InstanceGUIController: Initializable {
    val times = startTimes()
    lateinit var selectedView: ListView<String>
    @FXML
    lateinit var startTime: ComboBox<String>
    @FXML
    lateinit var endTime: ComboBox<String>
    @FXML
    lateinit var monday: ListView<String>
    @FXML
    lateinit var tuesday: ListView<String>
    @FXML
    lateinit var wednesday: ListView<String>
    @FXML
    lateinit var thursday: ListView<String>
    @FXML
    lateinit var friday: ListView<String>
    @FXML
    lateinit var saturday: ListView<String>
    @FXML
    lateinit var sunday: ListView<String>
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    lateinit var listViews: List<ListView<String>>

    fun handleTestButton() {
        if(this::selectedView.isInitialized && startTime.value != null && endTime.value != null) {
            selectedView.items.add(startTime.value + "-" + endTime.value)
            startTime.selectionModel.clearSelection()
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        listViews = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday)
        for((index, view) in listViews.withIndex()) {
            view.setOnMouseClicked { selectedView = view }
        }
        startTime.items.addAll(times)
    }

    private fun startTimes(): List<String> {
        val increments = listOf("00","15", "30", "45")
        val times = mutableListOf<String>()
        val twenty = listOf(0,1,2,3)
        for(i in 0..9)
            for(min in increments) {
                times.add("0$i:$min")
                times.add("1$i:$min")
                if(twenty.contains(i)) times.add("2$i:$min")
            }
        times.sort()
        return times
    }

    fun calculateEndTimes() {
        endTime.items.clear()
        val validTimes = mutableListOf<String>()
        val start = startTime.value ?: return
        val (hour, minute) = start.split(":")
        val end = hour.toInt() + 13

        if(end > 23) {
            // Valid end times from current day
            validTimes.addAll(times.filter { it > start })

            // Valid end times from next day
            val labelIndex = when(val viewIndex = listViews.indexOf(selectedView)) {
                in 0..5 ->  viewIndex + 1
                else -> 0
            }
            for(time in times.filter { it <= (end - 24).toString() + ":$minute" })
                validTimes.add("$time (" + days[labelIndex] + ")")
        }
        else
            validTimes.addAll(times.filter { it > start && it <= "$end:$minute" })

        endTime.items.addAll(validTimes)
    }
}
