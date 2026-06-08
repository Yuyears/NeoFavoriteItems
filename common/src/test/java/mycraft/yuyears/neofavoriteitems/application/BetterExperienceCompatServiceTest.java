package mycraft.yuyears.neofavoriteitems.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class BetterExperienceCompatServiceTest {
    @Test
    void findIdentityIndexUsesReferenceIdentity() {
        Object first = new Object();
        Object second = new Object();
        Object equalButDifferentReference = new String("same");
        Object candidate = new String("same");

        assertEquals(1, BetterExperienceCompatService.findIdentityIndex(
            List.of(first, second, equalButDifferentReference),
            second
        ));
        assertEquals(-1, BetterExperienceCompatService.findIdentityIndex(
            List.of(first, second, equalButDifferentReference),
            candidate
        ));
    }

    @Test
    void findIdentityIndexHandlesMissingInputs() {
        Object candidate = new Object();

        assertEquals(-1, BetterExperienceCompatService.findIdentityIndex(null, candidate));
        assertEquals(-1, BetterExperienceCompatService.findIdentityIndex(List.of(candidate), null));
        assertEquals(-1, BetterExperienceCompatService.findIdentityIndex(List.of(), candidate));
    }
}
