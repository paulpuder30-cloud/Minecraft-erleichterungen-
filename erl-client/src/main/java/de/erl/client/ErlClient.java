/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client;

import de.erl.client.addons.AddonManager;
import de.erl.client.addons.ErlAddon;
import de.erl.client.events.erl.KeyInputEvent;
import de.erl.client.events.erl.MouseClickEvent;
import de.erl.client.events.world.TickEvent;
import de.erl.client.gui.GuiThemes;
import de.erl.client.gui.WidgetScreen;
import de.erl.client.gui.tabs.Tabs;
import de.erl.client.systems.Systems;
import de.erl.client.systems.config.Config;
import de.erl.client.systems.hud.screens.AddHudElementScreen;
import de.erl.client.systems.hud.screens.HudEditorScreen;
import de.erl.client.systems.hud.screens.HudElementPresetsScreen;
import de.erl.client.systems.hud.screens.HudElementScreen;
import de.erl.client.systems.modules.Categories;
import de.erl.client.systems.modules.Modules;
import de.erl.client.utils.PostInit;
import de.erl.client.utils.PreInit;
import de.erl.client.utils.ReflectInit;
import de.erl.client.utils.Utils;
import de.erl.client.utils.misc.Version;
import de.erl.client.utils.misc.input.KeyAction;
import de.erl.client.utils.misc.input.KeyBinds;
import de.erl.client.utils.network.OnlinePlayers;
import de.erl.client.lib.orbit.EventBus;
import de.erl.client.lib.orbit.EventHandler;
import de.erl.client.lib.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class ErlClient implements ClientModInitializer {
    public static final String MOD_ID = "erl-client";
    /** Namespace, unter dem Addons ihre Metadaten (z. B. "erl-client:color") deklarieren. */
    public static final String ADDON_META_NAMESPACE = "erl-client";
    public static final ModMetadata MOD_META;
    public static final String NAME;
    public static final Version VERSION;
    public static final String BUILD_NUMBER;

    public static ErlClient INSTANCE;
    public static ErlAddon ADDON;

    public static Minecraft mc;
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final File FOLDER = FabricLoader.getInstance().getGameDir().resolve(MOD_ID).toFile();
    public static final Logger LOG;

    static {
        MOD_META = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();

        NAME = MOD_META.getName();
        LOG = LoggerFactory.getLogger(NAME);

        String versionString = MOD_META.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];
        // When building and running through IntelliJ and not Gradle it doesn't replace the version so just use a dummy
        if (versionString.equals("${version}")) versionString = "0.0.0";

        VERSION = new Version(versionString);
        BUILD_NUMBER = MOD_META.getCustomValue(ErlClient.MOD_ID + ":build_number").getAsString();
    }

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            INSTANCE = this;
            return;
        }

        // Global minecraft client accessor
        mc = Minecraft.getInstance();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOG.info("Force loading mixins");
            MixinEnvironment.getCurrentEnvironment().audit();
        }

        LOG.info("Initializing {}", NAME);

        // Pre-load
        if (!FOLDER.exists()) {
            FOLDER.getParentFile().mkdirs();
            FOLDER.mkdir();
        }

        // Register addons
        AddonManager.init();

        // Register event handlers
        AddonManager.ADDONS.forEach(addon -> {
            try {
                EVENT_BUS.registerLambdaFactory(addon.getPackage(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
            } catch (AbstractMethodError e) {
                throw new RuntimeException("Addon \"%s\" is too old and cannot be ran.".formatted(addon.name), e);
            }
        });

        // Register init classes
        ReflectInit.registerPackages();

        // Pre init
        ReflectInit.init(PreInit.class);

        // Register module categories
        Categories.init();

        // Load systems
        Systems.init();

        // Subscribe after systems are loaded
        EVENT_BUS.subscribe(this);

        // Initialise addons
        AddonManager.ADDONS.forEach(ErlAddon::onInitialize);

        // Sort modules after addons have added their own
        Modules.get().sortModules();

        // Load configs
        Systems.load();

        // Post init
        ReflectInit.init(PostInit.class);

        // Save on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            OnlinePlayers.leave();
            Systems.save();
            GuiThemes.save();
        }));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.gui.screen() == null && mc.gui.overlay() == null && KeyBinds.OPEN_COMMANDS.consumeClick()) {
            mc.gui.setScreen(new ChatScreen(Config.get().prefix.get(), true));
        }
    }

    @EventHandler
    private void onKey(KeyInputEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matches(event.input)) {
            toggleGui();
        }
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesMouse(event.click)) {
            toggleGui();
        }
    }

    private void toggleGui() {
        if (Utils.canCloseGui()) mc.gui.screen().onClose();
        else if (Utils.canOpenGui()) Tabs.get().getFirst().openScreen(GuiThemes.get());
    }

    // Hide HUD

    public static boolean shouldHideHud() {
        if (!GuiThemes.get().hideHUD()) return false;

        Screen screen = mc.gui.screen();
        if (!(screen instanceof WidgetScreen)) return false;

        return !(screen instanceof HudEditorScreen)
            && !(screen instanceof AddHudElementScreen)
            && !(screen instanceof HudElementScreen)
            && !(screen instanceof HudElementPresetsScreen);
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(ErlClient.MOD_ID, path);
    }
}
