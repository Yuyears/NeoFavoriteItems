package mycraft.yuyears.neofavoriteitems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformFavoriteSupportTest {
    @Test
    void prefersServerDataIpForClientStorageNamespace() {
        assertEquals(
            "example.com:25565",
            PlatformFavoriteSupport.selectClientStorageNamespace("example.com:25565", "/127.0.0.1:25565")
        );
    }

    @Test
    void fallsBackToRemoteAddressWhenServerDataIsUnavailable() {
        assertEquals(
            "127.0.0.1:25565",
            PlatformFavoriteSupport.selectClientStorageNamespace(null, "/127.0.0.1:25565")
        );
    }

    @Test
    void usesDefaultNamespaceOnlyWhenNoServerIdentityExists() {
        assertEquals(
            NeoFavoriteItemsConstants.DEFAULT_SERVER_DIRECTORY,
            PlatformFavoriteSupport.selectClientStorageNamespace(" ", null)
        );
    }

    @Test
    void clientLocalPersistenceIsDisabledWhenServerIsAuthoritative() {
        assertFalse(PlatformFavoriteSupport.usesClientLocalPersistence(true));
    }

    @Test
    void clientLocalPersistenceIsEnabledForClientOnlyMode() {
        assertTrue(PlatformFavoriteSupport.usesClientLocalPersistence(false));
    }

    @Test
    void singleplayerServerIsAuthoritativeBeforeNetworkChannelIsDetected() {
        assertTrue(PlatformFavoriteSupport.isServerAuthoritative(false, true));
    }

    @Test
    void remoteConnectionIsAuthoritativeWhenServerChannelIsDetected() {
        assertTrue(PlatformFavoriteSupport.isServerAuthoritative(true, false));
    }

    @Test
    void remoteConnectionWithoutServerChannelUsesClientLocalPersistence() {
        assertFalse(PlatformFavoriteSupport.isServerAuthoritative(false, false));
    }

    @Test
    void serverAuthoritativeContextDoesNotUseClientLocalPersistenceCleanup() {
        assertFalse(PlatformFavoriteSupport.usesClientLocalPersistence(
            PlatformFavoriteSupport.isServerAuthoritative(false, true)
        ));
    }

    @Test
    void recognizesForgeAndNeoForgeFakePlayers() {
        assertTrue(PlatformFavoriteSupport.isSyntheticPlayerClassName(
            "net.neoforged.neoforge.common.util.FakePlayer"
        ));
        assertTrue(PlatformFavoriteSupport.isSyntheticPlayerClassName(
            "net.minecraftforge.common.util.FakePlayer"
        ));
    }

    @Test
    void doesNotRecognizeRegularPlayersAsSynthetic() {
        assertFalse(PlatformFavoriteSupport.isSyntheticPlayerClassName(
            "net.minecraft.server.level.ServerPlayer"
        ));
        assertFalse(PlatformFavoriteSupport.isSyntheticPlayerClassName(
            "com.example.FakePlayerLikeThing"
        ));
    }
}
