package mycraft.yuyears.neofavoriteitems.client;

import java.util.List;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsConfig;
import mycraft.yuyears.neofavoriteitems.PlatformFavoriteSupport;
import mycraft.yuyears.neofavoriteitems.OverlayProfileConfig;
import mycraft.yuyears.neofavoriteitems.ServerConfigAccess;
import mycraft.yuyears.neofavoriteitems.ServerRulesSnapshot;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiTheme;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiUiRenderer;
import mycraft.yuyears.neofavoriteitems.client.ui.NfiCanvases;
import mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValueBinding;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiColorPicker;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiCycleButton;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiSlider;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiTabs;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiDropdown;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiSearchDropdown;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiNumberField;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiButton;
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiToggle;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiConfigPanel;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiConfigRow;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiWidgetGroup;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.render.OverlayMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.OverlayColorMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayPlacement;
import mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog;
import mycraft.yuyears.neofavoriteitems.render.SlotRenderTarget;
import mycraft.yuyears.neofavoriteitems.render.pipeline.OverlayDrawEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import mycraft.yuyears.neofavoriteitems.NeoFavoriteItemsMod;

/** Shared client configuration screen kept independent of loader event APIs. */
public final class NeoFavoriteItemsConfigScreen extends Screen {
    private static final long SOUND_PREVIEW_COOLDOWN_MS = 200L;
    private static final List<PreviewMode> PREVIEW_MODES = List.of(PreviewMode.values());
    private static final List<OverlayMaterialMode> MATERIAL_MODES = List.of(OverlayMaterialMode.values());
    private static final List<NfiUiTheme> THEMES = List.of(NfiUiTheme.values());
    private static final List<OverlayColorMode> COLOR_MODES = List.of(
        OverlayColorMode.TINT, OverlayColorMode.NATIVE, OverlayColorMode.MULTIPLY,
        OverlayColorMode.GRAYSCALE, OverlayColorMode.REPLACE
    );
    private static final List<OverlayProfileConfig.OpacityBehavior> OPACITY_BEHAVIORS = List.of(OverlayProfileConfig.OpacityBehavior.values());
    private static final List<OverlayPlacement.Anchor> ANCHORS = List.of(OverlayPlacement.Anchor.values());
    private static final List<ConfigPage> CONFIG_PAGES = List.of(ConfigPage.values());
    private static final List<NeoFavoriteItemsConfig.SlotMoveBehavior> MOVE_BEHAVIORS =
        List.of(NeoFavoriteItemsConfig.SlotMoveBehavior.values());

    private final OverlayDrawEngine drawEngine = new OverlayDrawEngine();
    private final Screen previousScreen;
    private OverlayProfileConfig lockedProfile;
    private OverlayProfileConfig bypassProfile;
    private OverlayProfileConfig lockableProfile;
    private OverlayProfileConfig unlockableProfile;
    private NfiUiTheme theme = NfiUiTheme.DEFAULT;
    private PreviewMode previewMode = PreviewMode.NORMAL;
    private PreviewMode profileTab = PreviewMode.NORMAL;
    private ConfigPage configPage = ConfigPage.RENDERING;
    private String assetSummary = "";
    private NfiTabs<PreviewMode> profileTabs;
    private NfiTabs<ConfigPage> configTabs;
    private NfiCycleButton<OverlayMaterialMode> styleControl;
    private NfiCycleButton<NfiUiTheme> themeControl;
    private NfiCycleButton<OverlayColorMode> colorModeControl;
    private NfiDropdown<String> materialControl;
    private NfiCycleButton<OverlayProfileConfig.OpacityBehavior> opacityBehaviorControl;
    private NfiCycleButton<OverlayPlacement.Anchor> anchorControl;
    private NfiNumberField<Float> offsetXControl, offsetYControl, widthControl, heightControl, scaleControl, rotationControl, zIndexControl;
    private NfiToggle overflowControl, clipControl;
    private NfiCycleButton<PreviewMode> previewModeControl;
    private NfiSlider opacitySlider;
    private NfiColorPicker colorPicker;
    private Button assetsButton;
    private NfiButton assetsFolderButton;
    private NfiToggle showVisualFeedbackControl, playSoundFeedbackControl;
    private NfiSearchDropdown<String> feedbackSoundControl;
    private NfiSlider feedbackVolumeControl, feedbackPitchControl;
    private boolean showVisualFeedbackDraft, playSoundFeedbackDraft;
    private String feedbackSoundDraft;
    private float feedbackVolumeDraft, feedbackPitchDraft;
    private long lastSoundPreviewAt = -SOUND_PREVIEW_COOLDOWN_MS;
    private SoundInstance lastSoundPreview;
    private ServerRulesDraft serverRulesDraft;
    private final java.util.List<AbstractWidget> soundDependentControls = new java.util.ArrayList<>();
    private final java.util.List<AbstractWidget> serverRuleControls = new java.util.ArrayList<>();
    private long observedServerAccessRevision = -1L;
    private boolean dropdownCapturedMouse;
    private NfiConfigPanel configPanel;
    private int contentLeft;
    private int contentWidth;
    private int configTop;
    private int configHeight;
    private int previewX;
    private int previewY;

    private NeoFavoriteItemsConfigScreen(Screen previousScreen) {
        super(Component.translatable("screen.neo_favorite_items.title"));
        this.previousScreen = previousScreen;
        loadDraft();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Keep world view sharp; this screen draws its own bounded panel.
    }

    @Override
    public void tick() {
        // Poll split config files while screen is open; dirty drafts are never overwritten.
        ConfigManager.getInstance().reloadIfChanged();
        if (feedbackSoundControl != null) feedbackSoundControl.tick();
        super.tick();
    }

    public static void open(Minecraft minecraft) {
        if (!(minecraft.screen instanceof NeoFavoriteItemsConfigScreen)) {
            ServerConfigAccess.request();
            minecraft.setScreen(new NeoFavoriteItemsConfigScreen(minecraft.screen));
        }
    }

