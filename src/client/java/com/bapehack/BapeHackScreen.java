package com.bapehack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class BapeHackScreen extends Screen {

    private static final int PW = 220;
    private static final int PH = 120;

    private static final int PANEL = 0xC8101216;
    private static final int CARD = 0xC81A1D22;
    private static final int CARD_HOVER = 0xC8262A31;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int TEXT_DIM = 0xFF8A8F98;
    private static final int ON = 0xFF4ADE80;
    private static final int OFF = 0xFF3A3D44;

    private boolean settings = false;

    public BapeHackScreen() {
        super(Text.literal("BapeHack"));
    }

    private int px() { return (this.width - PW) / 2; }
    private int py() { return (this.height - PH) / 2; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // zaokraglony prostokat
    private static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
        if (h - 2 * r > 0) c.fill(x, y + r, x + w, y + h - r, col);
    }

    private static void toggle(DrawContext c, int x, int y, boolean on) {
        rrect(c, x, y, 26, 12, 6, on ? ON : OFF);
        int kx = on ? x + 15 : x + 1;
        rrect(c, kx, y + 1, 10, 10, 5, 0xFFFFFFFF);
    }

    // pozycje kart
    private int cardX() { return px() + 10; }
    private int cardY(int i) { return py() + 36 + i * 36; }
    private static final int CW = 200;
    private static final int CH = 30;

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);

        int x = px();
        int y = py();
        rrect(c, x, y, PW, PH, 8, PANEL);

        if (!settings) {
            c.drawText(this.textRenderer, "BapeHack", x + 12, y + 12, TEXT, true);
            c.drawText(this.textRenderer, "LPM: włącz   PPM: ustawienia", x + 80, y + 12, TEXT_DIM, false);

            boolean hov = in(mx, my, cardX(), cardY(0), CW, CH);
            rrect(c, cardX(), cardY(0), CW, CH, 6, hov ? CARD_HOVER : CARD);
            c.drawText(this.textRenderer, "HideCart", cardX() + 10, cardY(0) + 11, TEXT, false);
            c.drawText(this.textRenderer, ">", cardX() + CW - 46, cardY(0) + 11, TEXT_DIM, false);
            toggle(c, cardX() + CW - 36, cardY(0) + 9, BapeHackClient.hideCartEnabled);
        } else {
            boolean backHov = in(mx, my, x + 8, y + 8, 50, 16);
            c.drawText(this.textRenderer, "< Wróć", x + 12, y + 12, backHov ? TEXT : TEXT_DIM, false);
            c.drawText(this.textRenderer, "HideCart", x + PW - 12 - this.textRenderer.getWidth("HideCart"),
                    y + 12, TEXT, true);

            row(c, mx, my, 0, "Ukryj wagony", BapeHackClient.hideMinecarts);
            row(c, mx, my, 1, "Bez kolizji wagonów", BapeHackClient.noMinecartHit);
        }
    }

    private void row(DrawContext c, int mx, int my, int i, String label, boolean on) {
        boolean hov = in(mx, my, cardX(), cardY(i), CW, CH);
        rrect(c, cardX(), cardY(i), CW, CH, 6, hov ? CARD_HOVER : CARD);
        c.drawText(this.textRenderer, label, cardX() + 10, cardY(i) + 11, TEXT, false);
        toggle(c, cardX() + CW - 36, cardY(i) + 9, on);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settings) {
            if (in(mx, my, cardX(), cardY(0), CW, CH)) {
                if (button == 0) {
                    BapeHackClient.hideCartEnabled = !BapeHackClient.hideCartEnabled;
                } else if (button == 1) {
                    settings = true;
                }
                return true;
            }
        } else {
            if (button == 1 || (button == 0 && in(mx, my, px() + 8, py() + 8, 50, 16))) {
                settings = false;
                return true;
            }
            if (button == 0) {
                if (in(mx, my, cardX(), cardY(0), CW, CH)) {
                    BapeHackClient.hideMinecarts = !BapeHackClient.hideMinecarts;
                    return true;
                }
                if (in(mx, my, cardX(), cardY(1), CW, CH)) {
                    BapeHackClient.noMinecartHit = !BapeHackClient.noMinecartHit;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
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
