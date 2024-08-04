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
package problemDomain

import kotlin.Exception

data class SolutionData(
    val assignments: List<Assignment>,
    val shifts: List<Shift>,
    val doctors: List<MiddleGrade>,
    val days: List<Day>,
) {
    fun copy(): SolutionData {
        val assignments = mutableListOf<Assignment>()
        val shifts = mutableListOf<Shift>()
        val days = mutableListOf<Day>()
        val doctors = mutableListOf<MiddleGrade>()
        this.assignments.forEach { assignments.add(it.copy()) }
        this.shifts.forEach { shifts.add(it.copy()) }
        this.doctors.forEach { doctors.add(it.copy()) }
        this.days.forEach { days.add(it.copy()) }
        return SolutionData(assignments, shifts, doctors, days)
    }
}

// The cause of a shift's infeasibility for a given doctor
enum class Cause {
    Leave, Training, Rest
}

// The source of a shift's infeasibility for a given doctor
sealed class Source {
    // Stores the ID of the seven-day-long block
    data class RowOfSevenDays(val block: Int): Source()
    // Stores the IDs of the two blocks making the day infeasible
    data class WouldCauseRowTooLarge(val blocks: Pair<Int, Int>): Source()
    // Stores the IDs of the two blocks making the overlapping shifts infeasible
    data class WouldCauseRowTooLargeOverlap(val blocks: Pair<Int, Int>): Source()
    // If a block is 6 days long, relevant overlapping shifts need to be made infeasible
    data class RowOfSixOverlap(val block: Int): Source()
    data class InsufficientRest(val blocks: Pair<Int, Int>): Source()
    data class InsufficientRestOverlap(val blocks: Pair<Int, Int>): Source()
    data class InsufficientRestMid(val blocks: Triple<Int, Int, Int>): Source()
    data class InsufficientRestMidOverlap(val blocks: Triple<Int, Int, Int>): Source()
    data class RowOfFourLongShifts(val block: Int): Source()
    data class WouldCauseTooLargeRowOfLongShifts(val blocks: Pair<Int, Int>): Source()
    data class InsufficientRestForRowOfFourLongShifts(val blockAndShifts: Pair<Int, Set<Int>>): Source()
    data class InsufficientRestForRowOfFourLongShiftsMid(val blocksAndShifts: Triple<Int, Int, Set<Int>>): Source()
    data class WeekendWorked(val dayID: Int): Source()
    // Stores the IDs of days with nights worked by the doctor that are necessitating rest
    data class RowOfNights(val days: Set<Int>): Source()
    // Stores the ID of a shift that has made another shift infeasible
    data class ShiftWorked(val shiftID: Int): Source()
}

// Instantiated when a shift becomes infeasible for a doctor, keeps track of the source(s) of infeasibility
class ShiftInfeasibility(val cause : Cause) {
    val sources = mutableSetOf<Source>()

    fun copy(): ShiftInfeasibility {
        val infeasibility = ShiftInfeasibility(cause)
        infeasibility.sources.addAll(sources)
        return infeasibility
    }

