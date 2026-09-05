package mycraft.yuyears.neofavoriteitems.client;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import mycraft.yuyears.neofavoriteitems.ConfigManager;
import mycraft.yuyears.neofavoriteitems.DebugLogger;
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
import mycraft.yuyears.neofavoriteitems.client.ui.control.NfiEditBox;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiConfigPanel;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiConfigSection;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiConfigRow;
import mycraft.yuyears.neofavoriteitems.client.ui.layout.NfiWidgetGroup;
import mycraft.yuyears.neofavoriteitems.domain.LogicalSlotIndex;
import mycraft.yuyears.neofavoriteitems.render.OverlayMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayProfile;
import mycraft.yuyears.neofavoriteitems.render.OverlayLayer;
import mycraft.yuyears.neofavoriteitems.render.OverlayColorMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayPlacement;
import mycraft.yuyears.neofavoriteitems.render.OverlayMaterialMode;
import mycraft.yuyears.neofavoriteitems.render.OverlayTextureCatalog;
import mycraft.yuyears.neofavoriteitems.render.OverlayLayerList;
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
    private int layerIndex;
    private long lastLayerActionNanos;
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
    private NfiButton keepDraftButton;
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
    private long lastDraftMutationNanos;
    private boolean autoSavedDraft;
    private ClientDraftSnapshot openedSnapshot;
    private boolean dropdownCapturedMouse;
    private NfiConfigPanel configPanel;
    private int contentLeft;
    private int contentWidth;
    private int configTop;
    private int configHeight;
    private int previewX;
    private int previewY;
    private final Map<String, Boolean> sectionExpansion = new HashMap<>();
    private final Map<PreviewMode, List<OverlayProfileConfig>> layerDrafts = new java.util.EnumMap<>(PreviewMode.class);
    /** Runtime controls grouped by owning Layer; prevents one Layer build from overwriting another's sync state. */
    private final Map<OverlayProfileConfig, LayerControlSet> layerControls = new java.util.IdentityHashMap<>();
    private boolean rebuildRequested;
    private int uiBuildGeneration;

    private record LayerControlSet(
        NfiCycleButton<OverlayMaterialMode> style,
        NfiDropdown<String> material,
        NfiCycleButton<OverlayColorMode> colorMode,
        NfiCycleButton<OverlayProfileConfig.OpacityBehavior> opacityBehavior,
        NfiCycleButton<OverlayPlacement.Anchor> anchor,
        NfiSlider opacity,
        NfiColorPicker colorPicker,
        NfiNumberField<Float> offsetX, NfiNumberField<Float> offsetY,
        NfiNumberField<Float> width, NfiNumberField<Float> height,
        NfiNumberField<Float> scale, NfiNumberField<Float> rotation,
        NfiNumberField<Float> zIndex, NfiToggle overflow, NfiToggle clip) {}

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
        if (ConfigManager.getInstance().isDraftDirty()
            && !ConfigManager.getInstance().isExternalChangeDetected()
            && lastDraftMutationNanos != 0L
            && System.nanoTime() - lastDraftMutationNanos >= 400_000_000L) {
            applyClientDraftToConfig();
            ConfigManager.getInstance().saveClientConfig();
            ConfigManager.getInstance().setDraftDirty(false);
            autoSavedDraft = true;
            lastDraftMutationNanos = 0L;
        }
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
        int generation = ++uiBuildGeneration;
        DebugLogger.debug("UI-DIAG init begin gen={} screen={} oldPanel={}", generation,
            System.identityHashCode(this), configPanel == null ? 0 : System.identityHashCode(configPanel));
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
        configPanel = new NfiConfigPanel(innerLeft, configTop, innerWidth, labelWidth, sectionExpansion);
        configPanel.setViewportHeight(configHeight);

        if (configPage == ConfigPage.RENDERING) {
            buildLayerControls();
        } else if (configPage == ConfigPage.CLIENT_LOGIC) {
            buildClientLogicControls();
        } else {
            buildServerRuleControls();
        }
        configPanel.finalizeLayout();
        syncMaterialPopupBounds();
        configPanel.sectionHeaders().forEach(this::addRenderableWidget);
        DebugLogger.debug("UI-DIAG init widgets gen={} panel={} headers={} page={} profile={}", generation,
            System.identityHashCode(configPanel), configPanel.sectionHeaders().size(), configPage, profileTab);

        int buttonLeft = contentLeft + (contentWidth - 284) / 2;
        addRenderableWidget(new NfiButton(buttonLeft, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.reset"), ignored -> resetActivePage()));
        addRenderableWidget(new NfiButton(buttonLeft + 72, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.reload"), ignored -> reloadDraft()));
        addRenderableWidget(new NfiButton(buttonLeft + 144, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.apply"), ignored -> applyAndClose()));
        addRenderableWidget(new NfiButton(buttonLeft + 216, height - 31, 68, 20,
            Component.translatable("screen.neo_favorite_items.cancel"), ignored -> onClose()));
        keepDraftButton = new NfiButton(contentLeft + contentWidth - 90, height - 50, 80, 16,
            Component.translatable("screen.neo_favorite_items.keep_draft"), ignored -> keepDraft());
        keepDraftButton.visible = false;
        addRenderableWidget(keepDraftButton);
        if (configPage == ConfigPage.RENDERING) refreshAssets();
        syncControls();
    }

    private void buildRenderingControls(OverlayProfileConfig targetProfile) {

        var materialsSection = configPanel.beginSection(Component.translatable("screen.neo_favorite_items.group.material"));

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
        NfiConfigRow assetsRow = new NfiConfigRow(Component.translatable("screen.neo_favorite_items.custom_assets_label"), assetsGroup,
            List.of(assetsButton, assetsFolderButton));
        assetsRow.setWidgetGap(0);
        configPanel.add(assetsRow);
        addRenderableWidget(assetsButton);
        addRenderableWidget(assetsFolderButton);

        styleControl = new NfiCycleButton<>(0, 0, 1, 20, materialModeBinding(targetProfile), MATERIAL_MODES,
            value -> Component.translatable("screen.neo_favorite_items.material_mode." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.material_mode_label"), styleControl.widget());

        materialControl = new NfiDropdown<>(0, 0, configPanel.controlWidth(), 4, materialBinding(targetProfile), materialValues(), value -> Component.literal(materialLabel(value)));
        addRenderableWidget(materialControl.button());
        configPanel.add(new NfiConfigRow(Component.translatable("screen.neo_favorite_items.material_label"),
            materialControl.group(), List.of(materialControl.button())));
        configPanel.endSection();

        configPanel.beginSection(Component.translatable("screen.neo_favorite_items.group.color"));

        colorModeControl = new NfiCycleButton<>(0, 0, 1, 20, colorModeBinding(targetProfile), COLOR_MODES,
            value -> Component.translatable("screen.neo_favorite_items.color_mode." + value.name().toLowerCase(java.util.Locale.ROOT)));
        colorModeControl.setTooltipFactory(value -> Component.translatable(
            "screen.neo_favorite_items.color_mode.tooltip." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.color_mode_label"), colorModeControl.widget());

        colorPicker = new NfiColorPicker(font, 0, 0, 1, colorBinding(targetProfile));
        colorPicker.setWidth(configPanel.controlWidth());
        configPanel.add(new NfiConfigRow(Component.translatable("screen.neo_favorite_items.color_label"),
            colorPicker.group(), List.of(colorPicker.button())));
        addRenderableWidget(colorPicker.button());
        colorPicker.allPopupWidgets().forEach(this::addRenderableWidget);

        if (profileTab == PreviewMode.CTRL) {
            opacityBehaviorControl = new NfiCycleButton<>(0, 0, 1, 20, opacityBehaviorBinding(targetProfile), OPACITY_BEHAVIORS,
                value -> Component.translatable("screen.neo_favorite_items.opacity_behavior." + value.name().toLowerCase(java.util.Locale.ROOT)));
            addRow(configPanel, Component.translatable("screen.neo_favorite_items.opacity_behavior_label"), opacityBehaviorControl.widget());
        } else {
            opacityBehaviorControl = null;
        }

        opacitySlider = new NfiSlider(0, 0, 1, 20, 0.0D, 1.0D, opacityBinding(targetProfile),
            value -> Component.translatable("screen.neo_favorite_items.opacity", Math.round(value * 100.0D)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.opacity_label"), opacitySlider);

        configPanel.endSection();

        configPanel.beginSection(Component.translatable("screen.neo_favorite_items.group.geometry"));

        anchorControl = new NfiCycleButton<>(0, 0, 1, 20, anchorBinding(targetProfile), ANCHORS,
            value -> Component.translatable("screen.neo_favorite_items.anchor." + value.name().toLowerCase(java.util.Locale.ROOT)));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.anchor_label"), anchorControl.widget());

        offsetXControl = numberField(offsetXBinding(targetProfile));
        offsetYControl = numberField(offsetYBinding(targetProfile));
        widthControl = numberField(widthBinding(targetProfile));
        heightControl = numberField(heightBinding(targetProfile));
        scaleControl = numberField(scaleBinding(targetProfile));
        rotationControl = numberField(rotationBinding(targetProfile));
        zIndexControl = numberField(zIndexBinding(targetProfile));
        overflowControl = new NfiToggle(0, 0, 1, 20, overflowBinding(targetProfile), value -> Component.translatable(value ? "options.on" : "options.off"));
        clipControl = new NfiToggle(0, 0, 1, 20, clipBinding(targetProfile), value -> Component.translatable(value ? "options.on" : "options.off"));
        int pairWidth = Math.max(1, (configPanel.controlWidth() - 8) / 2);
        offsetXControl.widget().setWidth(pairWidth);
        offsetYControl.widget().setWidth(pairWidth);
        widthControl.widget().setWidth(pairWidth);
        heightControl.widget().setWidth(pairWidth);
        NfiWidgetGroup offsetGroup = new NfiWidgetGroup(0, 0);
        offsetGroup.addRelative(offsetXControl.widget(), 0, 0);
        offsetGroup.addRelative(offsetYControl.widget(), pairWidth + 8, 0);
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.offset"), offsetGroup,
            List.of(offsetXControl.widget(), offsetYControl.widget()));
        NfiWidgetGroup sizeGroup = new NfiWidgetGroup(0, 0);
        sizeGroup.addRelative(widthControl.widget(), 0, 0);
        sizeGroup.addRelative(heightControl.widget(), pairWidth + 8, 0);
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.render_size"), sizeGroup,
            List.of(widthControl.widget(), heightControl.widget()));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.scale"), scaleControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.rotation"), rotationControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.z_index"), zIndexControl.widget());
        configPanel.endSection();

        configPanel.beginSection(Component.translatable("screen.neo_favorite_items.group.bounds"));
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.overflow"), overflowControl.widget());
        addRow(configPanel, Component.translatable("screen.neo_favorite_items.clip"), clipControl.widget());
        configPanel.endSection();

        layerControls.put(targetProfile, new LayerControlSet(styleControl, materialControl, colorModeControl,
            opacityBehaviorControl, anchorControl, opacitySlider, colorPicker, offsetXControl, offsetYControl,
            widthControl, heightControl, scaleControl, rotationControl, zIndexControl, overflowControl, clipControl));

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
        layerControls.clear();
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
                rebuildRenderingPage();
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
            this::rebuildRenderingPage
        );
    }

    private NfiValueBinding<OverlayMaterialMode> materialModeBinding(OverlayProfileConfig profile) {
        return NfiValueBinding.unchecked(
            () -> profile.materialMode,
            value -> {
                profile.materialMode = value;
                if (value == OverlayMaterialMode.MATERIAL && (OverlayTextureCatalog.NO_MATERIAL.equals(profile.materialId)
                    || OverlayTextureCatalog.presetStyle(profile.materialId) == NeoFavoriteItemsConfig.OverlayStyle.COLOR_OVERLAY)) {
                    profile.materialId = OverlayTextureCatalog.presetId(NeoFavoriteItemsConfig.OverlayStyle.MARK);
                    profile.style = NeoFavoriteItemsConfig.OverlayStyle.MARK;
                }
            },
            () -> OverlayMaterialMode.MATERIAL,
            () -> {
                markDirty();
                syncMaterialControlAvailability();
            }
        );
    }

    private NfiValueBinding<String> materialBinding(OverlayProfileConfig profile) {
        return NfiValueBinding.unchecked(() -> profile.materialId, value -> {
            profile.materialId = value;
            var preset = OverlayTextureCatalog.presetStyle(value);
            if (preset != null) profile.style = preset;
        }, () -> profile.materialId, this::markDirty);
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

    private NfiValueBinding<OverlayProfileConfig.OpacityBehavior> opacityBehaviorBinding(OverlayProfileConfig profile) {
        return NfiValueBinding.unchecked(() -> profile.opacityBehavior, v -> profile.opacityBehavior = v,
            () -> OverlayProfileConfig.OpacityBehavior.FIXED, this::markDirty);
    }

    private NfiValueBinding<OverlayPlacement.Anchor> anchorBinding(OverlayProfileConfig profile) {
        return NfiValueBinding.unchecked(() -> profile.anchor, v -> profile.anchor = v,
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

    private NfiValueBinding<Float> offsetXBinding(OverlayProfileConfig p) { return floatBinding(() -> p.offsetX, v -> p.offsetX = v, 0.0f, -1024.0f); }
    private NfiValueBinding<Float> offsetYBinding(OverlayProfileConfig p) { return floatBinding(() -> p.offsetY, v -> p.offsetY = v, 0.0f, -1024.0f); }
    private NfiValueBinding<Float> widthBinding(OverlayProfileConfig p) { return floatBinding(() -> p.width, v -> p.width = v, 16.0f, 0.01f); }
    private NfiValueBinding<Float> heightBinding(OverlayProfileConfig p) { return floatBinding(() -> p.height, v -> p.height = v, 16.0f, 0.01f); }
    private NfiValueBinding<Float> scaleBinding(OverlayProfileConfig p) { return floatBinding(() -> p.scale, v -> p.scale = v, 1.0f, 0.01f); }
    private NfiValueBinding<Float> rotationBinding(OverlayProfileConfig p) { return floatBinding(() -> p.rotationDegrees, v -> p.rotationDegrees = v, 0.0f, -360.0f); }
    private NfiValueBinding<Float> zIndexBinding(OverlayProfileConfig p) { return floatBinding(() -> (float) p.zIndex, v -> p.zIndex = normalizeZ(v), 2.0f, 0.0f); }
    private int normalizeZ(float value) {
        int normalized = (int) Math.clamp(value, 0.0f, 1000.0f);
        return normalized == 1 ? 2 : normalized;
    }
    private NfiValueBinding<Boolean> overflowBinding(OverlayProfileConfig p) { return NfiValueBinding.unchecked(() -> p.allowOverflow, v -> p.allowOverflow = v, () -> false, this::markDirty); }
    private NfiValueBinding<Boolean> clipBinding(OverlayProfileConfig p) { return NfiValueBinding.unchecked(() -> p.clipToSlot, v -> p.clipToSlot = v, () -> false, this::markDirty); }

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
            activeLayers().set(Math.clamp(layerIndex, 0, activeLayers().size() - 1), defaultProfile);
            syncDraftLegacyProfiles();
            syncControls();
        } else if (configPage == ConfigPage.CLIENT_LOGIC) {
            var defaults = new NeoFavoriteItemsConfig().feedback;
            showVisualFeedbackDraft = defaults.showVisualFeedback;
            playSoundFeedbackDraft = defaults.playSoundFeedback;
            feedbackSoundDraft = defaults.feedbackSound;
            feedbackVolumeDraft = defaults.feedbackVolume;
            feedbackPitchDraft = defaults.feedbackPitch;
            rebuildRenderingPage();
        } else if (ServerConfigAccess.canEdit()) {
            serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
            rebuildRenderingPage();
        }
        markDirty();
    }

    private NfiValueBinding<NfiUiTheme> themeBinding() {
        return NfiValueBinding.unchecked(() -> theme, value -> theme = value, () -> NfiUiTheme.DEFAULT, this::markDirty);
    }

    private NfiValueBinding<OverlayColorMode> colorModeBinding(OverlayProfileConfig p) {
        return NfiValueBinding.unchecked(
            () -> p.colorMode,
            value -> p.colorMode = value,
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

    private NfiValueBinding<Double> opacityBinding(OverlayProfileConfig p) {
        return NfiValueBinding.unchecked(
            () -> (double) p.opacity,
            value -> p.opacity = (float) Math.clamp(value, 0.0D, 1.0D),
            () -> 0.7D,
            this::markDirty
        );
    }

    private NfiValueBinding<Integer> colorBinding(OverlayProfileConfig p) {
        return NfiValueBinding.unchecked(
            () -> p.color,
            value -> p.color = value,
            () -> new NeoFavoriteItemsConfig().overlay.locked.color,
            this::markDirty
        );
    }

    private void markDirty() {
        if (configPage != ConfigPage.SERVER_RULES) {
            applyClientDraftToConfig();
            ConfigManager.getInstance().markRuntimeConfigChanged();
        }
        ConfigManager.getInstance().setDraftDirty(true);
        lastDraftMutationNanos = System.nanoTime();
    }

    private void buildLayerControls() {
        List<OverlayProfileConfig> layers = activeLayers();
        // Stable outer container keeps Layer nodes and add action in one tree branch.
        NfiConfigSection layerList = configPanel.beginSection(
            Component.translatable("screen.neo_favorite_items.layer_label"),
            "layer-list." + profileTab.name().toLowerCase(java.util.Locale.ROOT));
        layerList.setExpandedSilently(true);
        // Each layer is a child section; selected layer owns the editable groups.
        for (int i = 0; i < layers.size(); i++) {
            final int target = i;
            String key = "layer." + profileTab.name().toLowerCase(java.util.Locale.ROOT) + "." + i;
            NfiConfigSection layerSection = configPanel.beginSection(
                Component.translatable("screen.neo_favorite_items.layer.name", i + 1), key);
            if (i > 0) {
                NfiButton remove = new NfiButton(0, 0, 20, 20, Component.literal("x"), ignored -> removeLayer(target));
                remove.setPlain(true);
                layerSection.setTrailingWidget(remove);
            }
            layerSection.setToggleListener(() -> {
                if (layerIndex != target) {
                    closeTransientPopups();
                    layerIndex = target;
                    // All layers already own real controls. Switching target must
                    // not rebuild the tree: rebuilding resets widget visibility
                    // while another layer may be expanded off-screen.
                    syncControls();
                }
            });
            // Build real controls for every layer. Each binding captures this
            // layer's config object, so scrolling or switching layers cannot
            // invalidate another layer's expanded subtree.
            buildRenderingControls(layers.get(i));
            configPanel.endSection();
        }
        if (layers.size() < OverlayLayerList.MAX_LAYERS) {
            NfiButton add = new NfiButton(0, 0, configPanel.controlWidth(), 20,
                Component.translatable("screen.neo_favorite_items.add_layer"), ignored -> addLayer());
            addRenderableWidget(add);
            configPanel.addLeftWidget(add);
        }
        configPanel.endSection();
    }

    private List<OverlayProfileConfig> activeLayers() {
        return layerDrafts.computeIfAbsent(profileTab, ignored -> new java.util.ArrayList<>(switch (profileTab) {
            case NORMAL -> OverlayLayerList.normalize(ConfigManager.getInstance().getConfig().overlay.lockedLayers, OverlayProfileConfig::defaultLocked);
            case CTRL -> OverlayLayerList.normalize(ConfigManager.getInstance().getConfig().overlay.bypassLayers, OverlayProfileConfig::defaultBypass);
            case ALT -> OverlayLayerList.normalize(ConfigManager.getInstance().getConfig().overlay.lockableLayers, OverlayProfileConfig::defaultLockable);
            case UNLOCKABLE -> OverlayLayerList.normalize(ConfigManager.getInstance().getConfig().overlay.unlockableLayers, OverlayProfileConfig::defaultUnlockable);
        }));
    }

    private void selectLayer(int index) {
        layerIndex = Math.clamp(index, 0, activeLayers().size() - 1);
        rebuildRenderingPage();
    }

    private void addLayer() {
        if (!layerActionReady()) return;
        List<OverlayProfileConfig> layers = activeLayers();
        if (layers.size() >= OverlayLayerList.MAX_LAYERS) return;
        layers.add(defaultLayerFor(profileTab));
        markDirty();
        lastLayerActionNanos = System.nanoTime();
        rebuildRenderingPage();
    }

    private void removeLayer(int index) {
        if (!layerActionReady()) return;
        List<OverlayProfileConfig> layers = activeLayers();
        if (index <= 0 || index >= layers.size()) return;
        layers.remove(index);
        closeTransientPopups();
        String prefix = "layer." + profileTab.name().toLowerCase(java.util.Locale.ROOT) + "." + index;
        sectionExpansion.keySet().removeIf(key -> key.equals(prefix) || key.startsWith(prefix + "."));
        layerIndex = Math.min(layerIndex, layers.size() - 1);
        markDirty();
        lastLayerActionNanos = System.nanoTime();
        rebuildRenderingPage();
    }

    private boolean layerActionReady() {
        return System.nanoTime() - lastLayerActionNanos >= 200_000_000L;
    }

    private void closeTransientPopups() {
        layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).forEach(NfiDropdown::close);
        if (feedbackSoundControl != null) feedbackSoundControl.close();
        if (colorPicker != null) colorPicker.close();
        setFocused(null);
        dropdownCapturedMouse = false;
    }

    private static OverlayProfileConfig defaultLayerFor(PreviewMode mode) {
        return switch (mode) {
            case NORMAL -> OverlayProfileConfig.defaultLocked();
            case CTRL -> OverlayProfileConfig.defaultBypass();
            case ALT -> OverlayProfileConfig.defaultLockable();
            case UNLOCKABLE -> OverlayProfileConfig.defaultUnlockable();
        };
    }

    private void rebuildRenderingPage() {
        rebuildRequested = true;
    }

    private void rebuildWidgetsNow() {
        DebugLogger.debug("UI-DIAG rebuild begin screen={} panel={}", System.identityHashCode(this),
            configPanel == null ? 0 : System.identityHashCode(configPanel));
        rebuildRequested = false;
        if (configPanel != null) configPanel.hideAllSectionWidgets();
        clearWidgets();
        init();
    }

    private void loadDraft() {
        // Every screen opening starts collapsed; expansion state is session-local.
        sectionExpansion.clear();
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
        layerDrafts.clear();
        layerDrafts.put(PreviewMode.NORMAL, new java.util.ArrayList<>(overlay.layers(OverlayMode.LOCKED)));
        layerDrafts.put(PreviewMode.CTRL, new java.util.ArrayList<>(overlay.layers(OverlayMode.BYPASS_LOCKED)));
        layerDrafts.put(PreviewMode.ALT, new java.util.ArrayList<>(overlay.layers(OverlayMode.LOCKABLE)));
        layerDrafts.put(PreviewMode.UNLOCKABLE, new java.util.ArrayList<>(overlay.layers(OverlayMode.UNLOCKABLE)));
        showVisualFeedbackDraft = config.feedback.showVisualFeedback;
        playSoundFeedbackDraft = config.feedback.playSoundFeedback;
        feedbackSoundDraft = config.feedback.feedbackSound;
        feedbackVolumeDraft = config.feedback.feedbackVolume;
        feedbackPitchDraft = config.feedback.feedbackPitch;
        if (ServerConfigAccess.snapshot() != null) {
            serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
        }
        ConfigManager.getInstance().setDraftDirty(false);
        if (openedSnapshot == null) openedSnapshot = ClientDraftSnapshot.capture(config);
    }

    private void reloadDraft() {
        ConfigManager.getInstance().reload();
        ServerConfigAccess.request();
        openedSnapshot = null;
        autoSavedDraft = false;
        loadDraft();
        rebuildRenderingPage();
    }

    private void applyAndClose() {
        applyClientDraftToConfig();
        ConfigManager.getInstance().saveClientConfig();
        if (serverRulesDraft != null && ServerConfigAccess.canEdit()) {
            ServerConfigAccess.submit(serverRulesDraft.toSnapshot());
        }
        autoSavedDraft = false;
        ConfigManager.getInstance().setDraftDirty(false);
        onClose();
    }

    private void keepDraft() {
        ConfigManager.getInstance().keepDraftAfterExternalChange();
        lastDraftMutationNanos = System.nanoTime() - 400_000_000L;
    }

    private void applyClientDraftToConfig() {
        var config = ConfigManager.getInstance().getConfig();
        var overlay = config.overlay;
        syncDraftLegacyProfiles();
        overlay.locked.copyFrom(lockedProfile);
        overlay.bypass.copyFrom(bypassProfile);
        overlay.lockable.copyFrom(lockableProfile);
        overlay.unlockable.copyFrom(unlockableProfile);
        overlay.lockedLayers = List.copyOf(layerDrafts.getOrDefault(PreviewMode.NORMAL, List.of(lockedProfile.copy())));
        overlay.bypassLayers = List.copyOf(layerDrafts.getOrDefault(PreviewMode.CTRL, List.of(bypassProfile.copy())));
        overlay.lockableLayers = List.copyOf(layerDrafts.getOrDefault(PreviewMode.ALT, List.of(lockableProfile.copy())));
        overlay.unlockableLayers = List.copyOf(layerDrafts.getOrDefault(PreviewMode.UNLOCKABLE, List.of(unlockableProfile.copy())));
        config.feedback.showVisualFeedback = showVisualFeedbackDraft;
        config.feedback.playSoundFeedback = playSoundFeedbackDraft;
        config.feedback.feedbackSound = feedbackSoundDraft;
        config.feedback.feedbackVolume = Math.clamp(feedbackVolumeDraft, 0.0f, 1.0f);
        config.feedback.feedbackPitch = Math.clamp(feedbackPitchDraft, 0.0f, 2.0f);
        config.feedback.uiTheme = theme.name();
    }

    private void syncDraftLegacyProfiles() {
        if (layerDrafts.get(PreviewMode.NORMAL) != null && !layerDrafts.get(PreviewMode.NORMAL).isEmpty()) lockedProfile.copyFrom(layerDrafts.get(PreviewMode.NORMAL).getFirst());
        if (layerDrafts.get(PreviewMode.CTRL) != null && !layerDrafts.get(PreviewMode.CTRL).isEmpty()) bypassProfile.copyFrom(layerDrafts.get(PreviewMode.CTRL).getFirst());
        if (layerDrafts.get(PreviewMode.ALT) != null && !layerDrafts.get(PreviewMode.ALT).isEmpty()) lockableProfile.copyFrom(layerDrafts.get(PreviewMode.ALT).getFirst());
        if (layerDrafts.get(PreviewMode.UNLOCKABLE) != null && !layerDrafts.get(PreviewMode.UNLOCKABLE).isEmpty()) unlockableProfile.copyFrom(layerDrafts.get(PreviewMode.UNLOCKABLE).getFirst());
    }

    @Override
    public void onClose() {
        stopSoundPreview();
        if (autoSavedDraft && openedSnapshot != null) {
            openedSnapshot.restore(ConfigManager.getInstance().getConfig());
            ConfigManager.getInstance().saveClientConfig();
        }
        ConfigManager.getInstance().setDraftDirty(false);
        minecraft.setScreen(previousScreen);
    }

    private static final class ClientDraftSnapshot {
        private final OverlayProfileConfig locked, bypass, lockable, unlockable;
        private final List<OverlayProfileConfig> lockedLayers, bypassLayers, lockableLayers, unlockableLayers;
        private final boolean visual, sound;
        private final String soundId, theme;
        private final float volume, pitch;

        private ClientDraftSnapshot(NeoFavoriteItemsConfig config) {
            locked = config.overlay.locked.copy();
            bypass = config.overlay.bypass.copy();
            lockable = config.overlay.lockable.copy();
            unlockable = config.overlay.unlockable.copy();
            lockedLayers = new java.util.ArrayList<>(config.overlay.lockedLayers).stream().map(OverlayProfileConfig::copy).toList();
            bypassLayers = new java.util.ArrayList<>(config.overlay.bypassLayers).stream().map(OverlayProfileConfig::copy).toList();
            lockableLayers = new java.util.ArrayList<>(config.overlay.lockableLayers).stream().map(OverlayProfileConfig::copy).toList();
            unlockableLayers = new java.util.ArrayList<>(config.overlay.unlockableLayers).stream().map(OverlayProfileConfig::copy).toList();
            visual = config.feedback.showVisualFeedback;
            sound = config.feedback.playSoundFeedback;
            soundId = config.feedback.feedbackSound;
            volume = config.feedback.feedbackVolume;
            pitch = config.feedback.feedbackPitch;
            theme = config.feedback.uiTheme;
        }

        static ClientDraftSnapshot capture(NeoFavoriteItemsConfig config) { return new ClientDraftSnapshot(config); }

        void restore(NeoFavoriteItemsConfig config) {
            config.overlay.locked.copyFrom(locked);
            config.overlay.bypass.copyFrom(bypass);
            config.overlay.lockable.copyFrom(lockable);
            config.overlay.unlockable.copyFrom(unlockable);
            config.overlay.lockedLayers = lockedLayers;
            config.overlay.bypassLayers = bypassLayers;
            config.overlay.lockableLayers = lockableLayers;
            config.overlay.unlockableLayers = unlockableLayers;
            config.feedback.showVisualFeedback = visual;
            config.feedback.playSoundFeedback = sound;
            config.feedback.feedbackSound = soundId;
            config.feedback.feedbackVolume = volume;
            config.feedback.feedbackPitch = pitch;
            config.feedback.uiTheme = theme;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.isOpen()) {
            boolean handled = colorPicker.mouseClicked(mouseX, mouseY, button);
            setFocused(colorPicker.activePopupWidget());
            if (!handled) setFocused(null);
            return handled;
        }
        if (feedbackSoundControl != null && feedbackSoundControl.isOpen()) {
            dropdownCapturedMouse = true;
            handleFeedbackSoundClick(mouseX, mouseY, button);
            return true;
        }
        NfiDropdown<String> openMaterial = layerControls.values().stream()
            .map(LayerControlSet::material).filter(java.util.Objects::nonNull)
            .filter(NfiDropdown::isOpen).findFirst().orElse(null);
        if (openMaterial != null) {
            dropdownCapturedMouse = true;
            openMaterial.mouseClicked(mouseX, mouseY, button);
            setFocused(openMaterial.button());
            return true;
        }
        for (LayerControlSet controls : layerControls.values()) {
            NfiDropdown<String> material = controls.material();
            if (material != null && material.button().visible && material.button().active
                && material.button().isMouseOver(mouseX, mouseY)) {
                // Handle trigger explicitly; relying on Screen widget dispatch can
                // miss composite dropdowns after nested reflow.
                material.mouseClicked(mouseX, mouseY, button);
                if (!material.isOpen()) material.open();
                material.layoutPopup(height);
            dropdownCapturedMouse = material.isOpen();
            if (feedbackSoundControl != null) feedbackSoundControl.close();
            setFocused(material.button());
            return true;
            }
        }
        if (feedbackSoundControl != null && feedbackSoundControl.trigger().visible && feedbackSoundControl.trigger().active
            && feedbackSoundControl.mouseClicked(mouseX, mouseY, button)) {
            dropdownCapturedMouse = feedbackSoundControl.isOpen();
            if (materialControl != null) materialControl.close();
            focusFeedbackSoundControl();
            return true;
        }
        if (configPanel != null && configPanel.mouseClicked(mouseX, mouseY, button)) {
            setFocused(null);
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled) setFocused(null);
        return handled;
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
        if (layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).anyMatch(control -> control.keyPressed(keyCode))) return true;
        if (feedbackSoundControl != null && feedbackSoundControl.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (feedbackSoundControl != null && feedbackSoundControl.isOpen()) {
            feedbackSoundControl.mouseScrolled(mouseX, mouseY, verticalDelta);
            return true;
        }
        if (layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).anyMatch(control -> {
                if (!control.isOpen()) return false;
                control.mouseScrolled(mouseX, mouseY, verticalDelta);
                return true;
            })) {
            return true;
        }
        if (layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).anyMatch(control -> control.mouseScrolled(mouseX, mouseY, verticalDelta))) return true;
        if (feedbackSoundControl != null && feedbackSoundControl.mouseScrolled(mouseX, mouseY, verticalDelta)) return true;
        if (configPanel != null && configPanel.mouseScrolled(mouseX, mouseY, verticalDelta)) {
            layerControls.values().stream().map(LayerControlSet::material)
                .filter(java.util.Objects::nonNull).forEach(control -> { control.close(); control.layoutPopup(height); });
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
        layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).forEach(control -> { control.setValues(materialValues()); control.layoutPopup(height); });
    }

    private void syncMaterialPopupBounds() {
        layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).forEach(NfiDropdown::syncPopupToButton);
    }

    private void syncControls() {
        if (configTabs != null) configTabs.syncFromBinding();
        if (profileTabs == null) return;
        profileTabs.syncFromBinding();
        LayerControlSet controls = layerControls.get(activeProfile());
        if (controls == null) return;
        controls.style().syncFromBinding();
        if (controls.material() != null) controls.material().syncFromBinding();
        syncMaterialControlAvailability();
        controls.colorMode().syncFromBinding();
        if (controls.opacityBehavior() != null) controls.opacityBehavior().syncFromBinding();
        if (controls.anchor() != null) controls.anchor().syncFromBinding();
        themeControl.syncFromBinding();
        previewModeControl.syncFromBinding();
        controls.opacity().syncFromBinding();
        controls.colorPicker().syncFromBinding();
        controls.offsetX().syncFromBinding(); controls.offsetY().syncFromBinding(); controls.width().syncFromBinding();
        controls.height().syncFromBinding(); controls.scale().syncFromBinding(); controls.rotation().syncFromBinding();
        controls.zIndex().syncFromBinding(); controls.overflow().syncFromBinding(); controls.clip().syncFromBinding();
    }

    private void syncMaterialControlAvailability() {
        LayerControlSet controls = layerControls.get(activeProfile());
        if (controls == null || controls.material() == null) return;
        boolean enabled = activeProfile().materialMode == OverlayMaterialMode.MATERIAL;
        controls.material().button().active = enabled;
        if (!enabled) controls.material().close();
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
        List<OverlayProfileConfig> layers = activeLayers();
        return layers.get(Math.clamp(layerIndex, 0, layers.size() - 1));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        NfiUiRenderer.setTheme(theme);
        renderBlurredBackground(delta);
        boolean stateRequiresRebuild = rebuildRequested;
        if (observedServerAccessRevision != ServerConfigAccess.revision()) {
            observedServerAccessRevision = ServerConfigAccess.revision();
            if (ServerConfigAccess.snapshot() != null) {
                serverRulesDraft = ServerRulesDraft.from(ServerConfigAccess.snapshot());
            }
            if (configPage == ConfigPage.SERVER_RULES && !ServerConfigAccess.canEdit()) {
                configPage = ConfigPage.RENDERING;
            }
            stateRequiresRebuild = true;
        }
        var config = ConfigManager.getInstance().getConfig();
        if (!ConfigManager.getInstance().isDraftDirty() && draftDiffersFrom(config)) {
            loadDraft();
            stateRequiresRebuild = true;
        }
        if (stateRequiresRebuild) rebuildWidgetsNow();
        syncMaterialPopupBounds();
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
        if (keepDraftButton != null) {
            keepDraftButton.visible = ConfigManager.getInstance().isExternalChangeDetected();
        }
        NfiUiRenderer.divider(graphics, contentLeft + 10, configTop - 5, contentWidth - 20);
        configPanel.renderLabels(graphics, font, theme.text);
        NfiUiRenderer.divider(graphics, contentLeft + 10, configTop + configHeight + 3, contentWidth - 20);
        boolean dropdownOpen = layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).anyMatch(NfiDropdown::isOpen)
            || feedbackSoundControl != null && feedbackSoundControl.isOpen();
        super.render(graphics, dropdownOpen ? Integer.MIN_VALUE : mouseX,
            dropdownOpen ? Integer.MIN_VALUE : mouseY, delta);
        layerControls.values().stream().map(LayerControlSet::material)
            .filter(java.util.Objects::nonNull).filter(NfiDropdown::isOpen)
            .forEach(control -> control.renderPopup(graphics, mouseX, mouseY, delta));
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
        return !activeLayersEqual(overlay)
            || showVisualFeedbackDraft != config.feedback.showVisualFeedback
            || playSoundFeedbackDraft != config.feedback.playSoundFeedback
            || !java.util.Objects.equals(feedbackSoundDraft, config.feedback.feedbackSound)
            || Float.compare(feedbackVolumeDraft, config.feedback.feedbackVolume) != 0
            || Float.compare(feedbackPitchDraft, config.feedback.feedbackPitch) != 0
            || !java.util.Objects.equals(theme.name(), config.feedback.uiTheme);
    }

    private boolean activeLayersEqual(NeoFavoriteItemsConfig.Overlay overlay) {
        return layersEqual(layerDrafts.get(PreviewMode.NORMAL), overlay.lockedLayers)
            && layersEqual(layerDrafts.get(PreviewMode.CTRL), overlay.bypassLayers)
            && layersEqual(layerDrafts.get(PreviewMode.ALT), overlay.lockableLayers)
            && layersEqual(layerDrafts.get(PreviewMode.UNLOCKABLE), overlay.unlockableLayers);
    }

    private static boolean layersEqual(List<OverlayProfileConfig> left, List<OverlayProfileConfig> right) {
        if (left == null || right == null || left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) if (!left.get(i).sameValues(right.get(i))) return false;
        return true;
    }

    private void renderPreview(GuiGraphics graphics, int x, int y) {
        PreviewMode activeMode = Screen.hasControlDown()
            ? PreviewMode.CTRL
            : Screen.hasAltDown() ? PreviewMode.ALT : previewMode;
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
                        draftRenderProfile(mode, activeMode)
                    );
                    drawEngine.render(graphics);
                }
            }
        }
    }

    private OverlayProfile draftRenderProfile(OverlayMode mode, PreviewMode preview) {
        List<OverlayLayer> compiled = new java.util.ArrayList<>();
        for (OverlayProfileConfig config : activeLayersFor(preview)) {
            compiled.addAll(OverlayProfile.fromConfig(mode, config, lockedProfile.opacity).layers());
        }
        compiled.sort(java.util.Comparator.comparingInt(OverlayLayer::zIndex));
        return new OverlayProfile(mode, compiled);
    }

    private List<OverlayProfileConfig> activeLayersFor(PreviewMode mode) {
        return layerDrafts.getOrDefault(mode, List.of());
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
