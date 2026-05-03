-- =============================================================================
--  SAPCIS — Complete Database Setup (SQL Server)
-- =============================================================================
--  Single file that:
--    1. Creates the sapcis database (if not exists)
--    2. Drops all tables (clean slate)
--    3. Creates all tables with correct schema (including all migrations)
--    4. Seeds all reference data + demo users
--    5. Seeds master timetable (teacher_assignments)
--    6. Seeds class sessions, enrollments, course-teacher mappings
--    7. Seeds dashboard demo data (sections A-D, multi-batch teachers)
--    8. Adds indexes and unique constraints
--
--  Safe to re-run at any time for a clean slate.
--  Run in SQL Server Management Studio (SSMS) against the master database,
--  or use: sqlcmd -S localhost -i sapcis_complete.sql
-- =============================================================================

USE master;
GO

IF DB_ID('sapcis') IS NULL
BEGIN
    CREATE DATABASE sapcis;
    PRINT 'Database sapcis created.';
END
GO

USE sapcis;
GO

-- =============================================================================
--  STEP 1: DROP ALL TABLES
--  Strategy: explicitly drop all FK constraints first (by name), then drop
--  tables in child-to-parent order. This is the only 100% reliable approach
--  on SQL Server when FKs reference tables being dropped.
-- =============================================================================

-- ── Drop all FK constraints explicitly ───────────────────────────────────────
-- (Safe to run even if the constraint doesn't exist — wrapped in IF EXISTS)

IF OBJECT_ID('dbo.teacher_assignments_baseline', 'U') IS NOT NULL
    DROP TABLE dbo.teacher_assignments_baseline;

-- substitute_assignments FKs
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_sub_assignment')
    ALTER TABLE substitute_assignments DROP CONSTRAINT fk_sub_assignment;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_sub_original')
    ALTER TABLE substitute_assignments DROP CONSTRAINT fk_sub_original;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_sub_substitute')
    ALTER TABLE substitute_assignments DROP CONSTRAINT fk_sub_substitute;

-- course_teacher_assignments FKs
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_cta_course')
    ALTER TABLE course_teacher_assignments DROP CONSTRAINT FK_cta_course;
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_cta_teacher')
    ALTER TABLE course_teacher_assignments DROP CONSTRAINT FK_cta_teacher;

-- batch_dept_sections FKs
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE object_id IN (
    SELECT object_id FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('batch_dept_sections')))
BEGIN
    DECLARE @bds_fk NVARCHAR(200);
    DECLARE bds_cur CURSOR FOR
        SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('batch_dept_sections');
    OPEN bds_cur;
    FETCH NEXT FROM bds_cur INTO @bds_fk;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE batch_dept_sections DROP CONSTRAINT [' + @bds_fk + ']');
        FETCH NEXT FROM bds_cur INTO @bds_fk;
    END
    CLOSE bds_cur; DEALLOCATE bds_cur;
END

-- teacher_assignments FKs
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'uq_teacher_assignment_slot')
    ALTER TABLE teacher_assignments DROP CONSTRAINT uq_teacher_assignment_slot;
DECLARE @ta_fk NVARCHAR(200);
DECLARE ta_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('teacher_assignments');
OPEN ta_cur;
FETCH NEXT FROM ta_cur INTO @ta_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE teacher_assignments DROP CONSTRAINT [' + @ta_fk + ']');
    FETCH NEXT FROM ta_cur INTO @ta_fk;
END
CLOSE ta_cur; DEALLOCATE ta_cur;

-- timetable_db FKs
DECLARE @tdb_fk NVARCHAR(200);
DECLARE tdb_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('timetable_db');
OPEN tdb_cur;
FETCH NEXT FROM tdb_cur INTO @tdb_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE timetable_db DROP CONSTRAINT [' + @tdb_fk + ']');
    FETCH NEXT FROM tdb_cur INTO @tdb_fk;
END
CLOSE tdb_cur; DEALLOCATE tdb_cur;

-- notifications FKs
DECLARE @notif_fk NVARCHAR(200);
DECLARE notif_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('notifications');
OPEN notif_cur;
FETCH NEXT FROM notif_cur INTO @notif_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE notifications DROP CONSTRAINT [' + @notif_fk + ']');
    FETCH NEXT FROM notif_cur INTO @notif_fk;
END
CLOSE notif_cur; DEALLOCATE notif_cur;

-- class_sessions FKs
DECLARE @cs_fk NVARCHAR(200);
DECLARE cs_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('class_sessions');
OPEN cs_cur;
FETCH NEXT FROM cs_cur INTO @cs_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE class_sessions DROP CONSTRAINT [' + @cs_fk + ']');
    FETCH NEXT FROM cs_cur INTO @cs_fk;
END
CLOSE cs_cur; DEALLOCATE cs_cur;

-- sections FKs
DECLARE @sec_fk NVARCHAR(200);
DECLARE sec_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('sections');
OPEN sec_cur;
FETCH NEXT FROM sec_cur INTO @sec_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE sections DROP CONSTRAINT [' + @sec_fk + ']');
    FETCH NEXT FROM sec_cur INTO @sec_fk;
END
CLOSE sec_cur; DEALLOCATE sec_cur;

-- students FKs
DECLARE @stu_fk NVARCHAR(200);
DECLARE stu_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('students');
OPEN stu_cur;
FETCH NEXT FROM stu_cur INTO @stu_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE students DROP CONSTRAINT [' + @stu_fk + ']');
    FETCH NEXT FROM stu_cur INTO @stu_fk;
END
CLOSE stu_cur; DEALLOCATE stu_cur;

-- rules FKs
DECLARE @rul_fk NVARCHAR(200);
DECLARE rul_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('rules');
OPEN rul_cur;
FETCH NEXT FROM rul_cur INTO @rul_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE rules DROP CONSTRAINT [' + @rul_fk + ']');
    FETCH NEXT FROM rul_cur INTO @rul_fk;