    @Override
    protected void init() {
        clearPageControlReferences();
        contentWidth = Math.min(420, Math.max(300, width - 16));
        contentLeft = Math.max(8, (width - contentWidth) / 2);
        int innerLeft = contentLeft + 10;
        int innerWidth = contentWidth - 20;
        configTabs = new NfiTabs<>(innerLeft, 32, innerWidth, 20, pageBinding(), CONFIG_PAGES, this::pageLabel);
        configTabs.setPrimaryStyle(true);
        configTabs.setEnabled(ConfigPage.SERVER_RULES, ServerConfigAccess.canEdit());
        configTabs.widgets().forEach(this::addRenderableWidget);

        if (configPage == ConfigPage.RENDERING) {
            int profileTabsInset = Math.min(16, innerWidth / 16);
            profileTabs = new NfiTabs<>(innerLeft + profileTabsInset, 57,
                innerWidth - profileTabsInset * 2, 18, profileBinding(), PREVIEW_MODES, this::profileLabel);
            profileTabs.setSegmentedStyle(true);
            profileTabs.widgets().forEach(this::addRenderableWidget);
            previewX = contentLeft + contentWidth - 108;
            previewY = 92;
            int previewControlsWidth = Math.max(120, previewX - innerLeft - 8);
            int previewControlWidth = (previewControlsWidth - 4) / 2;
            previewModeControl = new NfiCycleButton<>(innerLeft, 128, previewControlWidth, 20,
                previewModeBinding(), PREVIEW_MODES,
                value -> Component.translatable("screen.neo_favorite_items.preview_mode", profileLabel(value)));
            themeControl = new NfiCycleButton<>(innerLeft + previewControlWidth + 4, 128,
                previewControlsWidth - previewControlWidth - 4, 20, themeBinding(), THEMES,
                value -> Component.translatable("screen.neo_favorite_items.theme", value.name()));
            addRenderableWidget(previewModeControl.widget());
            addRenderableWidget(themeControl.widget());
            configTop = 156;
        } else {
            profileTabs = null;
            configTop = 68;
        }
        configHeight = Math.max(24, height - configTop - 42);
        int labelWidth = Math.clamp(innerWidth / 3, 96, 128);
        configPanel = new NfiConfigPanel(innerLeft, configTop, innerWidth, labelWidth);
        configPanel.setViewportHeight(configHeight);

        if (configPage == ConfigPage.RENDERING) {
            buildRenderingControls();
        } else if (configPage == ConfigPage.CLIENT_LOGIC) {
            buildClientLogicControls();
        } else {
            buildServerRuleControls();
        }

        int buttonLeft = contentLeft + (contentWidth - 284) / 2;
        addRenderableWidget(new NfiButton(buttonLeft, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.reset"), ignored -> resetActivePage()));
        addRenderableWidget(new NfiButton(buttonLeft + 72, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.reload"), ignored -> reloadDraft()));
        addRenderableWidget(new NfiButton(buttonLeft + 144, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.apply"), ignored -> applyAndClose()));
        addRenderableWidget(new NfiButton(buttonLeft + 216, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.cancel"), ignored -> onClose()));
        if (configPage == ConfigPage.RENDERING) refreshAssets();
        syncControls();
    }

    private void buildRenderingControls() {

        int assetsWidth = Math.max(1, (configPanel.controlWidth() * 2) / 3);
        int folderWidth = Math.max(1, assetsWidth / 2);
        assetsButton = new NfiButton(0, 0, assetsWidth, 20,
            Component.translatable("screen.neo_favorite_items.refresh_assets"), ignored -> refreshAssets());
        assetsFolderButton = new NfiButton(assetsWidth, 0, folderWidth, 20, Component.empty(), ignored -> {
            if (assetsButton != null) assetsButton.setFocused(false);
            PlatformFavoriteSupport.openCustomAssetsDirectory();
        });
        assetsFolderButton.setIcon(ResourceLocation.fromNamespaceAndPath(NeoFavoriteItemsMod.MOD_ID, "textures/folder.png"));
        assetsFolderButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("screen.neo_favorite_items.open_assets_folder")));
        NfiWidgetGroup assetsGroup = new NfiWidgetGroup(0, 0);
        assetsGroup.addRelative(assetsButton, 0, 0);
        assetsGroup.addRelative(assetsFolderButton, assetsWidth, 0);
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.custom_assets_label"), assetsGroup, List.of(assetsButton, assetsFolderButton));

        styleControl = new NfiCycleButton<>(0, 0, 1, 20, materialModeBinding(), MATERIAL_MODES,
            value -> Component.translatable("screen.neo_favorite_items.material_mode." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.material_mode_label"), styleControl.widget());

        materialControl = new NfiDropdown<>(0, 0, configPanel.controlWidth(), 4, materialBinding(), materialValues(), value -> Component.literal(materialLabel(value)));
        addRenderableWidget(materialControl.button());
        configPanel.add(new NfiConfigRow(Component.translatable("screen.neo_favorite_items.material_label"),
            materialControl.group(), List.of(materialControl.button())));

        colorModeControl = new NfiCycleButton<>(0, 0, 1, 20, colorModeBinding(), COLOR_MODES,
            value -> Component.translatable("screen.neo_favorite_items.color_mode." + value.name().toLowerCase(java.util.Locale.ROOT)));
        colorModeControl.setTooltipFactory(value -> Component.translatable(
            "screen.neo_favorite_items.color_mode.tooltip." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.color_mode_label"), colorModeControl.widget());

        if (profileTab == PreviewMode.CTRL) {
            opacityBehaviorControl = new NfiCycleButton<>(0, 0, 1, 20, opacityBehaviorBinding(), OPACITY_BEHAVIORS,
                value -> Component.translatable("screen.neo_favorite_items.opacity_behavior." + value.name().toLowerCase(java.util.Locale.ROOT)));
            addRow(configPanel, Component.translatable("screen.neo_favorite_items.opacity_behavior_label"), opacityBehaviorControl.widget());
        } else {
            opacityBehaviorControl = null;
        }

        anchorControl = new NfiCycleButton<>(0, 0, 1, 20, anchorBinding(), ANCHORS,
            value -> Component.translatable("screen.neo_favorite_items.anchor." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.anchor_label"), anchorControl.widget());

        opacitySlider = new NfiSlider(0, 0, 1, 20, 0.0D, 1.0D, opacityBinding(),
            value -> Component.translatable("screen.neo_favorite_items.opacity", Math.round(value * 100.0D)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.opacity_label"), opacitySlider);

        offsetXControl = numberField(offsetXBinding());
        offsetYControl = numberField(offsetYBinding());
        widthControl = numberField(widthBinding());
        heightControl = numberField(heightBinding());
        scaleControl = numberField(scaleBinding());
        rotationControl = numberField(rotationBinding());
        zIndexControl = numberField(zIndexBinding());
        overflowControl = new NfiToggle(0, 0, 1, 20, overflowBinding(), value -> Component.translatable(value ? "options.on" : "options.off"));
        clipControl = new NfiToggle(0, 0, 1, 20, clipBinding(), value -> Component.translatable(value ? "options.on" : "options.off"));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.offset_x"), offsetXControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.offset_y"), offsetYControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.width"), widthControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.height"), heightControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.scale"), scaleControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.rotation"), rotationControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.z_index"), zIndexControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.overflow"), overflowControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.clip"), clipControl.widget());

        colorPicker = new NfiColorPicker(font, 0, 0, 1, colorBinding());
        colorPicker.setWidth(configPanel.controlWidth());
        configPanel.add(new NfiConfigRow(Component.translatable("screen.neo_favorite_items.color_label"),
            colorPicker.group(), List.of(colorPicker.button())));
        addRenderableWidget(colorPicker.button());
        colorPicker.allPopupWidgets().forEach(this::addRenderableWidget);
    }

    private void buildClientLogicControls() {
        showVisualFeedbackControl = toggle(() -> showVisualFeedbackDraft, value -> showVisualFeedbackDraft = value,
            this::markDirty);
        playSoundFeedbackControl = toggle(() -> playSoundFeedbackDraft, value -> playSoundFeedbackDraft = value, () -> {
            markDirty();
            updateLogicDependencies();
        });
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.logic.visual_feedback"), showVisualFeedbackControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.logic.sound_feedback"), playSoundFeedbackControl.widget());

        int soundWidth = configPanel.controlWidth();
        feedbackSoundControl = new NfiSearchDropdown<>(font, 0, 0, soundWidth,
            NfiValueBinding.unchecked(() -> feedbackSoundDraft, value -> feedbackSoundDraft = value,
                () -> new NeoFavoriteItemsConfig().feedback.feedbackSound, this::markDirty),
            value -> value == null ? "" : value, java.util.function.Function.identity(),
            this::soundIdPage, NfiSearchDropdown.Options.searchableLazyQuery(),
            Component.translatable("screen.neo_favorite_items.no_more"));
        feedbackSoundControl.setOptionAction(NfiCanvases.PLAY, this::previewSound);
        feedbackSoundControl.setOptionTooltipFactory(this::soundSubtitle);
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.logic.sound_id"),
            feedbackSoundControl.group(), feedbackSoundControl.widgets());

        feedbackVolumeControl = new NfiSlider(0, 0, 1, 20, 0.0D, 1.0D,
            NfiValueBinding.unchecked(() -> (double) feedbackVolumeDraft, value -> feedbackVolumeDraft = value.floatValue(),
                () -> 0.5D, this::markDirty),
            value -> Component.translatable("screen.neo_favorite_items.logic.volume_value", Math.round(value * 100.0D)));
        feedbackPitchControl = new NfiSlider(0, 0, 1, 20, 0.0D, 2.0D,
            NfiValueBinding.unchecked(() -> (double) feedbackPitchDraft, value -> feedbackPitchDraft = value.floatValue(),
                () -> 1.0D, this::markDirty),
            value -> Component.translatable("screen.neo_favorite_items.logic.pitch_value", String.format(java.util.Locale.ROOT, "%.2f", value)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.logic.volume"), feedbackVolumeControl);
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.logic.pitch"), feedbackPitchControl);
        soundDependentControls.clear();
        soundDependentControls.addAll(feedbackSoundControl.widgets());
        soundDependentControls.add(feedbackVolumeControl);
        soundDependentControls.add(feedbackPitchControl);
        updateLogicDependencies();
    }

    private void buildServerRuleControls() {
        if (serverRulesDraft == null || !ServerConfigAccess.canEdit()) return;
        serverRuleControls.clear();
        NfiToggle autoUnlock = serverToggle(() -> serverRulesDraft.autoUnlockEmptySlots,
            value -> serverRulesDraft.autoUnlockEmptySlots = value, this::updateServerDependencies);
        NfiToggle lockEmpty = serverToggle(() -> serverRulesDraft.lockEmptySlots,
            value -> serverRulesDraft.lockEmptySlots = value, this::updateServerDependencies);
        NfiToggle allowIntoEmpty = serverToggle(() -> serverRulesDraft.allowItemsIntoLockedEmptySlots,
            value -> serverRulesDraft.allowItemsIntoLockedEmptySlots = value, () -> {});
        addServerRow("auto_unlock_empty", autoUnlock.widget());
        addServerRow("lock_empty", lockEmpty.widget());
        addServerRow("allow_into_locked_empty", allowIntoEmpty.widget());
        addServerToggle("prevent_click", () -> serverRulesDraft.preventClick, value -> serverRulesDraft.preventClick = value);
        addServerToggle("prevent_drop", () -> serverRulesDraft.preventDrop, value -> serverRulesDraft.preventDrop = value);
        addServerToggle("prevent_quick_move", () -> serverRulesDraft.preventQuickMove, value -> serverRulesDraft.preventQuickMove = value);
        addServerToggle("prevent_shift_click", () -> serverRulesDraft.preventShiftClick, value -> serverRulesDraft.preventShiftClick = value);
        addServerToggle("prevent_drag", () -> serverRulesDraft.preventDrag, value -> serverRulesDraft.preventDrag = value);
        addServerToggle("prevent_swap", () -> serverRulesDraft.preventSwap, value -> serverRulesDraft.preventSwap = value);
        addServerToggle("allow_bypass", () -> serverRulesDraft.allowBypassWithKey, value -> serverRulesDraft.allowBypassWithKey = value);
        var moveBehavior = new NfiCycleButton<>(0, 0, 1, 20,
            NfiValueBinding.unchecked(() -> serverRulesDraft.moveBehavior, value -> serverRulesDraft.moveBehavior = value,
                () -> NeoFavoriteItemsConfig.SlotMoveBehavior.STAY_AT_POSITION, this::markDirty),
            MOVE_BEHAVIORS, value -> Component.translatable("screen.neo_favorite_items.server.move_behavior." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addServerRow("move_behavior", moveBehavior.widget());
        addServerToggle("preserve_on_death", () -> serverRulesDraft.preserveLockedSlotContents,
            value -> serverRulesDraft.preserveLockedSlotContents = value);
        addServerToggle("debug", () -> serverRulesDraft.debugEnabled, value -> serverRulesDraft.debugEnabled = value);
        serverRuleControls.add(lockEmpty.widget());
        serverRuleControls.add(allowIntoEmpty.widget());
        updateServerDependencies();
    }

    private void addRow(NfiConfigPanel panel, Component label, AbstractWidget widget) {
        addRenderableWidget(widget);
        panel.add(new NfiConfigRow(label, widget));
    }

    private void addRow(NfiConfigPanel panel, Component label, NfiWidgetGroup group, List<AbstractWidget> widgets) {
        widgets.forEach(this::addRenderableWidget);
        panel.add(new NfiConfigRow(label, group, widgets));
    }

    private void clearPageControlReferences() {
        profileTabs = null;
        styleControl = null;
        materialControl = null;
        colorModeControl = null;
        themeControl = null;
        opacityBehaviorControl = null;
        anchorControl = null;
        previewModeControl = null;
        opacitySlider = null;
        colorPicker = null;
        assetsButton = null;
        feedbackSoundControl = null;
        soundDependentControls.clear();
        serverRuleControls.clear();
    }

    private NfiValueBinding<ConfigPage> pageBinding() {
        return NfiValueBinding.unchecked(() -> configPage, value -> {
            if (value != ConfigPage.SERVER_RULES || ServerConfigAccess.canEdit()) configPage = value;
        },
            () -> ConfigPage.RENDERING, () -> {
                clearWidgets();
                init();
            });
    }

    private Component pageLabel(ConfigPage page) {
        return Component.translatable("screen.neo_favorite_items.page." + page.name().toLowerCase(java.util.Locale.ROOT));
    }

    private NfiToggle toggle(java.util.function.Supplier<Boolean> getter,
                             java.util.function.Consumer<Boolean> setter, Runnable changed) {
        return new NfiToggle(0, 0, 1, 20,
            NfiValueBinding.unchecked(getter, setter, () -> false, changed),
            value -> Component.translatable(value ? "options.on" : "options.off"));
    }

    private NfiToggle serverToggle(java.util.function.Supplier<Boolean> getter,
                                   java.util.function.Consumer<Boolean> setter, Runnable after) {
        return toggle(getter, setter, () -> {
            markDirty();
            after.run();
        });
    }

    private void addServerToggle(String key, java.util.function.Supplier<Boolean> getter,
                                 java.util.function.Consumer<Boolean> setter) {
        addServerRow(key, serverToggle(getter, setter, () -> {}).widget());
    }

    private void addServerRow(String key, AbstractWidget widget) {
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.server." + key), widget);
    }

    private void updateLogicDependencies() {
        soundDependentControls.forEach(widget -> widget.active = playSoundFeedbackDraft);
    }

    private void updateServerDependencies() {
        if (serverRuleControls.size() < 2 || serverRulesDraft == null) return;
        boolean emptyLocksAllowed = !serverRulesDraft.autoUnlockEmptySlots;
        serverRuleControls.get(0).active = emptyLocksAllowed;
        serverRuleControls.get(1).active = emptyLocksAllowed && serverRulesDraft.lockEmptySlots;
    }

    private NfiValueBinding<PreviewMode> profileBinding() {
        return NfiValueBinding.unchecked(
            () -> profileTab,
            value -> profileTab = value,
            () -> PreviewMode.NORMAL,
            () -> {
                clearWidgets();
                init();
            }
        );
    }

    private NfiValueBinding<OverlayMaterialMode> materialModeBinding() {
        return NfiValueBinding.unchecked(
            () -> activeProfile().materialMode,
            value -> {
                activeProfile().materialMode = value;
                if (value == OverlayMaterialMode.MATERIAL && (OverlayTextureCatalog.NO_MATERIAL.equals(activeProfile().materialId)
                    || OverlayTextureCatalog.presetStyle(activeProfile().materialId) == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY)) {
                    activeProfile().materialId = OverlayTextureCatalog.presetId(NeoFavoriteItemsConfig.OverlayStyle.MARK);
                    activeProfile().style = NeoFavoriteItemsConfig.OverlayStyle.MARK;
                }
            },
            () -> OverlayMaterialMode.MATERIAL,
            () -> {
                markDirty();
                syncMaterialControlAvailability();
            }
        );
    }

    private NfiValueBinding<String> materialBinding() {
        return NfiValueBinding.unchecked(() -> activeProfile().materialId, value -> {
            activeProfile().materialId = value;
            var preset = OverlayTextureCatalog.presetStyle(value);
            if (preset != null) activeProfile().style = preset;
        }, () -> activeProfile().materialId, this::markDirty);
    }

    private List<String> materialValues() {
        List<String> values = new java.util.ArrayList<>(OverlayTextureCatalog.presetIds());
        var registry = PlatformFavoriteSupport.getCustomAssetRegistry();
        if (registry != null) registry.entries().forEach(entry -> values.add(entry.id()));
        return values;
    }

    private String materialLabel(String id) {
        var style = OverlayTextureCatalog.presetStyle(id);
        if (OverlayTextureCatalog.NO_MATERIAL.equals(id)) return Component.translatable("screen.neo_favorite_items.material_mode.no_material").getString();
        if (style != null) return "*" + style.name() + "*";
        if (id != null && id.startsWith("preset:")) {
            return "*" + id.substring("preset:".length()).toUpperCase(java.util.Locale.ROOT) + "*";
        }
        return id == null ? "" : id.replace("custom:", "");
    }

    private NfiValueBinding<OverlayProfileConfig.OpacityBehavior> opacityBehaviorBinding() {
        return NfiValueBinding.unchecked(() -> activeProfile().opacityBehavior, v -> activeProfile().opacityBehavior = v,
            () -> OverlayProfileConfig.OpacityBehavior.FIXED, this::markDirty);
    }

    private NfiValueBinding<OverlayPlacement.Anchor> anchorBinding() {
        return NfiValueBinding.unchecked(() -> activeProfile().anchor, v -> activeProfile().anchor = v,
            () -> OverlayPlacement.Anchor.SLOT_TOP_LEFT, this::markDirty);
    }

    private NfiNumberField<Float> numberField(NfiValueBinding<Float> binding) {
        return new NfiNumberField<Float>(font, 0, 0, 1, 20, Component.empty(), binding, Float::parseFloat, value -> value.toString());
    }

    private NfiValueBinding<Float> floatBinding(java.util.function.Supplier<Float> getter,
                                                java.util.function.Consumer<Float> setter,
                                                float fallback, float min) {
        return new NfiValueBinding<>(getter, setter, () -> fallback, value -> value != null && Float.isFinite(value) && value >= min
            ? mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValidationResult.ok()
            : mycraft.yuyears.neofavoriteitems.client.ui.binding.NfiValidationResult.error(Component.translatable("screen.neo_favorite_items.invalid_number")), this::markDirty);
    }

    private NfiValueBinding<Float> offsetXBinding() { return floatBinding(() -> activeProfile().offsetX, v -> activeProfile().offsetX = v, 0.0f, -1024.0f); }
    private NfiValueBinding<Float> offsetYBinding() { return floatBinding(() -> activeProfile().offsetY, v -> activeProfile().offsetY = v, 0.0f, -1024.0f); }
    private NfiValueBinding<Float> widthBinding() { return floatBinding(() -> activeProfile().width, v -> activeProfile().width = v, 16.0f, 0.01f); }
    private NfiValueBinding<Float> heightBinding() { return floatBinding(() -> activeProfile().height, v -> activeProfile().height = v, 16.0f, 0.01f); }
    private NfiValueBinding<Float> scaleBinding() { return floatBinding(() -> activeProfile().scale, v -> activeProfile().scale = v, 1.0f, 0.01f); }
    private NfiValueBinding<Float> rotationBinding() { return floatBinding(() -> activeProfile().rotationDegrees, v -> activeProfile().rotationDegrees = v, 0.0f, -360.0f); }
    private NfiValueBinding<Float> zIndexBinding() { return floatBinding(() -> (float) activeProfile().zIndex, v -> activeProfile().zIndex = normalizeZ(v), 2.0f, 0.0f); }
    private int normalizeZ(float value) {
        int normalized = (int) Math.clamp(value, 0.0f, 1000.0f);
        return normalized == 1 ? 2 : normalized;
    }
    private NfiValueBinding<Boolean> overflowBinding() { return NfiValueBinding.unchecked(() -> activeProfile().allowOverflow, v -> activeProfile().allowOverflow = v, () -> false, this::markDirty); }
    private NfiValueBinding<Boolean> clipBinding() { return NfiValueBinding.unchecked(() -> activeProfile().clipToSlot, v -> activeProfile().clipToSlot = v, () -> false, this::markDirty); }

    private void syncGeometryControls() {
        if (offsetXControl == null) return;
        offsetXControl.syncFromBinding(); offsetYControl.syncFromBinding(); widthControl.syncFromBinding();
        heightControl.syncFromBinding(); scaleControl.syncFromBinding(); rotationControl.syncFromBinding();
        zIndexControl.syncFromBinding(); overflowControl.syncFromBinding(); clipControl.syncFromBinding();
    }

    private void resetActivePage() {
        if (configPage == ConfigPage.RENDERING) {
            OverlayProfileConfig defaultProfile = switch (profileTab) {
                case NORMAL -> OverlayProfileConfig.defaultLocked();
                case CTRL -> OverlayProfileConfig.defaultBypass();
                case ALT -> OverlayProfileConfig.defaultLockable();
                case UNLOCKABLE -> OverlayProfileConfig.defaultUnlockable();
            };
            if (defaultProfile != null) activeProfile().copyFrom(defaultProfile);
            syncControls();
        } else if (configPage == ConfigPage.CLIENT_LOGIC) {
            var defaults = new NeoFavoriteItemsConfig().feedback;
            showVisualFeedbackDraft = defaults.showVisualFeedback;
            playSoundFeedbackDraft = defaults.playSoundFeedback;
            feedbackSoundDraft = defaults.feedbackSound;
            feedbackVolumeDraft = defaults.feedbackVolume;
            feedbackPitchDraft = defaults.feedbackPitch;
            clearWidgets();
            init();
        } else if (ServerConfigAccess.canEdit()) {
            serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
            clearWidgets();
            init();
        }
        markDirty();
    }

    private NfiValueBinding<NfiUiTheme> themeBinding() {
        return NfiValueBinding.unchecked(() -> theme, value -> theme = value, () -> NfiUiTheme.DEFAULT, this::markDirty);
    }

    private NfiValueBinding<OverlayColorMode> colorModeBinding() {
        return NfiValueBinding.unchecked(
            () -> activeProfile().colorMode,
            value -> activeProfile().colorMode = value,
            () -> OverlayColorMode.MULTIPLY,
            this::markDirty
        );
    }

    private NfiValueBinding<PreviewMode> previewModeBinding() {
        return NfiValueBinding.unchecked(
            () -> previewMode,
            value -> previewMode = value,
            () -> PreviewMode.NORMAL,
            () -> {}
        );
    }

    private NfiValueBinding<Double> opacityBinding() {
        return NfiValueBinding.unchecked(
            () -> (double) activeProfile().opacity,
            value -> activeProfile().opacity = (float) Math.clamp(value, 0.0D, 1.0D),
            () -> 0.7D,
            this::markDirty
        );
    }

    private NfiValueBinding<Integer> colorBinding() {
        return NfiValueBinding.unchecked(
            () -> activeProfile().color,
            value -> activeProfile().color = value,
            () -> new NeoFavoriteItemsConfig().overlay.locked.color,
            this::markDirty
        );
    }

    private void markDirty() {
        ConfigManager.getInstance().setDraftDirty(true);
    }

    private void loadDraft() {
        var config = ConfigManager.getInstance().getConfig();
        try {
            theme = NfiUiTheme.valueOf(config.feedback.uiTheme);
        } catch (IllegalArgumentException ignored) {
            theme = NfiUiTheme.DEFAULT;
        }
        var overlay = config.overlay;
        lockedProfile = overlay.locked.copy();
        bypassProfile = overlay.bypass.copy();
        lockableProfile = overlay.lockable.copy();
        unlockableProfile = overlay.unlockable.copy();
        showVisualFeedbackDraft = config.feedback.showVisualFeedback;
        playSoundFeedbackDraft = config.feedback.playSoundFeedback;
        feedbackSoundDraft = config.feedback.feedbackSound;
        feedbackVolumeDraft = config.feedback.feedbackVolume;
        feedbackPitchDraft = config.feedback.feedbackPitch;
        if (ServerConfigAccess.snapshot() != null) {
            serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
        }
        ConfigManager.getInstance().setDraftDirty(false);
    }

    private void reloadDraft() {
        ConfigManager.getInstance().reload();
        ServerConfigAccess.request();
        loadDraft();
        clearWidgets();
        init();
    }

    private void applyAndClose() {
        var config = ConfigManager.getInstance().getConfig();
        var overlay = config.overlay;
        overlay.locked.copyFrom(lockedProfile);
        overlay.bypass.copyFrom(bypassProfile);
        overlay.lockable.copyFrom(lockableProfile);
        overlay.unlockable.copyFrom(unlockableProfile);
        config.feedback.showVisualFeedback = showVisualFeedbackDraft;
        config.feedback.playSoundFeedback = playSoundFeedbackDraft;
        config.feedback.feedbackSound = feedbackSoundDraft;
        config.feedback.feedbackVolume = Math.clamp(feedbackVolumeDraft, 0.0f, 1.0f);
        config.feedback.feedbackPitch = Math.clamp(feedbackPitchDraft, 0.0f, 2.0f);
        config.feedback.uiTheme = theme.name();
        ConfigManager.getInstance().saveClientConfig();
        if (serverRulesDraft != null && ServerConfigAccess.canEdit()) {
            ServerConfigAccess.submit(serverRulesDraft.toSnapshot());
        }
        ConfigManager.getInstance().setDraftDirty(false);
        onClose();
    }

    @Override
    public void onClose() {
        stopSoundPreview();
        ConfigManager.getInstance().setDraftDirty(false);
        minecraft.setScreen(previousScreen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.isOpen()) {
            boolean handled = colorPicker.mouseClicked(mouseX, mouseY, button);
            setFocused(colorPicker.activePopupWidget());
            return handled;
        }
        if (feedbackSoundControl != null && feedbackSoundControl.isOpen()) {
            dropdownCapturedMouse = true;
            handleFeedbackSoundClick(mouseX, mouseY, button);
            return true;
        }
        if (materialControl != null && materialControl.isOpen()) {
            dropdownCapturedMouse = true;
            materialControl.mouseClicked(mouseX, mouseY, button);
            setFocused(materialControl.button());
            return true;
        }
        if (materialControl != null && materialControl.mouseClicked(mouseX, mouseY, button)) {
            dropdownCapturedMouse = materialControl.isOpen();
            if (feedbackSoundControl != null) feedbackSoundControl.close();
            setFocused(materialControl.button());
            return true;
        }
        if (feedbackSoundControl != null && feedbackSoundControl.mouseClicked(mouseX, mouseY, button)) {
            dropdownCapturedMouse = feedbackSoundControl.isOpen();
            if (materialControl != null) materialControl.close();
            focusFeedbackSoundControl();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleFeedbackSoundClick(double mouseX, double mouseY, int button) {
        feedbackSoundControl.mouseClicked(mouseX, mouseY, button);
        focusFeedbackSoundControl();
    }

    private void focusFeedbackSoundControl() {
        setFocused(feedbackSoundControl.searchBox().isFocused()
            ? feedbackSoundControl.searchBox()
            : feedbackSoundControl.trigger());
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.isOpen()) return colorPicker.mouseReleased(mouseX, mouseY, button);
        if (dropdownCapturedMouse) {
            dropdownCapturedMouse = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPicker != null && colorPicker.isOpen()) return colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (dropdownCapturedMouse) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && colorPicker != null && colorPicker.isOpen()) {
            colorPicker.close();
            return true;
        }
        if (materialControl != null && materialControl.keyPressed(keyCode)) return true;
        if (feedbackSoundControl != null && feedbackSoundControl.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (feedbackSoundControl != null && feedbackSoundControl.isOpen()) {
            feedbackSoundControl.mouseScrolled(mouseX, mouseY, verticalDelta);
            return true;
        }
        if (materialControl != null && materialControl.isOpen()) {
            materialControl.mouseScrolled(mouseX, mouseY, verticalDelta);
            return true;
        }
        if (materialControl != null && materialControl.mouseScrolled(mouseX, mouseY, verticalDelta)) return true;
        if (feedbackSoundControl != null && feedbackSoundControl.mouseScrolled(mouseX, mouseY, verticalDelta)) return true;
        if (configPanel != null && configPanel.mouseScrolled(mouseX, mouseY, verticalDelta)) {
            if (materialControl != null) {
                materialControl.close();
                materialControl.layoutPopup(height);
            }
            if (colorPicker != null) colorPicker.close();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    private void refreshAssets() {
        var registry = PlatformFavoriteSupport.getCustomAssetRegistry();
        if (registry == null) {
            assetSummary = Component.translatable("screen.neo_favorite_items.assets_unavailable").getString();
            return;
        }
        var textureRefresh = PlatformFavoriteSupport.refreshCustomTextures();
        var entries = textureRefresh == null ? registry.refresh() : textureRefresh.entries();
        assetSummary = entries.isEmpty()
            ? Component.translatable("screen.neo_favorite_items.assets_none").getString()
            : Component.translatable("screen.neo_favorite_items.assets_count", entries.size()).getString();
        if (assetsButton != null) {
            assetsButton.setMessage(Component.translatable("screen.neo_favorite_items.refresh_assets_count", entries.size()));
        }
        if (materialControl != null) {
            materialControl.setValues(materialValues());
            materialControl.layoutPopup(height);
        }
    }

    private void syncControls() {
        if (configTabs != null) configTabs.syncFromBinding();
        if (profileTabs == null) return;
        profileTabs.syncFromBinding();
        styleControl.syncFromBinding();
        if (materialControl != null) materialControl.syncFromBinding();
        syncMaterialControlAvailability();
        colorModeControl.syncFromBinding();
        if (opacityBehaviorControl != null) opacityBehaviorControl.syncFromBinding();
        if (anchorControl != null) anchorControl.syncFromBinding();
        themeControl.syncFromBinding();
        previewModeControl.syncFromBinding();
        opacitySlider.syncFromBinding();
        colorPicker.syncFromBinding();
        syncGeometryControls();
    }

    private void syncMaterialControlAvailability() {
        if (materialControl == null) return;
        boolean enabled = activeProfile().materialMode == OverlayMaterialMode.MATERIAL;
        materialControl.button().active = enabled;
        if (!enabled) materialControl.close();
    }

    private Component profileLabel(PreviewMode mode) {
        return switch (mode) {
            case NORMAL -> Component.translatable("screen.neo_favorite_items.tab.normal");
            case CTRL -> Component.translatable("screen.neo_favorite_items.tab.ctrl");
            case ALT -> Component.translatable("screen.neo_favorite_items.tab.alt_lock");
            case UNLOCKABLE -> Component.translatable("screen.neo_favorite_items.tab.alt_unlock");
        };
    }

    private OverlayProfileConfig activeProfile() {
        return switch (profileTab) {
            case NORMAL -> lockedProfile;
            case CTRL -> bypassProfile;
            case ALT -> lockableProfile;
            case UNLOCKABLE -> unlockableProfile;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        NfiUiRenderer.setTheme(theme);
        renderBlurredBackground(delta);
        if (observedServerAccessRevision != ServerConfigAccess.revision()) {
            observedServerAccessRevision = ServerConfigAccess.revision();
            if (ServerConfigAccess.snapshot() != null) {
                serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
            }
            if (configPage == ConfigPage.SERVER_RULES && !ServerConfigAccess.canEdit()) {
                configPage = ConfigPage.RENDERING;
            }
            clearWidgets();
            init();
        }
        var config = ConfigManager.getInstance().getConfig();
        if (!ConfigManager.getInstance().isDraftDirty() && draftDiffersFrom(config)) {
            loadDraft();
            clearWidgets();
            init();
        }
        graphics.fill(0, 0, width, height, 0x30101010);
        NfiUiRenderer.panel(graphics, contentLeft, 8, contentWidth, height - 16, theme.background);
        NfiUiRenderer.centeredText(graphics, font, title, width / 2, 17, theme.text);
        if (configPage == ConfigPage.RENDERING) {
            NfiUiRenderer.divider(graphics, contentLeft + 10, 81, contentWidth - 20);
            NfiUiRenderer.text(graphics, font, Component.translatable("screen.neo_favorite_items.preview"), contentLeft + 12, 96, theme.accent);
            NfiUiRenderer.text(graphics, font, Component.translatable("screen.neo_favorite_items.assets", assetSummary), contentLeft + 12, 112, theme.text);
            renderPreview(graphics, previewX, previewY);
        } else {
            NfiUiRenderer.divider(graphics, contentLeft + 10, 57, contentWidth - 20);
        }
        if (ConfigManager.getInstance().isExternalChangeDetected()) {
            NfiUiRenderer.text(graphics, font,
                Component.translatable("screen.neo_favorite_items.external_change"),
                contentLeft + 12, height - 44, 0xFFFFC857);
        }
        NfiUiRenderer.divider(graphics, contentLeft + 10, configTop - 5, contentWidth - 20);
        configPanel.renderLabels(graphics, font, theme.text);
        NfiUiRenderer.divider(graphics, contentLeft + 10, configTop + configHeight + 3, contentWidth - 20);
        boolean dropdownOpen = materialControl != null && materialControl.isOpen()
            || feedbackSoundControl != null && feedbackSoundControl.isOpen();
        super.render(graphics, dropdownOpen ? Integer.MIN_VALUE : mouseX,
            dropdownOpen ? Integer.MIN_VALUE : mouseY, delta);
        if (materialControl != null) materialControl.renderPopup(graphics, mouseX, mouseY, delta);
        if (feedbackSoundControl != null) feedbackSoundControl.renderPopup(graphics, mouseX, mouseY, delta, height);
        if (colorPicker != null) {
            colorPicker.layoutPopup(height);
            colorPicker.renderPopup(graphics, mouseX, mouseY, delta, theme.text);
        }
    }

    private NfiSearchDropdown.Page<String> soundIdPage(String query, int offset, int limit) {
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return new NfiSearchDropdown.Page<>(BuiltInRegistries.SOUND_EVENT.keySet().stream()
            .map(ResourceLocation::toString)
            .filter(id -> needle.isEmpty() || id.toLowerCase(java.util.Locale.ROOT).contains(needle))
            .sorted()
            .skip(offset)
            .limit(limit)
            .toList());
    }

    private Component soundSubtitle(String soundId) {
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) return null;
        var event = minecraft.getSoundManager().getSoundEvent(id);
        return event == null ? null : event.getSubtitle();
    }

    private void previewSound(String soundId) {
        long now = net.minecraft.Util.getMillis();
        if (!soundPreviewReady(lastSoundPreviewAt, now)) return;
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) return;
        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound -> {
            stopSoundPreview();
            lastSoundPreviewAt = now;
            lastSoundPreview = SimpleSoundInstance.forUI(
                sound,
                Math.clamp(feedbackPitchDraft, 0.0f, 2.0f),
                Math.clamp(feedbackVolumeDraft, 0.0f, 1.0f)
            );
            minecraft.getSoundManager().play(lastSoundPreview);
        });
    }

    private void stopSoundPreview() {
        if (lastSoundPreview == null) return;
        minecraft.getSoundManager().stop(lastSoundPreview);
        lastSoundPreview = null;
    }

    static boolean soundPreviewReady(long previousAt, long now) {
        return now - previousAt >= SOUND_PREVIEW_COOLDOWN_MS;
    }

    private boolean draftDiffersFrom(NeoFavoriteItemsConfig config) {
        var overlay = config.overlay;
        return !lockedProfile.sameValues(overlay.locked)
            || !bypassProfile.sameValues(overlay.bypass)
            || !lockableProfile.sameValues(overlay.lockable)
            || !unlockableProfile.sameValues(overlay.unlockable)
            || showVisualFeedbackDraft != config.feedback.showVisualFeedback
            || playSoundFeedbackDraft != config.feedback.playSoundFeedback
            || !java.util.Objects.equals(feedbackSoundDraft, config.feedback.feedbackSound)
            || Float.compare(feedbackVolumeDraft, config.feedback.feedbackVolume) != 0
            || Float.compare(feedbackPitchDraft, config.feedback.feedbackPitch) != 0
            || !java.util.Objects.equals(theme.name(), config.feedback.uiTheme);
    }

    private void renderPreview(GuiGraphics graphics, int x, int y) {
        PreviewMode activeMode = Screen.hasControlDown()
            ? PreviewMode.CTRL
            : Screen.hasAltDown() ? PreviewMode.ALT : previewMode;
        OverlayProfileConfig profileConfig = switch (activeMode) {
            case CTRL -> bypassProfile;
            case ALT -> lockableProfile;
            case UNLOCKABLE -> unlockableProfile;
            case NORMAL -> lockedProfile;
        };
        OverlayMode mode = switch (activeMode) {
            case CTRL -> OverlayMode.BYPASS_LOCKED;
            case ALT -> OverlayMode.LOCKABLE;
            case UNLOCKABLE -> OverlayMode.UNLOCKABLE;
            case NORMAL -> OverlayMode.LOCKED;
        };
        NfiUiRenderer.panel(graphics, x - 6, y - 6, 102, 66);
        graphics.fill(x - 5, y - 5, x + 95, y + 59, theme.well);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                int slotX = x + column * 18;
                int slotY = y + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
                graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737);
                if (row == 1 && column == 2) {
                    graphics.renderItem(Items.DIAMOND.getDefaultInstance(), slotX + 1, slotY + 1);
                    drawEngine.beginFrame();
                    drawEngine.submit(
                        SlotRenderTarget.standard(LogicalSlotIndex.of(0), true, slotX + 1, slotY + 1),
                        OverlayProfile.fromConfig(mode, profileConfig, lockedProfile.opacity)
                    );
                    drawEngine.render(graphics);
                }
            }
        }
    }

    private enum ConfigPage { RENDERING, CLIENT_LOGIC, SERVER_RULES }
    private enum PreviewMode { NORMAL, CTRL, ALT, UNLOCKABLE }

    private static final class ServerRulesDraft {
        boolean autoUnlockEmptySlots;
        boolean lockEmptySlots;
        boolean allowItemsIntoLockedEmptySlots;
        boolean preventClick;
        boolean preventDrop;
        boolean preventQuickMove;
        boolean preventShiftClick;
        boolean preventDrag;
        boolean preventSwap;
        boolean allowBypassWithKey;
        NeoFavoriteItemsConfig.SlotMoveBehavior moveBehavior;
        boolean preserveLockedSlotContents;
        boolean debugEnabled;

        static ServerRulesDraft from(ServerRulesSnapshot source) {
            ServerRulesDraft draft = new ServerRulesDraft();
            draft.autoUnlockEmptySlots = source.autoUnlockEmptySlots();
            draft.lockEmptySlots = source.lockEmptySlots();
            draft.allowItemsIntoLockedEmptySlots = source.allowItemsIntoLockedEmptySlots();
            draft.preventClick = source.preventClick();
            draft.preventDrop = source.preventDrop();
            draft.preventQuickMove = source.preventQuickMove();
            draft.preventShiftClick = source.preventShiftClick();
            draft.preventDrag = source.preventDrag();
            draft.preventSwap = source.preventSwap();
            draft.allowBypassWithKey = source.allowBypassWithKey();
            draft.moveBehavior = source.moveBehavior();
            draft.preserveLockedSlotContents = source.preserveLockedSlotContents();
            draft.debugEnabled = source.debugEnabled();
            return draft;
        }

        ServerRulesSnapshot toSnapshot() {
            return new ServerRulesSnapshot(
                autoUnlockEmptySlots, lockEmptySlots, allowItemsIntoLockedEmptySlots,
                preventClick, preventDrop, preventQuickMove, preventShiftClick,
                preventDrag, preventSwap, allowBypassWithKey, moveBehavior,
                preserveLockedSlotContents, debugEnabled
            );
        }
    }
}
