package service;

import dao.SessionRepositoryDAO;
import java.util.Map;

/**
 * // GRASP Pattern: Pure Fabrication + Information Expert
 * Used in UC-12
 */
public class SessionRepository {
    private SessionRepositoryDAO sessionRepositoryDAO;

    public SessionRepository() {
        this.sessionRepositoryDAO = new SessionRepositoryDAO();
    }

    public Object fetchHistoricalData(String reportType, String dateRange) {
        try {
            return sessionRepositoryDAO.fetchHistoricalData(reportType, dateRange);
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Integer> aggregateSessionStates() {
        try {
            System.out.println("SessionRepository: Aggregating session states...");
            return sessionRepositoryDAO.aggregateSessionStates();
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return null;
        }
    }
}
