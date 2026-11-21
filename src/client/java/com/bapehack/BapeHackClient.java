package com.bapehack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class BapeHackClient implements ClientModInitializer {

    // modul HideCart (glowny przelacznik) i jego ustawienia
    public static boolean hideCartEnabled = false;
    public static boolean hideMinecarts = false;
    public static boolean noMinecartHit = false;

    private static boolean openConfig = false;

    @Override
    public void onInitializeClient() {
        loadConfig();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("bh")
                        .then(ClientCommandManager.literal("config")
                                .executes(ctx -> {
                                    openConfig = true;
                                    return 1;
                                }))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfig) {
                openConfig = false;
                client.setScreen(new BapeHackScreen());
            }
        });
    }

    // ---------------- config (plik) ----------------

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("bapehack.properties");
    }

    public static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("hideCartEnabled", String.valueOf(hideCartEnabled));
        p.setProperty("hideMinecarts", String.valueOf(hideMinecarts));
        p.setProperty("noMinecartHit", String.valueOf(noMinecartHit));
        try (OutputStream out = Files.newOutputStream(configPath())) {
            p.store(out, "BapeHack");
        } catch (IOException ignored) {
        }
    }

    private static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        hideCartEnabled = Boolean.parseBoolean(p.getProperty("hideCartEnabled", "false").trim());
        hideMinecarts = Boolean.parseBoolean(p.getProperty("hideMinecarts", "false").trim());
        noMinecartHit = Boolean.parseBoolean(p.getProperty("noMinecartHit", "false").trim());
    }
}
