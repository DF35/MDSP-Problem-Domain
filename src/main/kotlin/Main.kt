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

    /*val gen = InstanceGenerator(Random(25032024))
    gen.generateInstance("experiment_department1_hard.txt", 1, 8, 4, 4, 0.5, 0.5, 4)*/

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
        hyperHeuristic.timeLimit = totalExecutionTime
        hyperHeuristic.loadProblemDomain(problem)
        hyperHeuristic.run()
        val writer = BufferedWriter(FileWriter("src/test/resources/test_instance2_log-$i.txt"))
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
        hyperHeuristic.timeLimit = totalExecutionTime
        hyperHeuristic.loadProblemDomain(problem)
        hyperHeuristic.run()
        val writer = BufferedWriter(FileWriter("src/test/resources/test_instance1_log-$i.txt"))
        writer.write(problem.bestSolution.assignmentLog)
        writer.close()
    }*/


    // Debugging a doctor log file
    /*val seedGenerator = Random(25022024)
    val seed = seedGenerator.nextLong()
    val problem = MDSP(seed)
    problem.loadInstance(6)
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