END
CLOSE rul_cur; DEALLOCATE rul_cur;

-- section_course_assignments FKs
DECLARE @sca_fk NVARCHAR(200);
DECLARE sca_cur CURSOR FOR
    SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('section_course_assignments');
OPEN sca_cur;
FETCH NEXT FROM sca_cur INTO @sca_fk;
WHILE @@FETCH_STATUS = 0
BEGIN
    EXEC('ALTER TABLE section_course_assignments DROP CONSTRAINT [' + @sca_fk + ']');
    FETCH NEXT FROM sca_cur INTO @sca_fk;
END
CLOSE sca_cur; DEALLOCATE sca_cur;
GO

-- ── Now drop tables in safe child-to-parent order ────────────────────────────
IF OBJECT_ID('substitute_assignments',        'U') IS NOT NULL DROP TABLE substitute_assignments;
IF OBJECT_ID('course_teacher_assignments',    'U') IS NOT NULL DROP TABLE course_teacher_assignments;
IF OBJECT_ID('batch_dept_sections',           'U') IS NOT NULL DROP TABLE batch_dept_sections;
IF OBJECT_ID('timetable_db',                  'U') IS NOT NULL DROP TABLE timetable_db;
IF OBJECT_ID('notifications',                 'U') IS NOT NULL DROP TABLE notifications;
IF OBJECT_ID('schedule_adjustment_requests',  'U') IS NOT NULL DROP TABLE schedule_adjustment_requests;
IF OBJECT_ID('section_course_assignments',    'U') IS NOT NULL DROP TABLE section_course_assignments;
IF OBJECT_ID('teacher_assignments',           'U') IS NOT NULL DROP TABLE teacher_assignments;
IF OBJECT_ID('class_sessions',                'U') IS NOT NULL DROP TABLE class_sessions;
IF OBJECT_ID('rules',                         'U') IS NOT NULL DROP TABLE rules;
IF OBJECT_ID('students',                      'U') IS NOT NULL DROP TABLE students;
IF OBJECT_ID('sections',                      'U') IS NOT NULL DROP TABLE sections;
IF OBJECT_ID('schedules',                     'U') IS NOT NULL DROP TABLE schedules;
IF OBJECT_ID('classrooms',                    'U') IS NOT NULL DROP TABLE classrooms;
IF OBJECT_ID('batches',                       'U') IS NOT NULL DROP TABLE batches;
IF OBJECT_ID('departments',                   'U') IS NOT NULL DROP TABLE departments;
IF OBJECT_ID('courses',                       'U') IS NOT NULL DROP TABLE courses;
IF OBJECT_ID('users',                         'U') IS NOT NULL DROP TABLE users;
GO

-- =============================================================================
--  STEP 2: CREATE TABLES
-- =============================================================================

-- ── users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    uid              VARCHAR(50)  PRIMARY KEY,
    name             VARCHAR(100),
    email            VARCHAR(100) UNIQUE,
    role             VARCHAR(50),
    password         VARCHAR(100),
    notificationList TEXT,
    level            INT
);
GO

-- ── courses ──────────────────────────────────────────────────────────────────
CREATE TABLE courses (
    courseCode  VARCHAR(50) PRIMARY KEY,
    courseName  VARCHAR(100),
    description TEXT,
    credits     INT
);
GO

-- ── departments ──────────────────────────────────────────────────────────────
CREATE TABLE departments (
    deptId   VARCHAR(50)  PRIMARY KEY,
    deptName VARCHAR(100) UNIQUE
);
GO

-- ── batches ──────────────────────────────────────────────────────────────────
CREATE TABLE batches (
    batchId   VARCHAR(50) PRIMARY KEY,
    batchYear VARCHAR(20) UNIQUE
);
GO

-- ── classrooms ───────────────────────────────────────────────────────────────
CREATE TABLE classrooms (
    roomId       VARCHAR(50)  PRIMARY KEY,
    roomName     VARCHAR(100),
    capacity     INT,
    hasProjector BIT,
    location     VARCHAR(100)
);
GO

-- ── schedules ────────────────────────────────────────────────────────────────
CREATE TABLE schedules (
    scheduleId VARCHAR(50) PRIMARY KEY,
    startDate  DATE,
    endDate    DATE,
    classList  TEXT
);
GO

-- ── sections ─────────────────────────────────────────────────────────────────
CREATE TABLE sections (
    sectionId    VARCHAR(50) PRIMARY KEY,
    sectionName  VARCHAR(50),
    startTime    TIME,
    endTime      TIME,
    timetableSlot VARCHAR(50),
    roomNumber   VARCHAR(50),
    teacherId    VARCHAR(50),
    FOREIGN KEY (teacherId) REFERENCES users(uid)
);
GO

-- ── students ─────────────────────────────────────────────────────────────────
CREATE TABLE students (
    uid     VARCHAR(50) PRIMARY KEY,
    rollNo  VARCHAR(50),
    batch   VARCHAR(50),
    dept    VARCHAR(50),
    section VARCHAR(50),
    FOREIGN KEY (uid) REFERENCES users(uid)
);
GO

-- ── rules ────────────────────────────────────────────────────────────────────
CREATE TABLE rules (
    ruleId      VARCHAR(50)  PRIMARY KEY,
    ruleName    VARCHAR(100),
    description TEXT,
    type        VARCHAR(50),
    value       VARCHAR(255),
    isActive    BIT,
    roomId      VARCHAR(50)  NULL,
    FOREIGN KEY (roomId) REFERENCES classrooms(roomId)
);
GO

