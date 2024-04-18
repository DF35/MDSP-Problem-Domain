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
import javafx.scene.text.Text
import problemDomain.MiddleGrade

class DoctorViewController {
    @FXML
    lateinit var doctorID: Text
    @FXML
    lateinit var doctorGrade: Text
    @FXML
    lateinit var dayPref: Text
    @FXML
    lateinit var dayViolation: Text
    @FXML
    lateinit var nightPref: Text
    @FXML
    lateinit var nightViolation: Text
    @FXML
    lateinit var shiftPref: Text
    @FXML
    lateinit var shiftViolation: Text
    @FXML
    lateinit var targetHours: Text
    @FXML
    lateinit var avgHours: Text
    @FXML
    lateinit var dayTarget: Text
    @FXML
    lateinit var dayShifts: Text
    @FXML
    lateinit var nightTarget: Text
    @FXML
    lateinit var nightShifts: Text

    fun initialise(doctor: MiddleGrade) {
        doctorID.text = "Doctor ${doctor.id}"
        doctorGrade.text = "Grade: ${doctor.grade}"
        dayPref.text = when {
            doctor.dayRange == 1..7 -> "No preference for day shifts"
            doctor.dayRange.min() == doctor.dayRange.max() -> "Prefers ${doctor.dayRange.min()} days in a row"
            else -> "Prefers ${doctor.dayRange.min()}-${doctor.dayRange.max()} days in a row"
        }
        dayViolation.text = "Day preference violated 0 times"
        nightPref.text = when {
            doctor.nightRange == 1..4 -> "No preference for night shifts"
            doctor.nightRange.min() == doctor.nightRange.max() -> "Prefers ${doctor.nightRange.min()} nights in a row"
            else -> "Prefers ${doctor.nightRange.min()}-${doctor.nightRange.max()} nights in a row"
        }
        nightViolation.text = "Night preference violated 0 times"
        shiftPref.text = when(doctor.shiftsToAvoid.isNotEmpty()) {
            true -> "Shifts preferably avoided: ${doctor.shiftsToAvoid}"
            else -> "No preference for shifts worked"
        }
        shiftViolation.text = "Shift preferences violated 0 times"
        targetHours.text = "Target average hours per week: ${doctor.targetHours}"
        avgHours.text = "Average weekly hours (adjusted for leave): ${doctor.hoursWorked/doctor.averageHoursDenominator}"
        dayTarget.text = "Target number of day shifts: ${doctor.targetDayShifts}"
        dayShifts.text = "Number of day shifts worked: ${doctor.dayShiftsWorked}"
        nightTarget.text = "Target number of night shifts: ${doctor.targetNightShifts}"
        nightShifts.text = "Number of night shifts worked: ${doctor.nightShiftsWorked}"
    }

    fun refresh(
        doctor: MiddleGrade,
        dayViolations: Int,
        nightViolations: Int,
        shiftViolations: Int,
    ) {
        dayViolation.text = "Day preference violated $dayViolations times"
        nightViolation.text = "Night preference violated $nightViolations times"
        shiftViolation.text = "Shift preferences violated $shiftViolations times"
        avgHours.text = "Average weekly hours (adjusted for leave): ${doctor.hoursWorked/doctor.averageHoursDenominator}"
        dayShifts.text = "Number of day shifts worked: ${doctor.dayShiftsWorked}"
        nightShifts.text = "Number of night shifts worked: ${doctor.nightShiftsWorked}"

    }
}