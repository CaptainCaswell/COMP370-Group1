# COMP 370 - Group 1 - Mini-Project 1

## Quick Links

[Use Case Diagram](UseCase.png)

[Class Diagram - Simplified version](ClassDiagramV5.png)

[Class Diagram - Detailed version](ClassDiagramV6.png)

[Sequence Diagram](SequenceDiagramV3.png)

[Sequence Diagram 2](SequenceDiagram2V3.png)

## Automated Testing

### Admin Panel

Launching `ui.bat` will compile all required java files to the bin folder, then launch the Admin user interface for the system. From here the user can:

* Start clients
* Start slow clients
* Kill clients
* Start servers
* Start slow servers
* Kill servers
* Run testing scenarios

Please note that scenarios will close all current servers. Manual launching of any nodes while scenarios are running will alter the scenario.

## Semi-Automated Testing

### basic.bat

This batch file simply compiles the java files into the bin folder, then launches one each of a client, server, and monitor.

### advanced.bat

This batch file compiles the java files into the bin folders, then launches one server, one monitor, three clients. After waiting for 2 seconds it starts another 2 servers. Crashing of servers can be simulated by closing the command prompt window.

## Manual Testing

### compile.bat

This will only compile the java files to allow for future manual launching of processes.

### Manual Commands

`java -cp bin Client <id> [delay]` will start a client. ID must be unique. Delay is optional. If there is a delay, it will get incrementally longer until the monitor issues a shutdown command due to being over the threshold. The delay is in miliseconds.

`java -cp bin Server <id> [delay]`will start a server. ID must be unique. Delay is optional. If there is a delay, it will get incrementally longer until the monitor issues a shutdown command due to being over the threshold. The delay is in miliseconds.

`java -cp bin Monitor [ui]` will start the monitor. Adding the ui argument will launch the monitor UI. There can only be one monitor active.