    override fun toString(): String {
        var string = "$cause "
        for(source in sources)
            string += when(source) {
                is Source.ShiftWorked -> "ShiftWorked: ${source.shiftID} "
                is Source.WeekendWorked -> "WeekendWorked ${source.dayID} "
                is Source.RowOfNights -> "NightsWorked ${source.days} "
                is Source.RowOfSevenDays -> "RowOfSevenDays: ${source.block} "
                is Source.RowOfFourLongShifts -> "RowOfFourLongShifts: Block ${source.block}"
                is Source.RowOfSixOverlap -> "RowOfSixOverlap: Block ${source.block} "
                is Source.InsufficientRest ->
                    "InsufficientRest: Block ${source.blocks.first}, Block ${source.blocks.second} "
                is Source.InsufficientRestOverlap ->
                    "InsufficientRestOverlap: Block ${source.blocks.first}, Block ${source.blocks.second} "
                is Source.WouldCauseRowTooLarge ->
                    "WouldCauseRowTooLarge: ${source.blocks.first}, ${source.blocks.second} "
                is Source.WouldCauseRowTooLargeOverlap ->
                    "WouldCauseRowTooLargeOverlap: ${source.blocks.first}, ${source.blocks.second} "
                is Source.InsufficientRestMid ->
                    "InsufficientRestMid: Block ${source.blocks.first}, Block ${source.blocks.second}, Block ${source.blocks.third} "
                is Source.InsufficientRestMidOverlap ->
                    "InsufficientRestMidOverlap: Block ${source.blocks.first}, Block ${source.blocks.second}, Block ${source.blocks.third} "
                is Source.InsufficientRestForRowOfFourLongShifts ->
                    "InsufficientRestForRowOfFourLongShifts: Block ${source.blockAndShifts.first}, Shifts ${source.blockAndShifts.second}"
                is Source.InsufficientRestForRowOfFourLongShiftsMid ->
                    "InsufficientRestForRowOfFourLongShiftsMid: Block ${source.blocksAndShifts.first}, Block ${source.blocksAndShifts.second}, Shifts ${source.blocksAndShifts.third}"
                is Source.WouldCauseTooLargeRowOfLongShifts ->
                    "WouldCauseRowTooLargeRowOfLongShifts: Block ${source.blocks.first}, Block ${source.blocks.second}"
            }
        return string
    }

    fun debug() {
        println(this.toString())
    }
}

/*
 * Used to simplify the assignment of multiple doctors to one shift as well as the
 * requirement for specific numbers of doctors of a given grade
 */
class Assignment(
    val id: Int,
    val shift: Int,
    val requiredGrade: String,
    //Doctors that do not meet the required grade for the assignment
    val infeasibleDoctors: Set<Int>
) {
    var assignee: Int? = null

    fun copy(): Assignment {
        val assignment = Assignment(id, shift, requiredGrade, infeasibleDoctors)
        assignment.assignee = assignee
        return assignment
    }

    fun debug() {
        println("Assignment: $id")
        println("Shift: $shift")
        println("RequiredGrade: $requiredGrade")
        println("Assignee: $assignee")
        println("Infeasible Doctors: $infeasibleDoctors")
        println()
    }

    // Allocates the given doctor to the assignment
    fun assign(doctor: Int) { assignee = doctor }

    /*
     * If the doctor is assigned, they are removed and the shiftId and doctor ID are returned for use in updating
     * the feasibility of shifts, null is returned if the doctor is not assigned
     */
    fun unAssign(): Pair<Int,Int>?{
        val doctor : Int
        when (assignee){
            null -> return assignee
            else -> doctor = assignee as Int
        }
        assignee = null
        return Pair(doctor, shift)
    }
}