-- ── class_sessions ───────────────────────────────────────────────────────────
CREATE TABLE class_sessions (
    sessionId     VARCHAR(50) PRIMARY KEY,
    startTime     TIME,
    endTime       TIME,
    status        VARCHAR(50),
    timetableSlot VARCHAR(50),
    roomNumber    VARCHAR(50),
    courseId      VARCHAR(50),
    sectionId     VARCHAR(50),
    FOREIGN KEY (roomNumber) REFERENCES classrooms(roomId),
    FOREIGN KEY (courseId)   REFERENCES courses(courseCode),
    FOREIGN KEY (sectionId)  REFERENCES sections(sectionId)
);
GO

-- ── schedule_adjustment_requests (UC-02: Room Swap) ──────────────────────────
--  Uses requestId (not assignmentId) — correct column name.
CREATE TABLE schedule_adjustment_requests (
    requestId   VARCHAR(40)   NOT NULL PRIMARY KEY,   -- SAR-XXXXXXXX
    classId     NVARCHAR(100) NOT NULL,               -- FK → class_sessions.sessionId
    reason      NVARCHAR(500) NOT NULL,
    requestType VARCHAR(30)   NOT NULL DEFAULT 'ROOM_SWAP',
    capacity    INT           NOT NULL DEFAULT 0,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    createdAt   DATETIME      NOT NULL DEFAULT GETDATE()
);
GO

CREATE INDEX idx_sar_classId ON schedule_adjustment_requests(classId);
CREATE INDEX idx_sar_status  ON schedule_adjustment_requests(status);
GO

-- ── timetable_db ─────────────────────────────────────────────────────────────
CREATE TABLE timetable_db (
    entryId   VARCHAR(50) PRIMARY KEY,
    sessionId VARCHAR(50),
    dataType  VARCHAR(50),
    dataRange VARCHAR(100),
    dataValue TEXT,
    FOREIGN KEY (sessionId) REFERENCES class_sessions(sessionId)
);
GO

-- ── notifications ────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    notificationId   VARCHAR(50) PRIMARY KEY,
    message          TEXT,
    timestamp        DATETIME,
    targetUserId     VARCHAR(50),
    notificationType VARCHAR(50),
    FOREIGN KEY (targetUserId) REFERENCES users(uid)
);
GO

-- ── section_course_assignments ───────────────────────────────────────────────
CREATE TABLE section_course_assignments (
    assignmentId INT PRIMARY KEY IDENTITY(1,1),
    section      VARCHAR(50),
    courseId     VARCHAR(50),
    FOREIGN KEY (courseId) REFERENCES courses(courseCode)
);
GO

-- ── teacher_assignments (master timetable) ───────────────────────────────────
--  Includes sessionStatus column (UC-02 room-swap status tracking).
CREATE TABLE teacher_assignments (
    assignmentId  INT          PRIMARY KEY IDENTITY(1,1),
    teacherUid    VARCHAR(50),
    courseCode    VARCHAR(50),
    sectionName   VARCHAR(50),
    deptId        VARCHAR(50),
    batchId       VARCHAR(50),
    dayOfWeek     VARCHAR(20),
    startTime     TIME,
    endTime       TIME,
    roomId        VARCHAR(50),
    sessionStatus VARCHAR(30)  NOT NULL DEFAULT 'ON-TIME',
    FOREIGN KEY (teacherUid) REFERENCES users(uid),
    FOREIGN KEY (courseCode) REFERENCES courses(courseCode),
    FOREIGN KEY (deptId)     REFERENCES departments(deptId),
    FOREIGN KEY (batchId)    REFERENCES batches(batchId),
    FOREIGN KEY (roomId)     REFERENCES classrooms(roomId)
);
GO

-- Unique constraint: no duplicate slot per teacher
ALTER TABLE teacher_assignments
ADD CONSTRAINT uq_teacher_assignment_slot
UNIQUE (teacherUid, courseCode, sectionName, dayOfWeek, startTime, endTime);
GO

-- ── substitute_assignments (UC-08) ───────────────────────────────────────────
CREATE TABLE substitute_assignments (
    substituteId         VARCHAR(50)  PRIMARY KEY,
    assignmentId         INT          NOT NULL,
    originalTeacherUid   VARCHAR(50)  NOT NULL,
    substituteTeacherUid VARCHAR(50)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason               VARCHAR(500) NULL,
    createdAt            DATETIME     NOT NULL DEFAULT GETDATE(),
    respondedAt          DATETIME     NULL,
    CONSTRAINT fk_sub_assignment  FOREIGN KEY (assignmentId)         REFERENCES teacher_assignments(assignmentId),
    CONSTRAINT fk_sub_original    FOREIGN KEY (originalTeacherUid)   REFERENCES users(uid),
    CONSTRAINT fk_sub_substitute  FOREIGN KEY (substituteTeacherUid) REFERENCES users(uid),
    CONSTRAINT chk_sub_status     CHECK (status IN ('PENDING','ACCEPTED','REJECTED','REQUESTED_BY_TEACHER'))
);
GO

-- ── course_teacher_assignments (fine-grained teacher mapping) ────────────────
CREATE TABLE course_teacher_assignments (
    assignmentId INT          PRIMARY KEY IDENTITY(1,1),
    dept         VARCHAR(100) NOT NULL,
    batch        VARCHAR(50)  NOT NULL,
    section      VARCHAR(50)  NOT NULL,
    courseId     VARCHAR(50)  NOT NULL,
    teacherId    VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_cta         UNIQUE (dept, batch, section, courseId),
    CONSTRAINT FK_cta_course  FOREIGN KEY (courseId)  REFERENCES courses(courseCode),
    CONSTRAINT FK_cta_teacher FOREIGN KEY (teacherId) REFERENCES users(uid)
);
GO

