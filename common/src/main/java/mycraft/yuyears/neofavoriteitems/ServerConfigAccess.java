package mycraft.yuyears.neofavoriteitems;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ServerConfigAccess {
    public enum State { UNKNOWN, UNAVAILABLE, DENIED, ALLOWED }

    private static BooleanSupplier serverPresent = () -> false;
    private static Runnable requestSender = () -> {};
    private static Consumer<ServerRulesSnapshot> updateSender = ignored -> {};
    private static State state = State.UNKNOWN;
    private static ServerRulesSnapshot snapshot;
    private static long revision;

    private ServerConfigAccess() {}

    public static void configureClient(BooleanSupplier present, Runnable request,
                                       Consumer<ServerRulesSnapshot> update) {
        serverPresent = present;
        requestSender = request;
        updateSender = update;
    }

    public static void request() {
        if (!serverPresent.getAsBoolean()) {
            receive(false, false, null);
            return;
        }
        state = State.UNKNOWN;
        revision++;
        requestSender.run();
    }

    public static boolean submit(ServerRulesSnapshot rules) {
        if (!canEdit() || rules == null) return false;
        updateSender.accept(rules);
        return true;
    }

    public static void receive(boolean serverSupported, boolean allowed, ServerRulesSnapshot rules) {
        state = !serverSupported ? State.UNAVAILABLE : allowed ? State.ALLOWED : State.DENIED;
        snapshot = allowed ? rules : null;
        revision++;
    }

    public static void reset() {
        state = State.UNKNOWN;
        snapshot = null;
        revision++;
    }

    public static State state() { return state; }
    public static boolean canEdit() { return state == State.ALLOWED; }
    public static ServerRulesSnapshot snapshot() { return snapshot; }
    public static long revision() { return revision; }
}
