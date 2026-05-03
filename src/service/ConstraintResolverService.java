package service;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * =============================================================================
 *  ConstraintResolverService — Proportional Time-Scaling Rebalancer (T-SQL)
 * -----------------------------------------------------------------------------
 *  GRASP: Pure Fabrication — behavioural service that coordinates the
 *  proportional schedule rebalance when university operating hours change.
 *
 *  Algorithm:
 *    Phase 1: Mathematical helpers (scale, newDuration, newGap)
 *    Phase 2: Audit — identify all sessions outside the new window
 *    Phase 3: Proportional Squeeze & Shift — scale durations, find new slots
 *    Phase 4: Quarantine — emergency fallback for unresolvable sessions
 *
 *  Transaction-safe: entire rebalance runs inside a single JDBC transaction
 *  with manual commit / rollback.
 * =============================================================================
 */
public class ConstraintResolverService {

    // ──────────────────────────────────────────────────────────────────
    //  Inner helper — snapshot of one violating session
    // ──────────────────────────────────────────────────────────────────
    private static class ViolatingSession {
        final String sessionId;
        final String teacherUid;
        final String roomId;
        final Time   startTime;
        final Time   endTime;
        final int    oldDurationMins;

        ViolatingSession(String sessionId, String teacherUid, String roomId,
                         Time startTime, Time endTime) {
            this.sessionId       = sessionId;
            this.teacherUid      = teacherUid;
            this.roomId          = roomId;
            this.startTime       = startTime;
            this.endTime         = endTime;
            this.oldDurationMins = minutesBetween(startTime, endTime);
        }
    }

    // ==================================================================
    //  Phase 1 — Mathematical Helpers
    // ==================================================================

    /**
     * Calculates the proportional scale factor S.
     * S = totalNewMins / totalOldMins
     */
    private double calculateScale(Time oldOpen, Time oldClose,
                                   Time newOpen, Time newClose) {
        double totalOldMins = minutesBetween(oldOpen, oldClose);
        double totalNewMins = minutesBetween(newOpen, newClose);
        if (totalOldMins <= 0) return 1.0;
        return totalNewMins / totalOldMins;
    }

    /**
     * Scales a class duration proportionally, rounded to the nearest 30 minutes.
     * Formula: Math.round((oldDuration * S) / 30.0) * 30
     * Ensures minimum of 30 minutes.
     */
    private int calculateNewDuration(int oldDurationMins, double scale) {
        int scaled = (int) (Math.round((oldDurationMins * scale) / 30.0) * 30);
        return Math.max(scaled, 30);   // never less than 30 min
    }

    /**
     * Scales the gap between classes proportionally, rounded to the nearest 5 minutes.
     * Formula: Math.round((oldGap * (S * S)) / 5.0) * 5
     * Ensures gap never drops below 5 minutes.
     */
    private int calculateNewGap(int oldGapMins, double scale) {
        int scaled = (int) (Math.round((oldGapMins * (scale * scale)) / 5.0) * 5);
        return Math.max(scaled, 5);    // never less than 5 min
    }

    // ==================================================================
    //  PUBLIC API
    // ==================================================================