-- ── batch_dept_sections (Admin-managed section registry) ─────────────────────
CREATE TABLE batch_dept_sections (
    id          INT         PRIMARY KEY IDENTITY(1,1),
    deptId      VARCHAR(50),
    batchId     VARCHAR(50),
    sectionName VARCHAR(50),
    CONSTRAINT uq_bds UNIQUE (deptId, batchId, sectionName),
    FOREIGN KEY (deptId)  REFERENCES departments(deptId),
    FOREIGN KEY (batchId) REFERENCES batches(batchId)
);
GO

-- =============================================================================
--  STEP 3: SEED REFERENCE DATA
-- =============================================================================

-- ── Users: Admin ─────────────────────────────────────────────────────────────
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('ADM-001', 'Admin User', 'admin@sapcis.edu', 'Admin', 'admin123', 1);

-- ── Users: Core Teachers (one per department) ────────────────────────────────
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('T-CS-001', 'Dr. Ahmed Khan',   'ahmed.khan@sapcis.edu',   'Teacher', 'teacher123', 2);
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('T-SE-001', 'Dr. Sara Ali',     'sara.ali@sapcis.edu',     'Teacher', 'teacher123', 2);
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('T-IT-001', 'Dr. Usman Tariq',  'usman.tariq@sapcis.edu',  'Teacher', 'teacher123', 2);
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('T-AI-001', 'Dr. Fatima Noor',  'fatima.noor@sapcis.edu',  'Teacher', 'teacher123', 2);

-- ── Users: Students ──────────────────────────────────────────────────────────
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('S-CS-001', 'Ali Hassan',    'ali@sapcis.edu',    'Student', 'student123', 3);
INSERT INTO users (uid, name, email, role, password, level)
VALUES ('S-SE-001', 'Zainab Malik',  'zainab@sapcis.edu', 'Student', 'student123', 3);
GO

-- ── Students table ───────────────────────────────────────────────────────────
INSERT INTO students (uid, rollNo, batch, dept, section)
VALUES ('S-CS-001', 'CS-2024-001', '2024', 'Computer Science', 'A');
INSERT INTO students (uid, rollNo, batch, dept, section)
VALUES ('S-SE-001', 'SE-2024-001', '2024', 'Software Engineering', 'A');
GO

-- ── Departments ──────────────────────────────────────────────────────────────
INSERT INTO departments (deptId, deptName) VALUES ('CS', 'Computer Science');
INSERT INTO departments (deptId, deptName) VALUES ('SE', 'Software Engineering');
INSERT INTO departments (deptId, deptName) VALUES ('IT', 'Information Technology');
INSERT INTO departments (deptId, deptName) VALUES ('AI', 'Artificial Intelligence');
GO

-- ── Batches ──────────────────────────────────────────────────────────────────
INSERT INTO batches (batchId, batchYear) VALUES ('B2022', '2022');
INSERT INTO batches (batchId, batchYear) VALUES ('B2023', '2023');
INSERT INTO batches (batchId, batchYear) VALUES ('B2024', '2024');
INSERT INTO batches (batchId, batchYear) VALUES ('B2025', '2025');
GO

-- ── Courses ──────────────────────────────────────────────────────────────────
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('CS101', 'Introduction to Programming',  'Foundations of coding',          3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('CS201', 'Data Structures & Algorithms', 'Arrays, trees, graphs',          3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('CS202', 'Data Structures & Algorithms', 'Arrays, trees, graphs (v2)',     4);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('CS301', 'Database Systems',             'SQL, normalisation, JDBC',       3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('SE301', 'Software Engineering',         'SDLC, GRASP, patterns',          3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('IT201', 'Networking Fundamentals',      'TCP/IP, routing, protocols',     3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('AI101', 'Introduction to AI',           'Search, ML basics',              3);
INSERT INTO courses (courseCode, courseName, description, credits)
VALUES ('DB401', 'Database Systems (Advanced)',  'Transactions, optimisation',     3);
GO

-- ── Classrooms ───────────────────────────────────────────────────────────────
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R-101', 'CS Lab 1',        40, 1, 'Block A - Ground Floor');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R-102', 'Computer Lab 2',  35, 1, 'Block A - 1st Floor');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R-201', 'Lecture Hall 1',  80, 1, 'Block B - Ground Floor');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R-202', 'Seminar Room 1',  30, 0, 'Block B - 2nd Floor');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R-301', 'AI Research Lab', 25, 1, 'Block C - 3rd Floor');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R101',  'Room 101',        40, 1, 'Block A');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('R102',  'Room 102',        40, 1, 'Block A');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('LAB1',  'Computer Lab 1',  30, 1, 'Block B');
INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location)
VALUES ('LAB2',  'Computer Lab 2',  30, 1, 'Block B');
GO

-- ── Sections ─────────────────────────────────────────────────────────────────
-- Times aligned with teacher_assignments and class_sessions (slot1 = 08:30–10:00)
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-CS-A', 'A', '08:30', '10:00', 'Monday 08:30',    'R-101', 'T-CS-001');
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-CS-B', 'B', '10:15', '11:45', 'Monday 10:15',    'R-102', 'T-CS-001');
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-SE-A', 'A', '10:15', '11:45', 'Tuesday 10:15',   'R-201', 'T-SE-001');
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-IT-A', 'A', '12:00', '13:30', 'Wednesday 12:00', 'R-202', 'T-IT-001');
-- Dashboard demo sections
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-A', 'A', '08:30', '10:00', 'Monday 08:30',    'R101',  NULL);
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-B', 'B', '08:30', '10:00', 'Monday 08:30',    'R102',  NULL);
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-C', 'C', '10:15', '11:45', 'Monday 10:15',    'LAB1',  NULL);
INSERT INTO sections (sectionId, sectionName, startTime, endTime, timetableSlot, roomNumber, teacherId)
VALUES ('SEC-D', 'D', '12:00', '13:30', 'Monday 12:00',    'LAB2',  NULL);
GO

-- ── Section ↔ Course Assignments ─────────────────────────────────────────────
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'CS101');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'CS201');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'CS301');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'SE301');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'IT201');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'AI101');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'CS202');
INSERT INTO section_course_assignments (section, courseId) VALUES ('A', 'DB401');
INSERT INTO section_course_assignments (section, courseId) VALUES ('B', 'CS101');
INSERT INTO section_course_assignments (section, courseId) VALUES ('B', 'CS201');
INSERT INTO section_course_assignments (section, courseId) VALUES ('B', 'CS202');
INSERT INTO section_course_assignments (section, courseId) VALUES ('B', 'DB401');
INSERT INTO section_course_assignments (section, courseId) VALUES ('C', 'CS101');
INSERT INTO section_course_assignments (section, courseId) VALUES ('C', 'CS202');
INSERT INTO section_course_assignments (section, courseId) VALUES ('C', 'SE301');
INSERT INTO section_course_assignments (section, courseId) VALUES ('C', 'DB401');
INSERT INTO section_course_assignments (section, courseId) VALUES ('D', 'CS101');
INSERT INTO section_course_assignments (section, courseId) VALUES ('D', 'CS202');
INSERT INTO section_course_assignments (section, courseId) VALUES ('D', 'SE301');
INSERT INTO section_course_assignments (section, courseId) VALUES ('D', 'DB401');
GO

