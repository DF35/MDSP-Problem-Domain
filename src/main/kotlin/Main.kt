import fieldExperimentInterface.ExperimentGUI
import hyperheuristic.acceptance.AcceptanceCriterionType
import hyperheuristic.hh.GIHH
import hyperheuristic.selection.SelectionMethodType
import hyperheuristic.util.WriteInfo
import javafx.application.Application
import problemDomain.MDSP
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.random.Random

fun main(args: Array<String>) {
    // If "--instance" is passed as an argument, we run the instance gui
    if (args.isNotEmpty() && args.first() == "--instance") {
        val gui = ExperimentGUI()
        Application.launch(gui::class.java)
        return
    }

    // Code for Instance Experimentation
    val seedGenerator = Random(22032024)
    val totalExecutionTime: Long = 60000 * 15 // 15 minutes
    val selectionType = SelectionMethodType.AdaptiveLimitedLAassistedDHSMentorSTD
    val acceptanceType = AcceptanceCriterionType.AdaptiveIterationLimitedListBasedTA
    val resultFileName = "GIHH_"
    val today = Date()
    val dateFormatter = SimpleDateFormat("ddMMyyyyHHmmss")
    WriteInfo.resultSubFolderName = dateFormatter.format(today)

    /*for(minutes in listOf(15)) {
        val execTime = (minutes * 60000).toLong()
        for(h in 1..2) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, execTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(2)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = execTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department1_easy_${minutes}_minutes-$h.txt"))
            var assignments = ""
            for (assignment in problem.bestSolution.data.assignments)
                if (assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }

        for(h in 1..2) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, execTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(3)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = execTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department1_hard_${minutes}_minutes-$h.txt"))
            var assignments = ""
            for (assignment in problem.bestSolution.data.assignments)
                if (assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }

        for(h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, execTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(4)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = execTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department2_easy_${minutes}_minutes-$h.txt"))
            var assignments = ""
            for (assignment in problem.bestSolution.data.assignments)
                if (assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)

        }

        for(h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, execTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(5)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = execTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department2_hard_${minutes}_minutes-$h.txt"))
            var assignments = ""
            for (assignment in problem.bestSolution.data.assignments)
                if (assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }
    }*/

    val instances = listOf(
        "department1_baseline",
        /*"department2_baseline",
        "leave_partTime_mix/department1_20Percent",
        "leave_partTime_mix/department1_40Percent",
        "leave_partTime_mix/department1_60Percent",
        "leave_partTime_mix/department1_80Percent",
        "leave_partTime_mix/department2_20Percent",
        "leave_partTime_mix/department2_40Percent",
        "leave_partTime_mix/department2_60Percent",
        "leave_partTime_mix/department2_80Percent",
        "leave_tests/department1_20PercentLeave",
        "leave_tests/department1_40PercentLeave",
        "leave_tests/department1_60PercentLeave",
        "leave_tests/department1_80PercentLeave",*/
        "leave_tests/department2_20PercentLeave",
        "leave_tests/department2_40PercentLeave",
        "leave_tests/department2_60PercentLeave",
        "leave_tests/department2_80PercentLeave",
        /*"partTime_tests/department1_20PercentPartTime",
        "partTime_tests/department1_40PercentPartTime",
        "partTime_tests/department1_60PercentPartTime",
        "partTime_tests/department1_80PercentPartTime",*/
        "partTime_tests/department2_20PercentPartTime",
        "partTime_tests/department2_40PercentPartTime",
        "partTime_tests/department2_60PercentPartTime",
        "partTime_tests/department2_80PercentPartTime",
        /*"training_schedules/department1_fixedAfternoon2Weeks",
        "training_schedules/department1_oneDayPerMonth",
        "training_schedules/department1_oneDayPerWeek",
        "training_schedules/department1_randomDay",
        "training_schedules/department1_twoDaysPerMonth",
        "training_schedules/department1_weeklyAfternoon",*/
        "training_schedules/department2_fixedAfternoon2Weeks",
        "training_schedules/department2_oneDayPerMonth",
        "training_schedules/department2_oneDayPerWeek",
        "training_schedules/department2_randomDay",
        "training_schedules/department2_twoDaysPerMonth",
        "training_schedules/department2_weeklyAfternoon",
        /*"understaffing_tests/department1_1Junior",
        "understaffing_tests/department1_1Junior_1Senior",
        "understaffing_tests/department1_1Senior",
        "understaffing_tests/department1_2Junior",
        "understaffing_tests/department1_2Senior",*/
        "understaffing_tests/department2_1Junior",
        "understaffing_tests/department2_1Junior1Senior",
        "understaffing_tests/department2_1Senior",
        "understaffing_tests/department2_2Junior",
        "understaffing_tests/department2_2Senior"
    )

    for(instance in instances) {
        println(instance)

        for (h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, totalExecutionTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(instance)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = totalExecutionTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/$instance-${h}.txt"))
            var assignments = ""
            for (assignment in problem.bestSolution.data.assignments)
                if (assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }
    }


    /*for(i in 1..1) {
        val seedGenerator = Random(25022024)
        val seed = seedGenerator.nextLong()
        val problem = MDSP(seed)
        problem.loadInstance("test")
        val solution = problem.blankSolution()

        val log = File("results/test.txt")
        val scanner = Scanner(log)
        var lineNum = 0
        while (scanner.hasNextLine()) {
            lineNum++
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            when (tokens[0]) {
                "al" -> solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
                "de" -> solution.deallocateAssignment(tokens[1].toInt())
            }
        }
        for(doctor in solution.data.doctors) {
            println(doctor.hoursWorked)
            println(doctor.varianceHoursWorked())
        }
        solution.calculateObjectiveValue()
        println(solution.descriptiveObjectiveFunction(true))
    }*/

    /*val writer = BufferedWriter(FileWriter("results/field_study/graph_data/comparative_descriptive.csv"))
    writer.write("Instance/Method,Total Coverage Contribution,Total Doctor Target Contribution,Total Preference Contribution\n")

    for (i in 1..8) {
        val problem = MDSP(25032024)
        problem.loadInstance(2)
        val solution = problem.blankSolution()

        val log = File("results/field_study/dep1_easy-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("1-easy-man,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for(i in 1..5) {
        val problem = MDSP(25032024)
        problem.loadInstance(2)
        val solution = problem.blankSolution()

        val log = File("results/time_limit/experiment_department1_easy_15_minutes-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("1-easy-aut,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for (i in 1..6) {
        val problem = MDSP(25032024)
        problem.loadInstance(3)
        val solution = problem.blankSolution()

        val log = File("results/field_study/dep1_hard-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("1-hard-man,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for(i in 1..5) {
        val problem = MDSP(25032024)
        problem.loadInstance(3)
        val solution = problem.blankSolution()

        val log = File("results/time_limit/experiment_department1_hard_15_minutes-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("1-hard-aut,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for (i in 1..6) {
        val problem = MDSP(25032024)
        problem.loadInstance(4)
        val solution = problem.blankSolution()

        val log = File("results/field_study/dep2_easy-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("2-easy-man,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for(i in 1..5) {
        val problem = MDSP(25032024)
        problem.loadInstance(4)
        val solution = problem.blankSolution()

        val log = File("results/time_limit/experiment_department2_easy_15_minutes-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("2-easy-aut,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for (i in 1..8) {
        val problem = MDSP(25032024)
        problem.loadInstance(5)
        val solution = problem.blankSolution()

        val log = File("results/field_study/dep2_hard-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("2-hard-man,${solution.descriptiveObjectiveFunction(false)}\n")
    }

    for(i in 1..5) {
        val problem = MDSP(25032024)
        problem.loadInstance(5)
        val solution = problem.blankSolution()

        val log = File("results/time_limit/experiment_department2_hard_15_minutes-$i.txt")
        val scanner = Scanner(log)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }
        solution.calculateObjectiveValue()
        println(solution.objectiveValue)
        writer.write("2-hard-aut,${solution.descriptiveObjectiveFunction(false)}\n")
    }
    writer.close()*/

    /*val writer = BufferedWriter(FileWriter("results/understaffing_tests/graph_data/department2_2Senior.csv"))
    writer.write("Objective Function, Total Coverage Contribution, Total Doctor Target Contribution, Total Preference Contribution\n")

    for(i in 1..5) {
        val problem = MDSP(25032024)
        problem.loadInstance("understaffing_tests/department2_2Senior")
        val solution = problem.blankSolution()

        val log = File("results/understaffing_tests/department2_2Senior-$i.txt")
        val scanner = Scanner(log)
        while(scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val tokens = line.split(" ")
            solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
        }

        writer.write("${solution.descriptiveObjectiveFunction(false)}\n")
    }
    writer.close()*/

    /*val generator = InstanceGenerator(Random(12042024))
    generator.generateInstance("test.txt", 1, 8, 6, 6, 0.2, 0.2, 4)*/

}
