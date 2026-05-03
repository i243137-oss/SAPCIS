<div align="center">

<img src="https://img.shields.io/badge/SAPCIS-Smart%20Academic%20Planning%20%26%20Class%20Information%20System-1572FE?style=for-the-badge&logo=java&logoColor=white" alt="SAPCIS Banner"/>

# 🎓 SAPCIS
### Smart Academic Presence & Classroom Intelligence System

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=flat-square&logo=java&logoColor=white)](https://openjfx.io/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-2019+-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![JDBC](https://img.shields.io/badge/JDBC-mssql--jdbc%2013.4-green?style=flat-square)](https://learn.microsoft.com/sql/connect/jdbc/)
[![License](https://img.shields.io/badge/License-Academic-purple?style=flat-square)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen?style=flat-square)]()

> **A full-stack desktop application** for managing university class schedules, teacher assignments, room swaps, emergency overrides, and student timetables — built with JavaFX and Microsoft SQL Server.

---

[✨ Features](#-features) • [🏗️ Architecture](#️-architecture) • [🚀 Quick Start](#-quick-start) • [📸 Screenshots](#-screenshots) • [🗄️ Database](#️-database-setup) • [👥 User Roles](#-user-roles)

</div>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 👨‍💼 Admin
- 🏫 **Campus Core Setup** — Departments, Batches, Courses, Rooms, Sections, Teachers
- 📅 **Timetable Assignment** — Assign teachers to slots with full rule validation
- 📏 **Policy Engine** — Max courses/day, rest gaps, opening/closing times
- 🚨 **Emergency Override** — Change day/time, swap rooms, override status
- 👨‍🏫 **Substitute Management** — Find & assign qualified substitutes
- 🏫 **Room Swap Approval** — Review and approve/reject teacher requests
- 📊 **Analytics & Reports** — Faculty load, classroom utilization, timetables

</td>
<td width="50%">

### 👩‍🏫 Teacher
- 📋 **Live Schedule** — Real-time ONGOING/UPCOMING status from system clock
- ✏️ **Report Delays/Cancellations** — Notify students instantly
- 🔄 **Request Room Swap** — Submit room change requests to admin
- 🔔 **Notifications** — Receive schedule change alerts

### 🎓 Student
- 📆 **Weekly Timetable** — Full 5-day schedule with live status
- 🔴 **Critical Alerts** — Cancelled/delayed class banners
- 📱 **Day-by-Day View** — Filter by Monday–Saturday
- 🔔 **Smart Alerts** — Subscribe to class notifications

</td>
</tr>
</table>

---

## 🏗️ Architecture

```
SAPCIS/
├── src/
│   ├── Main.java                    # Application entry point
│   ├── controller/                  # GRASP Use-Case Controllers
│   │   ├── DashboardController.java # UC-04: Student Dashboard
│   │   ├── TeacherController.java   # UC-01: Teacher Dashboard
│   │   ├── OverrideController.java  # UC-11: Emergency Override
│   │   ├── RoomSwapController.java  # UC-02: Room Swap Request
│   │   ├── SubstituteController.java# UC-08: Substitute Management
│   │   ├── ScheduleController.java  # UC-03: Schedule Management
│   │   └── ReportController.java    # UC-12: Analytics & Reports
│   ├── model/                       # Domain Entities
│   │   ├── Teacher.java
│   │   ├── Student.java
│   │   ├── Course.java
│   │   ├── Schedule.java
│   │   ├── ClassSession.java
│   │   ├── Classroom.java
│   │   ├── Section.java
│   │   ├── Rule.java
│   │   ├── Notification.java
│   │   ├── SubstituteAssignment.java
│   │   └── ScheduleAdjustmentRequest.java
│   ├── service/                     # Pure Fabrication Services
│   │   ├── NotificationService.java # GoF Observer Pattern
│   │   ├── ConstraintResolverService.java
│   │   └── DirectoryService.java
│   ├── dao/                         # Data Access Objects
│   │   ├── SessionRepository.java
│   │   ├── ClassSessionDAO.java
│   │   ├── RoomRepository.java
│   │   ├── StudentDAO.java
│   │   └── TimetableDBDAO.java
│   ├── db/
│   │   └── DBConnection.java        # SQL Server connection pool
│   ├── ui/                          # JavaFX Controllers + FXML
│   │   ├── AdminDashboardController.java
│   │   ├── AdminDashboard.fxml
│   │   ├── TeacherDashboard.fxml
│   │   ├── StudentDashboard.fxml
│   │   ├── Login.fxml
│   │   ├── StudentSignup.fxml
│   │   └── styles.css
│   ├── exception/                   # Custom Exceptions
│   └── utils/                       # Utilities (UserSession, etc.)
├── lib/
│   └── mssql-jdbc.jar               # Microsoft JDBC Driver
├── sapcis_complete.sql              # Full database schema + seed data
└── README.md
```

### Design Patterns Used
| Pattern | Where |
|---------|-------|
| **GRASP Controller** | All `controller/` classes |
| **Information Expert** | `RoomRepository`, `StudentDAO` |
| **Creator** | `RoomSwapController.createRequest()` |
| **Pure Fabrication** | `NotificationService`, `ConstraintResolverService` |
| **GoF Observer** | `NotificationService.pushAlerts()` |
| **GoF Strategy** | `ConstraintResolverService` rule evaluation |

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| ☕ JDK | 17+ | [OpenJDK](https://adoptium.net/) |
| 🗄️ SQL Server | 2019+ | [SQL Server Express](https://www.microsoft.com/sql-server/sql-server-downloads) |
| 🖥️ SQL Server Management Studio | Any | [SSMS](https://learn.microsoft.com/sql/ssms/download-sql-server-management-studio-ssms) |
| 📦 JavaFX SDK | 17+ | [OpenJFX](https://gluonhq.com/products/javafx/) |

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/your-username/SAPCIS.git
cd SAPCIS
```

---

### Step 2 — Database Setup

1. **Open SQL Server Management Studio (SSMS)**

2. **Create the database and run the schema:**
   ```sql
   -- In SSMS, open a new query window and run:
   USE master;
   GO
   CREATE DATABASE sapcis_db;
   GO
   USE sapcis_db;
   GO
   ```

3. **Run the complete SQL script:**
   - Open `sapcis_complete.sql` in SSMS
   - Execute it (`F5` or click **Execute**)
   - This creates all tables + seeds demo data

4. **Verify tables were created:**
   ```sql
   USE sapcis_db;
   SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES ORDER BY TABLE_NAME;
   ```

---

### Step 3 — Configure Database Connection

Edit `src/db/DBConnection.java` and update the connection string:

```java
// Option A: Windows Authentication (recommended for local dev)
private static final String URL =
    "jdbc:sqlserver://localhost:1433;databaseName=sapcis_db;" +
    "integratedSecurity=true;trustServerCertificate=true;";

// Option B: SQL Server Authentication
private static final String URL =
    "jdbc:sqlserver://localhost:1433;databaseName=sapcis_db;" +
    "user=YOUR_USERNAME;password=YOUR_PASSWORD;trustServerCertificate=true;";
```

> 💡 **Tip:** If SQL Server is on a named instance (e.g. `SQLEXPRESS`), use:
> `localhost\\SQLEXPRESS:1433` instead of `localhost:1433`

---

### Step 4 — Add JavaFX to Your IDE

#### 🔵 IntelliJ IDEA
1. **File → Project Structure → Libraries → + → Java**
2. Navigate to your JavaFX SDK `lib/` folder and add all JARs
3. **Run → Edit Configurations → VM Options:**
   ```
   --module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
   ```

#### 🟠 Eclipse
1. **Project → Properties → Java Build Path → Libraries → Add External JARs**
2. Add all JARs from JavaFX SDK `lib/`
3. **Run Configurations → Arguments → VM Arguments:**
   ```
   --module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
   ```

#### 🟢 VS Code
Add to `.vscode/launch.json`:
```json
{
    "type": "java",
    "name": "SAPCIS",
    "request": "launch",
    "mainClass": "Main",
    "vmArgs": "--module-path \"C:/path/to/javafx-sdk-17/lib\" --add-modules javafx.controls,javafx.fxml"
}
```

---

### Step 5 — Build & Run

#### Using Command Line
```bash
# Compile
javac --module-path /path/to/javafx-sdk/lib \
      --add-modules javafx.controls,javafx.fxml \
      -cp "lib/mssql-jdbc.jar" \
      -d bin \
      src/**/*.java

# Run
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp "bin:lib/mssql-jdbc.jar" \
     Main
```

#### Using Your IDE
Simply run `src/Main.java` as the main class.

---

## 🔐 Demo Login Credentials

| Role | Username / Email | Password |
|------|-----------------|----------|
| 👨‍💼 **Admin** | `ADM-001` | `admin123` |
| 👩‍🏫 **Teacher** | `T-SE-001` | `teacher123` |
| 🎓 **Student** | `STU-001` | `student123` |

> ⚠️ Change these credentials after first login in a production environment.

---

## 👥 User Roles

```
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

## 🗄️ Database Setup

### Key Tables

| Table | Description |
|-------|-------------|
| `users` | All users (Admin, Teacher, Student) with roles |
| `teacher_assignments` | **Source of truth** for all timetable slots |
| `class_sessions` | Live session status (ONGOING/CANCELLED/DELAYED) |
| `schedule_adjustment_requests` | Room swap requests from teachers |
| `substitute_assignments` | Substitute teacher records |
| `rules` | Campus policy rules (max courses, rest gap, etc.) |
| `notifications` | System notifications for all users |
| `classrooms` | Room details with capacity |
| `courses` | Course catalog |
| `sections` | Batch + Department + Section mappings |
| `departments` | Department registry |
| `batches` | Academic batch/year registry |

### Live Status Logic
```
Status is computed dynamically from the system clock:

  IF class is CANCELLED or DELAYED (manually set)
      → Show that status (sticky override)
  ELSE IF today == class day AND now >= startTime AND now < endTime
      → ONGOING  🟢
  ELSE
      → UPCOMING  🔵
```

---

## 🔧 Troubleshooting

<details>
<summary><b>❌ "Cannot connect to SQL Server"</b></summary>

1. Ensure SQL Server service is running:
   - Open **Services** → Find **SQL Server (MSSQLSERVER)** → Start
2. Enable TCP/IP in **SQL Server Configuration Manager**
3. Check firewall allows port `1433`
4. Verify `trustServerCertificate=true` in connection string

</details>

<details>
<summary><b>❌ "JavaFX runtime components are missing"</b></summary>

Add VM arguments to your run configuration:
```
--module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
```

</details>

<details>
<summary><b>❌ "Class not found: com.microsoft.sqlserver.jdbc.SQLServerDriver"</b></summary>

Ensure `lib/mssql-jdbc.jar` is on the classpath. In IntelliJ:
- **File → Project Structure → Modules → Dependencies → + → JARs**
- Add `lib/mssql-jdbc.jar`

</details>

<details>
<summary><b>❌ Schedule shows no data for student/teacher</b></summary>

Run the seed data script:
```sql
USE sapcis_db;
-- Check teacher_assignments has data:
SELECT COUNT(*) FROM teacher_assignments;
-- Check students table:
SELECT * FROM students;
-- Verify section names match:
SELECT DISTINCT sectionName FROM teacher_assignments;
SELECT section FROM students;
```

</details>

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

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is developed for academic purposes at **FAST National University of Computer and Emerging Sciences**.

---

<div align="center">

**Built with ❤️ using Java + JavaFX + SQL Server**

⭐ Star this repo if you found it helpful!

</div>
