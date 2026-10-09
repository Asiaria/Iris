@echo off
call build.bat
if errorlevel 1 exit /b 1
java --add-modules jdk.incubator.vector -jar simd-bench.jar %*
