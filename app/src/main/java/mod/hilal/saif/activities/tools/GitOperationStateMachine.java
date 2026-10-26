package mod.hilal.saif.activities.tools;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Cooper Git Integration Engine operation lifecycle.
 * Invalid transitions fail closed; callers must persist state before side effects.
 */
public final class GitOperationStateMachine {
    public enum State {
        CREATED, INSPECTING, PLANNED, AWAITING_APPROVAL, EXECUTING, VERIFYING,
        SUCCEEDED, PARTIAL, FAILED, CANCELLED, DENIED, RECOVERY_REQUIRED
    }

    private static final Map<State, Set<State>> ALLOWED;
    static {
        EnumMap<State, Set<State>> map = new EnumMap<>(State.class);
        map.put(State.CREATED, EnumSet.of(State.INSPECTING, State.CANCELLED, State.DENIED));
        map.put(State.INSPECTING, EnumSet.of(State.PLANNED, State.FAILED, State.CANCELLED, State.RECOVERY_REQUIRED));
        map.put(State.PLANNED, EnumSet.of(State.AWAITING_APPROVAL, State.EXECUTING, State.CANCELLED, State.DENIED, State.FAILED));
        map.put(State.AWAITING_APPROVAL, EnumSet.of(State.EXECUTING, State.CANCELLED, State.DENIED, State.PLANNED));
        map.put(State.EXECUTING, EnumSet.of(State.VERIFYING, State.PARTIAL, State.FAILED, State.CANCELLED, State.RECOVERY_REQUIRED));
        map.put(State.VERIFYING, EnumSet.of(State.SUCCEEDED, State.PARTIAL, State.FAILED, State.RECOVERY_REQUIRED));
        map.put(State.RECOVERY_REQUIRED, EnumSet.of(State.INSPECTING, State.VERIFYING, State.PARTIAL, State.FAILED, State.CANCELLED));
        map.put(State.PARTIAL, EnumSet.of(State.RECOVERY_REQUIRED, State.VERIFYING, State.FAILED));
        map.put(State.SUCCEEDED, Collections.emptySet());
        map.put(State.FAILED, Collections.emptySet());
        map.put(State.CANCELLED, Collections.emptySet());
        map.put(State.DENIED, Collections.emptySet());
        ALLOWED = Collections.unmodifiableMap(map);
    }

    private GitOperationStateMachine() {}

    public static boolean canTransition(State from, State to) {
        return from != null && to != null && ALLOWED.containsKey(from) && ALLOWED.get(from).contains(to);
    }

    public static State transition(State from, State to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Invalid Cooper Git operation transition: " + from + " -> " + to);
        }
        return to;
    }

    public static Set<State> allowedNextStates(State state) {
        Set<State> next = ALLOWED.get(state);
        return next == null ? Collections.emptySet() : Collections.unmodifiableSet(next);
    }
}
