@echo off

REM Compile all Java files
javac ServerProcess.java
javac Client.java
javac Monitor.java

REM Start Monitor
start "MONITOR" java Monitor

REM Start first server with titled window
start "SERVER1" java ServerProcess 2000

REM Start clients
start "CLIENT1" java Client
start "CLIENT2" java Client

REM Wait 5 seconds
timeout /t 5

REM Start additional servers
start "SERVER2"  java ServerProcess 3000
start "SERVER3"  java ServerProcess 4000

REM Wait 20 seconds before simulating crash
timeout /t 20

echo --- Killing Server 2000 ---
taskkill /F /FI "WINDOWTITLE eq SERVER1"

REM Wait 60 seconds
timeout /t 60

echo --- Simulation over ---

REM Clean up all processes
taskkill /F /FI "WINDOWTITLE eq SERVER2"
taskkill /F /FI "WINDOWTITLE eq SERVER3"
taskkill /F /FI "WINDOWTITLE eq MONITOR"
taskkill /F /FI "WINDOWTITLE eq CLIENT1"
taskkill /F /FI "WINDOWTITLE eq CLIENT2"

pause