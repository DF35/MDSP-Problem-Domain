package problemDomain

import javafx.application.Application

fun main(args: Array<String>) {
    // If "--instance" is passed as an argument, we run the instance input gui
    if(args.isNotEmpty() && args.first() == "--instance"){
        val gui = InstanceGUI()
        Application.launch(gui::class.java)
        return
    }

    val pd = MDSP(13022024)
    pd.loadInstance(0)

    pd.initialiseSolution(0)
    println(pd.solutionMemory[0]!!.objectiveValue)
    for(doctor in pd.solutionMemory[0]!!.doctors){
        doctor.debug()
    }

    println("\n\n\n")

    println(pd.applyHeuristic(1,0,1))
    for(doctor in pd.solutionMemory[1]!!.doctors){
        doctor.debug()
    }

    println(pd.applyHeuristic(2, 1, 1))
    for(doctor in pd.solutionMemory[1]!!.doctors){
        doctor.debug()
    }

    println(pd.applyHeuristic(3,1,1))
    for(doctor in pd.solutionMemory[1]!!.doctors){
        doctor.debug()
    }

    pd.setMemorySize(3)
    println(pd.applyHeuristic(4, 0, 1, 2))
    for(doctor in pd.solutionMemory[2]!!.doctors){
        doctor.debug()
    }

    println(pd.applyHeuristic(5, 2, 2))
    for(doctor in pd.solutionMemory[2]!!.doctors){
        doctor.debug()
    }

    println(pd.applyHeuristic(6, 2, 2))
    for(doctor in pd.solutionMemory[2]!!.doctors){
        doctor.debug()
    }

    println(pd.applyHeuristic(8, 2, 2))
    for(doctor in pd.solutionMemory[2]!!.doctors){
        doctor.debug()
    }

    println(pd.getFunctionValue(2))
}