    /**
     * Rebalances the entire schedule when the university operating window
     * changes. Uses proportional time-scaling to shrink durations and gaps
     * rather than deleting classes.
     *
     * @param oldOpen            previous campus opening time
     * @param oldClose           previous campus closing time
     * @param newOpen            new campus opening time
     * @param newClose           new campus closing time
     * @param oldStandardGapMins previous standard gap between classes (minutes)
     * @return human-readable summary string
     */
    public String resolveRuleChanges(Time oldOpen, Time oldClose,
                                     Time newOpen, Time newClose,
                                     int oldStandardGapMins) {

        int scaledShifted = 0;
        int quarantined   = 0;

        // Phase 1: calculate scale factor
        double S = calculateScale(oldOpen, oldClose, newOpen, newClose);
        int newGapMins = calculateNewGap(oldStandardGapMins, S);

        System.out.println("[ConstraintResolverService] Scale factor S = " + S);
        System.out.println("[ConstraintResolverService] Old gap = " + oldStandardGapMins
                         + "m → New gap = " + newGapMins + "m");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // ────────────────────────────────────────────────────
                //  Phase 2 — AUDIT: collect every violating session
                // ────────────────────────────────────────────────────
                List<ViolatingSession> violations = auditViolations(conn, newOpen, newClose);

                System.out.println("[ConstraintResolverService] Found "
                                 + violations.size() + " violating session(s).");

                for (ViolatingSession v : violations) {

                    // Phase 1 applied: scale the duration
                    int newDuration = calculateNewDuration(v.oldDurationMins, S);

                    System.out.println("  Session " + v.sessionId
                                     + ": old=" + v.oldDurationMins + "m → scaled=" + newDuration + "m");

                    // ────────────────────────────────────────────────
                    //  Phase 3 — PROPORTIONAL SQUEEZE & SHIFT
                    // ────────────────────────────────────────────────
                    Time slot = findAvailableSlot(conn, v.teacherUid, v.roomId,
                                                  newDuration, newOpen, newClose,
                                                  newGapMins, v.sessionId);
                    if (slot != null) {
                        Time newEnd = addMinutes(slot, newDuration);
                        updateSession(conn, v.sessionId, slot, newEnd);
                        System.out.println("  Session " + v.sessionId
                                         + " scaled from " + v.oldDurationMins + "m to "
                                         + newDuration + "m and shifted.");
                        scaledShifted++;
                        continue;
                    }

                    // ────────────────────────────────────────────────
                    //  Phase 4 — QUARANTINE (emergency fallback)
                    // ────────────────────────────────────────────────
                    quarantineSession(conn, v.sessionId);
                    System.out.println("  Scaling failed to resolve conflict. "
                                     + "Quarantined Session: " + v.sessionId);
                    quarantined++;
                }

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            return "Rebalance FAILED — " + ex.getMessage();
        }

