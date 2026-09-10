<div align="center">

<img src="https://img.shields.io/badge/SAPCIS-Smart%20Academic%20Planning%20%26%20Class%20Information%20System-1572FE?style=for-the-badge&logo=java&logoColor=white" alt="SAPCIS Banner"/>

# 🎓 SAPCIS
### Smart Academic Presence & Classroom Intelligence System

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=flat-square&logo=java&logoColor=white)](https://openjfx.io/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019+-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![JDBC](https://img.shields.io/badge/JDBC-mssql--jdbc%2013.4-green?style=flat-square)](https://learn.microsoft.com/sql/connect/jdbc/)
[![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen?style=flat-square)]()

> **A full-stack desktop application** for managing university class schedules, teacher assignments, room swaps, emergency overrides, and student timetables — built with JavaFX and Microsoft SQL Server.

</div>

---

## ✨ Features

### 👨‍💼 Admin
- 🏫 Campus core setup — Departments, Batches, Courses, Rooms, Sections, Teachers
- 📅 Timetable assignment with rule validation
- 📏 Policy engine — max courses/day, rest gaps, opening/closing times
- 🚨 Emergency overrides — change day/time, swap rooms, override status
- 👨‍🏫 Substitute teacher management
- 🏫 Room swap approval
- 📊 Analytics and reports

### 👩‍🏫 Teacher
- 📋 Live schedule with ONGOING/UPCOMING status
- ✏️ Report delays and cancellations
- 🔄 Request room swaps
- 🔔 Receive notifications

### 🎓 Student
- 📆 Weekly timetable
- 🔴 Critical class alerts
- 📱 Day-by-day timetable view
- 🔔 Class notifications

---

## 🏗️ Architecture

```text
SAPCIS/
├── src/
│   ├── Main.java
│   ├── controller/                  # GRASP Use-Case Controllers
│   │   ├── DashboardController.java
│   │   ├── TeacherController.java
│   │   ├── OverrideController.java
│   │   ├── RoomSwapController.java
│   │   ├── SubstituteController.java
│   │   ├── ScheduleController.java
│   │   └── ReportController.java
│   ├── model/                       # Domain Entities
│   ├── service/                     # Application Services
│   ├── dao/                         # Data Access Objects
│   ├── db/DBConnection.java
│   ├── ui/                          # JavaFX Controllers + FXML
│   ├── exception/                   # Custom Exceptions
│   └── utils/                       # Utilities
├── lib/mssql-jdbc.jar
├── sapcis_complete.sql
├── build.xml
├── .github/workflows/
└── README.md
```

### Design Patterns Used

| Pattern | Where |
|---------|-------|
| **GRASP Controller** | Use-case controller classes |
| **Information Expert** | Repository and DAO classes |
| **Creator** | `RoomSwapController.createRequest()` |
| **Pure Fabrication** | Application service classes |
| **GoF Observer** | `NotificationService` |
| **GoF Strategy** | Constraint/rule evaluation |

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| ☕ JDK | 17+ |
| 🖥️ JavaFX | 17+ |
| 🗄️ SQL Server | 2019+ |
| 🛠️ SQL Server Management Studio | Any recent version |

### 1. Clone the Repository

```bash
git clone https://github.com/i243137-oss/SAPCIS.git
cd SAPCIS
```

### 2. Set Up the Database

Open SQL Server Management Studio and run:

```sql
USE master;
GO
CREATE DATABASE sapcis_db;
GO
USE sapcis_db;
GO
```

Then open `sapcis_complete.sql` and execute the complete script. It creates the required tables and seed data.

Verify the setup:

```sql
USE sapcis_db;
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
ORDER BY TABLE_NAME;
```

### 3. Configure the Database Connection

For local development, update `src/db/DBConnection.java` with your SQL Server configuration.

Windows Authentication example:

```java
private static final String URL =
    "jdbc:sqlserver://localhost:1433;databaseName=sapcis_db;" +
    "integratedSecurity=true;trustServerCertificate=true;";
```

SQL Server Authentication example:

```java
private static final String URL =
    "jdbc:sqlserver://localhost:1433;databaseName=sapcis_db;" +
    "user=YOUR_USERNAME;password=YOUR_PASSWORD;trustServerCertificate=true;";
```

> **Important:** Do not commit real database credentials. Use local configuration or environment-specific settings.

### 4. Configure JavaFX

Add the JavaFX SDK libraries to your IDE and provide the required module path.

Example VM arguments:

```text
--module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
```

For VS Code, configure the same module path in `.vscode/launch.json`.

### 5. Build and Run

SAPCIS is a JavaFX desktop application. You can run `src/Main.java` from a configured IDE, or use the included Ant build configuration where appropriate.

If using command-line compilation, make sure JavaFX and `lib/mssql-jdbc.jar` are available on the module/class path.

---

## 🔧 Build & CI Notes

The repository includes a GitHub Actions build workflow for checking the Java source code.

Recent build fixes include:

- `RoomSwapController.requestSpecificRoom()` now declares the checked `RoomUnavailableException` it can propagate.
- `SubstituteController` now provides `rejectSubstituteByTeacher(String, String)` for teacher rejection handling.
- `AdminOverrideUIController` now converts the session/assignment ID from the UI text field to the integer expected by `OverrideController`.

The `db.properties` warning in CI is separate from these Java compilation errors. The repository provides `db.properties.example`; CI/database credentials should be configured separately rather than committed to the repository.

---

## 🔐 Demo Login Credentials

| Role | Username / Email | Password |
|------|-----------------|----------|
| 👨‍💼 **Admin** | `ADM-001` | `admin123` |
| 👩‍🏫 **Teacher** | `T-SE-001` | `teacher123` |
| 🎓 **Student** | `STU-001` | `student123` |

> ⚠️ These are demo credentials for academic/local development only. Change or remove them before production deployment.

---

## 👥 User Roles

```text
┌─────────────────────────────────────────────────────────┐
│                        SAPCIS                           │
├──────────────┬──────────────────┬───────────────────────┤
│    Admin     │     Teacher      │       Student         │
├──────────────┼──────────────────┼───────────────────────┤
│ Full system  │ View schedule    │ View timetable        │
│ management   │ Report delays    │ See live status       │
│ Rule config  │ Request room     │ Get notifications     │
│ Overrides    │ swap             │ Filter by day         │
│ Substitutes  │ Mark cancelled   │ Critical alerts       │
│ Reports      │ Notifications    │                       │
└──────────────┴──────────────────┴───────────────────────┘
```

---

## 🗄️ Database

### Key Tables

| Table | Description |
|-------|-------------|
| `users` | All users and roles |
| `teacher_assignments` | Source of truth for timetable slots |
| `class_sessions` | Live session status |
| `schedule_adjustment_requests` | Room swap requests |
| `substitute_assignments` | Substitute teacher records |
| `rules` | Campus policy rules |
| `notifications` | User notifications |
| `classrooms` | Classroom details and capacity |
| `courses` | Course catalog |
| `sections` | Batch, department and section mappings |
| `departments` | Department registry |
| `batches` | Academic batch/year registry |

### Live Status Logic

```text
IF class is CANCELLED or DELAYED
    → Show the manually assigned status
ELSE IF today == class day AND now >= startTime AND now < endTime
    → ONGOING
ELSE
    → UPCOMING
```

---

## 🔧 Troubleshooting

### Cannot connect to SQL Server

1. Ensure the SQL Server service is running.
2. Enable TCP/IP in SQL Server Configuration Manager.
3. Check that the configured port is accessible.
4. Verify the database name and connection string.
5. For local development, use `trustServerCertificate=true` when appropriate.

### JavaFX runtime components are missing

Add the JavaFX module path and modules to your run configuration:

```text
--module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
```

### SQLServerDriver class not found

Ensure `lib/mssql-jdbc.jar` is included in the classpath.

### Schedule shows no data

Run the seed data script and verify:

```sql
USE sapcis_db;
SELECT COUNT(*) FROM teacher_assignments;
SELECT * FROM students;
SELECT DISTINCT sectionName FROM teacher_assignments;
SELECT section FROM students;
```

---

## 📋 Use Cases Implemented

| UC | Name | Status |
|----|------|--------|
| UC-01 | Teacher Reports Delay/Cancellation | ✅ Complete |
| UC-02 | Teacher Requests Room Swap | ✅ Complete |
| UC-03 | Schedule Management | ✅ Complete |
| UC-04 | Student Views Live Dashboard | ✅ Complete |
| UC-08 | Substitute Teacher Management | ✅ Complete |
| UC-10 | Campus Policy Rules Engine | ✅ Complete |
| UC-11 | Emergency Override (Admin) | ✅ Complete |
| UC-12 | Analytics & Reports | ✅ Complete |

---

## 🤝 Contributing

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add some feature"
   ```
4. Push the branch:
   ```bash
   git push origin feature/your-feature
   ```
5. Open a Pull Request.

---

## 📄 License

This project is developed for academic purposes at **FAST National University of Computer and Emerging Sciences**.

---

<div align="center">

**Built with ❤️ using Java + JavaFX + SQL Server**

⭐ Star this repo if you found it helpful!

</div>
