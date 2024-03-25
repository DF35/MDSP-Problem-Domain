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
    // If a day is 6 days long, relevant overlapping shifts need to be made infeasible
    data class RowOfSixOverlap(val block: Int): Source()
    // Stores the ID of the block and the ID of the day preventing it from being extended
    data class InsufficientRest(val blocks: Pair<Int, Int>): Source()
    // Stores the IDs of the block and the day that make the overlapping shifts infeasible
    data class InsufficientRestOverlap(val blocks: Pair<Int, Int>): Source()
    data class InsufficientRestMid(val blocks: Triple<Int, Int, Int>): Source()
    data class InsufficientRestMidOverlap(val blocks: Triple<Int, Int, Int>): Source()
    data class WeekendWorked(val dayID: Int): Source() // Need to alter
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
                is Source.InsufficientRest -> "InsufficientRest: Block ${source.blocks.first}, Block ${source.blocks.second} "
                is Source.InsufficientRestOverlap -> "InsufficientRestOverlap: Block ${source.blocks.first}, Block ${source.blocks.second} "
                is Source.RowOfSevenDays -> "RowOfSevenDays: ${source.block} "
                is Source.WouldCauseRowTooLarge -> "WouldCauseRowTooLarge: ${source.blocks.first}, ${source.blocks.second} "
                is Source.WouldCauseRowTooLargeOverlap -> "WouldCauseRowTooLargeOverlap: ${source.blocks.first}, ${source.blocks.second} "
                is Source.RowOfSixOverlap -> "RowOfSixOverlap: Block ${source.block} "
                is Source.InsufficientRestMid -> "InsufficientRestMid: Block ${source.blocks.first}, Block ${source.blocks.second}, Block ${source.blocks.third} "
                is Source.InsufficientRestMidOverlap -> "InsufficientRestMidOverlap: Block ${source.blocks.first}, Block ${source.blocks.second}, Block ${source.blocks.third} "
            }
        return string + "\n"
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
    var iterationAssigned = Int.MAX_VALUE

    fun copy(): Assignment {
        val assignment = Assignment(id, shift, requiredGrade, infeasibleDoctors)
        assignment.assignee = assignee
        assignment.iterationAssigned = iterationAssigned
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
    val day: Int,
    val feasibleDoctors: MutableSet<Int>,
    // Duration of the shift in hours
    val duration: Double
) {
    // Key = doctor ID, value = reason for infeasibility - used to map doctors to their causes of infeasibility
    val causesOfInfeasibility: MutableMap<Int, ShiftInfeasibility> = mutableMapOf()
    // IDs of Doctors assigned to the shift
    val assignees = mutableSetOf<Int>()

    abstract fun copy(): Shift

    fun debug() {
        println("Shift: $id")
        println("Assignment ids: " + assignmentIDs.contentToString())
        println("Shifts within 11 hours: $shiftsWithin11Hours")
        println("Shifts 48 hours after: $shifts48HoursAfter")
        when(this) {
            is DayShift -> println("Night Shifts 48 hours before: $nightShifts48HoursBefore")
            is NightShift -> println("DayShifts 48 hours after: $dayShifts48HoursAfter")
        }
        println("Day: $day")
        println("Feasible Doctors: $feasibleDoctors")
        causesOfInfeasibility.forEach { print("Doctor ${it.key}: "); it.value.debug(); println() }
        println()
        println("Assignees: $assignees")
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
    val nightShifts48HoursBefore: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Double
) : Shift(id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = DayShift(id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, nightShifts48HoursBefore, day,
                                feasibleDoctors.toMutableSet(), duration)
        for((key, value) in causesOfInfeasibility)
            shift.causesOfInfeasibility[key] = value.copy()
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
    val dayShifts48HoursAfter: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Double,
    // true if night shift overlaps into next day, false if not
    val overlaps: Boolean
) : Shift(id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = NightShift(
            id, assignmentIDs, shiftsWithin11Hours, shifts48HoursAfter, dayShifts48HoursAfter, day,
            feasibleDoctors.toMutableSet(), duration, overlaps
        )
        for ((key, value) in causesOfInfeasibility)
            shift.causesOfInfeasibility[key] = value.copy()
        shift.assignees.addAll(this.assignees)
        return shift
    }
}

