# MDSP Problem Domain

This repository holds the codebase for my Undergraduate Dissertation project "An Investigation into the 
Efficacy of Hyper-Heuristics for Middle Grade Doctor Shift Scheduling" , that I completed as part of my 
BSc in Computer Science at the University of Nottingham. It includes the Hyflex-compatible problem
domain that I encoded as well as the pre-existing GIHH hyper-heuristic that I applied to it as part of the investigation. If you are interested in reading the paper, it can be seen [here](Undergraduate_Dissertation.pdf).

The problem domain dynamically calculates feasibility for shift assignments based on constraints stipulated by the European Working Time Directive and the 2018 Junior Doctor Contract refresh, thereby providing a realistic formulation of a hitherto unexplored timetabling problem. Also included is an instance generator, which is capable of generating realistic instances for two real-world departments, with doctor leave, preferences, and training being calculated based on statistics obtained from surveys of actual junior doctors. 

It should be noted that this was developed primarily as a research project so is not focussed on the end-user experience.
Furthermore, the project is intended only to be a proof-of-concept for the applicability of hyper-heuristics to the problem
and should not be used in a real-life scenario.

The code for the Hyflex interfaces and the hyper-heuristic itself were obtained from: https://github.com/seage/hyflex

## Usage
The Problem Domain will be compatible with any Hyper-Heuristic encoded using the Hyflex interface, see "Main.kt"
for an example of how the code would be set up to do this.

To run the system as is, with the GIHH hyper-heuristic, ensure you have Java 18 or later installed and simply run the project using Gradle from your command line (within the project folder):

 - `./gradlew run` for Linux/macOS
 - `.\gradlew run` for Windows Powershell

This will run the hyper-heuristic on the "department_1_baseline" instance for 15 minutes, with the created timetable being outputted to the "results" folder in a .txt format. 