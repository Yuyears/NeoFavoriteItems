package mycraft.yuyears.neofavoriteitems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
