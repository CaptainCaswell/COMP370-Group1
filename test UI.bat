@echo off

REM Compile all Java files
javac -d bin Client.java ClientHandler.java FailureDetector.java HeartbeatSender.java Logger.java MonitorConnectionHandler.java MonitorCore.java MonitorMain.java MonitorUI.java NodeInfo.java ServerInfo.java ServerCore.java ServerMain.java ServerState.java

REM Delete old logs
rd /S /Q "log"

REM Start Monitor
start "MONITOR" java -cp bin MonitorMain

pause