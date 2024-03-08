import problemDomain.*

class Generator {
    val allDoctors = mutableSetOf(0,1,2,3,4,5)
    val juniors = setOf(0,1,2)
    val seniors = setOf(3,4,5)

    fun generateShiftsAndDays(numDays: Int): Pair<List<Shift>,List<Day>> {
        val max = (numDays-1) * 2 + 1
        tailrec fun recurse(days: MutableList<Day>, shifts: MutableList<Shift>, prev: Int) {
            if(prev == 0) return
            val day = numDays - prev
            val idDayShift = day * 2
            val idNightShift = day * 2 + 1
            val shiftsWithin11Hours = {i: Int -> arrayOf(i-1, i+1).filter{j: Int -> j in 0..max}.toSet()}
            val nights48HoursBefore = {i: Int -> arrayOf(i-3, i-1).filter{j: Int -> j in 0..max}.toSet()}
            val dayShifts48HoursAfter = {i: Int -> arrayOf(i+1, i + 3).filter{j: Int -> j in 0..max}.toSet()}
            val assignmentIds = {i: Int -> intArrayOf(i*2, i*2+1)}

            days.add(Day(day, setOf(idDayShift), setOf(idNightShift)))
            shifts.add(
                DayShift(
                    idDayShift,
                    assignmentIds(idDayShift),
                    shiftsWithin11Hours(idDayShift),
                    nights48HoursBefore(idDayShift),
                    day,
                    allDoctors.toMutableSet(),
                    10
                )
            )
            shifts.add(
                NightShift(
                    idNightShift,
                    assignmentIds(idNightShift),
                    shiftsWithin11Hours(idNightShift),
                    dayShifts48HoursAfter(idNightShift),
                    day,
                    allDoctors.toMutableSet(),
                    10
                )
            )
            recurse(days, shifts, prev - 1)
        }

        val days = mutableListOf<Day>()
        val shifts = mutableListOf<Shift>()
        recurse(days, shifts, numDays)
        return Pair(shifts, days)
    }

    fun generateAssignments(shifts: List<Shift>): List<Assignment> {
        fun genAssignmentsShift(list: MutableList<Assignment>, shift: Shift, senior: Boolean) {
            val grades = when(senior) {
                true -> arrayOf("Any", "Senior")
                false -> arrayOf("Any", "Any")
            }

            shift.assignmentIDs.forEachIndexed { index, id ->
                list.add(Assignment(id, shift.id, grades[index], getInfeasibleDoctors(grades[index])))
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

    private fun getInfeasibleDoctors(grade: String): Set<Int> {
        return when(grade) {
            "Senior" -> juniors
            "Junior" -> seniors
            else -> emptySet()
        }
    }
}