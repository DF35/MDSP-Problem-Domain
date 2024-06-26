/*
 * Copyright 2024 Daniel Ferring
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fieldExperimentInterface

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import problemDomain.*
import java.io.BufferedWriter
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExperimentGUI: Application(), EventHandler<javafx.event.ActionEvent> {
    lateinit var solution: Solution
    lateinit var pd: MDSP
    val assignmentViews = mutableListOf<AssignmentView>()
    val doctorViews = mutableMapOf<Int, DoctorViewController>()

    override fun start(primaryStage: Stage) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val time = LocalDateTime.now().format(formatter)
        val stage = Stage()
        pd = MDSP(25022024)
        pd.loadInstance(2)
        solution = pd.blankSolution()
        val content = generateContent(stage, "dep1_easy_$time.txt")
        val scene = Scene(content)

        stage.title = "Timetabling Experiment Interface"
        stage.scene = scene
        stage.showAndWait()

        assignmentViews.clear()
        doctorViews.clear()
        val stage2 = Stage()
        pd = MDSP(25022024)
        pd.loadInstance(5)
        solution = pd.blankSolution()
        val content2 = generateContent(stage2, "dep2_hard_$time.txt")
        val scene2 = Scene(content2)
        stage2.title = "Timetabling Experiment Interface"
        stage2.scene = scene2
        stage2.showAndWait()
    }

    private fun generateContent(stage: Stage, filename: String): HBox {
        val content = HBox()

        val shiftContent = ListView<VBox>()
        shiftContent.prefWidth = 1050.00
        shiftContent.prefHeight = 590.00

        for(day in solution.data.days)
            initialiseDay(day, shiftContent)

        val submitButton = Button("Submit")
        submitButton.onAction = EventHandler {
            var solutionString = ""
            for(assignment in solution.data.assignments)
                if(assignment.assignee != null)
                    solutionString += "al ${assignment.id} ${assignment.assignee}\n"
            val writer = BufferedWriter(FileWriter(filename))
            writer.write(solutionString)
            writer.close()
            stage.close()
        }

        val buttonContainer = VBox(submitButton)
        buttonContainer.alignment = Pos.BASELINE_CENTER
        buttonContainer.padding = Insets(30.00)
        shiftContent.items.add(buttonContainer)

        val doctorContent = initialiseDoctorContent()

        content.children.addAll(shiftContent, doctorContent)
        return content
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

        controller.initialise(this, shiftAssignments, shiftID, time, assignmentRequirements)
        shifts.children.add(sView)
    }

    private fun initialiseDoctorContent(): ListView<VBox> {
        val doctorList = ListView<VBox>()
        doctorList.prefWidth = 350.0

        for(doctor in solution.data.doctors) {
            val loader = FXMLLoader(javaClass.classLoader.getResource("fxml/DoctorView.fxml"))
            val dView = loader.load<VBox>()
            val controller = loader.getController<DoctorViewController>()
            controller.initialise(doctor)
            doctorList.items.add(dView)
            doctorViews[doctor.id] = controller
        }

        return doctorList
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

        for((doctorID, controller) in doctorViews) {
            val doctor = solution.data.doctors[doctorID]
            val dayViolations = solution.dayRangeViolations[doctorID]!!
            val nightViolations = solution.nightRangeViolations[doctorID]!!
            val shiftViolations = solution.shiftPrefsViolated[doctorID]!!
            controller.refresh(doctor, dayViolations, nightViolations, shiftViolations)
        }
    }

}
