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
}