-- =============================================================================
--  STEP 4: SEED CLASS SESSIONS (weekly timetable)
-- =============================================================================
--
--  All sessions comply with campus rules:
--    Opening: 08:30  |  Closing: 18:00
--    Max duration: 90 min  |  Rest gap: ≥15 min  |  Max 3 classes/day/teacher
--    No teacher overlap  |  No room overlap on same day/time
--
--  Time slots used:
--    Slot 1: 08:30 – 10:00  (90 min)
--    Slot 2: 10:15 – 11:45  (90 min, 15 min gap after slot 1)
--    Slot 3: 12:00 – 13:30  (90 min, 15 min gap after slot 2)
--    Slot 4: 14:00 – 15:30  (90 min, 30 min gap after slot 3)
--    Slot 5: 15:45 – 17:15  (90 min, 15 min gap after slot 4)
-- =============================================================================

-- ── Core sessions — CS Section A (SEC-CS-A) ──────────────────────────────────
-- T-CS-001: Monday slot1 CS101, slot2 CS201 (2 classes, gap=15min ✓)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-MON-CS101-A', '08:30', '10:00', 'UPCOMING', 'Monday 08:30', 'R-101', 'CS101', 'SEC-CS-A');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-MON-CS201-A', '10:15', '11:45', 'UPCOMING', 'Monday 10:15', 'R-102', 'CS201', 'SEC-CS-A');

-- T-CS-001: Tuesday slot1 CS301 (1 class)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-TUE-CS301-A', '08:30', '10:00', 'UPCOMING', 'Tuesday 08:30', 'R-201', 'CS301', 'SEC-CS-A');

-- T-SE-001: Tuesday slot2 SE301 (different teacher, same room R-201 is free at 10:15 ✓)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-TUE-SE301-A', '10:15', '11:45', 'UPCOMING', 'Tuesday 10:15', 'R-201', 'SE301', 'SEC-SE-A');

-- T-CS-001: Wednesday slot1 CS101 (1 class)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-WED-CS101-A', '08:30', '10:00', 'UPCOMING', 'Wednesday 08:30', 'R-101', 'CS101', 'SEC-CS-A');

-- T-IT-001: Wednesday slot3 IT201 (different teacher, different room ✓)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-WED-IT201-A', '12:00', '13:30', 'UPCOMING', 'Wednesday 12:00', 'R-202', 'IT201', 'SEC-IT-A');

-- T-CS-001: Thursday slot1 CS201 (1 class)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-THU-CS201-A', '08:30', '10:00', 'UPCOMING', 'Thursday 08:30', 'R-102', 'CS201', 'SEC-CS-A');

-- T-AI-001: Thursday slot2 AI101 (different teacher, different room ✓)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-THU-AI101-A', '10:15', '11:45', 'UPCOMING', 'Thursday 10:15', 'R-301', 'AI101', 'SEC-CS-A');

-- T-CS-001: Friday slot1 CS301 (1 class)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-FRI-CS301-A', '08:30', '10:00', 'UPCOMING', 'Friday 08:30', 'R-201', 'CS301', 'SEC-CS-A');

-- T-SE-001: Friday slot2 SE301 (different teacher, same room R-201 free at 10:15 ✓)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-FRI-SE301-A', '10:15', '11:45', 'UPCOMING', 'Friday 10:15', 'R-201', 'SE301', 'SEC-SE-A');

-- ── Dashboard demo sessions — Section A (SEC-A) ──────────────────────────────
-- Uses R101 (cap 40). Teacher assignments drive these sessions.
-- Monday: CS101 slot1 (UPCOMING)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-A-CS101',   '08:30', '10:00', 'UPCOMING',  'Monday 08:30',    'R101', 'CS101', 'SEC-A');
-- Tuesday: CS202 slot2 (ONGOING — currently in progress)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-A-CS202',   '10:15', '11:45', 'ONGOING',   'Tuesday 10:15',   'R101', 'CS202', 'SEC-A');
-- Wednesday: SE301 slot4 (DELAYED — teacher running late)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-A-SE301',   '14:00', '15:30', 'DELAYED',   'Wednesday 14:00', 'LAB1', 'SE301', 'SEC-A');
-- Thursday: DB401 slot2 (CANCELLED — room issue)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-A-DB401',   '10:15', '11:45', 'CANCELLED', 'Thursday 10:15',  'LAB2', 'DB401', 'SEC-A');
-- Friday: CS101 slot3 (UPCOMING)
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-A-CS101-F', '12:00', '13:30', 'UPCOMING',  'Friday 12:00',    'R101', 'CS101', 'SEC-A');

