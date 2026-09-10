import React, { useState } from 'react';
import {
  INITIAL_USERS,
  INITIAL_CLASSROOMS,
  INITIAL_SESSIONS,
  INITIAL_SWAP_REQUESTS,
  INITIAL_SUBSTITUTES,
  INITIAL_RULES,
  INITIAL_NOTIFICATIONS,
  User,
  ClassSession,
  Classroom,
  SwapRequest,
  SubstituteRequest,
  SystemRule,
  NotificationItem
} from './data';
import {
  Calendar,
  Clock,
  MapPin,
  AlertTriangle,
  CheckCircle,
  Bell,
  LogOut,
  ShieldCheck,
  UserCheck,
  Building,
  RefreshCw,
  FileText,
  Sliders,
  Send,
  Lock,
  ArrowRight,
  Sparkles,
  Users
} from 'lucide-react';

export default function App() {
  // Authentication & Role State
  const [currentUser, setCurrentUser] = useState<User | null>(INITIAL_USERS[0]); // Default to Student S-CS-001
  const [isRegistering, setIsRegistering] = useState(false);
  
  // Registration Form State
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regDept, setRegDept] = useState('CS');
  const [regBatch, setRegBatch] = useState('2024');
  const [regSection, setRegSection] = useState('A');

  // Core Data Collections (Stateful & Interactive)
  const [sessions, setSessions] = useState<ClassSession[]>(INITIAL_SESSIONS);
  const [classrooms] = useState<Classroom[]>(INITIAL_CLASSROOMS);
  const [swapRequests, setSwapRequests] = useState<SwapRequest[]>(INITIAL_SWAP_REQUESTS);
  const [substitutes, setSubstitutes] = useState<SubstituteRequest[]>(INITIAL_SUBSTITUTES);
  const [rules, setRules] = useState<SystemRule[]>(INITIAL_RULES);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);

  // Active View Tabs
  const [activeTab, setActiveTab] = useState<string>('timetable');
  const [selectedDay, setSelectedDay] = useState<string>('All');
  const [showNotifications, setShowNotifications] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Teacher Forms
  const [delaySessionId, setDelaySessionId] = useState('');
  const [delayMins, setDelayMins] = useState(15);
  const [delayReason, setDelayReason] = useState('Traffic and faculty transit delay');
  const [actionType, setActionType] = useState<'DELAYED' | 'CANCELLED'>('DELAYED');

  const [swapSessionId, setSwapSessionId] = useState('');
  const [swapTargetRoom, setSwapTargetRoom] = useState('LAB-1');
  const [swapReason, setSwapReason] = useState('Requires multimedia projector & lab terminals');

  const [subSessionId, setSubSessionId] = useState('');
  const [subReason, setSubReason] = useState('Attending official academic conference');

  // Admin Override Form
  const [overrideSessionId, setOverrideSessionId] = useState('SES-MON-CS101-A');
  const [overrideStatus, setOverrideStatus] = useState<'SCHEDULED' | 'RESCHEDULED' | 'CANCELLED'>('RESCHEDULED');
  const [overrideRoom, setOverrideRoom] = useState('AUD-1');
  const [overrideReason, setOverrideReason] = useState('HVAC emergency maintenance in original hall');

  // Admin New Rule Form
  const [newRuleName, setNewRuleName] = useState('');
  const [newRuleType, setNewRuleType] = useState<'GLOBAL_FACULTY' | 'ROOM_CAPACITY' | 'SCHEDULE_WINDOW'>('GLOBAL_FACULTY');
  const [newRuleValue, setNewRuleValue] = useState('4');

  const showToast = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
    setFeedbackMsg({ text, type });
    setTimeout(() => setFeedbackMsg(null), 4500);
  };

  // Helper to add notification
  const addNotification = (message: string, targetUser: string, type: 'ALERT' | 'SWAP' | 'SUBSTITUTE' | 'OVERRIDE') => {
    const newItem: NotificationItem = {
      id: 'NOTIF-' + Date.now(),
      message,
      timestamp: 'Just now',
      targetUserId: targetUser,
      type,
      read: false
    };
    setNotifications(prev => [newItem, ...prev]);
  };

  // Handle Student Registration (UC-03)
  const handleRegister = (e: React.FormEvent) => {
    e.preventDefault();
    if (!regName.trim() || !regEmail.trim()) {
      showToast('Please provide your name and email address', 'error');
      return;
    }
    const newUid = `S-${regDept}-${Math.floor(100 + Math.random() * 900)}`;
    const newUser: User = {
      uid: newUid,
      name: regName.trim(),
      email: regEmail.trim(),
      role: 'Student',
      dept: regDept,
      batch: regBatch,
      section: regSection
    };

    setCurrentUser(newUser);
    setIsRegistering(false);
    showToast(`Account registered successfully! Auto-enrolled in ${regDept} Batch ${regBatch} Section ${regSection}.`);
    addNotification(
      `Welcome ${regName}! You have been automatically enrolled into Section ${regSection} courses.`,
      newUid,
      'ALERT'
    );
  };

  // Handle Teacher Reporting Delay / Cancellation (UC-01)
  const handleReportDelay = (e: React.FormEvent) => {
    e.preventDefault();
    const session = sessions.find(s => s.sessionId === delaySessionId);
    if (!session) {
      showToast('Please select a valid class session', 'error');
      return;
    }

    setSessions(prev =>
      prev.map(s => {
        if (s.sessionId === delaySessionId) {
          return {
            ...s,
            status: actionType,
            delayMinutes: actionType === 'DELAYED' ? delayMins : undefined,
            delayReason: delayReason
          };
        }
        return s;
      })
    );

    const alertMessage = actionType === 'DELAYED'
      ? `ATTENTION: ${session.courseId} (${session.courseName}) delayed by ${delayMins} mins. Reason: ${delayReason}`
      : `CANCELLED: ${session.courseId} (${session.courseName}) today has been cancelled.`;

    addNotification(alertMessage, 'ALL_STUDENTS', 'ALERT');
    showToast(`Session ${actionType.toLowerCase()} published. Enrolled students alerted immediately.`);
  };

  // Handle Teacher Room Swap Request (UC-02)
  const handleRequestRoomSwap = (e: React.FormEvent) => {
    e.preventDefault();
    const session = sessions.find(s => s.sessionId === swapSessionId);
    if (!session) {
      showToast('Please select an active session to swap', 'error');
      return;
    }

    const target = classrooms.find(c => c.roomId === swapTargetRoom);
    const newReq: SwapRequest = {
      requestId: 'SAR-' + Math.random().toString(36).substring(2, 8).toUpperCase(),
      classId: session.sessionId,
      courseCode: session.courseId,
      teacherName: session.teacherName,
      currentRoom: session.roomNumber,
      targetRoom: swapTargetRoom,
      capacity: target ? target.capacity : 50,
      reason: swapReason,
      status: 'PENDING',
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setSwapRequests(prev => [newReq, ...prev]);
    setSessions(prev =>
      prev.map(s => (s.sessionId === swapSessionId ? { ...s, status: 'SWAP_PENDING' } : s))
    );

    showToast(`Room swap requested for ${swapTargetRoom}. Pending Academic Coordinator review.`);
    addNotification(
      `New Room Swap Request from ${session.teacherName} for room ${swapTargetRoom}`,
      'ADM-001',
      'SWAP'
    );
  };

  // Handle Teacher Requesting Substitute (UC-08)
  const handleRequestSubstitute = (e: React.FormEvent) => {
    e.preventDefault();
    const session = sessions.find(s => s.sessionId === subSessionId);
    if (!session) {
      showToast('Please select a session for substitute request', 'error');
      return;
    }

    const newSub: SubstituteRequest = {
      substituteId: 'SUB-' + Math.random().toString(36).substring(2, 8).toUpperCase(),
      sessionId: session.sessionId,
      courseCode: session.courseId,
      originalTeacherUid: session.teacherUid,
      originalTeacherName: session.teacherName,
      status: 'REQUESTED_BY_TEACHER',
      reason: subReason,
      createdAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setSubstitutes(prev => [newSub, ...prev]);
    showToast(`Substitute assistance requested for ${session.courseId}. Sent to Admin queue.`);
    addNotification(`Teacher absence reported for ${session.courseId}. Substitute assignment needed.`, 'ADM-001', 'SUBSTITUTE');
  };

  // Handle Admin Approving / Rejecting Room Swap (UC-02)
  const handleResolveSwap = (reqId: string, approve: boolean) => {
    const req = swapRequests.find(r => r.requestId === reqId);
    if (!req) return;

    if (approve && req.targetRoom) {
      setSessions(prev =>
        prev.map(s => {
          if (s.sessionId === req.classId) {
            return {
              ...s,
              roomNumber: req.targetRoom!,
              status: 'SCHEDULED'
            };
          }
          return s;
        })
      );
    } else {
      setSessions(prev =>
        prev.map(s => (s.sessionId === req.classId ? { ...s, status: 'SCHEDULED' } : s))
      );
    }

    setSwapRequests(prev =>
      prev.map(r => (r.requestId === reqId ? { ...r, status: approve ? 'APPROVED' : 'REJECTED' } : r))
    );

    const msg = approve
      ? `Room swap approved: Session ${req.classId} moved to ${req.targetRoom}.`
      : `Room swap request ${reqId} was declined by academic coordinator.`;

    showToast(msg, approve ? 'success' : 'info');
    addNotification(msg, 'ALL', 'SWAP');
  };

  // Handle Admin Assigning Substitute (UC-08)
  const handleAssignSubstitute = (subId: string, substituteTeacherUid: string) => {
    const teacher = INITIAL_USERS.find(u => u.uid === substituteTeacherUid);
    if (!teacher) return;

    setSubstitutes(prev =>
      prev.map(s => {
        if (s.substituteId === subId) {
          return {
            ...s,
            substituteTeacherUid,
            substituteTeacherName: teacher.name,
            status: 'PENDING'
          };
        }
        return s;
      })
    );

    showToast(`Substitute teacher ${teacher.name} assigned. Awaiting confirmation.`);
    addNotification(`You have been designated as substitute teacher for session.`, substituteTeacherUid, 'SUBSTITUTE');
  };

  // Handle Admin Emergency Override (UC-11)
  const handleExecuteEmergencyOverride = (e: React.FormEvent) => {
    e.preventDefault();
    const session = sessions.find(s => s.sessionId === overrideSessionId);
    if (!session) {
      showToast('Select a session for emergency override', 'error');
      return;
    }

    setSessions(prev =>
      prev.map(s => {
        if (s.sessionId === overrideSessionId) {
          return {
            ...s,
            status: overrideStatus,
            roomNumber: overrideRoom
          };
        }
        return s;
      })
    );

    const alertText = `🚨 EMERGENCY OVERRIDE: ${session.courseId} (${session.courseName}) rescheduled to Room ${overrideRoom}. Status: ${overrideStatus}. Reason: ${overrideReason}`;
    addNotification(alertText, 'ALL', 'OVERRIDE');
    showToast(`Emergency override executed and logged in audit trail.`);
  };

  // Toggle or add Rule (UC-10)
  const toggleRule = (ruleId: string) => {
    setRules(prev =>
      prev.map(r => (r.ruleId === ruleId ? { ...r, isActive: !r.isActive } : r))
    );
    showToast('Rule constraint policy state updated');
  };

  const handleAddRule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRuleName.trim()) return;
    const newRule: SystemRule = {
      ruleId: 'RULE-' + Math.floor(100 + Math.random() * 900),
      ruleName: newRuleName.trim(),
      description: 'Custom academic administration policy constraint',
      type: newRuleType,
      value: newRuleValue,
      isActive: true
    };
    setRules(prev => [...prev, newRule]);
    setNewRuleName('');
    showToast('New constraint policy enforced in scheduler');
  };

  // Filtered Sessions for display
  const filteredSessions = sessions.filter(s => {
    if (selectedDay !== 'All' && s.dayOfWeek !== selectedDay) return false;
    if (currentUser?.role === 'Teacher') {
      return s.teacherUid === currentUser.uid;
    }
    return true;
  });

  const criticalAlerts = notifications.filter(n => n.type === 'ALERT' || n.type === 'OVERRIDE');

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col antialiased">
      {/* Top Banner / Toast */}
      {feedbackMsg && (
        <div
          id="system-toast-alert"
          className={`fixed top-4 right-4 z-50 flex items-center gap-3 px-5 py-3 rounded-xl shadow-lg border text-sm font-medium transition-all ${
            feedbackMsg.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : feedbackMsg.type === 'info'
              ? 'bg-blue-50 border-blue-200 text-blue-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          {feedbackMsg.type === 'error' ? (
            <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0" />
          ) : feedbackMsg.type === 'info' ? (
            <Sparkles className="w-5 h-5 text-blue-600 shrink-0" />
          ) : (
            <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
          )}
          <span>{feedbackMsg.text}</span>
        </div>
      )}

      {/* Main Header */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white font-bold text-lg shadow-sm">
              S
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-slate-900 tracking-tight">SAPCIS</span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                  Smart Timetable & Presence
                </span>
              </div>
              <p className="text-xs text-slate-500 hidden sm:block">
                Academic Planning & Class Information System
              </p>
            </div>
          </div>

          {/* Role Switcher & Live Session State */}
          <div className="flex items-center gap-3">
            <div className="hidden md:flex items-center gap-2 px-3 py-1.5 bg-slate-100 rounded-lg text-xs text-slate-600 border border-slate-200">
              <Clock className="w-3.5 h-3.5 text-blue-600" />
              <span>Term: Fall 2026</span>
              <span className="text-slate-300">•</span>
              <span className="font-medium text-emerald-600">Active Campus DB</span>
            </div>

            {/* Switch Personas quickly for testing */}
            <div className="relative">
              <select
                id="role-persona-selector"
                value={currentUser?.uid || ''}
                onChange={e => {
                  const u = INITIAL_USERS.find(user => user.uid === e.target.value);
                  if (u) {
                    setCurrentUser(u);
                    setIsRegistering(false);
                    showToast(`Switched active session to: ${u.name} (${u.role})`);
                  }
                }}
                className="text-xs bg-slate-100 hover:bg-slate-200 border border-slate-300 rounded-lg px-3 py-1.5 font-medium text-slate-700 cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <optgroup label="Switch Persona">
                  {INITIAL_USERS.map(u => (
                    <option key={u.uid} value={u.uid}>
                      {u.name} — {u.role} ({u.uid})
                    </option>
                  ))}
                </optgroup>
              </select>
            </div>

            {/* Notifications Bell */}
            <div className="relative">
              <button
                id="notifications-toggle-button"
                onClick={() => setShowNotifications(!showNotifications)}
                className="p-2 rounded-lg text-slate-600 hover:bg-slate-100 relative focus:outline-none"
                title="Notifications"
              >
                <Bell className="w-5 h-5" />
                {notifications.some(n => !n.read) && (
                  <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-rose-500 rounded-full ring-2 ring-white"></span>
                )}
              </button>

              {showNotifications && (
                <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-200 p-4 z-50 animate-in fade-in slide-in-from-top-2">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
                    <h3 className="font-semibold text-sm text-slate-900 flex items-center gap-2">
                      <Bell className="w-4 h-4 text-blue-600" /> Notifications & Alerts
                    </h3>
                    <button
                      onClick={() => setNotifications(prev => prev.map(n => ({ ...n, read: true })))}
                      className="text-xs text-blue-600 hover:underline"
                    >
                      Mark all read
                    </button>
                  </div>
                  <div className="max-h-72 overflow-y-auto space-y-2.5 pr-1">
                    {notifications.length === 0 ? (
                      <p className="text-xs text-slate-400 text-center py-4">No notifications yet.</p>
                    ) : (
                      notifications.map(n => (
                        <div
                          key={n.id}
                          className={`p-2.5 rounded-lg text-xs transition-colors border ${
                            n.type === 'OVERRIDE' || n.type === 'ALERT'
                              ? 'bg-amber-50 border-amber-200 text-amber-900'
                              : 'bg-slate-50 border-slate-200 text-slate-800'
                          }`}
                        >
                          <p className="font-medium">{n.message}</p>
                          <span className="text-[10px] text-slate-400 mt-1 block">{n.timestamp}</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Logout / Switch to Sign Up */}
            <button
              id="auth-logout-btn"
              onClick={() => {
                if (currentUser) {
                  setCurrentUser(null);
                  setIsRegistering(false);
                  showToast('Logged out of SAPCIS session');
                } else {
                  setCurrentUser(INITIAL_USERS[0]);
                }
              }}
              className="p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
              title={currentUser ? 'Log out' : 'Log in'}
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Critical Broadcast Banner if any urgent alert */}
      {criticalAlerts.length > 0 && (
        <div className="bg-gradient-to-r from-amber-600 via-amber-700 to-rose-600 text-white px-4 py-2.5 text-xs font-medium">
          <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
            <div className="flex items-center gap-2 overflow-hidden">
              <span className="bg-black/20 px-2 py-0.5 rounded uppercase font-bold tracking-wider text-[10px]">
                Active Alert
              </span>
              <span className="truncate">{criticalAlerts[0].message}</span>
            </div>
            <span className="shrink-0 text-[11px] opacity-80">{criticalAlerts[0].timestamp}</span>
          </div>
        </div>
      )}

      {/* Main Container */}
      {!currentUser && !isRegistering ? (
        /* Sign In Screen */
        <div className="flex-1 flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8 space-y-6">
            <div className="text-center space-y-2">
              <div className="w-12 h-12 bg-blue-600 text-white rounded-2xl flex items-center justify-center mx-auto text-xl font-bold shadow-md shadow-blue-500/20">
                S
              </div>
              <h2 className="text-2xl font-bold text-slate-900">Sign in to SAPCIS</h2>
              <p className="text-sm text-slate-500">
                Smart Academic Planning & Class Information System
              </p>
            </div>

            <div className="space-y-3">
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Select Quick Login Persona:
              </p>
              {INITIAL_USERS.map(u => (
                <button
                  key={u.uid}
                  id={`quick-login-${u.uid}`}
                  onClick={() => {
                    setCurrentUser(u);
                    showToast(`Logged in as ${u.name}`);
                  }}
                  className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-200 hover:border-blue-500 hover:bg-blue-50/50 transition-all text-left group"
                >
                  <div>
                    <div className="font-semibold text-sm text-slate-900 group-hover:text-blue-600">
                      {u.name}
                    </div>
                    <div className="text-xs text-slate-500">
                      {u.email} • <span className="font-medium text-blue-600">{u.role}</span>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600 transition-transform group-hover:translate-x-1" />
                </button>
              ))}
            </div>

            <div className="pt-4 border-t border-slate-100 text-center">
              <p className="text-xs text-slate-500">
                New Student?{' '}
                <button
                  onClick={() => setIsRegistering(true)}
                  className="font-semibold text-blue-600 hover:underline"
                >
                  Create student account & auto-enroll
                </button>
              </p>
            </div>
          </div>
        </div>
      ) : !currentUser && isRegistering ? (
        /* Student Registration Screen (UC-03) */
        <div className="flex-1 flex items-center justify-center p-6">
          <div className="max-w-lg w-full bg-white rounded-2xl shadow-sm border border-slate-200 p-8 space-y-6">
            <div className="text-center space-y-1">
              <h2 className="text-2xl font-bold text-slate-900">Student Registration</h2>
              <p className="text-xs text-slate-500">
                Creates student profile & automatically enrollees into section timetable
              </p>
            </div>

            <form onSubmit={handleRegister} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Zaid Khan"
                  value={regName}
                  onChange={e => setRegName(e.target.value)}
                  className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Institutional Email</label>
                <input
                  type="email"
                  required
                  placeholder="zaid@sapcis.edu"
                  value={regEmail}
                  onChange={e => setRegEmail(e.target.value)}
                  className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Department</label>
                  <select
                    value={regDept}
                    onChange={e => setRegDept(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg px-2.5 py-2 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="CS">Computer Science (CS)</option>
                    <option value="SE">Software Eng (SE)</option>
                    <option value="AI">Artificial Intell (AI)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Batch</label>
                  <select
                    value={regBatch}
                    onChange={e => setRegBatch(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg px-2.5 py-2 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="2024">2024</option>
                    <option value="2023">2023</option>
                    <option value="2022">2022</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Section</label>
                  <select
                    value={regSection}
                    onChange={e => setRegSection(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg px-2.5 py-2 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="A">Section A</option>
                    <option value="B">Section B</option>
                    <option value="C">Section C</option>
                  </select>
                </div>
              </div>

              <div className="p-3 bg-blue-50 rounded-xl border border-blue-100 text-xs text-blue-800 space-y-1">
                <span className="font-semibold flex items-center gap-1.5">
                  <UserCheck className="w-4 h-4 text-blue-600" /> Automated Schedule Provisioning
                </span>
                <p>
                  Upon registration, timetable courses for {regDept} Batch {regBatch} Section {regSection} will be immediately mapped to your dashboard.
                </p>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsRegistering(false)}
                  className="w-1/2 py-2.5 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="register-submit-btn"
                  className="w-1/2 py-2.5 text-xs font-semibold text-white bg-blue-600 rounded-xl hover:bg-blue-700 shadow-sm"
                >
                  Complete Registration
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : (
        /* Authenticated Dashboard View */
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex-1 w-full space-y-6">
          {/* Top User Greeting & Role Navigation Bar */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold text-slate-900">
                  Welcome back, {currentUser?.name}
                </h1>
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-300">
                  {currentUser?.role}
                </span>
                {currentUser?.dept && (
                  <span className="text-xs px-2.5 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                    Dept: {currentUser.dept} {currentUser.section ? `• Sec ${currentUser.section}` : ''}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500">
                {currentUser?.role === 'Student'
                  ? 'Real-time enrolled class schedules, instructor delays, and auditorium reassignments.'
                  : currentUser?.role === 'Teacher'
                  ? 'Instructional schedule manager, delay broadcast, and room swap dispatch.'
                  : 'Institutional oversight, master timetable, policy rules, and emergency overrides.'}
              </p>
            </div>

            {/* Navigation Tabs based on Role */}
            <div className="flex flex-wrap gap-2">
              {currentUser?.role === 'Student' && (
                <>
                  <button
                    id="tab-student-timetable"
                    onClick={() => setActiveTab('timetable')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'timetable'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    My Weekly Timetable
                  </button>
                  <button
                    id="tab-student-live"
                    onClick={() => setActiveTab('today')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'today'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Today's Live Sessions
                  </button>
                </>
              )}

              {currentUser?.role === 'Teacher' && (
                <>
                  <button
                    id="tab-teacher-schedule"
                    onClick={() => setActiveTab('timetable')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'timetable'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    My Teaching Classes
                  </button>
                  <button
                    id="tab-teacher-delay"
                    onClick={() => setActiveTab('delay')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'delay'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Report Delay / Cancel
                  </button>
                  <button
                    id="tab-teacher-swap"
                    onClick={() => setActiveTab('swap')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'swap'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Request Room Swap
                  </button>
                  <button
                    id="tab-teacher-sub"
                    onClick={() => setActiveTab('substitute')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'substitute'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Request Substitute
                  </button>
                </>
              )}

              {currentUser?.role === 'Admin' && (
                <>
                  <button
                    id="tab-admin-master"
                    onClick={() => setActiveTab('timetable')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'timetable'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Master Timetable
                  </button>
                  <button
                    id="tab-admin-override"
                    onClick={() => setActiveTab('override')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'override'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Emergency Override (UC-11)
                  </button>
                  <button
                    id="tab-admin-swap"
                    onClick={() => setActiveTab('swap-approvals')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'swap-approvals'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Room Swaps ({swapRequests.filter(r => r.status === 'PENDING').length})
                  </button>
                  <button
                    id="tab-admin-sub"
                    onClick={() => setActiveTab('sub-mgmt')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'sub-mgmt'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Substitutes
                  </button>
                  <button
                    id="tab-admin-rules"
                    onClick={() => setActiveTab('rules')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'rules'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Rules & Constraints (UC-10)
                  </button>
                  <button
                    id="tab-admin-reports"
                    onClick={() => setActiveTab('reports')}
                    className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all ${
                      activeTab === 'reports'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    Analytics & Reports
                  </button>
                </>
              )}

              {/* Secrets and Security Tab */}
              <button
                id="tab-secrets-viewer"
                onClick={() => setActiveTab('secrets')}
                className={`px-3.5 py-2 text-xs font-semibold rounded-xl transition-all flex items-center gap-1.5 ${
                  activeTab === 'secrets'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-emerald-50 text-emerald-800 border border-emerald-200 hover:bg-emerald-100'
                }`}
              >
                <Lock className="w-3.5 h-3.5" />
                DB Secrets & Config
              </button>
            </div>
          </div>

          {/* TAB 1: Timetable View (Both Student, Teacher, Master) */}
          {activeTab === 'timetable' && (
            <div className="space-y-4">
              {/* Day filter pills */}
              <div className="flex items-center justify-between flex-wrap gap-3">
                <div className="flex items-center gap-1.5 bg-white p-1 rounded-xl border border-slate-200 shadow-sm text-xs">
                  {['All', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].map(day => (
                    <button
                      key={day}
                      onClick={() => setSelectedDay(day)}
                      className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                        selectedDay === day
                          ? 'bg-blue-600 text-white shadow-xs'
                          : 'text-slate-600 hover:bg-slate-100'
                      }`}
                    >
                      {day}
                    </button>
                  ))}
                </div>

                <div className="text-xs text-slate-500 font-medium">
                  Showing {filteredSessions.length} sessions
                </div>
              </div>

              {/* Timetable Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredSessions.map(s => {
                  const isOngoing = s.status === 'ONGOING';
                  const isDelayed = s.status === 'DELAYED';
                  const isCancelled = s.status === 'CANCELLED';
                  const isSwapPending = s.status === 'SWAP_PENDING';
                  const isRescheduled = s.status === 'RESCHEDULED';

                  return (
                    <div
                      key={s.sessionId}
                      id={`session-card-${s.sessionId}`}
                      className={`bg-white rounded-2xl p-5 border transition-all hover:shadow-md relative overflow-hidden ${
                        isCancelled
                          ? 'border-rose-300 bg-rose-50/30'
                          : isDelayed
                          ? 'border-amber-300 bg-amber-50/20'
                          : isOngoing
                          ? 'border-emerald-400 ring-2 ring-emerald-400/20'
                          : isRescheduled
                          ? 'border-indigo-300 bg-indigo-50/20'
                          : 'border-slate-200'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-800 border border-slate-200">
                              {s.courseId}
                            </span>
                            <span className="text-xs text-slate-400 font-medium">Sec {s.sectionId}</span>
                          </div>
                          <h3 className="font-bold text-slate-900 mt-1 text-sm">{s.courseName}</h3>
                        </div>

                        {/* Status Badge */}
                        <span
                          className={`text-[11px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider ${
                            isCancelled
                              ? 'bg-rose-100 text-rose-700'
                              : isDelayed
                              ? 'bg-amber-100 text-amber-800'
                              : isOngoing
                              ? 'bg-emerald-100 text-emerald-800 animate-pulse'
                              : isSwapPending
                              ? 'bg-purple-100 text-purple-800'
                              : isRescheduled
                              ? 'bg-indigo-100 text-indigo-800'
                              : 'bg-blue-50 text-blue-700'
                          }`}
                        >
                          {s.status}
                        </span>
                      </div>

                      {/* Details row */}
                      <div className="space-y-2 text-xs text-slate-600 mt-4 pt-3 border-t border-slate-100">
                        <div className="flex items-center justify-between">
                          <span className="flex items-center gap-1.5 text-slate-500">
                            <Clock className="w-3.5 h-3.5 text-blue-500" />
                            {s.dayOfWeek}
                          </span>
                          <span className="font-semibold text-slate-800 font-mono">
                            {s.startTime} - {s.endTime}
                          </span>
                        </div>

                        <div className="flex items-center justify-between">
                          <span className="flex items-center gap-1.5 text-slate-500">
                            <MapPin className="w-3.5 h-3.5 text-emerald-500" />
                            Classroom
                          </span>
                          <span className="font-semibold text-slate-800 bg-slate-100 px-2 py-0.5 rounded">
                            {s.roomNumber}
                          </span>
                        </div>

                        <div className="flex items-center justify-between">
                          <span className="flex items-center gap-1.5 text-slate-500">
                            <UserCheck className="w-3.5 h-3.5 text-indigo-500" />
                            Faculty
                          </span>
                          <span className="font-medium text-slate-800 truncate max-w-[160px]">
                            {s.teacherName}
                          </span>
                        </div>

                        {/* Delay / Reason note */}
                        {s.delayReason && (
                          <div className="mt-2 p-2 bg-amber-50 rounded-lg border border-amber-200 text-amber-900 text-[11px] flex items-start gap-1.5">
                            <AlertTriangle className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                            <div>
                              <span className="font-semibold">
                                {isDelayed ? `Delayed ${s.delayMinutes}m:` : 'Cancellation Note:'}
                              </span>{' '}
                              {s.delayReason}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* TAB 2: Today's Live Sessions */}
          {activeTab === 'today' && (
            <div className="space-y-4">
              <div className="bg-blue-600 text-white rounded-2xl p-6 shadow-sm">
                <h2 className="text-lg font-bold">Today's Live Campus Timetable</h2>
                <p className="text-xs text-blue-100 mt-1">
                  Synchronized with the campus clock. Instant badge feedback on upcoming, ongoing, and delayed lectures.
                </p>
              </div>

              <div className="space-y-3">
                {sessions.slice(0, 4).map(s => (
                  <div
                    key={s.sessionId}
                    className="bg-white rounded-xl p-4 border border-slate-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm"
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-slate-100 flex flex-col items-center justify-center font-bold text-slate-800">
                        <span className="text-[10px] text-slate-400 uppercase font-semibold">ROOM</span>
                        <span className="text-xs">{s.roomNumber}</span>
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-slate-900">{s.courseName}</span>
                          <span className="text-xs font-mono text-slate-500">({s.courseId})</span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">
                          Instructor: {s.teacherName} • Slot: {s.startTime} - {s.endTime}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="text-xs font-bold px-3 py-1 rounded-full bg-emerald-100 text-emerald-800">
                        {s.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: Teacher Delay / Cancellation Report (UC-01) */}
          {activeTab === 'delay' && (
            <div className="max-w-2xl mx-auto bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-6">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Broadcast Session Delay / Cancellation</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Notifies all enrolled students in real-time and logs the delay audit for campus analytics.
                </p>
              </div>

              <form onSubmit={handleReportDelay} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Select Teaching Session</label>
                  <select
                    id="delay-session-select"
                    value={delaySessionId}
                    onChange={e => setDelaySessionId(e.target.value)}
                    required
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">-- Choose Class Session --</option>
                    {sessions.map(s => (
                      <option key={s.sessionId} value={s.sessionId}>
                        {s.courseId} - {s.courseName} ({s.dayOfWeek} {s.startTime}, Room {s.roomNumber})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Action Type</label>
                    <select
                      value={actionType}
                      onChange={e => setActionType(e.target.value as any)}
                      className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="DELAYED">Report Delay</option>
                      <option value="CANCELLED">Cancel Session</option>
                    </select>
                  </div>

                  {actionType === 'DELAYED' && (
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Estimated Delay (Minutes)</label>
                      <input
                        type="number"
                        min={5}
                        max={60}
                        step={5}
                        value={delayMins}
                        onChange={e => setDelayMins(parseInt(e.target.value) || 15)}
                        className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Reason / Context</label>
                  <textarea
                    rows={3}
                    value={delayReason}
                    onChange={e => setDelayReason(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  id="submit-delay-btn"
                  className="w-full py-3 bg-amber-600 hover:bg-amber-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-colors"
                >
                  Broadcast Update to Enrolled Students
                </button>
              </form>
            </div>
          )}

          {/* TAB 4: Request Room Swap (UC-02) */}
          {activeTab === 'swap' && (
            <div className="max-w-2xl mx-auto bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-6">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Request Classroom Relocation / Swap</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Propose moving your scheduled lecture to an alternate hall or lab with higher seating capacity.
                </p>
              </div>

              <form onSubmit={handleRequestRoomSwap} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Scheduled Class</label>
                  <select
                    id="swap-session-select"
                    value={swapSessionId}
                    onChange={e => setSwapSessionId(e.target.value)}
                    required
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">-- Choose Class Session --</option>
                    {sessions.map(s => (
                      <option key={s.sessionId} value={s.sessionId}>
                        {s.courseId} - Current Room: {s.roomNumber} ({s.dayOfWeek} {s.startTime})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Requested Target Room</label>
                  <select
                    id="swap-target-room-select"
                    value={swapTargetRoom}
                    onChange={e => setSwapTargetRoom(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  >
                    {classrooms.map(c => (
                      <option key={c.roomId} value={c.roomId}>
                        {c.roomId} — {c.roomName} (Cap: {c.capacity}, {c.hasProjector ? 'Projector' : 'No Projector'})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Justification Reason</label>
                  <input
                    type="text"
                    value={swapReason}
                    onChange={e => setSwapReason(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  id="submit-room-swap-btn"
                  className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-colors"
                >
                  Submit Relocation Request
                </button>
              </form>
            </div>
          )}

          {/* TAB 5: Request Substitute (UC-08) */}
          {activeTab === 'substitute' && (
            <div className="max-w-2xl mx-auto bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-6">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Request Substitute Teacher (Absence Notification)</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Notify the coordinator of unavoidable absence so a free qualified peer can be assigned.
                </p>
              </div>

              <form onSubmit={handleRequestSubstitute} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Session Needing Coverage</label>
                  <select
                    id="sub-session-select"
                    value={subSessionId}
                    onChange={e => setSubSessionId(e.target.value)}
                    required
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">-- Choose Class Session --</option>
                    {sessions.map(s => (
                      <option key={s.sessionId} value={s.sessionId}>
                        {s.courseId} - {s.courseName} ({s.dayOfWeek} {s.startTime})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Reason for Absence</label>
                  <textarea
                    rows={3}
                    value={subReason}
                    onChange={e => setSubReason(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  id="submit-sub-req-btn"
                  className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-colors"
                >
                  Dispatch Substitute Request
                </button>
              </form>
            </div>
          )}

          {/* TAB 6: Admin Emergency Override (UC-11) */}
          {activeTab === 'override' && (
            <div className="max-w-2xl mx-auto bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-rose-100 flex items-center justify-center text-rose-700 font-bold">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-900">Emergency Timetable Override (UC-11)</h2>
                  <p className="text-xs text-slate-500">
                    Coordinator administrative override for room closures, power cuts, or weather incidents.
                  </p>
                </div>
              </div>

              <form onSubmit={handleExecuteEmergencyOverride} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Target Class Session</label>
                  <select
                    id="override-session-select"
                    value={overrideSessionId}
                    onChange={e => setOverrideSessionId(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  >
                    {sessions.map(s => (
                      <option key={s.sessionId} value={s.sessionId}>
                        {s.courseId} - {s.courseName} (Current: Room {s.roomNumber}, {s.status})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">New Room Assignment</label>
                    <select
                      value={overrideRoom}
                      onChange={e => setOverrideRoom(e.target.value)}
                      className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                    >
                      {classrooms.map(c => (
                        <option key={c.roomId} value={c.roomId}>
                          {c.roomId} ({c.roomName})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">New State Status</label>
                    <select
                      value={overrideStatus}
                      onChange={e => setOverrideStatus(e.target.value as any)}
                      className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="RESCHEDULED">RESCHEDULED</option>
                      <option value="SCHEDULED">SCHEDULED (Normal)</option>
                      <option value="CANCELLED">CANCELLED</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Emergency Audit Reason</label>
                  <input
                    type="text"
                    value={overrideReason}
                    onChange={e => setOverrideReason(e.target.value)}
                    className="w-full text-sm border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  id="execute-override-btn"
                  className="w-full py-3 bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-colors"
                >
                  Execute Emergency Override & Broadcast Alerts
                </button>
              </form>
            </div>
          )}

          {/* TAB 7: Admin Room Swap Approvals */}
          {activeTab === 'swap-approvals' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">Pending Classroom Relocation Requests</h2>
                  <p className="text-xs text-slate-500">
                    Review capacity compatibility before confirming room changes.
                  </p>
                </div>
              </div>

              {swapRequests.length === 0 ? (
                <div className="bg-white rounded-2xl p-8 text-center text-slate-400 border border-slate-200">
                  No active room swap requests.
                </div>
              ) : (
                <div className="space-y-3">
                  {swapRequests.map(r => (
                    <div
                      key={r.requestId}
                      className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                            {r.courseCode}
                          </span>
                          <span className="font-semibold text-slate-900 text-sm">{r.teacherName}</span>
                          <span
                            className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                              r.status === 'APPROVED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : r.status === 'REJECTED'
                                ? 'bg-rose-100 text-rose-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {r.status}
                          </span>
                        </div>
                        <p className="text-xs text-slate-600">
                          Current Room: <span className="font-semibold">{r.currentRoom}</span> → Requested Room:{' '}
                          <span className="font-semibold text-blue-600">{r.targetRoom || 'Admin choice'}</span> (Min Cap: {r.capacity})
                        </p>
                        <p className="text-xs text-slate-500 italic">"{r.reason}"</p>
                      </div>

                      {r.status === 'PENDING' && (
                        <div className="flex items-center gap-2 shrink-0">
                          <button
                            id={`approve-swap-${r.requestId}`}
                            onClick={() => handleResolveSwap(r.requestId, true)}
                            className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-xs"
                          >
                            Approve & Update
                          </button>
                          <button
                            id={`reject-swap-${r.requestId}`}
                            onClick={() => handleResolveSwap(r.requestId, false)}
                            className="px-3.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl"
                          >
                            Decline
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 8: Admin Substitute Management */}
          {activeTab === 'sub-mgmt' && (
            <div className="space-y-4">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Faculty Substitute Assignments (UC-08)</h2>
                <p className="text-xs text-slate-500">
                  Assign available faculty members to cover reported instructional absences.
                </p>
              </div>

              <div className="space-y-3">
                {substitutes.map(sub => (
                  <div
                    key={sub.substituteId}
                    className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-slate-900">{sub.courseCode}</span>
                        <span className="text-xs text-slate-500">Orig Faculty: {sub.originalTeacherName}</span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                          {sub.status}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600">Absence Reason: {sub.reason}</p>
                      {sub.substituteTeacherName && (
                        <p className="text-xs font-semibold text-emerald-700">
                          Assigned Substitute: {sub.substituteTeacherName}
                        </p>
                      )}
                    </div>

                    {sub.status === 'REQUESTED_BY_TEACHER' && (
                      <div className="flex items-center gap-2">
                        <select
                          id={`substitute-teacher-select-${sub.substituteId}`}
                          defaultValue="T-CS-001"
                          className="text-xs border border-slate-300 rounded-lg p-1.5"
                          onChange={e => handleAssignSubstitute(sub.substituteId, e.target.value)}
                        >
                          <option value="T-CS-001">Dr. Tariq Mahmood (CS)</option>
                          <option value="T-CS-002">Dr. Ayesha Malik (CS)</option>
                          <option value="T-SE-001">Engr. Usman Farooq (SE)</option>
                        </select>
                        <button
                          onClick={() => handleAssignSubstitute(sub.substituteId, 'T-CS-001')}
                          className="px-3 py-1.5 bg-blue-600 text-white rounded-xl text-xs font-semibold hover:bg-blue-700"
                        >
                          Confirm
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 9: Rules & Constraints (UC-10) */}
          {activeTab === 'rules' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2 space-y-4">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">Active Academic Scheduling Rules (UC-10)</h2>
                  <p className="text-xs text-slate-500">
                    Policy constraints evaluated by the schedule optimization engine.
                  </p>
                </div>

                <div className="space-y-3">
                  {rules.map(rule => (
                    <div
                      key={rule.ruleId}
                      className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex items-start justify-between gap-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-slate-900">{rule.ruleName}</span>
                          <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                            Threshold: {rule.value}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500">{rule.description}</p>
                        <span className="text-[10px] text-blue-600 font-medium">Type: {rule.type}</span>
                      </div>

                      <button
                        onClick={() => toggleRule(rule.ruleId)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                          rule.isActive
                            ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                            : 'bg-slate-100 text-slate-500 hover:bg-slate-200'
                        }`}
                      >
                        {rule.isActive ? 'Active' : 'Disabled'}
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Add rule card */}
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm h-fit space-y-4">
                <h3 className="font-bold text-sm text-slate-900 flex items-center gap-1.5">
                  <Sliders className="w-4 h-4 text-blue-600" /> Create Policy Constraint
                </h3>

                <form onSubmit={handleAddRule} className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Rule Title</label>
                    <input
                      type="text"
                      placeholder="e.g. Lab Session Max Duration"
                      value={newRuleName}
                      onChange={e => setNewRuleName(e.target.value)}
                      className="w-full text-xs border border-slate-300 rounded-lg p-2 focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Constraint Type</label>
                    <select
                      value={newRuleType}
                      onChange={e => setNewRuleType(e.target.value as any)}
                      className="w-full text-xs border border-slate-300 rounded-lg p-2 focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="GLOBAL_FACULTY">Faculty Constraint</option>
                      <option value="ROOM_CAPACITY">Room Seating Margin</option>
                      <option value="SCHEDULE_WINDOW">Schedule Transition Gap</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Constraint Value</label>
                    <input
                      type="text"
                      placeholder="e.g. 120 mins"
                      value={newRuleValue}
                      onChange={e => setNewRuleValue(e.target.value)}
                      className="w-full text-xs border border-slate-300 rounded-lg p-2 focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <button
                    type="submit"
                    className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl transition-colors"
                  >
                    Add Policy Constraint
                  </button>
                </form>
              </div>
            </div>
          )}

          {/* TAB 10: Reports & Analytics (UC-12) */}
          {activeTab === 'reports' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Institutional Scheduling Analytics (UC-12)</h2>
                <p className="text-xs text-slate-500">
                  Comprehensive audit data covering room utilization, session statuses, and delay frequencies.
                </p>
              </div>

              {/* Metric stats cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Total Classrooms
                    </span>
                    <Building className="w-4 h-4 text-blue-600" />
                  </div>
                  <div className="text-2xl font-bold text-slate-900 mt-2">{classrooms.length}</div>
                  <span className="text-[11px] text-emerald-600 font-medium mt-1 inline-block">
                    100% Operational
                  </span>
                </div>

                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Weekly Sessions
                    </span>
                    <Calendar className="w-4 h-4 text-indigo-600" />
                  </div>
                  <div className="text-2xl font-bold text-slate-900 mt-2">{sessions.length}</div>
                  <span className="text-[11px] text-slate-500 mt-1 inline-block">
                    5 Days • Mon - Fri
                  </span>
                </div>

                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Room Utilization
                    </span>
                    <FileText className="w-4 h-4 text-emerald-600" />
                  </div>
                  <div className="text-2xl font-bold text-slate-900 mt-2">78.4%</div>
                  <span className="text-[11px] text-emerald-600 font-medium mt-1 inline-block">
                    Optimal Seating Ratio
                  </span>
                </div>

                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Registered Faculty
                    </span>
                    <Users className="w-4 h-4 text-amber-600" />
                  </div>
                  <div className="text-2xl font-bold text-slate-900 mt-2">18</div>
                  <span className="text-[11px] text-slate-500 mt-1 inline-block">CS, SE & AI Depts</span>
                </div>
              </div>

              {/* Classroom utilization breakdown table */}
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                <h3 className="font-bold text-sm text-slate-900">Classroom Capacity & Allocation Audit</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-200 text-slate-400 uppercase tracking-wider">
                        <th className="pb-3 font-semibold">Room Code</th>
                        <th className="pb-3 font-semibold">Facility Name</th>
                        <th className="pb-3 font-semibold">Building</th>
                        <th className="pb-3 font-semibold">Capacity</th>
                        <th className="pb-3 font-semibold">Multimedia</th>
                        <th className="pb-3 font-semibold">Scheduled Hours</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {classrooms.map(c => {
                        const count = sessions.filter(s => s.roomNumber === c.roomId).length;
                        return (
                          <tr key={c.roomId} className="hover:bg-slate-50/60">
                            <td className="py-3 font-mono font-bold text-slate-900">{c.roomId}</td>
                            <td className="py-3 font-medium text-slate-800">{c.roomName}</td>
                            <td className="py-3 text-slate-500">{c.building}</td>
                            <td className="py-3 font-semibold text-slate-700">{c.capacity} seats</td>
                            <td className="py-3">
                              {c.hasProjector ? (
                                <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-medium">
                                  Yes
                                </span>
                              ) : (
                                <span className="text-slate-400 bg-slate-100 px-2 py-0.5 rounded">No</span>
                              )}
                            </td>
                            <td className="py-3 font-semibold text-blue-600">{count * 1.5} hrs/week</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 11: Secrets & Configuration Management */}
          {activeTab === 'secrets' && (
            <div className="max-w-3xl mx-auto space-y-6">
              <div className="bg-emerald-800 text-white rounded-2xl p-6 shadow-sm">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-700 flex items-center justify-center">
                    <Lock className="w-5 h-5 text-emerald-200" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold">Secure Secrets Management</h2>
                    <p className="text-xs text-emerald-200 mt-0.5">
                      All database credentials have been extracted from source code and isolated into external configuration files.
                    </p>
                  </div>
                </div>
              </div>

              {/* Status breakdown card */}
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-6">
                <div className="space-y-3">
                  <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                    <CheckCircle className="w-4 h-4 text-emerald-600" /> Security Audit Verification
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                    <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
                      <span className="font-semibold text-slate-700 block">External Config File</span>
                      <span className="font-mono text-emerald-700 font-bold block">db.properties</span>
                      <p className="text-slate-500 text-[11px]">
                        Holds database URL, user, and password properties safely outside Java classes.
                      </p>
                    </div>

                    <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
                      <span className="font-semibold text-slate-700 block">Git Exclusion</span>
                      <span className="font-mono text-emerald-700 font-bold block">.gitignore updated</span>
                      <p className="text-slate-500 text-[11px]">
                        Ensures sensitive credentials will never be committed to GitHub or public repositories.
                      </p>
                    </div>

                    <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
                      <span className="font-semibold text-slate-700 block">Template Reference</span>
                      <span className="font-mono text-emerald-700 font-bold block">db.properties.example</span>
                      <p className="text-slate-500 text-[11px]">
                        Provides teammates and deployment runners with a clean template for configuration.
                      </p>
                    </div>

                    <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
                      <span className="font-semibold text-slate-700 block">Dynamic JDBC Loader</span>
                      <span className="font-mono text-emerald-700 font-bold block">src/db/DBConnection.java</span>
                      <p className="text-slate-500 text-[11px]">
                        Loads credentials dynamically with multi-source fallback (properties, classpath, env).
                      </p>
                    </div>
                  </div>
                </div>

                {/* Secret preview (masked) */}
                <div className="space-y-2">
                  <label className="block text-xs font-semibold text-slate-700">
                    Configuration File Preview (<code>db.properties</code>)
                  </label>
                  <div className="bg-slate-900 text-slate-200 font-mono text-xs p-4 rounded-xl space-y-1">
                    <div><span className="text-slate-500"># SAPCIS Database Configuration</span></div>
                    <div><span className="text-blue-400">db.url</span>=jdbc:sqlserver://localhost:1433;databaseName=sapcis;encrypt=true;trustServerCertificate=true;</div>
                    <div><span className="text-blue-400">db.user</span>=sa</div>
                    <div><span className="text-blue-400">db.password</span>=******************** <span className="text-emerald-400">[Encrypted / Externalized]</span></div>
                  </div>
                </div>

                {/* Ant Build System Ready */}
                <div className="p-4 bg-blue-50 rounded-xl border border-blue-200 text-xs text-blue-900 space-y-1">
                  <div className="font-bold flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-blue-600" /> Java CI & Ant Build Ready
                  </div>
                  <p className="text-blue-700">
                    A complete <code>build.xml</code> has been generated to ensure the GitHub Actions Java workflow (<code>.github/workflows/ant.yml</code>) builds successfully and packages <code>SAPCIS.jar</code>.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
