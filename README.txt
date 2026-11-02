Railway Ticket Reservation and Waiting List Management System
Object-Oriented Programming Lab (PCC-CS593), Group 2 mini project

Requirements: JDK 8 or later (developed and tested with OpenJDK 21).

Compile (from this folder):
    Linux/macOS:  mkdir out && javac -d out $(find src -name "*.java")
    Windows:      dir /s /b src\*.java > files.txt
                  mkdir out && javac -d out @files.txt

Run the application:
    java -cp out railway.app.Main [dataDir] [compartmentsPerClass] [waitingListLimit]
    Defaults: data 4 5   (a small demo: java -cp out railway.app.Main demo 1 2)

Run the unit tests:
    java -cp out railway.test.TestRunner

Data files created in dataDir:
    tickets.txt        active tickets (restored on start-up)
    trip_history.txt   append-only audit log
    manifest_<no>.txt  passenger manifest written by menu option 6