// Stores data related to a specific day within the timetable
class Day(
    val id: Int,
    // IDs of all day shifts on this day
    val dayShifts: List<Int>,
    // IDs of all night shifts on this day that do not overlap to the next
    val nonOverlappingNightShifts: List<Int>,
    // IDs of all night shifts on this day that overlap to the next
    val overlappingNightShifts: List<Int>
) {
    // <DoctorID, ShiftID>
    var doctorsWorkingNight = mutableMapOf<Int, Int>()
    // <DoctorID, Set<ShiftID>>
    var doctorsWorkingDay = mutableMapOf<Int, MutableSet<Int>>()
    // Number of shifts that have at least one doctor assigned to them
    var numShiftsWithCoverage = 0
    // Tracks the block of each doctor the day is a part of (if it is) <DoctorID, BlockID>
    var block = mutableMapOf<Int, Int>()

    fun copy(): Day {
        val day = Day(id, dayShifts, nonOverlappingNightShifts, overlappingNightShifts)
        for((key, value) in doctorsWorkingNight)
            day.doctorsWorkingNight[key] = value
        for((key, value) in doctorsWorkingDay)
            day.doctorsWorkingDay[key] = value.toMutableSet()
        day.numShiftsWithCoverage = this.numShiftsWithCoverage
        day.block = this.block.toMutableMap()
        return day
    }

    fun debug() {
        println("Day: $id")
        println("Shifts: ${this.getShifts()}")
        println("Doctors Working Day:")
        doctorsWorkingDay.forEach { println("Doctor: ${it.key}, Shifts: ${it.value}")}
        println("Doctors Working Night:")
        doctorsWorkingNight.forEach { println("Doctor: ${it.key}, Shift ${it.value}") }
        println("Shifts made infeasible due to insufficient rest")
        println("Blocks: ")
        block.forEach { println("Doctor: ${it.key}, Block: ${it.value}") }
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
        val removed = doctorsWorkingDay[doctor]?.remove(shiftID) ?: throw Exception("removeWorkingDoctor: Doctor $doctor has no entry for working on day $id")
        if(!removed) throw Exception("removeWorkingDoctor: Doctor $doctor has no record of working shift $shiftID on day $id")
        if(doctorsWorkingDay[doctor]!!.isEmpty())
            doctorsWorkingDay.remove(doctor)
    }
}

// Represents where in the block a day was removed from
enum class DayRemovedPos {
    Start, // Leftmost day
    Middle, // From the middle of the block
    End, // Rightmost day
    Final // Was the last day in the block
}

// Stores information relating to a block of days worked by the doctor
class Block(val id: Int) {
    val days = mutableSetOf<Int>()
    val shiftsMadeInfeasible = mutableSetOf<Int>()

    fun copy(): Block {
        val block = Block(id)
        block.setDays(days.toSet())
        block.shiftsMadeInfeasible.addAll(this.shiftsMadeInfeasible)
        return block
    }

    fun addDay(day: Int) {
        if(!days.add(day))
            throw Exception("addDay: Block $id already contains $day")
    }

    // Clears [days] and accepts the new value; for use in merging and splitting blocks
    fun setDays(newDays: Set<Int>) {
        days.clear()
        days.addAll(newDays)
    }

    fun removeDay(day: Int, nextID: Int): Pair<DayRemovedPos, Pair<Block, Block>?> {
        val position = when {
            day == days.min() && day == days.max() -> DayRemovedPos.Final
            day == days.min() -> DayRemovedPos.Start
            day == days.max() -> DayRemovedPos.End
            else -> DayRemovedPos.Middle
        }

        if(!days.remove(day))
            throw Exception("removeDay: Block $id does not contain $day")

        val blocks =  when(position) {
            // The block is split into two, one has the original [id], the other, [nextID]
            DayRemovedPos.Middle -> {
                val (firstBlock, secondBlock) = days.partition { it < day }
                val block1 = Block(id)
                block1.setDays(firstBlock.toSet())
                val block2 = Block(nextID)
                block2.setDays(secondBlock.toSet())
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
    val blocksOfDays = mutableMapOf<Int, Block>() //<blockID, Block>
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
        println(hoursWorked)
        println(assignedAssignments)
        blocksOfDays.forEach { println("Block ${it.key}, Days = ${it.value.days}, InfShifts = ${it.value.shiftsMadeInfeasible}") }
        println()
    }
}

