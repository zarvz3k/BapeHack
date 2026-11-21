package com.bapehack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

public class BapeHackScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 264;
    private static final long ANIM_MS = 500;
    private static final int TEXT = 0xFF404040;

    private final long openTime = Util.getMeasuringTimeMs();
    private int yOffset = 0;
    private final Map<ClickableWidget, Integer> baseY = new HashMap<>();

    public BapeHackScreen() {
        super(Text.literal("BapeHack"));
    }

    private <T extends ClickableWidget> T track(T widget) {
        baseY.put(widget, widget.getY());
        return this.addDrawableChild(widget);
    }

    private static class ScaleSlider extends SliderWidget {
        ScaleSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(),
                    (BapeHackClient.scale - BapeHackClient.SCALE_MIN)
                            / (BapeHackClient.SCALE_MAX - BapeHackClient.SCALE_MIN));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Rozmiar: " + String.format("%.2fx", BapeHackClient.scale)));
        }

        @Override
        protected void applyValue() {
            float range = BapeHackClient.SCALE_MAX - BapeHackClient.SCALE_MIN;
            float raw = BapeHackClient.SCALE_MIN + (float) this.value * range;
            float snapped = Math.round(raw / 0.05f) * 0.05f;
            BapeHackClient.scale = Math.max(BapeHackClient.SCALE_MIN,
                    Math.min(BapeHackClient.SCALE_MAX, snapped));
            updateMessage();
        }
    }

    @Override
    protected void init() {
        baseY.clear();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        addNumberField(left + 38, top + 38, BapeHackClient.posX, v -> BapeHackClient.posX = v);
        addNumberField(left + 122, top + 38, BapeHackClient.posY, v -> BapeHackClient.posY = v);

        track(new ScaleSlider(left + 10, top + 66, 180, 20));

        TextFieldWidget color = new TextFieldWidget(this.textRenderer, left + 65, top + 106, 70, 18,
                Text.literal("kolor"));
        color.setMaxLength(6);
        color.setTextPredicate(s -> s.matches("[0-9a-fA-F]{0,6}"));
        color.setText(String.format("%06X", BapeHackClient.barColor));
        color.setChangedListener(s -> {
            if (s.length() == 6) {
                try {
                    BapeHackClient.barColor = Integer.parseInt(s, 16);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        track(color);

        track(ButtonWidget.builder(dynamicText(), b -> {
            BapeHackClient.dynamicColor = !BapeHackClient.dynamicColor;
            b.setMessage(dynamicText());
        }).dimensions(left + 10, top + 132, 180, 20).build());

        track(ButtonWidget.builder(styleText(), b -> {
            BapeHackClient.enchStyle = (BapeHackClient.enchStyle + 1) % 3;
            b.setMessage(styleText());
        }).dimensions(left + 10, top + 158, 180, 20).build());

        track(ButtonWidget.builder(hideText(), b -> {
            BapeHackClient.hideMinecarts = !BapeHackClient.hideMinecarts;
            b.setMessage(hideText());
        }).dimensions(left + 10, top + 184, 180, 20).build());

        track(ButtonWidget.builder(hitText(), b -> {
            BapeHackClient.noMinecartHit = !BapeHackClient.noMinecartHit;
            b.setMessage(hitText());
        }).dimensions(left + 10, top + 210, 180, 20).build());

        track(ButtonWidget.builder(Text.literal("Gotowe"), b -> this.close())
                .dimensions(left + 50, top + 238, 100, 20).build());
    }

    private static Text dynamicText() {
        return Text.literal("Kolor wg HP: " + (BapeHackClient.dynamicColor ? "ON" : "OFF"));
    }

    private static Text styleText() {
        String[] names = {"S5", "Sharp5", "Sharpness 5"};
        return Text.literal("Skróty enchantów: " + names[BapeHackClient.enchStyle]);
    }

    private static Text hideText() {
        return Text.literal("Ukryj wagony: " + (BapeHackClient.hideMinecarts ? "ON" : "OFF"));
    }

    private static Text hitText() {
        return Text.literal("Bez kolizji wagonów: " + (BapeHackClient.noMinecartHit ? "ON" : "OFF"));
    }

    private void addNumberField(int x, int y, int current, IntConsumer setter) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, 50, 18, Text.literal("pos"));
        field.setMaxLength(4);
        field.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        field.setText(String.valueOf(current));
        field.setChangedListener(s -> {
            int v = 0;
            if (!s.isEmpty()) {
                try {
                    v = Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                }
            }
            setter.accept(v);
        });
        track(field);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float t = Math.min(1.0f, (Util.getMeasuringTimeMs() - openTime) / (float) ANIM_MS);
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
        int top = (this.height - PANEL_H) / 2;
        yOffset = (int) ((1.0f - eased) * (this.height - top));

        for (Map.Entry<ClickableWidget, Integer> e : baseY.entrySet()) {
            e.getKey().setY(e.getValue() + yOffset);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2 + yOffset;
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;

        context.fill(left, top, right, bottom, 0xFF000000);
        context.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        context.fill(left + 1, top + 1, right - 1, top + 2, 0xFFFFFFFF);
        context.fill(left + 1, top + 1, left + 2, bottom - 1, 0xFFFFFFFF);
        context.fill(left + 1, bottom - 2, right - 1, bottom - 1, 0xFF555555);
        context.fill(right - 2, top + 1, right - 1, bottom - 1, 0xFF555555);

        int cx = this.width / 2;

        Text title = Text.literal("BapeHack");
        context.drawText(this.textRenderer, title, cx - this.textRenderer.getWidth(title) / 2,
                top + 10, TEXT, false);

        Text pos = Text.literal("Pozycja panelu");
        context.drawText(this.textRenderer, pos, cx - this.textRenderer.getWidth(pos) / 2,
                top + 24, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("X"), left + 26, top + 43, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Y"), left + 110, top + 43, TEXT, false);

        Text hex = Text.literal("Kolor paska HP (hex)");
        context.drawText(this.textRenderer, hex, cx - this.textRenderer.getWidth(hex) / 2,
                top + 94, TEXT, false);
    }

    @Override
    public void removed() {
        BapeHackClient.saveConfig();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