// Represents a single shift within the timetable - holds data and related functions
abstract class Shift(
    val id: Int,
    // A single shift can have multiple assignees - one assignment per needed doctor is made for each shift
    val assignmentIDs: IntArray,
    val shiftsWithin11Hours: Set<Int>,
    val shifts48HoursAfter: List<Int>,
    val longShifts48HoursBefore: List<Int>,
    val day: Int,
    val feasibleDoctors: MutableSet<Int>,
    // Duration of the shift in hours
    val duration: Double
) {
    // Key = doctor ID, value = reason for infeasibility - used to map doctors to their causes of infeasibility
    val causesOfInfeasibility = mutableMapOf<Int, ShiftInfeasibility>()
    val longShiftsMadeInfeasible = mutableMapOf<Int, MutableSet<Int>>()
    // IDs of Doctors assigned to the shift
    val assignees = mutableSetOf<Int>()
    // If a shift is more than 10 hours in length it is classed as "long" relevant to feasibility
    val long = if(duration > 10) true else false

    abstract fun copy(): Shift

    fun debug() {
        println("Shift: $id")
        println("Assignment ids: " + assignmentIDs.contentToString())
        println("Shifts within 11 hours: $shiftsWithin11Hours")
        println("Shifts 48 hours after: $shifts48HoursAfter")
        println("Long shifts 48 hours before: $longShifts48HoursBefore")
        when(this) {
            is DayShift -> println("Night Shifts 48 hours before: $nightShifts48HoursBefore")
            is NightShift -> println("DayShifts 48 hours after: $dayShifts48HoursAfter")
        }
        println("Day: $day")
        println("Feasible Doctors: $feasibleDoctors")
        causesOfInfeasibility.forEach { print("Doctor ${it.key}: "); it.value.debug() }
        println("Assignees: $assignees")
        println("longShiftsMadeInfeasible: $longShiftsMadeInfeasible")
        println()
    }

    // Creates a new infeasibility with cause REST for a given doctor
    fun restInfeasibility(doctor: Int, source: Source) {
        val infeasibility = causesOfInfeasibility[doctor]

        when {
            // If no REST infeasibility exists, a new infeasibility is created
            infeasibility == null -> createRestInfeasibility(doctor, source)
            // If a REST infeasibility exists, add the additional source
            infeasibility.cause == Cause.Rest -> infeasibility.sources.add(source)
            /*
             * If the existing infeasibility is not REST, do nothing
             * (REST is irrelevant as the shift will always be infeasible for that doctor)
             */
            else -> return
        }
    }

    // Creates a new infeasibility with cause REST and removes the given doctor from feasibleDoctors
    private fun createRestInfeasibility(doctor: Int, source: Source){
        val infeasibility = ShiftInfeasibility(Cause.Rest)
        infeasibility.sources.add(source)
        causesOfInfeasibility[doctor] = infeasibility
        // [doctor] is guaranteed to be in feasibleDoctors due to code within [problemDomain.Solution]
        feasibleDoctors.remove(doctor)
    }

    // Creates a new infeasibility for any non-REST causes for a given doctor
    fun createNonRestInfeasibility(doctor: Int, cause: Cause) {
        when {
            /*
             * There should only be one non-rest infeasibility, additionally, if there is already a rest infeasibility,
             * this function has been called mid-search, which should not happen
             */
            causesOfInfeasibility[doctor] != null -> throw Exception("createNonRestFeasibility: infeasibility already exists")
            // If the given cause is REST, restInfeasibility should have been called instead of this function
            cause == Cause.Rest -> throw Exception("createNonRestInfeasibility: REST given as cause")
        }

        // Creates a new infeasibility and removes the doctor from feasibleDoctors
        val infeasibility = ShiftInfeasibility(cause)
        causesOfInfeasibility[doctor] = infeasibility
        // [doctor] is guaranteed to be in feasibleDoctors due to code within [problemDomain.Solution]
        feasibleDoctors.remove(doctor)
    }


    /*
     *  Called when a doctor is removed from a shift. If the infeasibility arose from a rest requirement
     *  (i.e. not leave or training), the specified source is removed from the infeasibility
     */
    fun removeSource(doctor: Int, source: Source) {
        val infeasibility = causesOfInfeasibility[doctor] ?: throw Exception ("removeSource: Infeasibility does not exist")

        // If the infeasibility exists but is not a REST, the shift must always be infeasible for the doctor
        if (infeasibility.cause != Cause.Rest) return

        if(!infeasibility.sources.remove(source))
            throw Exception("removeSource: Source $source does not exist for shift $id")

        if(infeasibility.sources.isEmpty()) {
            causesOfInfeasibility.remove(doctor)
            feasibleDoctors.add(doctor)
        }
    }

    fun addInfeasibleShift(doctor: Int, shiftID: Int) {
        val set = longShiftsMadeInfeasible.getOrDefault(doctor, mutableSetOf())
        set.add(shiftID)
        longShiftsMadeInfeasible[doctor] = set
    }

    fun removeInfeasibleShift(doctor: Int, shiftID: Int) {
        if(longShiftsMadeInfeasible[doctor] == null)
            throw Exception("removeInfeasibleShift: no entry for doctor $doctor in longShiftsMadeInfeasible")
        longShiftsMadeInfeasible[doctor]!!.remove(shiftID)

        if(longShiftsMadeInfeasible[doctor]!!.isEmpty())
            longShiftsMadeInfeasible.remove(doctor)
    }
}

