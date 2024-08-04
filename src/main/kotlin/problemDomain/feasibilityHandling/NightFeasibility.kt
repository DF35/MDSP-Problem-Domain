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
package problemDomain.feasibilityHandling

import problemDomain.*


// Updates the feasibility of relevant shifts after a day shift is allocated or deallocated
fun updateFeasibilityNightShift(
    solutionData: SolutionData,
    shift: NightShift,
    doctor: Int,
    allocate: Boolean
) {
    val days = solutionData.days
    val shifts = solutionData.shifts
    /*
     * The conditions for adding or removing feasibility are contingent on the same boolean checks,
     * the actions are merely reversed, depending on the value of [allocate]; defining two lambda
     * expressions allows the conditional code to be reused as the correct action can be assigned as required
     */
    val feasibilityAction1: (Shift, Int) -> Unit
    val feasibilityAction2: (Shift, Int) -> Unit

    // The action applied to each shift depends on whether [shift] has been allocated or deallocated
    when(allocate) {
        true -> {
            // Shifts are made infeasible as a result of the allocation
            for(shiftID in shift.dayShifts48HoursAfter.union(shift.shiftsWithin11Hours))
                shifts[shiftID].restInfeasibility(doctor, Source.ShiftWorked(shift.id))

            days[shift.day].doctorsWorkingNight[doctor] = shift.id
            val row = consecutiveNightCheck(solutionData, doctor, shift.day)

            // Maximum number of nights allowed in a row
            if(row.size == 4) {
                val lastShift = days[row.max()].doctorsWorkingNight[doctor]
                    ?: throw Exception("updateFeasibilityNightShift: doctor $doctor absent from doctorsWorkingNight of day ${days[row.max()].id}")

                // Makes all shifts within 48 hours of the end of the last night shift infeasible
                for(shiftID in shifts[lastShift].shifts48HoursAfter)
                    shifts[shiftID].restInfeasibility(doctor, Source.RowOfNights(row))

                // Makes shifts the night before the row infeasible
                val prevNight = row.min() - 1
                if(prevNight in days.indices)
                    for(shiftID in days[prevNight].getNightShifts())
                        shifts[shiftID].restInfeasibility(doctor, Source.RowOfNights(row))
            }

            feasibilityAction1 = { s: Shift, workedID:Int -> s.removeSource(doctor, Source.ShiftWorked(workedID)) }
            feasibilityAction2 = { s: Shift, id: Int -> s.restInfeasibility(doctor, Source.ShiftWorked(id)) }
        }
        false -> {
            // [shift] will no longer be a source of infeasibility for the relevant shifts
            for(shiftID in shift.dayShifts48HoursAfter.union(shift.shiftsWithin11Hours))
                shifts[shiftID].removeSource(doctor, Source.ShiftWorked(shift.id))

            val row = consecutiveNightCheck(solutionData, doctor, shift.day)

            /*
             * Means that the night worked was previously part of a row of maximum size,
             * shifts will have been given a source of infeasibility that now needs to be
             * removed
             */
            if(row.size == 4) {
                // The shift at the end of the former row
                val lastShift = days[row.max()].doctorsWorkingNight[doctor]
                    ?: throw Exception("updateFeasibilityNightShift: doctor $doctor absent from doctorsWorkingNight of day ${days[row.max()].id}")

                val priorDay = row.min() - 1
                val priorDayShifts = when(priorDay in days.indices) {
                    true -> days[priorDay].getNightShifts()
                    false -> emptyList()
                }

                for(shiftID in shifts[lastShift].shifts48HoursAfter + priorDayShifts) {
                    /*
                     * - Check that the cause of infeasibility exists - throw exception if not
                     * - Check that the cause of infeasibility is REST - continue if not
                     * - Find the RowOfNights source that has an equivalent set of sources
                     */
                    val infeasibility = shifts[shiftID].causesOfInfeasibility[doctor]
                    when {
                        infeasibility == null -> throw Exception("updateFeasibilityNightShift: Doctor $doctor has no infeasibility for shift $shiftID")
                        infeasibility.cause == Cause.Leave -> continue
                        infeasibility.cause == Cause.Training -> continue
                        else -> {
                            val nightRowSources = infeasibility.sources.filterIsInstance<Source.RowOfNights>()
                                .filter { it.days == row }
                            if(nightRowSources.isEmpty())
                                throw Exception("updateFeasibilityNightShift: Shift $shiftID lacks night row infeasibility $row for doctor $doctor")
                            for(source in nightRowSources)
                                shifts[shiftID].removeSource(doctor, source)
                        }
                    }
                }
            }

            days[shift.day].doctorsWorkingNight.remove(doctor)

            feasibilityAction1 = { s: Shift, workedID:Int -> s.restInfeasibility(doctor, Source.ShiftWorked(workedID)) }
            feasibilityAction2 = { s: Shift, id: Int -> s.removeSource(doctor, Source.ShiftWorked(id)) }
        }
    }

    // Calculates the days that need to be checked
    data class Quadruple(val first: Int, val second: Int, val third: Int, val fourth: Int)
    val calcDays = { i: Int -> Quadruple(i - 1, i - 2, i + 1, i + 2) }
    val (prevDay, priorDay, nextDay, subsequentDay) = calcDays(shift.day)

    // Boolean conditions for the feasibility actions to be called
    val prevInIndices = prevDay in days.indices
    val workingPrevNight = if(prevInIndices) days[prevDay].doctorsWorkingNight.contains(doctor) else false
    val priorInIndices = priorDay in days.indices
    val nextInIndices = nextDay in days.indices
    val workingNextNight = if(nextInIndices) days[nextDay].doctorsWorkingNight.contains(doctor) else false
    val subInIndices = subsequentDay in days.indices

    if(prevInIndices && workingPrevNight && nextInIndices) {
        val workedShift = days[prevDay].getNightShifts().first { shifts[it].assignees.contains(doctor) }
        for (shiftID in days[nextDay].getNightShifts())
            feasibilityAction1(shifts[shiftID], workedShift)
    }

    if(subInIndices && !workingNextNight)
        for(shiftID in days[subsequentDay].getNightShifts())
            feasibilityAction2(shifts[shiftID], shift.id)

    if(nextInIndices && workingNextNight && prevInIndices) {
        val workedShift = days[nextDay].getNightShifts().first { shifts[it].assignees.contains(doctor) }
        for(shiftID in days[prevDay].getNightShifts())
            feasibilityAction1(shifts[shiftID], workedShift)
    }

    if(priorInIndices && !workingPrevNight)
        for(shiftID in days[priorDay].getNightShifts())
            feasibilityAction2(shifts[shiftID], shift.id)
}

// Checks for a consecutive row of nightsWorked and returns the set of days within this row
private fun consecutiveNightCheck(solutionData: SolutionData, doctor: Int, day: Int): Set<Int> {
    val days = solutionData.days
    val row = mutableSetOf(day)

    /*
     * Checks for worked nights to the left and right of [day], adding them to [block].
     * Stops checking in a direction as soon as [toCheck] is not worked by [doctor]
     */
    val nextFunctions = listOf({ i: Int -> i + 1 }, { i: Int -> i - 1 })
    for(next in nextFunctions) {
        var toCheck = next(day)
        while(toCheck in days.indices) {
            when(days[toCheck].doctorsWorkingNight.contains(doctor)) {
                false -> break
                true -> row.add(toCheck)
            }
            toCheck = next(toCheck)
        }
    }

    if(row.size > 4) throw Exception("consecutiveNightCheck: infeasible block size reached")

    return row
}