-- ── Dashboard demo sessions — Section B (SEC-B) ──────────────────────────────
-- Uses R102 (cap 40). Different rooms from Section A — no room conflict.
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-B-CS101',   '08:30', '10:00', 'UPCOMING', 'Monday 08:30',    'R102', 'CS101', 'SEC-B');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-B-CS202',   '10:15', '11:45', 'ONGOING',  'Tuesday 10:15',   'R102', 'CS202', 'SEC-B');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-B-SE301',   '14:00', '15:30', 'UPCOMING', 'Wednesday 14:00', 'R102', 'SE301', 'SEC-B');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-B-DB401',   '10:15', '11:45', 'UPCOMING', 'Thursday 10:15',  'LAB1', 'DB401', 'SEC-B');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-B-CS101-F', '12:00', '13:30', 'UPCOMING', 'Friday 12:00',    'R102', 'CS101', 'SEC-B');

-- ── Dashboard demo sessions — Section C (SEC-C) ──────────────────────────────
-- Uses LAB1 (cap 30). Staggered from A/B to avoid room conflicts.
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-C-CS101',   '10:15', '11:45', 'UPCOMING', 'Monday 10:15',    'LAB1', 'CS101', 'SEC-C');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-C-CS202',   '08:30', '10:00', 'UPCOMING', 'Tuesday 08:30',   'LAB1', 'CS202', 'SEC-C');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-C-SE301',   '10:15', '11:45', 'ONGOING',  'Wednesday 10:15', 'LAB1', 'SE301', 'SEC-C');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-C-DB401',   '08:30', '10:00', 'UPCOMING', 'Thursday 08:30',  'LAB1', 'DB401', 'SEC-C');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-C-CS101-F', '14:00', '15:30', 'UPCOMING', 'Friday 14:00',    'LAB1', 'CS101', 'SEC-C');

-- ── Dashboard demo sessions — Section D (SEC-D) ──────────────────────────────
-- Uses LAB2 (cap 30). Afternoon slots to spread load across the day.
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-D-CS101',   '12:00', '13:30', 'UPCOMING', 'Monday 12:00',    'LAB2', 'CS101', 'SEC-D');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-D-CS202',   '14:00', '15:30', 'UPCOMING', 'Tuesday 14:00',   'LAB2', 'CS202', 'SEC-D');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-D-SE301',   '15:45', '17:15', 'UPCOMING', 'Wednesday 15:45', 'LAB2', 'SE301', 'SEC-D');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-D-DB401',   '15:45', '17:15', 'ONGOING',  'Thursday 15:45',  'LAB2', 'DB401', 'SEC-D');
INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId)
VALUES ('SES-D-CS101-F', '15:45', '17:15', 'UPCOMING', 'Friday 15:45',    'LAB2', 'CS101', 'SEC-D');
GO

-- =============================================================================
--  STEP 5: SEED TIMETABLE_DB (Student Enrollments)
-- =============================================================================

-- Core student S-CS-001 enrollments
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-001', 'SES-MON-CS101-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-002', 'SES-MON-CS201-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-003', 'SES-TUE-CS301-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-004', 'SES-TUE-SE301-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-005', 'SES-WED-IT201-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-006', 'SES-WED-CS101-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-007', 'SES-THU-CS201-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-008', 'SES-THU-AI101-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-009', 'SES-FRI-CS301-A', 'STUDENT_ENROLLMENT', 'S-CS-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-010', 'SES-FRI-SE301-A', 'STUDENT_ENROLLMENT', 'S-CS-001');

-- Core student S-SE-001 enrollments
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-011', 'SES-MON-CS101-A', 'STUDENT_ENROLLMENT', 'S-SE-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-012', 'SES-TUE-SE301-A', 'STUDENT_ENROLLMENT', 'S-SE-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-013', 'SES-WED-CS101-A', 'STUDENT_ENROLLMENT', 'S-SE-001');
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES ('ENR-014', 'SES-FRI-SE301-A', 'STUDENT_ENROLLMENT', 'S-SE-001');
GO

-- Backfill enrollments for all students into their section's sessions
INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue)
SELECT
    LOWER(CONVERT(VARCHAR(36), NEWID())),
    cs.sessionId,
    'STUDENT_ENROLLMENT',
    CAST(st.uid AS VARCHAR(50))
FROM students st
JOIN sections s
    ON LTRIM(RTRIM(s.sectionName)) = LTRIM(RTRIM(st.section))
JOIN class_sessions cs
    ON cs.sectionId = s.sectionId
WHERE NOT EXISTS (
    SELECT 1 FROM timetable_db t
    WHERE t.sessionId = cs.sessionId
      AND t.dataType  = 'STUDENT_ENROLLMENT'
      AND LTRIM(RTRIM(CAST(t.dataValue AS VARCHAR(50)))) = LTRIM(RTRIM(st.uid))
);
GO

