package com.bapehack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public class BapeHackScreen extends Screen {

    private static final int PW = 320;
    private static final int PH = 150;
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

    // 0 = glowna, 1 = ustawienia HideCart, 2 = ustawienia NameProtect
    private int page = 0;
    private int bindingId = -1; // modul, ktoremu wlasnie ustawiamy bind
    private boolean closing = false;
    private long animStart = Util.getMeasuringTimeMs();
    private int offY = -2000;

    private TextFieldWidget hideField;
    private TextFieldWidget replaceField;

    public BapeHackScreen() {
        super(Text.literal("BapeHack"));
    }

    private int ph() { return PH; }
    private int px() { return (this.width - PW) / 2; }
    private int py() { return TOP + offY; }

    private int cardX(int col) { return px() + 10 + col * ((PW - 30) / 2 + 10); }
    private int cardY(int row) { return py() + 36 + row * 36; }
    private int cardW(boolean full) { return full ? PW - 20 : (PW - 30) / 2; }

    // modul id -> kolumna id % 2, wiersz id / 2
    private int modX(int id) { return cardX(id % 2); }
    private int modY(int id) { return cardY(id / 2); }
    private static boolean hasSettings(int id) { return id == 0 || id == 2; }

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

    @Override
    protected void init() {
        int fx = px() + 10;
        int fw = PW - 20;

        hideField = new TextFieldWidget(this.textRenderer, fx, 0, fw, 18, Text.literal("ukryj"));
        hideField.setMaxLength(32);
        hideField.setText(BapeHackClient.nameHide);
        hideField.setChangedListener(s -> BapeHackClient.nameHide = s);
        this.addDrawableChild(hideField);

        replaceField = new TextFieldWidget(this.textRenderer, fx, 0, fw, 18, Text.literal("zamien"));
        replaceField.setMaxLength(48);
        replaceField.setText(BapeHackClient.nameReplace);
        replaceField.setChangedListener(s -> BapeHackClient.nameReplace = s);
        this.addDrawableChild(replaceField);

        updateFields();
    }

    private void updateFields() {
        boolean v = page == 2 && !closing;
        hideField.visible = v;
        hideField.active = v;
        replaceField.visible = v;
        replaceField.active = v;
        if (!v) {
            hideField.setFocused(false);
            replaceField.setFocused(false);
        }
    }

    private void updateAnim() {
        float t = Math.min(1.0f, (Util.getMeasuringTimeMs() - animStart) / (float) ANIM_MS);
        int hidden = -(ph() + TOP);
        if (closing) {
            float e = t * t * t;
            offY = (int) (hidden * e);
        } else {
            float e = 1.0f - (float) Math.pow(1.0f - t, 3);
            offY = (int) (hidden * (1.0f - e));
        }
        if (closing && t >= 1.0f) {
            super.close();
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        updateAnim();
        hideField.setY(py() + 46);
        replaceField.setY(py() + 84);
        super.render(c, mx, my, delta);
    }

    // karta modulu na glownej stronie
    private void moduleCard(DrawContext c, int mx, int my, int id) {
        int x = modX(id), y = modY(id), w = cardW(false);
        rrect(c, x, y, w, CH, 6, in(mx, my, x, y, w, CH) ? CARD_HOVER : CARD);

        boolean showBind = bindingId == id || BapeHackClient.binds[id] >= 0;
        c.drawText(this.textRenderer, BapeHackClient.MODULE_NAMES[id], x + 10, y + (showBind ? 6 : 11), TEXT, false);

        if (showBind) {
            String b = bindingId == id ? "Bind:" : "Bind: " + BapeHackClient.keyName(BapeHackClient.binds[id]);
            c.getMatrices().push();
            c.getMatrices().translate(x + 10, y + 18, 0);
            c.getMatrices().scale(0.75f, 0.75f, 1.0f);
            c.drawText(this.textRenderer, b, 0, 0, TEXT_DIM, false);
            c.getMatrices().pop();
        }

        if (hasSettings(id)) {
            c.drawText(this.textRenderer, ">", x + w - 46, y + 11, TEXT_DIM, false);
        }
        toggle(c, x + w - 36, y + 9, BapeHackClient.isEnabled(id));
    }

    // panel i napisy pod polami tekstowymi
    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        super.renderBackground(c, mx, my, delta);

        int x = px();
        int y = py();
        rrect(c, x, y, PW, ph(), 8, PANEL);

        if (page == 0) {
            c.drawText(this.textRenderer, "BapeHack", x + 12, y + 12, TEXT, true);
            for (int id = 0; id < 4; id++) {
                moduleCard(c, mx, my, id);
            }
        } else if (page == 1) {
            header(c, mx, my, "HideCart");
            row(c, mx, my, 0, "Ukryj wagony", BapeHackClient.hideMinecarts);
            row(c, mx, my, 1, "Bez kolizji wagonów", BapeHackClient.noMinecartHit);
        } else {
            header(c, mx, my, "NameProtect");
            c.drawText(this.textRenderer, "Ukryj nick", x + 10, y + 34, TEXT_DIM, false);
            c.drawText(this.textRenderer, "Zamień nick", x + 10, y + 72, TEXT_DIM, false);
        }
    }

    private void header(DrawContext c, int mx, int my, String title) {
        int x = px();
        int y = py();
        boolean backHov = in(mx, my, x + 8, y + 8, 50, 16);
        c.drawText(this.textRenderer, "< Wróć", x + 12, y + 12, backHov ? TEXT : TEXT_DIM, false);
        c.drawText(this.textRenderer, title, x + PW - 12 - this.textRenderer.getWidth(title),
                y + 12, TEXT, true);
    }

    private void row(DrawContext c, int mx, int my, int i, String label, boolean on) {
        int cx = cardX(0), cy = cardY(i), w = cardW(true);
        rrect(c, cx, cy, w, CH, 6, in(mx, my, cx, cy, w, CH) ? CARD_HOVER : CARD);
        c.drawText(this.textRenderer, label, cx + 10, cy + 11, TEXT, false);
        toggle(c, cx + w - 36, cy + 9, on);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;

        if (page == 0) {
            for (int id = 0; id < 4; id++) {
                if (!in(mx, my, modX(id), modY(id), cardW(false), CH)) continue;
                if (button == 0) {
                    bindingId = -1;
                    BapeHackClient.toggle(id);
                } else if (button == 2) {
                    bindingId = id;               // srodkowy przycisk: tryb bindowania
                } else if (button == 1 && hasSettings(id)) {
                    bindingId = -1;
                    page = id == 0 ? 1 : 2;
                    updateFields();
                }
                return true;
            }
            bindingId = -1; // klik poza kartami anuluje bindowanie
        } else {
            if (button == 1 || (button == 0 && in(mx, my, px() + 8, py() + 8, 50, 16))) {
                page = 0;
                updateFields();
                return true;
            }
            if (page == 1 && button == 0) {
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
        // tryb bindowania ma pierwszenstwo
        if (bindingId >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // anulowanie, bind bez zmian
            } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                BapeHackClient.binds[bindingId] = -1;
            } else {
                BapeHackClient.binds[bindingId] = keyCode;
            }
            bindingId = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            animStart = Util.getMeasuringTimeMs();
            updateFields();
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