/*
 * Stores the additional data that needs to be associated with DayShifts, namely the night shifts that take
 * place 48 hours before them
 */
class DayShift(
    id: Int,
    assignmentIDs: IntArray,
    shiftsWithin11Hours: Set<Int>,
    shifts48HoursAfter: List<Int>,
    longShifts48HoursBefore: List<Int>,
    val nightShifts48HoursBefore: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Double
) : Shift(id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter,
            longShifts48HoursBefore, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = DayShift(
            id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, longShifts48HoursBefore,
            nightShifts48HoursBefore, day, feasibleDoctors.toMutableSet(), duration
        )
        for((key, value) in causesOfInfeasibility)
            shift.causesOfInfeasibility[key] = value.copy()
        for((key, value) in longShiftsMadeInfeasible)
            shift.longShiftsMadeInfeasible[key] = value.toMutableSet()
        shift.assignees.addAll(this.assignees)
        return shift
    }
}

/*
 * Stores the additional data that needs to be associated with NightShifts, namely the day shifts that take place
 * 48 hours after them
 */
class NightShift(
    id: Int,
    assignmentIDs: IntArray,
    shiftsWithin11Hours: Set<Int>,
    shifts48HoursAfter: List<Int>,
    longShifts48HoursBefore: List<Int>,
    val dayShifts48HoursAfter: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Double,
    // true if night shift overlaps into next day, false if not
    val overlaps: Boolean
) : Shift(id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter,
            longShifts48HoursBefore, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = NightShift(
            id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, longShifts48HoursBefore,
            dayShifts48HoursAfter, day, feasibleDoctors.toMutableSet(), duration, overlaps
        )
        for ((key, value) in causesOfInfeasibility)
            shift.causesOfInfeasibility[key] = value.copy()
        for((key, value) in longShiftsMadeInfeasible)
            shift.longShiftsMadeInfeasible[key] = value.toMutableSet()
        shift.assignees.addAll(this.assignees)
        return shift
    }
}

