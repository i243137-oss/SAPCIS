export interface User {
  uid: string;
  name: string;
  email: string;
  role: 'Student' | 'Teacher' | 'Admin';
  dept?: string;
  batch?: string;
  section?: string;
}

export interface ClassSession {
  sessionId: string;
  courseId: string;
  courseName: string;
  teacherUid: string;
  teacherName: string;
  roomNumber: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  status: 'SCHEDULED' | 'ONGOING' | 'DELAYED' | 'CANCELLED' | 'SWAP_PENDING' | 'RESCHEDULED';
  sectionId: string;
  delayReason?: string;
  delayMinutes?: number;
}

export interface Classroom {
  roomId: string;
  roomName: string;
  capacity: number;
  hasProjector: boolean;
  building: string;
}

export interface SwapRequest {
  requestId: string;
  classId: string;
  courseCode: string;
  teacherName: string;
  currentRoom: string;
  targetRoom?: string;
  capacity: number;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

export interface SubstituteRequest {
  substituteId: string;
  sessionId: string;
  courseCode: string;
  originalTeacherUid: string;
  originalTeacherName: string;
  substituteTeacherUid?: string;
  substituteTeacherName?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'REQUESTED_BY_TEACHER';
  reason: string;
  createdAt: string;
}

export interface NotificationItem {
  id: string;
  message: string;
  timestamp: string;
  targetUserId: string;
  type: 'ALERT' | 'SWAP' | 'SUBSTITUTE' | 'OVERRIDE';
  read: boolean;
}

export interface SystemRule {
  ruleId: string;
  ruleName: string;
  description: string;
  type: 'GLOBAL_FACULTY' | 'ROOM_CAPACITY' | 'SCHEDULE_WINDOW';
  value: string;
  isActive: boolean;
}

export const INITIAL_USERS: User[] = [
  { uid: 'S-CS-001', name: 'Ali Ahmed', email: 'student@sapcis.edu', role: 'Student', dept: 'CS', batch: '2024', section: 'A' },
  { uid: 'S-SE-001', name: 'Bilal Khan', email: 'bilal@sapcis.edu', role: 'Student', dept: 'SE', batch: '2024', section: 'A' },
  { uid: 'T-CS-001', name: 'Dr. Tariq Mahmood', email: 'tariq@sapcis.edu', role: 'Teacher', dept: 'CS' },
  { uid: 'T-CS-002', name: 'Dr. Ayesha Malik', email: 'ayesha@sapcis.edu', role: 'Teacher', dept: 'CS' },
  { uid: 'T-SE-001', name: 'Engr. Usman Farooq', email: 'usman@sapcis.edu', role: 'Teacher', dept: 'SE' },
  { uid: 'ADM-001', name: 'Campus Registrar & Admin', email: 'admin@sapcis.edu', role: 'Admin', dept: 'Administration' }
];

export const INITIAL_CLASSROOMS: Classroom[] = [
  { roomId: 'C-101', roomName: 'CS Lecture Hall 1', capacity: 60, hasProjector: true, building: 'Block A' },
  { roomId: 'C-102', roomName: 'CS Lecture Hall 2', capacity: 45, hasProjector: true, building: 'Block A' },
  { roomId: 'C-103', roomName: 'Seminar Hall 3', capacity: 50, hasProjector: false, building: 'Block A' },
  { roomId: 'LAB-1', roomName: 'Software Engineering Lab 1', capacity: 35, hasProjector: true, building: 'Block B' },
  { roomId: 'LAB-2', roomName: 'AI & Data Systems Lab', capacity: 40, hasProjector: true, building: 'Block B' },
  { roomId: 'AUD-1', roomName: 'Main Campus Auditorium', capacity: 150, hasProjector: true, building: 'Central Block' }
];

export const INITIAL_SESSIONS: ClassSession[] = [
  {
    sessionId: 'SES-MON-CS101-A',
    courseId: 'CS101',
    courseName: 'Intro to Programming',
    teacherUid: 'T-CS-001',
    teacherName: 'Dr. Tariq Mahmood',
    roomNumber: 'C-101',
    dayOfWeek: 'Monday',
    startTime: '08:30',
    endTime: '10:00',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-MON-CS201-A',
    courseId: 'CS201',
    courseName: 'Data Structures & Algorithms',
    teacherUid: 'T-CS-002',
    teacherName: 'Dr. Ayesha Malik',
    roomNumber: 'C-102',
    dayOfWeek: 'Monday',
    startTime: '10:15',
    endTime: '11:45',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-TUE-CS301-A',
    courseId: 'CS301',
    courseName: 'Database Systems',
    teacherUid: 'T-CS-001',
    teacherName: 'Dr. Tariq Mahmood',
    roomNumber: 'LAB-1',
    dayOfWeek: 'Tuesday',
    startTime: '09:00',
    endTime: '10:30',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-TUE-SE301-A',
    courseId: 'SE301',
    courseName: 'Software Architecture',
    teacherUid: 'T-SE-001',
    teacherName: 'Engr. Usman Farooq',
    roomNumber: 'C-103',
    dayOfWeek: 'Tuesday',
    startTime: '11:00',
    endTime: '12:30',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-WED-IT201-A',
    courseId: 'IT201',
    courseName: 'Web Engineering',
    teacherUid: 'T-SE-001',
    teacherName: 'Engr. Usman Farooq',
    roomNumber: 'LAB-2',
    dayOfWeek: 'Wednesday',
    startTime: '08:30',
    endTime: '10:00',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-THU-AI101-A',
    courseId: 'AI101',
    courseName: 'Artificial Intelligence Foundations',
    teacherUid: 'T-CS-002',
    teacherName: 'Dr. Ayesha Malik',
    roomNumber: 'AUD-1',
    dayOfWeek: 'Thursday',
    startTime: '10:00',
    endTime: '11:30',
    status: 'SCHEDULED',
    sectionId: 'A'
  },
  {
    sessionId: 'SES-FRI-CS301-A',
    courseId: 'CS301',
    courseName: 'Database Systems Lab',
    teacherUid: 'T-CS-001',
    teacherName: 'Dr. Tariq Mahmood',
    roomNumber: 'LAB-1',
    dayOfWeek: 'Friday',
    startTime: '09:30',
    endTime: '11:00',
    status: 'SCHEDULED',
    sectionId: 'A'
  }
];

export const INITIAL_SWAP_REQUESTS: SwapRequest[] = [
  {
    requestId: 'SAR-A81BC9F1',
    classId: 'SES-TUE-SE301-A',
    courseCode: 'SE301',
    teacherName: 'Engr. Usman Farooq',
    currentRoom: 'C-103',
    targetRoom: 'LAB-1',
    capacity: 45,
    reason: 'Need high-spec projector and workstations for architecture diagram presentation',
    status: 'PENDING',
    createdAt: '2026-09-10 08:30'
  }
];

export const INITIAL_SUBSTITUTES: SubstituteRequest[] = [
  {
    substituteId: 'SUB-49F123C0',
    sessionId: 'SES-MON-CS201-A',
    courseCode: 'CS201',
    originalTeacherUid: 'T-CS-002',
    originalTeacherName: 'Dr. Ayesha Malik',
    substituteTeacherUid: 'T-CS-001',
    substituteTeacherName: 'Dr. Tariq Mahmood',
    status: 'PENDING',
    reason: 'Attending faculty research symposium in morning',
    createdAt: '2026-09-10 09:00'
  }
];

export const INITIAL_RULES: SystemRule[] = [
  {
    ruleId: 'RULE-001',
    ruleName: 'Max Classes Per Teacher Per Day',
    description: 'Prevents faculty burnout by capping consecutive instructional load.',
    type: 'GLOBAL_FACULTY',
    value: '3',
    isActive: true
  },
  {
    ruleId: 'RULE-002',
    ruleName: 'Standard Room Seating Safety Margin',
    description: 'Ensures physical room enrollment never exceeds 90% of total capacity.',
    type: 'ROOM_CAPACITY',
    value: '90%',
    isActive: true
  },
  {
    ruleId: 'RULE-003',
    ruleName: 'Minimum Inter-Session Gap',
    description: 'Enforces student and teacher transition buffer between scheduled sessions.',
    type: 'SCHEDULE_WINDOW',
    value: '15 mins',
    isActive: true
  }
];

export const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 'NOTIF-1',
    message: 'Welcome to SAPCIS Semester Portal. Check your live schedule for real-time room and instructor assignments.',
    timestamp: 'Just now',
    targetUserId: 'S-CS-001',
    type: 'ALERT',
    read: false
  },
  {
    id: 'NOTIF-2',
    message: 'Notice: CS201 Data Structures has a pending substitute teacher assignment.',
    timestamp: '15m ago',
    targetUserId: 'S-CS-001',
    type: 'SUBSTITUTE',
    read: false
  }
];
