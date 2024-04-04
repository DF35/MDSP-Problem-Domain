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
import kotlin.random.Random

fun main(args: Array<String>) {
    // If "--instance" is passed as an argument, we run the instance gui
    if (args.isNotEmpty() && args.first() == "--instance") {
        val gui = ExperimentGUI()
        Application.launch(gui::class.java)
        return
    }

    val gen = InstanceGenerator(Random(25032024))
    gen.generateInstance("experiment_department1_easy.txt", 1, 8, 5, 5, 0.2, 0.2, 5)
    //gen.generateInstance("department2_baseline.txt", 2)

    /*val pd = MDSP(25032024)
    pd.loadInstance(6)
    val solution = pd.blankSolution()
    //solution.allocateAssignment(2, 0)
    //solution.allocateAssignment(6, 1)
    solution.allocateAssignment(10, 1)
    solution.allocateAssignment(14, 1)
    //solution.allocateAssignment(18, 0)
    solution.allocateAssignment(22, 1)
    solution.allocateAssignment(25, 1)
    //solution.allocateAssignment(30, 0)
    //solution.allocateAssignment(34, 0)

    //solution.deallocateAssignment(21)
    //solution.allocateAssignment(26, 0)
    //solution.deallocateAssignment(30)


    solution.debug()*/

    /*val seedGenerator = Random(22032024)
    var totalExecutionTime: Long = 0
    val toAdd = 60000
    val selectionType = SelectionMethodType.AdaptiveLimitedLAassistedDHSMentorSTD
    val acceptanceType = AcceptanceCriterionType.AdaptiveIterationLimitedListBasedTA
    val resultFileName = "GIHH_"
    val today = Date()
    val dateFormatter = SimpleDateFormat("ddMMyyyyHHmmss")
    WriteInfo.resultSubFolderName = dateFormatter.format(today)

    for(i in 1..10) {
        totalExecutionTime += toAdd

        for (h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, totalExecutionTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(2)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = totalExecutionTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department1_easy_${i}-minutes_${h}.txt"))
            var assignments = ""
            for(assignment in problem.bestSolution.data.assignments)
                if(assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }

        for (h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, totalExecutionTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(3)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = totalExecutionTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department1_hard_${i}_minutes-${h}.txt"))
            var assignments = ""
            for(assignment in problem.bestSolution.data.assignments)
                if(assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }

        for (h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, totalExecutionTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(4)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = totalExecutionTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department2_easy_${i}_minutes-${h}.txt"))
            var assignments = ""
            for(assignment in problem.bestSolution.data.assignments)
                if(assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }

        for (h in 1..5) {
            val seed = seedGenerator.nextLong()
            val problem = MDSP(seed)
            val hyperHeuristic = GIHH(
                seed, problem.numberOfHeuristics, totalExecutionTime,
                resultFileName, selectionType, acceptanceType
            )

            problem.loadInstance(5)
            problem.initialiseSolution(0)
            hyperHeuristic.timeLimit = totalExecutionTime
            hyperHeuristic.loadProblemDomain(problem)
            hyperHeuristic.run()
            val writer = BufferedWriter(FileWriter("results/time_limit/experiment_department2_hard_${i}_minutes-${h}.txt"))
            var assignments = ""
            for(assignment in problem.bestSolution.data.assignments)
                if(assignment.assignee != null)
                    assignments += "al ${assignment.id} ${assignment.assignee}\n"
            writer.write(assignments)
            writer.close()
            println(problem.bestSolutionValue)
        }
    }*/

    /*val writer = BufferedWriter(FileWriter("results/time_limit/graph_data/experiment_department2_hard.csv"))
    writer.write("Objective_Function,Minutes\n")

    for(i in listOf(5, 10, 15, 20)) {
        for (h in 1..5) {
            val problem = MDSP(25032024)
            problem.loadInstance(5)
            val solution = problem.blankSolution()

            val log = File("results/time_limit/experiment_department2_hard_${i}_minutes-${h}.txt")
            val scanner = Scanner(log)
            var lineNum = 0
            while(scanner.hasNextLine()) {
                lineNum++
                val line = scanner.nextLine()
                val tokens = line.split(" ")
                solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
            }

            /*println(solution.descriptiveObjectiveFunction(true))
            println("\n\n\n")*/
            solution.calculateObjectiveValue()
            writer.write("${solution.objectiveValue},$i\n")
        }
    }

    writer.close()*/

    // Debugging a doctor log file
    /*val seedGenerator = Random(25022024)
    val seed = seedGenerator.nextLong()
    val problem = MDSP(seed)
    problem.loadInstance(3)
    val solution = problem.blankSolution()

    val log = File("results/field_study/dep2_easy_2024-04-01_13-42.txt")
    val scanner = Scanner(log)
    var lineNum = 0
    while(scanner.hasNextLine()) {
        lineNum++
        val line = scanner.nextLine()
        val tokens = line.split(" ")
        when(tokens[0]) {
            "al" -> solution.allocateAssignment(tokens[1].toInt(), tokens[2].toInt())
            "de" -> solution.deallocateAssignment(tokens[1].toInt())
        }
    }
    solution.calculateObjectiveValue()
    println(solution.objectiveValue)*/

    /*val problem = MDSP(25032024)
    problem.loadInstance(1)
    val solution = problem.blankSolution()
    solution.calculateObjectiveValue()
    println(solution.nightRangeViolations)
    //solution.nightRangeViolations[3] = 6
    println(solution.calculatePreferenceDisparity())*/


    /*//solution.allocateAssignment(0, 3)

    //solution.allocateAssignment(4, 3)
    //solution.allocateAssignment(9, 3)
    //solution.allocateAssignment(14, 3)
    solution.allocateAssignment(18, 3)
    solution.allocateAssignment(22, 3)
    solution.allocateAssignment(26, 3)
    solution.allocateAssignment(30, 3)
    solution.allocateAssignment(34, 3)
    //solution.allocateAssignment(38, 3)
    solution.allocateAssignment(42, 3)
    //solution.allocateAssignment(46, 3)

    solution.allocateAssignment(38, 3)
    solution.deallocateAssignment(42)
    //solution.deallocateAssignment(38)
    //solution.deallocateAssignment(4)

    /*solution.allocateAssignment(3, 0)
    solution.allocateAssignment(7, 0)
    solution.allocateAssignment(11, 0)*/
    //println(solution.deallocateAssignment(22))

    solution.debug()*/

}
