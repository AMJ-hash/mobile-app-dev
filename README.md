 GradeOS Student Grade Calculator

 Overview

A Student Grade Calculator desktop application built in Kotlin as part of an Object-Oriented Programming course. It runs in both GUI mode (graphical window) and Console mode (terminal), supports multiple students, file import/export, and demonstrates core OOP principles.

 OOP Concepts Used

| Concept | Implementation |
Abstract Class** | `GradeCalculator` — base class, cannot be instantiated directly |
Interfaces** | `ICalculable`, `IExportable`, `ISensorAware` |
Inheritance** | 4 subclasses extend `GradeCalculator` |
Polymorphism** | `calculateGrade()`, `getGPA()`, `handleSensorEvent()` behave differently per subclass |
Encapsulation** | Private/protected fields with controlled public access |
Data Classes** | `Student`, `Grade`, `CourseEntry` — immutable value objects |
Enums** | `Major`, `Distinction`, `SensorType` |
Singleton** | `ExportEngine`, `GradeScale` as Kotlin `object` declarations |

---

## Features

- Multi-student roster — add, edit, delete students
- Per-course marks: Assignments (30%) / Midterm (30%) / Final Exam (40%)
- Automatic weighted GPA calculation (4.0 scale)
- **Distinction system**: Summa Cum Laude / Magna Cum Laude / Cum Laude / Honours / Pass / Fail
- File import — upload Excel (.xlsx) or CSV with multiple students at once
- Export results — Excel, PDF, HTML, XML, CSV
- 5 simulated sensors (Accelerometer, Proximity, Light, Gyroscope, Microphone)
- **GUI mode** + **Console mode**

---

## Distinction Scale

| Distinction | Min GPA |
|---|---|
| Summa Cum Laude | >= 3.9 |
| Magna Cum Laude | >= 3.7 |
| Cum Laude | >= 3.5 |
| Honours | >= 3.0 |
| Pass | >= 2.0 |
| Fail | < 2.0 |

---

## Tech Stack

- **Language:** Kotlin 1.9
- **GUI:** Jetpack Compose Desktop
- **Excel export:** Apache POI 5.2.5
- **PDF export:** iText 7
- **Build tool:** Gradle 8.5

---

## How to Run

**Prerequisite:** Java 17+ installed from https://adoptium.net/

```powershell
# Open PowerShell inside the GradeCalculatorKotlin folder, then:
powershell -ExecutionPolicy Bypass -File run.ps1

# Choose 1 for GUI, 2 for Console
```

First run downloads Gradle and dependencies (~300 MB) and takes 3-5 minutes. After that it is instant.

---

## Project Structure

```
GradeCalculatorKotlin/
  src/main/kotlin/gradecalculator/
    Main.kt                  <- GUI entry point (Compose Desktop)
    model/
      Interfaces.kt          <- ICalculable, IExportable, ISensorAware
      GradeCalculator.kt     <- Abstract base class
      Subclasses.kt          <- 4 concrete subclasses
      Student.kt             <- Student data class + Distinction enum
    sensor/SensorManager.kt  <- 5-sensor simulation
    export/ExportEngine.kt   <- Excel, PDF, HTML, XML, CSV
    console/ConsoleRunner.kt <- Console mode
  run.ps1                    <- One-click Windows launcher
  sample_files/              <- Sample CSV import template


GitHub

bash
git init
git add .
git commit -m "Initial commit: GradeOS Kotlin"
git remote add origin https://github.com/YOUR_USERNAME/GradeCalculatorKotlin.git
git branch -M main
git push -u origin main
```
