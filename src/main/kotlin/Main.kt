import hyperheuristic.acceptance.AcceptanceCriterionType
import hyperheuristic.hh.GIHH
import hyperheuristic.selection.SelectionMethodType
import hyperheuristic.util.WriteInfo
import problemDomain.MDSP
import java.io.BufferedWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// An example of the GIHH being run to produce a solution using the problem domain
fun main() {
    // Setup GIHH hyper-heuristic to run on instance
    val seed: Long = 22032024
    val totalExecutionTime: Long = 60000 * 15 // 15 minutes
    val selectionType = SelectionMethodType.AdaptiveLimitedLAassistedDHSMentorSTD
    val acceptanceType = AcceptanceCriterionType.AdaptiveIterationLimitedListBasedTA
    val resultFileName = "GIHH_"
    val today = Date()
    val dateFormatter = SimpleDateFormat("ddMMyyyyHHmmss")
    WriteInfo.resultSubFolderName = dateFormatter.format(today)
    val problem = MDSP(seed)
    val hyperHeuristic = GIHH(
        seed, problem.numberOfHeuristics, totalExecutionTime,
        resultFileName, selectionType, acceptanceType
    )

    // Load "test_instance_1" and run GIHH on it
    problem.loadInstance(0)
    problem.initialiseSolution(0)
    hyperHeuristic.timeLimit = totalExecutionTime
    hyperHeuristic.loadProblemDomain(problem)
    hyperHeuristic.run()

    // Write results to file
    val writer = BufferedWriter(FileWriter("results/department1_baseline.txt"))
    writer.write(problem.bestSolutionToString())
    writer.close()
    println(problem.bestSolutionValue)
}

