package tsystems.com.gitb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Component used to store sessions and their state.
 * <p/>
 * This class is key in maintaining an overall context across a request and one or more
 * responses. It allows mapping of received data to a given test session running in the
 * test bed.
 * <p/>
 * This implementation stores session information in memory. An alternative solution
 * that would be fault-tolerant could store test session data in a DB.
 */
@Component
public class StateManager {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);

    /** The map of in-memory active sessions. */
    private final Map<String, Map<String, Object>> sessions = new HashMap<>();
    /** Lock object to use for synchronisation. */
    private final Object lock = new Object();

    /**
     * Create a new session.
     *
     * @param sessionId The session ID to use (if null a new one will be generated).
     * @param callbackURL The callback URL to set for this session.
     * @return The session ID that was generated (generated if not provided).
     */
    public String createSession(String sessionId, String callbackURL) {
        if (callbackURL == null) {
            throw new IllegalArgumentException("A callback URL must be provided");
        }
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        synchronized (lock) {
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put(SessionData.CALLBACK_URL, callbackURL);
            sessions.put(sessionId, sessionInfo);
        }
        return sessionId;
    }

    /**
     * Remove the provided session from the list of tracked sessions.
     *
     * @param sessionId The session ID to remove.
     */
    public void destroySession(String sessionId) {
        synchronized (lock) {
            sessions.remove(sessionId);
        }
    }

    /**
     * Get a given item of information linked to a specific session.
     *
     * @param sessionId The session ID we want to lookup.
     * @param infoKey The key of the value that we want to retrieve.
     * @return The retrieved value.
     */
    public Object getSessionInfo(String sessionId, String infoKey) {
        synchronized (lock) {
            Object value = null;
            if (sessions.containsKey(sessionId)) {
                value = sessions.get(sessionId).get(infoKey);
            }
            return value;
        }
    }

    /**
     * Set the given information item for a session.
     *
     * @param sessionId The session ID to set the information for.
     * @param infoKey The information key.
     * @param infoValue The information value.
     */
    public void setSessionInfo(String sessionId, String infoKey, Object infoValue) {
        synchronized (lock) {
            sessions.get(sessionId).put(infoKey, infoValue);
        }
    }

    /**
     * Get all the active sessions.
     *
     * @return An unmodifiable map of the sessions.
     */
    public Map<String, Map<String, Object>> getAllSessions() {
        synchronized (lock) {
            return Collections.unmodifiableMap(sessions);
        }
    }

    /**
     * Constants used to identify data maintained as part of a session's state.
     */
    public static class SessionData {

        /** The URL on which the test bed is to be called back. */
        public static final String CALLBACK_URL = "callbackURL";

    }

}
