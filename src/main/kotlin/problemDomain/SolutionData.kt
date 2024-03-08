import kotlin.Exception

// The cause for a shift's infeasibility for a given doctor
enum class Cause {
    LEAVE, TRAINING, REST
}

// The source of a shift's infeasibility for a given doctor
sealed class Source {
    data object DaysWorked: Source()
    data class WeekendWorked(val dayID: Int): Source()
    data object NightsWorked: Source()
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
                Source.DaysWorked -> "Days Worked "
                Source.NightsWorked -> "Nights Worked "
                is Source.ShiftWorked -> "Shift Worked ${source.shiftID} "
                is Source.WeekendWorked -> "Weekend Worked ${source.dayID} "
            }
        return string + "\n"
    }

    fun debug() {
        print("$cause ")
        for(source in sources)
            when(source) {
                Source.DaysWorked -> println("DaysWorked")
                is Source.ShiftWorked -> println("ShiftWorked: " + source.shiftID)
                is Source.WeekendWorked -> println("WeekendWorked " + source.dayID)
                Source.NightsWorked -> println("NightsWorked")
            }
    }
}

// Represents a single shift within the timetable - holds data and related functions
abstract class Shift(
    val id: Int,
    // A single shift can have multiple assignees - one assignment per needed doctor is made for each shift
    val assignmentIDs: IntArray,
    val shiftsWithin11Hours: Set<Int>,
    val day: Int,
    val feasibleDoctors: MutableSet<Int>,
    // Duration of the shift in hours
    val duration: Int
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
        when(this) {
            is DayShift -> println("Night Shifts 48 hours before: $nightShifts48HoursBefore")
            is NightShift -> println("DayShifts 48 hours after: $dayShifts48HoursAfter")
        }
        println("Day: $day")
        println("Feasible Doctors: $feasibleDoctors")
        causesOfInfeasibility.forEach { print("Doctor ${it.key}: "); it.value.debug() }
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
            infeasibility.cause == Cause.REST -> infeasibility.sources.add(source)
            /*
             * If the existing infeasibility is not REST, do nothing
             * (REST is irrelevant as the shift will always be infeasible for that doctor)
             */
            else -> return
        }
    }

    // Creates a new infeasibility with cause REST and removes the given doctor from feasibleDoctors
    private fun createRestInfeasibility(doctor: Int, source: Source){
        val infeasibility = ShiftInfeasibility(Cause.REST)
        infeasibility.sources.add(source)
        causesOfInfeasibility[doctor] = infeasibility
        // [doctor] is guaranteed to be in feasibleDoctors due to code within [Solution]
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
            cause == Cause.REST -> throw Exception("createNonRestInfeasibility: REST given as cause")
        }

        // Creates a new infeasibility and removes the doctor from feasibleDoctors
        val infeasibility = ShiftInfeasibility(cause)
        causesOfInfeasibility[doctor] = infeasibility
        // [doctor] is guaranteed to be in feasibleDoctors due to code within [Solution]
        feasibleDoctors.remove(doctor)
    }


    /*
     *  Called when a doctor is removed from a shift. If the infeasibility arose from a rest requirement
     *  (i.e. not leave or training), the specified source is removed from the infeasibility
     */
    fun removeSource(doctor: Int, source: Source) {
        val infeasibility = causesOfInfeasibility[doctor] ?: throw Exception ("removeSource: Infeasibility does not exist")

        // If the infeasibility exists but is not a REST, the shift must always be infeasible for the doctor
        if (infeasibility.cause != Cause.REST) return

        if(!infeasibility.sources.remove(source))
            throw Exception("removeSource: Source $source does not exist")

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
    val nightShifts48HoursBefore: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Int
) : Shift(id, assignmentIDs, shiftsWithin11Hours, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = DayShift(id, assignmentIDs, shiftsWithin11Hours, nightShifts48HoursBefore, day,
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
    val dayShifts48HoursAfter: Set<Int>,
    day: Int,
    feasibleDoctors: MutableSet<Int>,
    duration: Int
) : Shift(id, assignmentIDs, shiftsWithin11Hours, day, feasibleDoctors, duration) {
    override fun copy(): Shift {
        val shift = NightShift(
            id, assignmentIDs, shiftsWithin11Hours, dayShifts48HoursAfter, day,
            feasibleDoctors.toMutableSet(), duration
        )
        for ((key, value) in causesOfInfeasibility)
            shift.causesOfInfeasibility[key] = value.copy()
        shift.assignees.addAll(this.assignees)
        return shift
    }
}

/*
 * Used to simplify the assignment of multiple doctors to one shift as well as the requirement for specific numbers
 * of doctors of a specific grade
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
        assignment.assignee = this.assignee
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

class Day(
    val id: Int,
    val dayShifts: Set<Int>,
    val nightShifts: Set<Int>,
) {
    /*
     * Key = doctor ID, Value = number of shifts worked on day. Keeps track of the number of shifts a doctor is working
     * on a given day
     */
    val doctorsWorkingDay: MutableMap<Int, Int> = mutableMapOf()
    // Keeps track of causes of infeasibility of a given doctor that relate to the days worked
    val causesOfInfeasibility: MutableMap<Int, MutableSet<DayNightInfeasibility>> = mutableMapOf()
    /*
     * IDs of days whose infeasibility was contributed to by this day. If the doctor in question stops working on this
     * day, the feasibility of these days will need to be reevaluated
     * */
    val toCheck: MutableMap<Int, MutableSet<Int>> = mutableMapOf()
    /*
     * Key = doctor ID, Value = number of night shifts worked on day. Keeps track of the number of night shifts a doctor
     * is working on a given day
     */
    val doctorsWorkingNight: MutableSet<Int> = mutableSetOf()
    // Keeps track of causes of infeasibility of a given doctor that relate to the nights worked
    val causesOfNightInfeasibility: MutableMap<Int, MutableSet<DayNightInfeasibility>> = mutableMapOf()
    /*
     * IDs of days whose night infeasibility was contributed to by this day's night shifts. If the doctor in question
     * stops working on this day's night shifts, the feasibility of these day's night shifts will need to be reevaluated
     */
    val toCheckNight: MutableMap<Int, MutableSet<Int>> = mutableMapOf()

    fun copy(): Day {
        val day = Day(id, dayShifts, nightShifts)
        day.doctorsWorkingDay.putAll(this.doctorsWorkingDay)
        for((key,value) in causesOfInfeasibility)
            day.causesOfInfeasibility[key] = value.toMutableSet()
        for((key, value) in toCheck)
            day.toCheck[key] = value.toMutableSet()
        day.doctorsWorkingNight.addAll(this.doctorsWorkingNight)
        for((key, value) in causesOfNightInfeasibility)
            day.causesOfNightInfeasibility[key] = value.toMutableSet()
        for((key, value) in toCheckNight)
            day.toCheckNight[key] = value.toMutableSet()

        return day
    }

    fun debug() {
        println(id)
        println(dayShifts.union(nightShifts))
        println("To Check: $toCheck")
        causesOfInfeasibility.forEach { print("Doctor ${it.key}: "); debugCause(it.value)}
        println("Working day: $doctorsWorkingDay")
        println("To Check Night: $toCheckNight")
        causesOfNightInfeasibility.forEach{print("Doctor ${it.key}: "); debugCause(it.value)}
        println("Working Night: $doctorsWorkingNight")
        println()
    }

    private fun debugCause(causes: MutableSet<DayNightInfeasibility>) {
        for(infeasibility in causes){
            print(infeasibility::class.simpleName + " " + infeasibility.sources + ", ")
        }
        println()
    }

    // Returns all shifts associated with the day
    fun getShifts(): Set<Int> { return dayShifts.union(nightShifts) }

    // Adds one to the number of shifts worked by the given doctor on that day
    fun addWorkingDoctor(doctor: Int) {
        doctorsWorkingDay[doctor] = doctorsWorkingDay.getOrDefault(doctor, 0) + 1
    }

    // Subtracts one from the number of shifts worked by the given doctor on that day
    fun removeWorkingDoctor(doctor: Int) {
        doctorsWorkingDay[doctor] = doctorsWorkingDay.getOrDefault(doctor, 0) -1
        if(doctorsWorkingDay[doctor]!! == 0)
            doctorsWorkingDay.remove(doctor)
        else if(doctorsWorkingDay[doctor]!! < 0)
            throw Exception("removeWorkingDoctor: Doctor $doctor was not working on day $id")
    }

    // Adds an infeasibility caused by the days worked by the doctor in question
    fun addInfeasibility(doctor: Int, cause: DayNightInfeasibility) {
        when(causesOfInfeasibility[doctor]) {
            // Creates a new infeasibility if one did not exist beforehand
            null -> causesOfInfeasibility[doctor] = mutableSetOf(cause)
            // If an infeasibility already exists for the doctor, the new cause is added
            else -> causesOfInfeasibility[doctor]?.add(cause)
        }
    }

    // Removes a cause of infeasibility, returns true if no causes of infeasibility remain, false if not
    fun removeInfeasibility(doctor: Int, cause: DayNightInfeasibility): Boolean {
        val removed = causesOfInfeasibility[doctor]?.remove(cause)
            ?: throw Exception("removeToCheck: no infeasibility for doctor $doctor in day $id")
        if (!removed) throw Exception("removeToCheck: $cause not present in day $id")

        if(causesOfInfeasibility[doctor]!!.isEmpty()){
            causesOfInfeasibility.remove(doctor)
            return true
        }
        return false
    }

    // Called when a day is made infeasible, with this day contributing to the necessity for rest
    fun addToCheck(doctor: Int, dayID: Int) {
        when(toCheck[doctor]) {
            null -> toCheck[doctor] = mutableSetOf(dayID)
            else -> toCheck[doctor]?.add(dayID)
        }
    }

    /*
     * The day specified is no longer made infeasible by this day, and therefore does not need to be updated if the
     * doctor in question stops working on this day
     */
    fun removeToCheck(doctor: Int, dayID: Int) {
        val removed = toCheck[doctor]?.remove(dayID)
            ?: throw Exception("removeToCheck: doctor $doctor has no entry in toCheck of $id")
        if (!removed) throw Exception("removeToCheck: day $dayID not present in toCheck of $id")
        if(toCheck[doctor]!!.isEmpty()) toCheck.remove(doctor)
    }

    // Adds an infeasibility for the night shifts of this day caused by the other night shifts worked by the doctor
    fun addNightInfeasibility(doctor: Int, cause: DayNightInfeasibility) {
        when(causesOfNightInfeasibility[doctor]) {
            null -> causesOfNightInfeasibility[doctor] = mutableSetOf(cause)
            else -> causesOfNightInfeasibility[doctor]?.add(cause)
        }
    }

    /*
     * Removes a cause of infeasibility from night shifts of this day, returns true if no causes of infeasibility
     * remain, false if not
     */
    fun removeNightInfeasibility(doctor: Int, cause: DayNightInfeasibility) : Boolean {
        val removed = causesOfNightInfeasibility[doctor]?.remove(cause)
            ?: throw Exception("removeNightInfeasibility: Doctor $doctor is not present in causesOfNightInfeasibility of day $id")
        if(!removed) throw Exception("removeNightInfeasibility: $cause not present in causesOfNightInfeasibility of day $id")

        if(causesOfNightInfeasibility[doctor]!!.isEmpty()){
            causesOfNightInfeasibility.remove(doctor)
            return true
        }
        return false
    }

    /*
     * Called when a night shift's infeasibility is contributed to by this day's night shifts; if the doctor no longer
     * works this night, it will be necessary to assess whether the night in question is still infeasible
     */
    fun addToCheckNight(doctor: Int, day: Int) {
        when(toCheckNight[doctor]) {
            null -> toCheckNight[doctor] = mutableSetOf(day)
            else -> toCheckNight[doctor]?.add(day)
        }
    }

    /*
     * The night specified is no longer made infeasible by the night shifts of this day, and does not need to be updated
     * if the doctor in question stops working on this day's night
     */
    fun removeToCheckNight(doctor: Int, day: Int) {
        val removed = toCheckNight[doctor]?.remove(day)
            ?: throw Exception("removeToCheckNight: Doctor $doctor has no entry in toCheckNight for day $id")
        if(!removed) throw Exception("removeToCheckNight: day $day not present in toCheckNight of $id")

        if(toCheckNight[doctor]!!.isEmpty()) toCheckNight.remove(doctor)
    }
}

