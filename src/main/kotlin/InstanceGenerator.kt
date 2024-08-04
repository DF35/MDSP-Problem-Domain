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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Scanner
import kotlin.random.Random

class InstanceGenerator(private val rand: Random) {

    fun generateInstance(
        filename: String,
        department: Int,
        numWeeks: Int,
        numJunior: Int,
        numSenior: Int,
        percentageOnLeave: Double,
        percentagePartTime: Double,
        numTrainingGroups: Int
    ) {
        val instance = when(department) {
            //0 -> generateTestInstance(numWeeks)
            1 -> generateDepartment1(
                numWeeks, numJunior, numSenior, percentageOnLeave, percentagePartTime,
                numTrainingGroups
            )
            2 -> generateDepartment2(
                numWeeks, numJunior, numSenior, percentageOnLeave, percentagePartTime,
                numTrainingGroups
            )
            else -> throw Exception("generateInstance: Invalid value for [department] passed")
        }
        val writer = BufferedWriter(FileWriter("src/main/resources/instances/$filename"))
        writer.write(instance)
        writer.close()
    }

    private fun generateDepartment1(
        numWeeks: Int,
        numJunior: Int,
        numSenior: Int,
        percentageOnLeave: Double,
        percentagePartTime: Double,
        numTrainingGroups: Int
    ): String {
        val max = (7*numWeeks) * 4 - 1
        val genShifts = { i: Int -> listOf(i*4, i*4+1, i*4+2, i*4+3) }
        val genDaysEntry = { l: List<Int> -> Pair(l.subList(0,1), l.subList(1,4)) }
        val genTimesAndTypes = { i: Int -> listOf("\n8.0\n08:00-16:00\nday\n", "\n8.0\n14:00-22:00\nday\n",
            "\n8.0\n17:00-01:00 (${nextDay(i)})\nnight overlaps\n", "\n10.5\n22:00-08:30 (${nextDay(i)})\nnight overlaps\nnext\n") }
        val grades = listOf("any senior\n", "any\n", "any\n", "any senior\n")
        val elevenHoursFunctions = listOf(
            { i: Int -> listOf(i+1, i+2, i+3, i-1, i-2, i-3) },
            { i: Int -> listOf(i+1, i+2, i+3, i-1, i-2, i-3) },
            { i: Int -> listOf(i+1, i+2, i+3, i-1, i-2, i-3) },
            { i: Int -> listOf(i+1, i+2, i+3, i-1, i-2, i-3) }
        )
        val fortyEightHoursFunctions = listOf(
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7, i+8, i+9) },
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7, i+8, i+9) },
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7, i+8, i+9) },
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7, i+8, i+9) }
        )
        val longFortyEightBeforeFunctions = listOf(
            { _: Int -> emptyList<Int>() },
            { _: Int -> emptyList() },
            { _: Int -> emptyList() },
            { _: Int -> emptyList() }
        )
        val relevantShiftFunctions = listOf(
            { i: Int -> listOf(i-1, i-2, i-5, i-6, i-9) },
            { i: Int -> listOf(i-2, i-3, i-6, i-7) },
            { i: Int -> listOf(i+2, i+3, i+6, i+7) },
            { i: Int -> listOf(i+1, i+2, i+5, i+6, i+9) }
        )

        val (shiftInfo, days) = generateShiftInfo(
            numWeeks, max, genShifts, genDaysEntry, genTimesAndTypes, grades,
            elevenHoursFunctions, fortyEightHoursFunctions, longFortyEightBeforeFunctions,
            relevantShiftFunctions
        )
        val doctors = generateDoctorInfo(numJunior, numSenior, percentagePartTime, percentageOnLeave, days, listOf(8.0,8.0,8.0,10.5), numTrainingGroups)

        return "junior senior any\n47\n23 12\n$numWeeks\n${numJunior+numSenior}\n$doctors$shiftInfo"
    }

    private fun generateDepartment2(
        numWeeks: Int,
        numJunior: Int,
        numSenior: Int,
        percentageOnLeave: Double,
        percentagePartTime: Double,
        numTrainingGroups: Int
    ): String {
        val max = (7*numWeeks) *3 -1
        val genShifts = { i: Int -> listOf(i*3, i*3+1, i*3+2) }
        val genDaysEntry = { l: List<Int> -> Pair(l.subList(0,2), l.subList(2,3)) }
        val genTimesAndTypes = { i: Int -> listOf("\n9.5\n08:00-17:30\nday\n", "\n12.0\n08:00-20:00\nday\n",
            "\n13\n19:30-08:30 (${nextDay(i)})\nnight overlaps\nnext\n")}
        val grades = listOf("any senior\n", "any\n", "any\n")
        val elevenHoursFunctions = listOf(
            { i: Int -> listOf(i-1, i+1, i+2) },
            { i: Int -> listOf(i-2, i-1, i+1) },
            { i: Int -> listOf(i-2, i-1, i+1, i+2) }
        )
        val fortyEightHoursFunctions = listOf(
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7) },
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7) },
            { i: Int -> listOf(i+1, i+2, i+3, i+4, i+5, i+6, i+7, i+8) }
        )
        val longFortyEightBeforeFunctions = listOf(
            { i: Int -> listOf(i-1, i-2, i-4, i-5, i-7) },
            { i: Int -> listOf(i-2, i-3, i-5, i-6, i-8) },
            { i: Int -> listOf(i-1, i-3, i-4, i-6, i-7)}
        )
        val relevantShiftFunctions = listOf(
            { i: Int -> listOf(i-1, i-4, i-7) },
            { i: Int -> listOf(i-2, i-5, i-8) },
            { i: Int -> listOf(i+1, i+2, i+4, i+5, i+7, i+8) }
        )

        val (shiftInfo, days) = generateShiftInfo(
            numWeeks, max, genShifts, genDaysEntry, genTimesAndTypes, grades,
            elevenHoursFunctions, fortyEightHoursFunctions, longFortyEightBeforeFunctions,
            relevantShiftFunctions
        )
        val doctors = generateDoctorInfo(
            numJunior, numSenior, percentagePartTime, percentageOnLeave, days, listOf(9.5,12.0,13.0), numTrainingGroups
        )

        return "junior senior any\n47\n20 7\n$numWeeks\n${numJunior+numSenior}\n$doctors$shiftInfo"
    }

    private fun generateShiftInfo(
        numWeeks: Int,
        max: Int,
        genShifts: (Int) -> List<Int>,
        genDaysEntry: (List<Int>) -> Pair<List<Int>, List<Int>>,
        genTimesAndTypes: (Int) -> List<String>,
        grades: List<String>,
        elevenHoursFunctions: List<(Int) -> List<Int>>,
        fortyEightHoursFunctions: List<(Int) -> List<Int>>,
        longFortyEightBeforeFunctions: List<(Int) -> List<Int>>,
        relevantShiftFunctions: List<(Int) -> List<Int>>
    ): Pair<String, Map<Int, Pair<List<Int>,List<Int>>>> {
        var shiftInfo = ""
        val days = mutableMapOf<Int, Pair<List<Int>,List<Int>>>()

        for(day in 0..<7*numWeeks) {
            val shifts = genShifts(day)
            days[day] = genDaysEntry(shifts)
            val timesAndTypes = genTimesAndTypes(day)

            for((index, shiftID) in shifts.withIndex()) {
                shiftInfo += "$shiftID ${grades[index]}"
                for(id in elevenHoursFunctions[index](shiftID).filter { it in 0..max }) shiftInfo += "$id "
                shiftInfo = shiftInfo.dropLast(1) + "\n"

                val shiftsInFortyEight = fortyEightHoursFunctions[index](shiftID).filter { it in 0..max }
                when(shiftsInFortyEight.isEmpty()) {
                    true -> shiftInfo += "NULL\n"
                    false -> {
                        for(id in shiftsInFortyEight) shiftInfo += "$id "
                        shiftInfo = shiftInfo.dropLast(1) + "\n"
                    }
                }

                val longDayShifts48Before = longFortyEightBeforeFunctions[index](shiftID).filter { it in 0..max }
                when(longDayShifts48Before.isEmpty()) {
                    true -> shiftInfo += "NULL\n"
                    false -> {
                        for(id in longDayShifts48Before) shiftInfo += "$id "
                        shiftInfo = shiftInfo.dropLast(1) + "\n"
                    }
                }

                val otherRelevantShifts = relevantShiftFunctions[index](shiftID).filter { it in 0..max }
                when(otherRelevantShifts.isEmpty()) {
                    true -> shiftInfo += "NULL"
                    false -> {
                        for(id in otherRelevantShifts) shiftInfo += "$id "
                        shiftInfo = shiftInfo.dropLast(1)
                    }
                }
                shiftInfo += timesAndTypes[index]
            }
        }

        return Pair(shiftInfo, days)
    }

    private fun generateDoctorInfo(
        numJunior: Int,
        numSenior: Int,
        percentagePartTime: Double,
        percentageOnLeave: Double,
        days: Map<Int, Pair<List<Int>,List<Int>>>,
        shiftDurations: List<Double>,
        numTrainingGroups: Int
        ): String {
        val numPartTime = ((numJunior+numSenior) * percentagePartTime).toInt()
        val numOnLeave = ((numJunior+numSenior) * percentageOnLeave).toInt()
        val juniors = (0..<numJunior).toList()
        val allDoctors = (0..<numJunior+numSenior).toList()
        val onLeave = mutableListOf<Int>()
        val partTime = mutableListOf<Int>()

        val doctorsAvailableForLeave = allDoctors.toMutableList()
        for(x in 1..numOnLeave) {
            val doctor = allDoctors[rand.nextInt(doctorsAvailableForLeave.size)]
            onLeave.add(doctor)
            doctorsAvailableForLeave.remove(doctor)
        }

        // Doctors can be both on leave and part-time
        val doctorsAvailableToBePartTime = allDoctors.toMutableList()
        for(x in 1..numPartTime) {
            val doctor = allDoctors[rand.nextInt(doctorsAvailableToBePartTime.size)]
            partTime.add(doctor)
            doctorsAvailableToBePartTime.remove(doctor)
        }

        // Randomly assigns the doctors to training groups
        val numPerGroup = allDoctors.size / numTrainingGroups
        val trainingGroups = mutableMapOf<Int,Int>()
        val toAssign = allDoctors.toMutableList()
        for(group in 1..numTrainingGroups) {
            for (x in 1..numPerGroup) {
                val doctor = toAssign[rand.nextInt(toAssign.size)]
                trainingGroups[doctor] = group
                toAssign.remove(doctor)
            }
        }

        // Generates schedules for each group
        val trainingSchedules = generateTraining(days, numTrainingGroups, shiftDurations)
        val preferences = calculatePreferences(days, numJunior + numSenior, rand)

        var doctors = ""

        for(doctor in allDoctors) {
            val leave = when(onLeave.contains(doctor)) {
                false -> "0 NULL"
                true -> generateLeave(days, shiftDurations)
            }

            val group = trainingGroups[doctor] ?: throw Exception("generateDoctorInfo: Doctor not assigned training group")
            val training = trainingSchedules[group-1]
            val preferenceInfo = preferences[doctor]

            val grade = when(juniors.contains(doctor)) {
                false -> "senior "
                true -> "junior "
            }

            val percentage = when(partTime.contains(doctor)) {
                false -> "NO\n"
                true -> "80\n"
            }

            doctors += "$leave $training $preferenceInfo$grade$percentage"
        }

        return doctors
    }

    private enum class LeaveType {
        Large, Medium, Small, Single, Weekend
    }

    fun generateLeave(days: Map<Int, Pair<List<Int>, List<Int>>>, durations: List<Double>): String {
        val prob = rand.nextDouble()
        val type = when {
            prob < 0.36 -> LeaveType.Large
            prob < 0.59 -> LeaveType.Medium
            prob < 0.7 -> LeaveType.Small
            prob < 0.91 -> LeaveType.Single
            else -> LeaveType.Weekend
        }

        var startDay = rand.nextInt(days.size)
        var hoursOff = 0.0
        var shiftsOff = ""
        var bound = 0

        when(type) {
            // Leave of 2-3 weeks
            LeaveType.Large -> {
                val numDays = if(rand.nextDouble() < 0.5) 14 else 21
                bound = numDays + startDay
                if(bound > days.size - 1)
                    bound = days.size - 1
            }
            // Leave of 3-12 Days
            LeaveType.Medium -> {
                val sel = rand.nextDouble()
                val numDays = when {
                    sel < 0.1 -> 3
                    sel < 0.2 -> 4
                    sel < 0.3 -> 5
                    sel < 0.4 -> 6
                    sel < 0.5 -> 7
                    sel < 0.6 -> 8
                    sel < 0.7 -> 9
                    sel < 0.8 -> 10
                    sel < 0.9 -> 11
                    else -> 12
                }
                bound = numDays + startDay
                if(bound > days.size - 1)
                    bound = days.size - 1
            }
            // Leave of 2 days
            LeaveType.Small -> {
                bound = 2 + startDay
                if(bound > days.size - 1)
                    bound = days.size - 1
            }
            // Selects a random weekend to take as leave
            LeaveType.Weekend -> {
                val weeks = (days.size / 7) - 1
                startDay = rand.nextInt(weeks) * 7 + 5
                bound = startDay + 1
            }
            // Leave of 1 day
            LeaveType.Single -> bound = startDay
        }

        for(day in startDay..bound) {
            for(shift in days[day]!!.first + days[day]!!.second) {
                shiftsOff += "$shift,"
                hoursOff += durations[shift % durations.size]
            }
        }
        shiftsOff = shiftsOff.dropLast(1)

        return "$hoursOff $shiftsOff"
    }

    enum class TrainingType {
        OneDayPerWeek, TwoDaysPerMonth, OneDayPerMonth, RandomDay, FixedAfternoon2Weeks, WeeklyAfternoon
    }

    fun generateTraining(days: Map<Int, Pair<List<Int>, List<Int>>>, numGroups: Int, durations: List<Double>
    ): List<String> {
        val prob = rand.nextDouble()
        val type = when {
            prob < 0.22 -> TrainingType.OneDayPerWeek
            prob < 0.29 -> TrainingType.TwoDaysPerMonth
            prob < 0.43 -> TrainingType.OneDayPerMonth
            prob < 0.50 -> TrainingType.RandomDay
            prob < 0.64 -> TrainingType.FixedAfternoon2Weeks
            else -> TrainingType.WeeklyAfternoon
        }

        val numWeeks = days.size / 7


        /*
         * Maps group to a Pair containing the list of days and the increment for repetition
         * e.g. 1: ([1], 7) would indicate training each week on Tuesday for group 1
         */
        val assignedDays = mutableMapOf<Int, Pair<List<Int>,Int>>()

        when(type) {
            TrainingType.OneDayPerWeek -> {
                // We assume that training does not occur on weekends
                val availableDays = mutableListOf(0,1,2,3,4)
                for(group in 1..numGroups) {
                    val day = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day)
                    assignedDays[group] = Pair(listOf(day), 7)
                }
            }
            TrainingType.TwoDaysPerMonth -> {
                val availableDays = mutableListOf(0,1,2,3,4,7,8,9,10,11,14,15,16,17,18,21,22,23,24,25)
                for(group in 1..numGroups) {
                    val day1 = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day1)
                    val day2 = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day2)
                    assignedDays[group] = Pair(listOf(day1, day2).filter { it < days.size }, 28)
                }
            }
            TrainingType.OneDayPerMonth -> {
                val availableDays = mutableListOf(0,1,2,3,4,7,8,9,10,11,14,15,16,17,18,21,22,23,24,25)
                for(group in 1..numGroups) {
                    val day = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day)
                    assignedDays[group] = Pair(listOf(day).filter { it < days.size }, 28)
                }
            }
            TrainingType.RandomDay -> {
                val daysList = List(numGroups) { mutableListOf<Int>() }
                for(week in 1..numWeeks) {
                    val availableDays = mutableListOf(0,1,2,3,4)
                    for(group in 1..numGroups) {
                        val day = availableDays[rand.nextInt(availableDays.size)]
                        availableDays.remove(day)
                        daysList[group-1].add(day)
                    }
                }
                for(group in 1..numGroups)
                    assignedDays[group] = Pair(daysList[group-1], 7)
            }
            TrainingType.FixedAfternoon2Weeks -> {
                val availableDays = mutableListOf(0,1,2,3,4,7,8,9,10,11)
                for(group in 1..numGroups) {
                    val day = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day)
                    assignedDays[group] = Pair(listOf(day), 14)
                }
            }
            TrainingType.WeeklyAfternoon -> {
                val availableDays = mutableListOf(0,1,2,3,4)
                for(group in 1..numGroups) {
                    val day = availableDays[rand.nextInt(availableDays.size)]
                    availableDays.remove(day)
                    assignedDays[group] = Pair(listOf(day), 7)
                }
            }
        }

        /*
         * Generates strings of shifts that the doctors in each group will have and adds
         * them to [trainingShifts] (index corresponds to group number)
         */
        val shiftGetter = when(type) {
            TrainingType.FixedAfternoon2Weeks -> { i: Int -> days[i]!!.first }
            TrainingType.WeeklyAfternoon -> { i: Int -> days[i]!!.first }
            else -> { i: Int -> days[i]!!.first + days[i]!!.second }
        }

        // Special case that needs to be handled differently
        if(type == TrainingType.RandomDay) {
            val trainingShifts = mutableListOf<String>()
            for((trainingDays, increment) in assignedDays.values) {
                var shifts = ""
                var hoursTraining = 0.00
                for((week, day) in trainingDays.withIndex()) {
                    for(shift in shiftGetter(week * increment + day)) {
                        shifts += "$shift,"
                        hoursTraining += durations[shift % durations.size]
                    }
                }
                shifts = "$hoursTraining " + shifts.dropLast(1)
                trainingShifts.add(shifts)
            }

            return trainingShifts
        }

        val trainingShifts = mutableListOf<String>()
        for((startDays, increment) in assignedDays.values) {
            var shifts = ""
            var hoursTraining = 0.00
            val bound = days.size
            for(day in startDays) {
                var currentDay = day
                // We ensure that the startDays are within the bound when selecting them
                do {
                    for(shift in shiftGetter(currentDay)) {
                        shifts += "$shift,"
                        hoursTraining += durations[shift % durations.size]
                    }
                    currentDay += increment
                } while(currentDay < bound)
            }
            shifts = "$hoursTraining " + shifts.dropLast(1)
            trainingShifts.add(shifts)
        }

        return trainingShifts
    }

    fun calculatePreferences(
        days: Map<Int, Pair<List<Int>, List<Int>>>,
        numDoctors: Int,
        rand: Random
    ): List<String> {
        val file = File("src/main/resources/doctor_preferences.txt")
        val scanner = Scanner(file)
        val prefTypes = mutableListOf<String>()
        while(scanner.hasNextLine())
            prefTypes.add(scanner.nextLine())

        val numWeeks = days.size / 7

        val preferences = mutableListOf<String>()
        for(i in 0..numDoctors) {
            val prefType = prefTypes[rand.nextInt(prefTypes.size)]
            val components = prefType.split("\\s+".toRegex())
            val specificDayOrNight = when(components[2]) {
                "None" -> "NULL "

                "Weekday" -> {
                    var shifts = ""
                    for(w in 0..<numWeeks) {
                        val day = rand.nextInt(7)
                        for(shift in days[w*7+day]!!.first)
                            shifts += "$shift,"
                    }
                    shifts = shifts.dropLast(1)
                    "$shifts "
                }

                "Night" -> {
                    var shifts = ""
                    for(w in 0..<numWeeks) {
                        val day = rand.nextInt(7)
                        for(shift in days[w*7+day]!!.second)
                            shifts += "$shift,"
                    }
                    shifts = shifts.dropLast(1)
                    "$shifts "
                }

                "Sunday" -> {
                    var shifts = ""
                    for(w in 0..<numWeeks) {
                        for(shift in days[w*7+6]!!.second)
                            shifts += "$shift,"
                    }
                    shifts = shifts.dropLast(1)
                    "$shifts "
                }

                else -> throw Exception("calculatePreferences: Invalid preference file used")
            }

            preferences.add("${components[0]} ${components[1]} $specificDayOrNight")
        }

        return preferences
    }

    private fun nextDay(day: Int): String {
        return when (day % 7) {
            0 -> "Tuesday"
            1 -> "Wednesday"
            2 -> "Thursday"
            3 -> "Friday"
            4 -> "Saturday"
            5 -> "Sunday"
            else -> "Monday"
        }
    }

    // A very simply toy instance that was used to test functions during the development process
    private fun generateTestInstance(numWeeks: Int): String {
        val max = (7*numWeeks) *2 -1
        val calcShiftsWithin11Hours = { i: Int -> arrayOf(i-1, i+1).filter{ j: Int -> j in 0..max }.toSet() }
        val calcNights48HoursBefore = { i: Int -> arrayOf(i-3, i-1).filter{ j: Int -> j in 0..max }.toSet() }
        val calcDayShifts48HoursAfter = { i: Int -> arrayOf(i+1, i + 3).filter{ j: Int -> j in 0..max }.toSet() }

        var string = ""

        for(day in 0..<7*numWeeks) {
            val idDayShift = day * 2
            val shiftsWithin11HoursDayShift = calcShiftsWithin11Hours(idDayShift)
            val nights48HoursBefore = calcNights48HoursBefore(idDayShift)
            val idNightShift = day * 2 + 1
            val shiftsWithin11HoursNightShift = calcShiftsWithin11Hours(idNightShift)
            val days48HoursAfter = calcDayShifts48HoursAfter(idNightShift)

            // DayShift
            string += "$idDayShift any any\n"
            for(id in shiftsWithin11HoursDayShift) string += "$id "
            string += "\n"
            when(nights48HoursBefore.isEmpty()) {
                true -> string += "NULL"
                false -> for(id in nights48HoursBefore) string += "$id "
            }
            string += "\n10\n8:00-18:00\nday\n"

            // NightShift
            string += "$idNightShift any senior\n"
            for(id in shiftsWithin11HoursNightShift) string += "$id "
            string += "\n"
            when(days48HoursAfter.isEmpty()) {
                true -> string += "NULL"
                false -> for(id in days48HoursAfter) string += "$id "
            }
            string += "\n12\n18:00-6:00 (${nextDay(day)})\nnight\nnext\n"
        }
        return string
    }
}