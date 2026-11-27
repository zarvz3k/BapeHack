package com.bapehack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class BapeHackClient implements ClientModInitializer {

    // moduly: 0 HideCart, 1 AntyItemFrame, 2 NameProtect, 3 SwingThrough
    public static final String[] MODULE_NAMES = {"HideCart", "AntyItemFrame", "NameProtect", "SwingThrough"};
    public static final int[] binds = {-1, -1, -1, -1};   // kod klawisza GLFW, -1 = brak
    private static final boolean[] prevDown = new boolean[4];

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

    // powiadomienie nad sercami
    private static final long NOTICE_HOLD_MS = 1250;
    private static final long NOTICE_FADE_MS = 250;
    private static final class Notice {
        final String name;
        final boolean on;
        final long start;
        Notice(String name, boolean on, long start) {
            this.name = name;
            this.on = on;
            this.start = start;
        }
    }
    private static final List<Notice> notices = new ArrayList<>();

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

            // wlasne bindy modulow
            long handle = client.getWindow().getHandle();
            for (int i = 0; i < binds.length; i++) {
                if (binds[i] < 0) {
                    prevDown[i] = false;
                    continue;
                }
                boolean down = InputUtil.isKeyPressed(handle, binds[i]);
                if (down && !prevDown[i] && client.currentScreen == null) {
                    toggle(i);
                }
                prevDown[i] = down;
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderNotice(ctx));

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

    // ---------------- moduly ----------------

    public static boolean isEnabled(int id) {
        return switch (id) {
            case 0 -> hideCartEnabled;
            case 1 -> antyItemFrameEnabled;
            case 2 -> nameProtectEnabled;
            default -> swingThroughEnabled;
        };
    }

    public static void toggle(int id) {
        boolean now = !isEnabled(id);
        switch (id) {
            case 0 -> hideCartEnabled = now;
            case 1 -> antyItemFrameEnabled = now;
            case 2 -> nameProtectEnabled = now;
            default -> swingThroughEnabled = now;
        }
        notices.add(new Notice(MODULE_NAMES[id], now, Util.getMeasuringTimeMs()));
        if (notices.size() > 8) notices.remove(0);
    }

    public static String keyName(int code) {
        return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
    }

    // ---------------- powiadomienie ----------------

    private static void renderNotice(DrawContext ctx) {
        if (notices.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        long now = Util.getMeasuringTimeMs();
        notices.removeIf(n -> now - n.start >= NOTICE_HOLD_MS + NOTICE_FADE_MS);

        int baseY = ctx.getScaledWindowHeight() - 72; // nad sercami
        int count = notices.size();
        for (int i = 0; i < count; i++) {
            Notice n = notices.get(i);
            long age = now - n.start;

            int a = 255;
            if (age > NOTICE_HOLD_MS) {
                a = (int) (255 * (1.0f - (age - NOTICE_HOLD_MS) / (float) NOTICE_FADE_MS));
            }
            if (a < 12) continue; // vanilla traktuje bardzo male alfa jako pelne krycie

            int white = (a << 24) | 0xFFFFFF;
            int state = (a << 24) | (n.on ? 0x55FF55 : 0xFF5555);

            String left = n.name + " - ";
            String right = n.on ? "ON" : "OFF";
            int total = mc.textRenderer.getWidth(left) + mc.textRenderer.getWidth(right);
            int x = (ctx.getScaledWindowWidth() - total) / 2;
            int y = baseY - (count - 1 - i) * 11; // najnowsze na dole, starsze wyzej

            ctx.drawTextWithShadow(mc.textRenderer, left, x, y, white);
            ctx.drawTextWithShadow(mc.textRenderer, right, x + mc.textRenderer.getWidth(left), y, state);
        }
    }

    // ---------------- SwingThrough ----------------

    public static void updateSwingScan() {
        MinecraftClient mc = MinecraftClient.getInstance();
        swingScan = swingThroughEnabled && mc.player != null
                && (mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
                || mc.player.getMainHandStack().isIn(ItemTags.AXES));
    }

    public static boolean isSwingBlock(BlockState s) {
        return s.isOf(Blocks.COBWEB) || s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS)
                || s.isOf(Blocks.FERN) || s.isOf(Blocks.LARGE_FERN);
    }

    // ---------------- NameProtect ----------------

    public static String protectNames(String s) {
        if (!nameProtectEnabled || s == null) return s;
        String hide = nameHide;
        if (hide.isEmpty() || s.length() < hide.length()) return s;

        MinecraftClient mc = MinecraftClient.getInstance();
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
        for (int i = 0; i < binds.length; i++) {
            p.setProperty("bind" + i, String.valueOf(binds[i]));
        }
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
        for (int i = 0; i < binds.length; i++) {
            try {
                binds[i] = Integer.parseInt(p.getProperty("bind" + i, "-1").trim());
            } catch (NumberFormatException e) {
                binds[i] = -1;
            }
        }
    }
}
