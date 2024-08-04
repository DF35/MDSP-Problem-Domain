import problemDomain.*
import java.util.*

class Generator {
    val allDoctors = mutableSetOf(0,1,2,3,4,5)

    fun generateSolution(numDays: Int): Solution {
        val doctors = generateDoctors()
        val(shifts, days) = generateShiftsAndDays(numDays)
        val assignments = generateAssignments(shifts)
        val data = SolutionData(assignments, shifts, doctors, days)

        return Solution(Random(202202), data)
    }

    private fun generateDoctors(): List<MiddleGrade> {
        val doctors = mutableListOf<MiddleGrade>()

        for(id in allDoctors) {
            doctors.add(
                MiddleGrade(id, "senior", 40.0, 12, 8,
                    1.0, emptySet(), 1..7,1..4)
            )
        }

        return doctors
    }

    private fun generateShiftsAndDays(numDays: Int): Pair<List<Shift>,List<Day>> {
        val max = (numDays-1) * 2 + 1
        tailrec fun recurse(days: MutableList<Day>, shifts: MutableList<Shift>, prev: Int) {
            if(prev == 0) return
            val day = numDays - prev
            val idDayShift = day * 2
            val idNightShift = day * 2 + 1
            val shiftsWithin11Hours = { i: Int -> arrayOf(i-1, i+1).filter{ j: Int -> j in 0..max }.toSet() }
            val shifts48HoursAfter = { i: Int -> arrayOf(i+1, i+2, i+3, i+4).filter { j: Int -> j in 0..max } }
            val nights48HoursBefore = { i: Int -> arrayOf(i-3, i-1).filter{ j: Int -> j in 0..max }.toSet() }
            val dayShifts48HoursAfter = { i: Int -> arrayOf(i+1, i + 3).filter{ j: Int -> j in 0..max }.toSet() }
            val assignmentIds = {i: Int -> intArrayOf(i*2, i*2+1)}

            days.add(Day(day, listOf(idDayShift), emptyList(), listOf(idNightShift), emptyList()))
            shifts.add(
                DayShift(
                    idDayShift,
                    assignmentIds(idDayShift),
                    shiftsWithin11Hours(idDayShift),
                    shifts48HoursAfter(idDayShift),
                    emptyList(),
                    nights48HoursBefore(idDayShift),
                    day,
                    allDoctors.toMutableSet(),
                    10.0
                )
            )
            shifts.add(
                NightShift(
                    idNightShift,
                    assignmentIds(idNightShift),
                    shiftsWithin11Hours(idNightShift),
                    shifts48HoursAfter(idNightShift),
                    emptyList(),
                    dayShifts48HoursAfter(idNightShift),
                    day,
                    allDoctors.toMutableSet(),
                    10.0,
                    true
                )
            )
            recurse(days, shifts, prev - 1)
        }

        val days = mutableListOf<Day>()
        val shifts = mutableListOf<Shift>()
        recurse(days, shifts, numDays)
        return Pair(shifts, days)
    }

    private fun generateAssignments(shifts: List<Shift>): List<Assignment> {
        fun genAssignmentsShift(list: MutableList<Assignment>, shift: Shift, senior: Boolean) {
            val grades = arrayOf("Any", "Any")
            shift.assignmentIDs.forEachIndexed { index, id ->
                list.add(Assignment(id, shift.id, grades[index], emptySet()))
            }
        }
        val assignments = mutableListOf<Assignment>()
        for(shift in shifts)
            when(shift) {
                is DayShift -> genAssignmentsShift(assignments, shift, false)
                is NightShift -> genAssignmentsShift(assignments, shift, true)
            }
        return assignments
    }
}