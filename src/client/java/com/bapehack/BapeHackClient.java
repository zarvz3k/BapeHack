package com.bapehack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BapeHackClient implements ClientModInitializer {

    // modul HideCart
    public static boolean hideCartEnabled = false;
    public static boolean hideMinecarts = false;
    public static boolean noMinecartHit = false;

    // modul AntyItemFrame
    public static boolean antyItemFrameEnabled = false;

    // modul NameProtect
    public static boolean nameProtectEnabled = false;
    public static String nameHide = "";
    public static String nameReplace = "";
    private static volatile Pattern namePattern;
    private static volatile String patternFor = "";

    // modul SwingThrough
    public static boolean swingThroughEnabled = false;
    public static boolean swingScan = false;

    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        loadConfig();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bapehack.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "BapeHack"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BapeHackScreen());
                }
            }
        });

        // AntyItemFrame: nie mozna wlozyc przedmiotu do pustej ramki
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!antyItemFrameEnabled || !world.isClient()) return ActionResult.PASS;
            if (entity instanceof ItemFrameEntity frame
                    && frame.getHeldItemStack().isEmpty()
                    && !player.getStackInHand(hand).isEmpty()) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    // SwingThrough: pomijaj bloki w raycascie niezaleznie od przedmiotu w rece
    public static void updateSwingScan() {
        MinecraftClient mc = MinecraftClient.getInstance();
        swingScan = swingThroughEnabled && mc.player != null;
    }

    public static boolean isSwingBlock(BlockState s) {
        return s.isOf(Blocks.COBWEB) || s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS)
                || s.isOf(Blocks.FERN) || s.isOf(Blocks.LARGE_FERN);
    }

    // NameProtect: podmiana nicku w kazdym rysowanym tekscie
    public static String protectNames(String s) {
        if (!nameProtectEnabled || s == null) return s;
        String hide = nameHide;
        if (hide.isEmpty() || s.length() < hide.length()) return s;

        MinecraftClient mc = MinecraftClient.getInstance();
        // w menu BapeHack pokazujemy prawdziwy tekst (pola do wpisywania)
        if (mc != null && mc.currentScreen instanceof BapeHackScreen) return s;

        Pattern p = namePattern;
        if (p == null || !hide.equals(patternFor)) {
            p = Pattern.compile(Pattern.quote(hide), Pattern.CASE_INSENSITIVE);
            namePattern = p;
            patternFor = hide;
        }
        return p.matcher(s).replaceAll(Matcher.quoteReplacement(nameReplace));
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
        p.setProperty("antyItemFrameEnabled", String.valueOf(antyItemFrameEnabled));
        p.setProperty("nameProtectEnabled", String.valueOf(nameProtectEnabled));
        p.setProperty("nameHide", nameHide);
        p.setProperty("nameReplace", nameReplace);
        p.setProperty("swingThroughEnabled", String.valueOf(swingThroughEnabled));
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
        antyItemFrameEnabled = Boolean.parseBoolean(p.getProperty("antyItemFrameEnabled", "false").trim());
        nameProtectEnabled = Boolean.parseBoolean(p.getProperty("nameProtectEnabled", "false").trim());
        nameHide = p.getProperty("nameHide", "");
        nameReplace = p.getProperty("nameReplace", "");
        swingThroughEnabled = Boolean.parseBoolean(p.getProperty("swingThroughEnabled", "false").trim());
    }
}