// Stores data related to a specific day within the timetable
class Day(
    val id: Int,
    // IDs of all non-long day shifts on this day
    private val dayShifts: List<Int>,
    // IDs of all night shifts on this day that do not overlap to the next
    private val nonOverlappingNightShifts: List<Int>,
    // IDs of all night shifts on this day that overlap to the next
    val overlappingNightShifts: List<Int>,
    val longShifts: List<Int>,
) {
    // <DoctorID, ShiftID>
    var doctorsWorkingNight = mutableMapOf<Int, Int>()
    // <DoctorID, Set<ShiftID>>
    var doctorsWorkingDay = mutableMapOf<Int, MutableSet<Int>>()
    // <DoctorID, ShiftID> (Doctor cannot work more than 1 long shift a day)
    var doctorsWorkingLongShift = mutableMapOf<Int, Int>()
    // Number of shifts that have at least one doctor assigned to them
    var numShiftsWithCoverage = 0
    // Tracks the block of each doctor the day is a part of (if it is) <DoctorID, BlockID>
    var block = mutableMapOf<Int, Int>()
    var longShiftBlock = mutableMapOf<Int, Int>()

    fun copy(): Day {
        val day = Day(id, dayShifts, nonOverlappingNightShifts, overlappingNightShifts, longShifts)
        day.doctorsWorkingNight = doctorsWorkingNight.toMutableMap()
        for((key, value) in doctorsWorkingDay)
            day.doctorsWorkingDay[key] = value.toMutableSet()
        day.doctorsWorkingLongShift = doctorsWorkingLongShift.toMutableMap()
        day.numShiftsWithCoverage = this.numShiftsWithCoverage
        day.block = this.block.toMutableMap()
        day.longShiftBlock = this.longShiftBlock.toMutableMap()
        return day
    }

    fun debug() {
        println("Day: $id")
        println("Shifts: ${this.getShifts()}")
        println("Doctors Working Day:")
        doctorsWorkingDay.forEach { println("Doctor: ${it.key}, Shifts: ${it.value}")}
        println("Doctors Working Night:")
        doctorsWorkingNight.forEach { println("Doctor: ${it.key}, Shift ${it.value}") }
        println("Blocks: ")
        block.forEach { println("Doctor: ${it.key}, Block: ${it.value}") }
        println("Blocks of long shifts: ")
        longShiftBlock.forEach { println("Doctor: ${it.key}, Block: ${it.value}") }
        println()
    }

    // Gets IDs of all shifts belonging to the day
    fun getShifts(): List<Int> {
        return dayShifts + nonOverlappingNightShifts + overlappingNightShifts
    }

    // Gets IDs of all night shifts belonging to this day
    fun getNightShifts(): List<Int> {
        return nonOverlappingNightShifts + overlappingNightShifts
    }

    /*
     * Adds a record of a doctor working a shift on this day - assumes that you are passing
     * a valid shift that is a part of the given day
     */
    fun addWorkingDoctor(doctor: Int, shiftID: Int) {
        doctorsWorkingDay.getOrPut(doctor) { mutableSetOf() }.add(shiftID)
    }

    /*
     * Removes the record of the doctor having worked the shift on this day, removes their
     * entry if no worked shifts remain
     */
    fun removeWorkingDoctor(doctor: Int, shiftID: Int) {
        val removed = doctorsWorkingDay[doctor]?.remove(shiftID)
            ?: throw Exception("removeWorkingDoctor: Doctor $doctor has no entry for working on day $id")
        if(!removed)
            throw Exception("removeWorkingDoctor: Doctor $doctor has no record of working shift $shiftID on day $id")
        if(doctorsWorkingDay[doctor]!!.isEmpty())
            doctorsWorkingDay.remove(doctor)
    }
}

// Represents where in the block an item was removed from
enum class ItemRemovedPos {
    Start, // Leftmost item
    Middle, // From the middle of the block
    End, // Rightmost item
    Final // Was the last item in the block
}

/*
 * Stores information relating to a block of either days worked or of days where a long
 * shift is worked
 */
class Block(val id: Int) {
    val items = mutableSetOf<Int>()
    val shiftsMadeInfeasible = mutableSetOf<Int>()

    fun copy(): Block {
        val block = Block(id)
        block.setItems(items.toSet())
        block.shiftsMadeInfeasible.addAll(this.shiftsMadeInfeasible)
        return block
    }

    fun addItem(item: Int) {
        if(!items.add(item))
            throw Exception("addItem: Block $id already contains $item")
    }

    // Clears [days] and accepts the new value; for use in merging and splitting blocks
    fun setItems(newDays: Set<Int>) {
        items.clear()
        items.addAll(newDays)
    }

    fun removeItem(item: Int, nextID: Int): Pair<ItemRemovedPos, Pair<Block, Block>?> {
        val position = when {
            item == items.min() && item == items.max() -> ItemRemovedPos.Final
            item == items.min() -> ItemRemovedPos.Start
            item == items.max() -> ItemRemovedPos.End
            else -> ItemRemovedPos.Middle
        }

        if(!items.remove(item))
            throw Exception("removeItem: Block $id does not contain $item")

        val blocks =  when(position) {
            // The block is split into two, one has the original [id], the other, [nextID]
            ItemRemovedPos.Middle -> {
                val (firstBlock, secondBlock) = items.partition { it < item }
                val block1 = Block(id)
                block1.setItems(firstBlock.toSet())
                val block2 = Block(nextID)
                block2.setItems(secondBlock.toSet())
                Pair(block1, block2)
            }
            else -> null
        }

        return Pair(position, blocks)
    }

}

