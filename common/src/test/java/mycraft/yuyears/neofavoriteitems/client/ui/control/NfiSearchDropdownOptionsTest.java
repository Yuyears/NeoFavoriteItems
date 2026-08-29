package mycraft.yuyears.neofavoriteitems.client.ui.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfiSearchDropdownOptionsTest {
    @Test
    void presetsCoverStaticSearchQueryAndLazyQuery() {
        var plain = NfiSearchDropdown.Options.plainDropdown();
        var search = NfiSearchDropdown.Options.searchableDropdown();
        var query = NfiSearchDropdown.Options.queryDropdown();
        var lazy = NfiSearchDropdown.Options.searchableLazyQuery();

        assertFalse(plain.searchable() || plain.query() || plain.lazyLoad());
        assertTrue(search.searchable());
        assertFalse(search.query() || search.lazyLoad());
        assertTrue(query.query());
        assertFalse(query.searchable() || query.lazyLoad());
        assertTrue(lazy.searchable() && lazy.query() && lazy.lazyLoad());
    }

    @Test
    void lazyLoadingRequiresPagedQuerySource() {
        assertThrows(IllegalArgumentException.class,
            () -> new NfiSearchDropdown.Options(true, false, true, 10, 500L));
    }
}
