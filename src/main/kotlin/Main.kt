import fieldExperimentInterface.ExperimentGUI
import hyperheuristic.acceptance.AcceptanceCriterionType
import hyperheuristic.hh.GIHH
import hyperheuristic.selection.SelectionMethodType
import hyperheuristic.util.WriteInfo
import javafx.application.Application
import problemDomain.MDSP
import java.io.BufferedWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

fun main(args: Array<String>) {
    // If "--instance" is passed as an argument, we run the instance input gui
    if (args.isNotEmpty() && args.first() == "--instance") {
        val gui = ExperimentGUI()
        Application.launch(gui::class.java)
        return
    }

    /*val gen = InstanceGenerator(Random(3032024))
    gen.generateInstance("test_instance_1.txt", 2, 8, 4, 4, 0.4, 0.4)*/

    val seedGenerator = Random(22032024)
    val totalExecutionTime: Long = 120000
    val selectionType = SelectionMethodType.AdaptiveLimitedLAassistedDHSMentorSTD
    val acceptanceType = AcceptanceCriterionType.AdaptiveIterationLimitedListBasedTA
    val resultFileName = "GIHH_"
    val today = Date()
    val dateFormatter = SimpleDateFormat("ddMMyyyyHHmmss")
    WriteInfo.resultSubFolderName = dateFormatter.format(today)

    for(i in 1..3) {
        val seed = seedGenerator.nextLong()
        val problem = MDSP(seed)
        val hyperHeuristic = GIHH(
            seed, problem.numberOfHeuristics, totalExecutionTime,
            resultFileName, selectionType, acceptanceType
        )

        problem.loadInstance(1)
        problem.initialiseSolution(0)
        println(problem.getFunctionValue(0))
        hyperHeuristic.timeLimit = totalExecutionTime
        hyperHeuristic.loadProblemDomain(problem)
        hyperHeuristic.run()

        val writer = BufferedWriter(FileWriter("test_instance2_log-$i.txt"))
        writer.write(problem.bestSolution.assignmentLog)
        writer.close()
    }

    for(i in 1..3) {
        val seed = seedGenerator.nextLong()
        val problem = MDSP(seed)
        val hyperHeuristic = GIHH(
            seed, problem.numberOfHeuristics, totalExecutionTime,
            resultFileName, selectionType, acceptanceType
        )

        problem.loadInstance(0)
        problem.initialiseSolution(0)
        println(problem.getFunctionValue(0))
        hyperHeuristic.timeLimit = totalExecutionTime
        hyperHeuristic.loadProblemDomain(problem)
        hyperHeuristic.run()

        val writer = BufferedWriter(FileWriter("test_instance1_log-$i.txt"))
        writer.write(problem.bestSolution.assignmentLog)
        writer.close()
    }

    // Debugging a doctor log file
    /*val seedGenerator = Random(25022024)
    val seed = seedGenerator.nextLong()
    val problem = MDSP(seed)
    problem.loadInstance(1)
    val solution = problem.blankSolution()

    val log = File("src/main/resources/docLog.txt")
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
    }*/

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
