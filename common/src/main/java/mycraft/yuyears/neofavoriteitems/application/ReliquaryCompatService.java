package mycraft.yuyears.neofavoriteitems.application;

public final class ReliquaryCompatService {
    private ReliquaryCompatService() {}

    public static int effectiveKeepQuantity(int total, int keep, int unlockedMatching) {
        int protectedMatching = Math.max(0, total - Math.max(0, unlockedMatching));
        return Math.max(Math.max(0, keep), protectedMatching);
    }
}