-- =============================================================================
--  STEP 6: SEED TEACHER_ASSIGNMENTS (master timetable)
-- =============================================================================
--
--  Rules verified for each teacher:
--
--  T-CS-001 (Dr. Ahmed Khan) — teaches CS101, CS201, CS301 (3 courses ≤ 4 ✓)
--    Monday:    08:30–10:00 CS101 (slot1), 10:15–11:45 CS201 (slot2) → 2 classes, gap=15min ✓
--    Tuesday:   08:30–10:00 CS301 (slot1) → 1 class ✓
--    Wednesday: 08:30–10:00 CS101 (slot1) → 1 class ✓
--    Thursday:  08:30–10:00 CS201 (slot1) → 1 class ✓
--    Friday:    08:30–10:00 CS301 (slot1) → 1 class ✓
--    Max/day = 2 ≤ 3 ✓  |  All times ≥ 08:30 ✓  |  All end ≤ 18:00 ✓
--
--  T-SE-001 (Dr. Sara Ali) — teaches SE301 (1 course ≤ 4 ✓)
--    Tuesday:   10:15–11:45 SE301 (slot2) → 1 class ✓
--    Friday:    10:15–11:45 SE301 (slot2) → 1 class ✓
--    No overlap with T-CS-001 (different teacher) ✓
--
--  T-IT-001 (Dr. Usman Tariq) — teaches IT201 (1 course ≤ 4 ✓)
--    Wednesday: 12:00–13:30 IT201 (slot3) → 1 class ✓
--    Gap from T-CS-001 Wed slot1 end (10:00): different teacher, no constraint ✓
--
--  T-AI-001 (Dr. Fatima Noor) — teaches AI101 (1 course ≤ 4 ✓)
--    Thursday:  10:15–11:45 AI101 (slot2) → 1 class ✓
--    Gap from T-CS-001 Thu slot1 end (10:00): different teacher, no constraint ✓
--
--  Room conflicts verified: each room used only once per day/slot ✓
-- =============================================================================

-- T-CS-001: Monday — CS101 (slot1) then CS201 (slot2), gap = 15 min ✓
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS101', 'A', 'CS', 'B2024', 'Monday',    '08:30', '10:00', 'R-101');
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS201', 'A', 'CS', 'B2024', 'Monday',    '10:15', '11:45', 'R-102');

-- T-CS-001: Tuesday — CS301 (slot1)
-- T-SE-001: Tuesday — SE301 (slot2, different teacher, R-201 free at 10:15 ✓)
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS301', 'A', 'CS', 'B2024', 'Tuesday',   '08:30', '10:00', 'R-201');
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-SE-001', 'SE301', 'A', 'SE', 'B2024', 'Tuesday',   '10:15', '11:45', 'R-201');

-- T-CS-001: Wednesday — CS101 (slot1)
-- T-IT-001: Wednesday — IT201 (slot3, different teacher, different room ✓)
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS101', 'A', 'CS', 'B2024', 'Wednesday', '08:30', '10:00', 'R-101');
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-IT-001', 'IT201', 'A', 'IT', 'B2024', 'Wednesday', '12:00', '13:30', 'R-202');

-- T-CS-001: Thursday — CS201 (slot1)
-- T-AI-001: Thursday — AI101 (slot2, different teacher, different room ✓)
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS201', 'A', 'CS', 'B2024', 'Thursday',  '08:30', '10:00', 'R-102');
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-AI-001', 'AI101', 'A', 'CS', 'B2024', 'Thursday',  '10:15', '11:45', 'R-301');

-- T-CS-001: Friday — CS301 (slot1)
-- T-SE-001: Friday — SE301 (slot2, different teacher, R-201 free at 10:15 ✓)
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-CS-001', 'CS301', 'A', 'CS', 'B2024', 'Friday',    '08:30', '10:00', 'R-201');
INSERT INTO teacher_assignments (teacherUid, courseCode, sectionName, deptId, batchId, dayOfWeek, startTime, endTime, roomId)
VALUES ('T-SE-001', 'SE301', 'A', 'SE', 'B2024', 'Friday',    '10:15', '11:45', 'R-201');
GO

-- =============================================================================
--  STEP 7: SEED BATCH_DEPT_SECTIONS
-- =============================================================================
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('CS', 'B2024', 'A');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('CS', 'B2024', 'B');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('SE', 'B2024', 'A');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('IT', 'B2024', 'A');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('AI', 'B2024', 'A');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('CS', 'B2025', 'A');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('CS', 'B2025', 'B');
INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES ('SE', 'B2025', 'A');
GO

-- =============================================================================
--  STEP 8: SEED COURSE_TEACHER_ASSIGNMENTS (core mappings)
-- =============================================================================
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Computer Science',       '2024', 'A', 'CS101', 'T-CS-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Computer Science',       '2024', 'A', 'CS201', 'T-CS-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Computer Science',       '2024', 'A', 'CS301', 'T-CS-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Computer Science',       '2024', 'A', 'AI101', 'T-AI-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Software Engineering',   '2024', 'A', 'SE301', 'T-SE-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Software Engineering',   '2024', 'A', 'CS101', 'T-CS-001');
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
VALUES ('Information Technology', '2024', 'A', 'IT201', 'T-IT-001');
GO

