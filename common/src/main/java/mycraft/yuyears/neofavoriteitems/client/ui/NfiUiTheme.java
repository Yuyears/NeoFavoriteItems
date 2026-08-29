package mycraft.yuyears.neofavoriteitems.client.ui;

public enum NfiUiTheme {
    DEFAULT(0xBFFFC0CB, 0xFF000000, 0xFFCDB6BD, 0xE6FFFFFF, 0xFFEFD5DD, 0xFFFF7FA5),
    DARK(0xBF000000, 0xFFFFFFFF, 0xFF3F3F3F, 0xFF252A31, 0xFF090909, 0xFF8AB4F8),
    BLUE(0xBF87CEEB, 0xFF000000, 0xFFACC7D2, 0xE6FFFFFF, 0xFFD5EDF7, 0xFF3188B5);

    public final int background;
    public final int text;
    public final int shadow;
    public final int control;
    public final int well;
    public final int accent;

    NfiUiTheme(int background, int text, int shadow, int control, int well, int accent) {
        this.background = background;
        this.text = text;
        this.shadow = shadow;
        this.control = control;
        this.well = well;
        this.accent = accent;
    }
}
