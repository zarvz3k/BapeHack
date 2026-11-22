package com.bapehack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class BapeHackScreen extends Screen {

    private static final int PW = 320;
    private static final int PH = 110;
    private static final int TOP = 10;
    private static final long ANIM_MS = 300;

    private static final int PANEL = 0xC8101216;
    private static final int CARD = 0xC81A1D22;
    private static final int CARD_HOVER = 0xC8262A31;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int TEXT_DIM = 0xFF8A8F98;
    private static final int ON = 0xFF4ADE80;
    private static final int OFF = 0xFF3A3D44;

    private static final int CH = 30;

    private boolean settings = false;
    private boolean closing = false;
    private long animStart = Util.getMeasuringTimeMs();
    private int offY = -(PH + TOP);

    public BapeHackScreen() {
        super(Text.literal("BapeHack"));
    }

    private int px() { return (this.width - PW) / 2; }
    private int py() { return TOP + offY; }

    // karta: kolumna 0 = lewa polowa, 1 = prawa; wiersz r
    private int cardX(int col) { return px() + 10 + col * ((PW - 30) / 2 + 10); }
    private int cardY(int row) { return py() + 36 + row * 36; }
    private int cardW(boolean full) { return full ? PW - 20 : (PW - 30) / 2; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

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

    private void updateAnim() {
        float t = Math.min(1.0f, (Util.getMeasuringTimeMs() - animStart) / (float) ANIM_MS);
        float e;
        if (closing) {
            e = t * t * t;                                   // wyjazd: przyspiesza
            offY = (int) (-(PH + TOP) * e);
        } else {
            e = 1.0f - (float) Math.pow(1.0f - t, 3);        // wjazd: wyhamowuje
            offY = (int) (-(PH + TOP) * (1.0f - e));
        }
        if (closing && t >= 1.0f) {
            super.close();
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        updateAnim();
        super.render(c, mx, my, delta);

        int x = px();
        int y = py();
        rrect(c, x, y, PW, PH, 8, PANEL);

        if (!settings) {
            c.drawText(this.textRenderer, "BapeHack", x + 12, y + 12, TEXT, true);

            // HideCart (lewa polowa, ze strzalka bo ma ustawienia)
            int cx = cardX(0);
            int cy = cardY(0);
            int w = cardW(false);
            boolean hov = in(mx, my, cx, cy, w, CH);
            rrect(c, cx, cy, w, CH, 6, hov ? CARD_HOVER : CARD);
            c.drawText(this.textRenderer, "HideCart", cx + 10, cy + 11, TEXT, false);
            c.drawText(this.textRenderer, ">", cx + w - 46, cy + 11, TEXT_DIM, false);
            toggle(c, cx + w - 36, cy + 9, BapeHackClient.hideCartEnabled);

            // AntyItemFrame (prawa polowa, bez strzalki)
            int ax = cardX(1);
            int ay = cardY(0);
            int aw = cardW(false);
            boolean ahov = in(mx, my, ax, ay, aw, CH);
            rrect(c, ax, ay, aw, CH, 6, ahov ? CARD_HOVER : CARD);
            c.drawText(this.textRenderer, "AntyItemFrame", ax + 10, ay + 11, TEXT, false);
            toggle(c, ax + aw - 36, ay + 9, BapeHackClient.antyItemFrameEnabled);
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
        int cx = cardX(0);
        int cy = cardY(i);
        int w = cardW(true);
        boolean hov = in(mx, my, cx, cy, w, CH);
        rrect(c, cx, cy, w, CH, 6, hov ? CARD_HOVER : CARD);
        c.drawText(this.textRenderer, label, cx + 10, cy + 11, TEXT, false);
        toggle(c, cx + w - 36, cy + 9, on);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        if (!settings) {
            // AntyItemFrame: tylko lewy klik, prawy nic nie robi
            if (in(mx, my, cardX(1), cardY(0), cardW(false), CH)) {
                if (button == 0) {
                    BapeHackClient.antyItemFrameEnabled = !BapeHackClient.antyItemFrameEnabled;
                }
                return true;
            }
            // HideCart: lewy = wlacz, prawy = ustawienia
            if (in(mx, my, cardX(0), cardY(0), cardW(false), CH)) {
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
                if (in(mx, my, cardX(0), cardY(0), cardW(true), CH)) {
                    BapeHackClient.hideMinecarts = !BapeHackClient.hideMinecarts;
                    return true;
                }
                if (in(mx, my, cardX(0), cardY(1), cardW(true), CH)) {
                    BapeHackClient.noMinecartHit = !BapeHackClient.noMinecartHit;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 344) { // RightControl
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // zamkniecie z animacja wyjazdu
    @Override
    public void close() {
        if (!closing) {
            closing = true;
            animStart = Util.getMeasuringTimeMs();
        }
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