// Represents whether a doctor has a preference for this aspect of timetabling
data class Preferences(
    val dayRange: Boolean,
    val nightRange: Boolean,
    val shiftsToAvoid: Boolean
)

// Stores information relating to each MiddleGrade
class MiddleGrade(
    val id: Int,
    val grade: String,
    // Equal to [averageHours] if full time, a percentage of its value if part-time
    val targetHours: Double,
    val targetDayShifts: Int,
    val targetNightShifts: Int,
    val averageHoursDenominator: Double,
    val shiftsToAvoid: Set<Int>,
    val dayRange: IntRange,
    val nightRange: IntRange
) {
    var hoursWorked = 0.00
    var dayShiftsWorked = 0
    var nightShiftsWorked = 0
    var assignedAssignments = mutableListOf<Int>()
    var assignedShifts = mutableSetOf<Int>()
    // nextBlockID is shared between [blocksOfDays] and [blocksOfLongShifts]
    val blocksOfDays = mutableMapOf<Int, Block>() //<blockID, Block>
    val blocksOfLongShifts = mutableMapOf<Int, Block>() //<blockID, Block>
    var nextBlockID = 0
    // Any element of the data class is true, if the doctor has a preference for that aspect
    val preferences = Preferences(dayRange != 1..7,
        nightRange != 1..4, shiftsToAvoid.isNotEmpty())

    fun varianceHoursWorked(): Double { return targetHours - (hoursWorked / averageHoursDenominator) }

    fun varianceDayShiftsWorked(): Int { return targetDayShifts - dayShiftsWorked }

    fun varianceNightShiftsWorked(): Int { return targetNightShifts - nightShiftsWorked }

    fun numberOfShiftPrefsViolated(): Int { return assignedShifts.intersect(shiftsToAvoid).size }

    fun copy(): MiddleGrade {
        val doctor = MiddleGrade(id, grade, targetHours, targetDayShifts, targetNightShifts,
            averageHoursDenominator, shiftsToAvoid, dayRange, nightRange)
        doctor.hoursWorked = this.hoursWorked
        doctor.dayShiftsWorked = this.dayShiftsWorked
        doctor.nightShiftsWorked = this.nightShiftsWorked
        doctor.assignedAssignments = this.assignedAssignments.toMutableList()
        doctor.assignedShifts = this.assignedShifts.toMutableSet()
        this.blocksOfDays.forEach { doctor.blocksOfDays[it.key] = it.value.copy() }
        this.blocksOfLongShifts.forEach { doctor.blocksOfLongShifts[it.key] = it.value.copy() }
        doctor.nextBlockID = this.nextBlockID
        return doctor
    }

    override fun toString(): String {
        val basicInfo = "\nDoctor: $id\nGrade: $grade\n"
        val targets = "Target Hours: $targetHours\nTarget Day Shifts: $targetDayShifts\nTarget Night Shifts: $targetNightShifts\n"
        val actual = "Hours Worked: $hoursWorked\nAverage Hours Worked (Adjusted for leave) ${hoursWorked/averageHoursDenominator}\nDay Shifts Worked: $dayShiftsWorked\nNight Shifts Worked: $nightShiftsWorked\n"
        return basicInfo + targets + actual
    }

    fun debug() {
        println(id)
        println(grade)
        println(targetHours)
        println(assignedAssignments)
        println("Blocks of Days:")
        blocksOfDays.forEach { println("Block ${it.key}, Days = ${it.value.items}, InfShifts = ${it.value.shiftsMadeInfeasible}") }
        println("Blocks of Long Shifts:")
        blocksOfLongShifts.forEach { println("Block ${it.key}, Days = ${it.value.items}, InfShifts = ${it.value.shiftsMadeInfeasible}") }
        println()
    }
}

