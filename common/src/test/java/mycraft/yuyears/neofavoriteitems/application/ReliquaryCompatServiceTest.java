package mycraft.yuyears.neofavoriteitems.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReliquaryCompatServiceTest {
    @Test
    void preservesLockedMatchesInAdditionToModeKeepQuantity() {
        assertEquals(128, ReliquaryCompatService.effectiveKeepQuantity(1728, 64, 1600));
    }

    @Test
    void leavesModeKeepQuantityUnchangedWhenItCoversLockedMatches() {
        assertEquals(64, ReliquaryCompatService.effectiveKeepQuantity(96, 64, 64));
    }

    @Test
    void fullInventoryModeDoesNotOverflow() {
        assertEquals(Integer.MAX_VALUE, ReliquaryCompatService.effectiveKeepQuantity(1728, Integer.MAX_VALUE, 1600));
    }
}
