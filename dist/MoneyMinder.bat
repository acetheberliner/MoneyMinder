@echo off
pushd "%~dp0"
java --module-path "C:\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml -jar MoneyMinder.jar
if errorlevel 1 pause