/*
 * Used to represent infeasibility caused by the days or nights a doctor works - shared between the two scenarios as the
 * causes of infeasibility are the same, the numerical threshold is the only difference e.g. a doctor can work at most
 * 7 days in a row, but can only work at most 4 nights in a row
 */
sealed class DayNightInfeasibility(val sources: Set<Int>) {
    class RestAfterRow(sources: Set<Int>): DayNightInfeasibility(sources)
    class WouldCauseRowTooLarge(sources: Set<Int>): DayNightInfeasibility(sources)
    class InsufficientRest(sources: Set<Int>): DayNightInfeasibility(sources)
}

// Stores information relating to each MiddleGrade
class MiddleGrade(
    val id: Int,
    val grade: String,
    // Equal to [averageHours] if full time, a percentage of its value if part-time
    val targetHours: Double,
    val targetDayShifts: Int,
    val targetNightShifts: Int,
    val averageHoursDenominator: Double
) {
    var hoursWorked = 0.00
    var dayShiftsWorked = 0
    var nighShiftsWorked = 0
    var assignedAssignments = mutableListOf<Int>()

    fun varianceHoursWorked(): Double { return targetHours - (hoursWorked / averageHoursDenominator) }

    fun varianceDayShiftsWorked(): Int { return targetDayShifts - dayShiftsWorked }

    fun varianceNightShiftsWorked(): Int { return targetNightShifts - nighShiftsWorked }

    fun copy(): MiddleGrade {
        val doctor = MiddleGrade(id, grade, targetHours, targetDayShifts, targetNightShifts, averageHoursDenominator)
        doctor.hoursWorked = this.hoursWorked
        doctor.dayShiftsWorked = this.dayShiftsWorked
        doctor.nighShiftsWorked = this.nighShiftsWorked
        doctor.assignedAssignments = this.assignedAssignments.toMutableList()
        return doctor
    }

    override fun toString(): String { return "\nDoctor: $id\nGrade: $grade\nHours Worked: $averageHoursDenominator\nDay Shifts Worked: $dayShiftsWorked\nNight Shifts Worked: $nighShiftsWorked\n" }

    fun debug() {
        println(id)
        println(grade)
        println(targetHours)
        println(hoursWorked)
        println("$assignedAssignments\n")
    }
}

