import hyperheuristic.acceptance.AcceptanceCriterionType
import hyperheuristic.hh.GIHH
import hyperheuristic.selection.SelectionMethodType
import hyperheuristic.util.WriteInfo
import javafx.application.Application
import problemDomain.InstanceGUI
import problemDomain.MDSP
import java.io.BufferedWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

fun main(args: Array<String>) {
    // If "--instance" is passed as an argument, we run the instance input gui
    if (args.isNotEmpty() && args.first() == "--instance") {
        val gui = InstanceGUI()
        Application.launch(gui::class.java)
        return
    }

    /*val seedGenerator = Random(25022024)
    val totalExecutionTime: Long = 120000
    val selectionType = SelectionMethodType.AdaptiveLimitedLAassistedDHSMentorSTD
    val acceptanceType = AcceptanceCriterionType.AdaptiveIterationLimitedListBasedTA
    val resultFileName = "GIHH_"
    val today = Date()
    val dateFormatter = SimpleDateFormat("ddMMyyyyHHmmss")
    WriteInfo.resultSubFolderName = dateFormatter.format(today)

    for(i in 1..5) {
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

        val writer = BufferedWriter(FileWriter("resultInst1-$i.txt"))
        writer.write("${problem.bestSolutionValue} \n")
        writer.write(problem.bestSolutionToString())
        writer.close()
    }*/


    /*val gen = InstanceGenerator(Random(3032024))
    gen.generateInstance("instance2.txt", 2, 8, 4, 4, 0.4, 0.4)*/

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

    val problem = MDSP(25032024)
    problem.loadInstance(1)
    val solution = problem.blankSolution()
    solution.allocateAssignment(0, 3)
    solution.allocateAssignment(4, 3)
    solution.allocateAssignment(9, 3)
    solution.allocateAssignment(14, 3)
    solution.allocateAssignment(18, 3)
    solution.allocateAssignment(22, 3)
    solution.allocateAssignment(26, 3)
    solution.allocateAssignment(3, 0)
    solution.allocateAssignment(7, 0)
    solution.allocateAssignment(11, 0)
    println(solution.deallocateAssignment(3))
    solution.debug()
}