-- =============================================================================
--  STEP 9: SEED DASHBOARD DEMO TEACHERS (one per dept/batch/section/course)
-- =============================================================================
;WITH Depts(dept, deptCode) AS (
    SELECT 'Computer Science',       'CS' UNION ALL
    SELECT 'Software Engineering',   'SE' UNION ALL
    SELECT 'Information Technology', 'IT'
),
Batches(batch) AS (
    SELECT '2024' UNION ALL SELECT '2025'
),
Sections(section) AS (
    SELECT 'A' UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D'
),
Courses(courseCode, shortName) AS (
    SELECT 'CS101', 'Programming' UNION ALL
    SELECT 'CS202', 'DSA'         UNION ALL
    SELECT 'SE301', 'SE'          UNION ALL
    SELECT 'DB401', 'Databases'
),
FirstNames(idx, fn) AS (
    SELECT 1,'Muhammad' UNION ALL SELECT 2,'Ayesha' UNION ALL
    SELECT 3,'Ahmed'    UNION ALL SELECT 4,'Fatima' UNION ALL
    SELECT 5,'Bilal'    UNION ALL SELECT 6,'Hina'   UNION ALL
    SELECT 7,'Usman'    UNION ALL SELECT 8,'Sana'
),
LastNames(idx, ln) AS (
    SELECT 1,'Khan'     UNION ALL SELECT 2,'Ahmed'    UNION ALL
    SELECT 3,'Raza'     UNION ALL SELECT 4,'Siddiqui' UNION ALL
    SELECT 5,'Malik'    UNION ALL SELECT 6,'Qureshi'  UNION ALL
    SELECT 7,'Iqbal'    UNION ALL SELECT 8,'Sheikh'
),
Assignment AS (
    SELECT
        d.dept, d.deptCode, b.batch, s.section, c.courseCode, c.shortName,
        ((ABS(CHECKSUM(d.deptCode + b.batch + s.section + c.courseCode))) % 8) + 1 AS fnIdx,
        ((ABS(CHECKSUM(c.courseCode + s.section + b.batch + d.deptCode))) % 8) + 1 AS lnIdx,
        CONCAT('T-', d.deptCode, '-', b.batch, '-', s.section, '-', c.courseCode) AS uid
    FROM Depts d CROSS JOIN Batches b CROSS JOIN Sections s CROSS JOIN Courses c
)
INSERT INTO users (uid, name, email, role, password)
SELECT
    a.uid,
    fn.fn + ' ' + ln.ln + ' (' + a.shortName + ', ' + a.section + '-' + a.batch + ', ' + a.deptCode + ')',
    LOWER(REPLACE(fn.fn,' ','') + '.' + REPLACE(ln.ln,' ','')) + '.' + LOWER(a.uid) + '@sapcis.edu',
    'Teacher',
    'teacher123'
FROM Assignment a
JOIN FirstNames fn ON fn.idx = a.fnIdx
JOIN LastNames  ln ON ln.idx = a.lnIdx
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.uid = a.uid);
GO

-- Course-teacher assignments for dashboard demo teachers
;WITH Depts(dept, deptCode) AS (
    SELECT 'Computer Science',       'CS' UNION ALL
    SELECT 'Software Engineering',   'SE' UNION ALL
    SELECT 'Information Technology', 'IT'
),
Batches(batch) AS (SELECT '2024' UNION ALL SELECT '2025'),
Sections(section) AS (SELECT 'A' UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D'),
Courses(courseCode) AS (SELECT 'CS101' UNION ALL SELECT 'CS202' UNION ALL SELECT 'SE301' UNION ALL SELECT 'DB401')
INSERT INTO course_teacher_assignments (dept, batch, section, courseId, teacherId)
SELECT d.dept, b.batch, s.section, c.courseCode,
       CONCAT('T-', d.deptCode, '-', b.batch, '-', s.section, '-', c.courseCode)
FROM Depts d CROSS JOIN Batches b CROSS JOIN Sections s CROSS JOIN Courses c
WHERE NOT EXISTS (
    SELECT 1 FROM course_teacher_assignments x
    WHERE x.dept = d.dept AND x.batch = b.batch
      AND x.section = s.section AND x.courseId = c.courseCode
);
GO

-- =============================================================================
--  STEP 10: SEED RULES (campus policies)
-- =============================================================================
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-001', 'Max Class Capacity',    'No class may exceed room capacity',          'CAPACITY',          '40',    1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-002', 'No Back-to-Back',       'Teachers must have a 30-min break',          'SCHEDULE',          '30',    1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-003', 'Max Courses/Teacher',   'Max distinct courses per teacher',           'MAX_COURSES',       '4',     1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-004', 'Max Classes/Day',       'Max classes per teacher per day',            'MAX_CLASSES_PER_DAY','3',    1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-005', 'Max Class Duration',    'Max duration of a single class (minutes)',   'MAX_DURATION',      '90',    1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-006', 'Min Rest Gap',          'Min gap between consecutive classes (min)',  'REST_GAP',          '15',    1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-007', 'University Opening',    'Campus opens at this time',                  'UNI_OPENING_TIME',  '08:30', 1);
INSERT INTO rules (ruleId, ruleName, description, type, value, isActive)
VALUES ('RULE-008', 'University Closing',    'Campus closes at this time',                 'UNI_CLOSING_TIME',  '18:00', 1);
GO

-- =============================================================================
--  STEP 11: CREATE BASELINE SNAPSHOT (for reversible time-scaling)
-- =============================================================================
--  This table is the "original" timetable reference.
--  The rebalancer always scales FROM this baseline, making changes reversible.
SELECT * INTO teacher_assignments_baseline FROM teacher_assignments;
GO

-- =============================================================================
--  DONE
-- =============================================================================
PRINT '=================================================================';
PRINT ' SAPCIS database created and fully seeded!';
PRINT '=================================================================';
PRINT '';
PRINT ' Demo Login Credentials:';
PRINT '   Admin   : admin@sapcis.edu          / admin123';
PRINT '   Teacher : ahmed.khan@sapcis.edu      / teacher123  (CS)';
PRINT '   Teacher : sara.ali@sapcis.edu        / teacher123  (SE)';
PRINT '   Teacher : usman.tariq@sapcis.edu     / teacher123  (IT)';
PRINT '   Teacher : fatima.noor@sapcis.edu     / teacher123  (AI)';
PRINT '   Student : ali@sapcis.edu             / student123  (CS, 2024, A)';
PRINT '   Student : zainab@sapcis.edu          / student123  (SE, 2024, A)';
PRINT '';
PRINT ' Tables created:';
PRINT '   users, courses, departments, batches, classrooms, schedules';
PRINT '   sections, students, rules, class_sessions';
PRINT '   schedule_adjustment_requests, timetable_db, notifications';
PRINT '   section_course_assignments, teacher_assignments';
PRINT '   substitute_assignments, course_teacher_assignments';
PRINT '   batch_dept_sections, teacher_assignments_baseline';
PRINT '=================================================================';
GO
