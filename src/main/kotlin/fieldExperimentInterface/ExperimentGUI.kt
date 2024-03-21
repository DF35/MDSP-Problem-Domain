package fieldExperimentInterface

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import problemDomain.*
import java.io.BufferedWriter
import java.io.FileWriter
import kotlin.system.exitProcess

class ExperimentGUI: Application(), EventHandler<javafx.event.ActionEvent> {
    lateinit var solution: Solution
    lateinit var pd: MDSP
    val assignmentViews = mutableListOf<AssignmentView>()

    override fun start(primaryStage: Stage) {
        pd = MDSP(25022024)
        pd.loadInstance(1)
        solution = pd.blankSolution()

        val content = ListView<VBox>()
        content.prefWidth = 1050.00
        content.prefHeight = 590.00

        for(day in solution.data.days)
            initialiseDay(day, content)

        val submitButton = Button("Submit")
        submitButton.onAction = EventHandler {
            var solutionString = ""
            for(assignment in solution.data.assignments)
                if(assignment.assignee != null)
                    solutionString += "al ${assignment.id} ${assignment.assignee}\n"
            val writer = BufferedWriter(FileWriter("timetable.txt"))
            writer.write(solutionString)
            writer.close()
            exitProcess(0)
        }
        val buttonContainer = VBox(submitButton)
        buttonContainer.alignment = Pos.BASELINE_CENTER
        buttonContainer.padding = Insets(30.00)
        content.items.add(buttonContainer)

        val pane = FXMLLoader.load<AnchorPane>(javaClass.classLoader.getResource("fxml/ExperimentGUI.fxml"))
        pane.children.add(content)
        val scene = Scene(pane)

        primaryStage.title = "Timetabling Experiment Interface"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun initialiseDay(day: Day, content: ListView<VBox>) {
        val shifts = HBox()
        for (shiftID in day.getShifts())
            initialiseShiftView(shiftID, shifts)


        val dayView = VBox()
        val dayName = Text(findDay(day.id))
        dayName.font = Font(20.00)
        dayView.children.addAll(dayName, shifts)
        dayView.alignment = Pos.BASELINE_CENTER
        dayView.spacing = 20.00

        content.items.add(dayView)
    }

    private fun findDay(dayID: Int): String {
        return when (dayID % 7) {
            0 -> "Monday\n"
            1 -> "Tuesday\n"
            2 -> "Wednesday\n"
            3 -> "Thursday\n"
            4 -> "Friday\n"
            5 -> "Saturday\n"
            else -> "Sunday\n"
        }
    }

    private fun initialiseShiftView(shiftID: Int, shifts: HBox) {
        val loader = FXMLLoader(javaClass.classLoader.getResource("fxml/ShiftView.fxml"))
        val sView = loader.load<VBox>()
        val controller = loader.getController<ShiftViewController>()

        val shift = solution.data.shifts[shiftID]
        val shiftAssignments = mutableListOf<AssignmentView>()
        val requiredGrades = mutableListOf<String>()

        for(assignmentID in shift.assignmentIDs) {
            val view = AssignmentView(assignmentID)

            val assignment = solution.data.assignments[assignmentID]
            view.setOptions(shift.feasibleDoctors.subtract(assignment.infeasibleDoctors))
            requiredGrades.add(assignment.requiredGrade)

            shiftAssignments.add(view)
            assignmentViews.add(view)
        }

        val time = when(shift) {
            is DayShift -> "${pd.shiftTimes[shiftID]} (Day Shift)"
            is NightShift -> "${pd.shiftTimes[shiftID]} (Night Shift)"
            else -> "" // Shouldn't happen
        }

        var assignmentRequirements = ""
        for((index, grade) in requiredGrades.withIndex())
            assignmentRequirements += "Assignment ${index+1} Required Grade = $grade\n"
        if(requiredGrades.size > 1) assignmentRequirements = assignmentRequirements.dropLast(1)

        controller.initialise(this, shiftAssignments, time, assignmentRequirements)
        shifts.children.add(sView)
    }

    // Used to handle the assignment buttons being pressed
    override fun handle(event: javafx.event.ActionEvent) {
        val button = event.source
        if(button !is AssignmentButton) throw Exception("ExperimentGUI has been added to incorrect component")

        // Handle actions relating to edited assignment
        val view = button.view
        when(view.editMode) {
            false -> {
                // If no doctor selected, do nothing
                if(view.options.value == null) return

                val selectedDoctor =
                    view.options.value.toString().split(" ")[1].toInt()
                solution.allocateAssignment(view.id, selectedDoctor)

                view.options.items.clear()
                view.doctor.text = "Doctor $selectedDoctor"
                button.text = "Edit"
                view.editMode = true
            }

            true -> {
                solution.deallocateAssignment(view.id)

                // Update user selection options
                val assignment = solution.data.assignments[view.id]
                view.setOptions(
                    solution.data.shifts[assignment.shift].feasibleDoctors.subtract(
                        assignment.infeasibleDoctors
                    )
                )

                view.doctor.text = ""
                button.text = "Assign"
                view.editMode = false
            }
        }

        // update assignment options of all relevant views
        for(v in assignmentViews)
            if(!v.editMode) {
                val assignment = solution.data.assignments[v.id]
                v.setOptions(solution.data.shifts[assignment.shift].feasibleDoctors
                    .subtract(assignment.infeasibleDoctors))
            }
    }

}
