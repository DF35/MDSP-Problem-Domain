# MDSP Problem Domain

This repository holds the codebase for my Undergraduate Dissertation project "An Investigation into the 
Efficacy of Hyper-Heuristics for Middle Grade Doctor Shift Scheduling" , that I completed as part of my 
BSc in Computer Science at the University of Nottingham. It includes the Hyflex-compatible Problem
Domain that I encoded as well as the pre-existing GIHH hyper-heuristic that I applied to it as part of the investigation.
The code for the Hyflex interfaces and the hyper-heuristic itself were obtained from: [experiment_department2_hard.txt](src%2Fmain%2Fresources%2Finstances%2Fexperiment_department2_hard.txt)

## Usage
The Problem Domain will be compatible with any Hyper-Heuristic encoded using the Hyflex interface, see "Main.kt"
for an example of how the code would be set up to do this.

To run the system as is, with the GIHH hyper-heuristic, simply run the project using Gradle from your command line (within the project folder):

 - `./gradlew run` for Linux/macOS
 - `.\gradlew run` for Windows Powershell

It should be noted that this was developed primarily as a research project so is not focussed on the end-user experience.
Furthermore, the project is intended only to be a proof-of-concept for the applicability of hyper-heuristics to the problem
and should not be used in a real-life scenario.