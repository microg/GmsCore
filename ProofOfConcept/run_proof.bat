@echo off
echo [Verify] Compiling RCS Proof Code...
kotlinc StartRcs.kt -include-runtime -d proof.jar
if errorlevel 1 (
    echo [Error] Kotlin compiler 'kotlinc' not found!
    echo Please install Kotlin or just use Android Studio to run this file.
    pause
    exit /b
)
echo [Verify] Running RCS Simulation...
java -jar proof.jar
pause
