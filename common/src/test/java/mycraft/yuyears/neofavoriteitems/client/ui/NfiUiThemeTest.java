package mycraft.yuyears.neofavoriteitems.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NfiUiThemeTest {
    @Test
    void presetsUseRequestedColorsAndOpacity() {
        assertEquals(0xBFFFC0CB, NfiUiTheme.DEFAULT.background);
        assertEquals(0xBF000000, NfiUiTheme.DARK.background);
        assertEquals(0xBF87CEEB, NfiUiTheme.BLUE.background);
        assertEquals(0xFF000000, NfiUiTheme.DEFAULT.text);
        assertEquals(0xFFFFFFFF, NfiUiTheme.DARK.text);
        assertEquals(0xFF000000, NfiUiTheme.BLUE.text);
        assertEquals(0xFFCDB6BD, NfiUiTheme.DEFAULT.shadow);
        assertEquals(0xFFACC7D2, NfiUiTheme.BLUE.shadow);
        assertEquals(0xFFEFD5DD, NfiUiTheme.DEFAULT.well);
        assertEquals(0xFFD5EDF7, NfiUiTheme.BLUE.well);
    }
}
