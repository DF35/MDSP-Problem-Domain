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

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.text.Text

class ShiftViewController {
    @FXML
    lateinit var assignmentView: ListView<AssignmentView>
    @FXML
    lateinit var time: Text
    @FXML
    lateinit var assignmentInfo: Text
    @FXML
    lateinit var shiftID: Text

    fun initialise(
        handler: ExperimentGUI,
        assignments: List<AssignmentView>,
        shiftID: Int,
        time: String,
        assignmentInfo: String
    ) {
        this.shiftID.text = "Shift $shiftID"
        this.time.text = time
        this.assignmentInfo.text = assignmentInfo
        assignments.forEach { assignmentView.items.add(it) }
        assignmentView.items.forEach { it.button.onAction = handler }
    }
}

class AssignmentView(val id: Int): HBox() {
    val button = AssignmentButton(this)
    val options = ComboBox<String>()
    val doctor = Text()
    var editMode = false

    init {
        children.addAll(options, button, doctor)
        spacing = 10.0
    }

    fun setOptions(doctors: Set<Int>) {
        options.items.clear()
        doctors.forEach { options.items.add("Doctor $it") }
    }
}

class AssignmentButton(val view: AssignmentView): Button() {
    init {
        this.text = "Assign"
    }
}