        String summary = "Rebalance Complete. Scaled & Shifted: " + scaledShifted
                       + ", Quarantined: " + quarantined + ".";
        System.out.println("[ConstraintResolverService] " + summary);
        return summary;
    }

    // ==================================================================
    //  Phase 2 — Audit: identify all sessions outside the new window
    // ==================================================================

    private List<ViolatingSession> auditViolations(Connection conn,
                                                    Time newOpen,
                                                    Time newClose) throws SQLException {
        List<ViolatingSession> list = new ArrayList<>();

        // T-SQL query: fetch sessions outside the new operating window
        // LEFT JOIN teacher_assignments to get the teacherUid for overlap checks
        String sql =
            "SELECT cs.sessionId, "
          + "       COALESCE(ta.teacherUid, '') AS teacherUid, "
          + "       cs.roomNumber               AS roomId, "
          + "       cs.startTime, "
          + "       cs.endTime "
          + "FROM class_sessions cs "
          + "LEFT JOIN teacher_assignments ta "
          + "       ON ta.courseCode = cs.courseId "
          + "      AND ta.roomId    = cs.roomNumber "
          + "      AND ta.startTime = cs.startTime "
          + "WHERE cs.startTime IS NOT NULL "
          + "  AND cs.endTime   IS NOT NULL "
          + "  AND (cs.startTime < ? OR cs.endTime > ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, newOpen);
            ps.setTime(2, newClose);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ViolatingSession(
                        rs.getString("sessionId"),
                        rs.getString("teacherUid"),
                        rs.getString("roomId"),
                        rs.getTime("startTime"),
                        rs.getTime("endTime")
                    ));
                }
            }
        }
        return list;
    }

    // ==================================================================
    //  Phase 3 — Find the earliest available slot (proportionally sized)
    // ==================================================================

    /**
     * Scans in 5-minute increments from {@code windowStart} looking for a
     * contiguous block of {@code durationMins} that does NOT overlap with
     * any existing session for the same teacher OR same room, honouring the
     * required gap on both sides.
     *
     * Uses strict T-SQL NOT EXISTS overlap checks.
     *
     * @param excludeSessionId  the session being moved (excluded so it
     *                          doesn't block itself)
     * @return the start {@link Time} of the first valid slot, or null
     */
    private Time findAvailableSlot(Connection conn,
                                   String teacherUid,
                                   String roomId,
                                   int durationMins,
                                   Time windowStart,
                                   Time windowEnd,
                                   int gapMins,
                                   String excludeSessionId) throws SQLException {

        long winStartMs = windowStart.getTime();
        long winEndMs   = windowEnd.getTime();
        long stepMs     = 5L * 60_000;       // scan in 5-minute increments
        long durMs      = (long) durationMins * 60_000;
        long gapMs      = (long) gapMins      * 60_000;

        for (long candidateMs = winStartMs;
             candidateMs + durMs <= winEndMs;
             candidateMs += stepMs) {

            // The "buffered" window: gap before + class + gap after
            Time bufferedStart = new Time(Math.max(winStartMs, candidateMs - gapMs));
            Time bufferedEnd   = new Time(Math.min(winEndMs,   candidateMs + durMs + gapMs));

            if (!hasOverlap(conn, teacherUid, roomId,
                            bufferedStart, bufferedEnd, excludeSessionId)) {
                return new Time(candidateMs);
            }
        }
        return null;   // no viable slot found
    }

    /**
     * Returns {@code true} if ANY existing session for the same teacher
     * OR the same room overlaps with the proposed [start, end) window.
     *
     * Uses T-SQL NOT EXISTS overlap predicate:
     *   existing.startTime < proposed.end  AND  existing.endTime > proposed.start
     */
    private boolean hasOverlap(Connection conn,
                               String teacherUid,
                               String roomId,
                               Time proposedStart,
                               Time proposedEnd,
                               String excludeSessionId) throws SQLException {

        // Check 1 — Room overlap via class_sessions
        String roomSql =
            "SELECT 1 WHERE EXISTS ("
          + "  SELECT 1 FROM class_sessions "
          + "  WHERE roomNumber = ? "
          + "    AND sessionId <> ? "
          + "    AND startTime IS NOT NULL "
          + "    AND startTime < ? "
          + "    AND endTime   > ?"
          + ")";

        try (PreparedStatement ps = conn.prepareStatement(roomSql)) {
            ps.setString(1, roomId);
            ps.setString(2, excludeSessionId);
            ps.setTime(3, proposedEnd);
            ps.setTime(4, proposedStart);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return true;
            }
        }

        // Check 2 — Teacher overlap via teacher_assignments
        if (teacherUid != null && !teacherUid.isEmpty()) {
            String teacherSql =
                "SELECT 1 WHERE EXISTS ("
              + "  SELECT 1 FROM teacher_assignments "
              + "  WHERE teacherUid = ? "
              + "    AND CAST(startTime AS TIME) < CAST(? AS TIME) "
              + "    AND CAST(endTime   AS TIME) > CAST(? AS TIME)"
              + ")";

            try (PreparedStatement ps = conn.prepareStatement(teacherSql)) {
                ps.setString(1, teacherUid);
                ps.setTime(2, proposedEnd);
                ps.setTime(3, proposedStart);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return true;
                }
            }
        }

        return false;
    }

    // ==================================================================
    //  UPDATE helpers (T-SQL with DATEADD)
    // ==================================================================

    /**
     * Phase 3 — moves a session to its new (scaled) time slot.
     * Uses direct parameter binding (already computed in Java).
     */
    private void updateSession(Connection conn, String sessionId,
                               Time newStart, Time newEnd) throws SQLException {
        String sql =
            "UPDATE class_sessions "
          + "SET startTime = ?, endTime = ? "
          + "WHERE sessionId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, newStart);
            ps.setTime(2, newEnd);
            ps.setString(3, sessionId);
            ps.executeUpdate();
        }
    }

    /**
     * Phase 4 — quarantines an unresolvable session.
     * Sets status to CONFLICT and NULLs out time/room.
     */
    private void quarantineSession(Connection conn, String sessionId) throws SQLException {
        String sql =
            "UPDATE class_sessions "
          + "SET status = 'CONFLICT', "
          + "    startTime = NULL, "
          + "    endTime = NULL, "
          + "    roomNumber = NULL "
          + "WHERE sessionId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        }
    }

    // ==================================================================
    //  Time arithmetic helpers
    // ==================================================================

    /** Returns the number of minutes between two Time values. */
    private static int minutesBetween(Time from, Time to) {
        return (int) ((to.getTime() - from.getTime()) / 60_000);
    }

    /** Returns a new Time that is {@code minutes} after {@code base}. */
    private static Time addMinutes(Time base, int minutes) {
        return new Time(base.getTime() + (long) minutes * 60_000);
    }
}
