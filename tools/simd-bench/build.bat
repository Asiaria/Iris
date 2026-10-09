@echo off
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac --release 25 --add-modules jdk.incubator.vector -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  exit /b 1
)
del sources.txt
jar --create --file simd-bench.jar --main-class simdbench.Bench -C out .
exit /b %errorlevel%
