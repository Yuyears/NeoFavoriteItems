package mycraft.yuyears.neofavoriteitems.domain;

public record InteractionDecision(boolean allowed, boolean bypassed, String reason) {
    public static InteractionDecision allow() {
        return new InteractionDecision(true, false, "allowed");
    }

    public static InteractionDecision allowBypass() {
        return new InteractionDecision(true, true, "bypassed");
    }

    public static InteractionDecision deny(String reason) {
        return new InteractionDecision(false, false, reason);
    }

    public boolean denied() {
        return !allowed;
    }
}
