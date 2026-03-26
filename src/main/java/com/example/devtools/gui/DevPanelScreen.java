package com.example.devtools.gui;

import com.example.devtools.core.FeatureRegistry;
import com.example.devtools.core.IDevFeature;
import com.example.devtools.features.camera.FlyFeature;
import com.example.devtools.features.camera.FreecamFeature;
import com.example.devtools.features.esp.ESPRenderer;
import com.example.devtools.features.hud.HudOverlay;
import com.example.devtools.mixin.render.ChunkBorderRenderer;
import com.example.devtools.mixin.render.HitboxRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tabbed Dev Panel – öffnet sich via F8.
 * isPauseScreen = false → Spiel läuft weiter!
 *
 * Tabs: Features | Render | HUD | Camera
 */
public class DevPanelScreen extends Screen {

    // ── Layout ───────────────────────────────────────────────────────────────
    private static final int W     = 330;
    private static final int H     = 230;
    private static final int TAB_H = 18;
    private static final int PAD   = 5;
    private static final int BTN_W = 145;
    private static final int BTN_H = 17;

    // ── Farben ───────────────────────────────────────────────────────────────
    private static final int COL_BG      = 0xEE0A0A12;
    private static final int COL_BORDER  = 0xFF2A2A4A;
    private static final int COL_TAB_ACT = 0xFF1E3A5F;
    private static final int COL_TAB_IDL = 0xFF101020;
    private static final int COL_SEP     = 0xFF333355;

    private enum Tab {
        FEATURES("§eFeatures"),
        RENDER  ("§bRender"),
        HUD     ("§aHUD"),
        CAMERA  ("§dCamera");

        final String label;
        Tab(String l) { this.label = l; }
    }

    private Tab activeTab = Tab.FEATURES;

    private record Label(int x, int y, String text) {}
    private final List<Label> labels = new ArrayList<>();

    public DevPanelScreen() {
        super(Component.literal("§8DevTools §7|§r "));
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        labels.clear();

        int px = (width  - W) / 2;
        int py = (height - H) / 2;

        // Tab-Buttons
        Tab[] tabs = Tab.values();
        int tw = W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            String raw  = t.label.substring(2); // §X entfernen
            addRenderableWidget(Button.builder(
                    Component.literal(t == activeTab ? t.label : "§7" + raw),
                    b -> { activeTab = t; rebuild(); })
                .bounds(px + i * tw, py, tw - 1, TAB_H)
                .build());
        }

        int cx  = px + PAD;
        int cy  = py + TAB_H + PAD + 2;
        int c2x = px + W / 2 + PAD;

        switch (activeTab) {
            case FEATURES -> buildFeatures(cx, cy, c2x);
            case RENDER   -> buildRender(cx, cy, c2x);
            case HUD      -> buildHud(cx, cy, c2x);
            case CAMERA   -> buildCamera(cx, cy, c2x);
        }

        // Close
        addRenderableWidget(Button.builder(
                Component.literal("§cClose [Esc]"),
                b -> onClose())
            .bounds(px + W - 80, py + H - 22, 74, 16)
            .build());
    }

    private void rebuild() { clearWidgets(); labels.clear(); init(); }

    // ── Tab: Features ────────────────────────────────────────────────────────
    private void buildFeatures(int x, int y, int c2x) {
        List<IDevFeature> all = new ArrayList<>(FeatureRegistry.getAll());
        int mid = (all.size() + 1) / 2;
        for (int i = 0; i < all.size(); i++) {
            IDevFeature f = all.get(i);
            int bx = i < mid ? x : c2x;
            int by = y + (i < mid ? i : i - mid) * (BTN_H + 3);
            addRenderableWidget(featureBtn(f, bx, by));
        }
    }

    // ── Tab: Render ──────────────────────────────────────────────────────────
    private void buildRender(int x, int y, int c2x) {
        ESPRenderer esp = ESPRenderer.INSTANCE;
        sectionLabel(x, y, "ESP Visualizer"); y += 11;
        addRenderableWidget(featureBtn(esp, x, y)); y += BTN_H + 3;
        y = boolBtn(x, y, "Players",  esp.showPlayers,  v -> esp.showPlayers  = v);
        y = boolBtn(x, y, "Mobs",     esp.showMobs,     v -> esp.showMobs     = v);
        y = boolBtn(x, y, "Items",    esp.showItems,    v -> esp.showItems    = v);
        y = boolBtn(x, y, "Animals",  esp.showAnimals,  v -> esp.showAnimals  = v);
        boolBtn(x, y,     "XRay",     esp.xrayMode,     v -> esp.xrayMode     = v);

        int ry = (height - H) / 2 + TAB_H + PAD + 2;
        HitboxRenderer hb = HitboxRenderer.INSTANCE;
        sectionLabel(c2x, ry, "Hitboxes"); ry += 11;
        addRenderableWidget(featureBtn(hb, c2x, ry)); ry += BTN_H + 3;
        ry = boolBtn(c2x, ry, "Eye Line",    hb.showEyeLine, v -> hb.showEyeLine = v);
        ry = boolBtn(c2x, ry, "Look Vector", hb.showLookVec, v -> hb.showLookVec = v);

        ry += 6;
        ChunkBorderRenderer cb = ChunkBorderRenderer.INSTANCE;
        sectionLabel(c2x, ry, "Chunk Borders"); ry += 11;
        addRenderableWidget(featureBtn(cb, c2x, ry)); ry += BTN_H + 3;
        ry = boolBtn(c2x, ry, "XRay",     cb.xray,     v -> cb.xray     = v);
        boolBtn(c2x, ry,      "Y-Bounds", cb.yBounds,  v -> cb.yBounds  = v);
    }

    // ── Tab: HUD ─────────────────────────────────────────────────────────────
    private void buildHud(int x, int y, int c2x) {
        HudOverlay hud = HudOverlay.INSTANCE;
        int topY = y;
        addRenderableWidget(featureBtn(hud, x, y)); y += BTN_H + 6;

        y = boolBtn(x, y, "FPS",         hud.showFps,      v -> hud.showFps      = v);
        y = boolBtn(x, y, "Ping",        hud.showPing,     v -> hud.showPing     = v);
        y = boolBtn(x, y, "Memory",      hud.showMemory,   v -> hud.showMemory   = v);
        y = boolBtn(x, y, "Coordinates", hud.showCoords,   v -> hud.showCoords   = v);
        boolBtn(x, y,     "Facing",      hud.showFacing,   v -> hud.showFacing   = v);

        int ry = topY + BTN_H + 6;
        ry = boolBtn(c2x, ry, "Chunk XZ",   hud.showChunk,    v -> hud.showChunk    = v);
        ry = boolBtn(c2x, ry, "Velocity",   hud.showVelocity, v -> hud.showVelocity = v);
        ry = boolBtn(c2x, ry, "Light",      hud.showLight,    v -> hud.showLight    = v);
        ry = boolBtn(c2x, ry, "Biome",      hud.showBiome,    v -> hud.showBiome    = v);
        boolBtn(c2x, ry,      "World Time", hud.showTime,     v -> hud.showTime     = v);
    }

    // ── Tab: Camera / Fly ────────────────────────────────────────────────────
    private void buildCamera(int x, int y, int c2x) {
        FlyFeature fly = FlyFeature.INSTANCE;

        sectionLabel(x, y, "Creative Fly"); y += 11;
        addRenderableWidget(featureBtn(fly, x, y)); y += BTN_H + 5;

        labels.add(new Label(x, y, "§7Speed (0.01–1.0):"));  y += 10;
        EditBox speedBox = new EditBox(font, x, y, 100, 15, Component.empty());
        speedBox.setValue(String.format("%.2f", fly.getSpeed()));
        speedBox.setResponder(s -> {
            try { fly.setSpeed(Float.parseFloat(s)); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(speedBox); y += 18;

        float[] presets = {0.05f, 0.10f, 0.25f, 0.50f, 1.00f};
        int pw = 26;
        for (int i = 0; i < presets.length; i++) {
            final float sp = presets[i];
            final EditBox sb = speedBox;
            addRenderableWidget(Button.builder(
                    Component.literal("§8" + sp),
                    b -> { fly.setSpeed(sp); sb.setValue(String.format("%.2f", sp)); })
                .bounds(x + i * (pw + 2), y, pw, 13)
                .build());
        }
        y += 18;

        FreecamFeature fc = FreecamFeature.INSTANCE;
        sectionLabel(x, y, "Freecam"); y += 11;
        addRenderableWidget(featureBtn(fc, x, y)); y += BTN_H + 5;

        labels.add(new Label(x, y, "§7Cam Speed (mult):"));  y += 10;
        EditBox fcBox = new EditBox(font, x, y, 100, 15, Component.empty());
        fcBox.setValue(String.format("%.2f", fc.speedMultiplier));
        fcBox.setResponder(s -> {
            try { fc.speedMultiplier = Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(fcBox);

        // Hinweise rechte Spalte
        int ry = (height - H) / 2 + TAB_H + PAD + 2;
        labels.add(new Label(c2x, ry,        "§8Fly Keybinds:"));
        labels.add(new Label(c2x, ry + 11,   "§7WASD – Bewegen"));
        labels.add(new Label(c2x, ry + 22,   "§7Space / Shift – Höhe"));
        labels.add(new Label(c2x, ry + 33,   "§7Sprint – 4× Speed"));
        labels.add(new Label(c2x, ry + 50,   "§8Freecam:"));
        labels.add(new Label(c2x, ry + 61,   "§7WASD + Maus"));
        labels.add(new Label(c2x, ry + 72,   "§7Space / Shift – Y-Achse"));
        labels.add(new Label(c2x, ry + 83,   "§7Spieler bleibt eingefroren"));
        labels.add(new Label(c2x, ry + 100,  "§c⚠ Fly: server.properties"));
        labels.add(new Label(c2x, ry + 111,  "§7allow-flight=true nötig"));
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int px = (width  - W) / 2;
        int py = (height - H) / 2;

        // Hintergrund + Rahmen
        g.fill(px - 1, py - 1, px + W + 1, py + H + 1, COL_BORDER);
        g.fill(px, py, px + W, py + H, COL_BG);

        // Tab-Bar
        g.fill(px, py, px + W, py + TAB_H, COL_TAB_IDL);
        Tab[] tabs = Tab.values();
        int tw = W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == activeTab)
                g.fill(px + i * tw, py, px + (i + 1) * tw - 1, py + TAB_H, COL_TAB_ACT);
        }

        // Trennlinien
        g.fill(px,       py + TAB_H,     px + W,         py + TAB_H + 1, COL_SEP);
        g.fill(px + W/2, py + TAB_H + 1, px + W/2 + 1,  py + H - 22,    COL_SEP);

        super.render(g, mx, my, delta);

        for (Label l : labels)
            g.drawString(font, l.text(), l.x(), l.y(), 0xFFFFFF, false);

        g.drawString(font, "§8F8 schließt / Tab wechselt",
            px + PAD, py + H - 10, 0x555555, false);
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Helfer ───────────────────────────────────────────────────────────────
    private Button featureBtn(IDevFeature f, int x, int y) {
        return Button.builder(fLabel(f), b -> {
                f.setEnabled(!f.isEnabled());
                b.setMessage(fLabel(f));
            })
            .bounds(x, y, BTN_W, BTN_H)
            .build();
    }

    private static Component fLabel(IDevFeature f) {
        return Component.literal((f.isEnabled() ? "§a[ON]  " : "§c[OFF] ") + "§f" + f.getDisplayName());
    }

    private int boolBtn(int x, int y, String label, boolean initial, Consumer<Boolean> setter) {
        final boolean[] state = {initial};
        addRenderableWidget(Button.builder(
                Component.literal(boolLabel(state[0], label)),
                b -> {
                    state[0] = !state[0];
                    setter.accept(state[0]);
                    b.setMessage(Component.literal(boolLabel(state[0], label)));
                })
            .bounds(x, y, BTN_W, BTN_H)
            .build());
        return y + BTN_H + 2;
    }

    private static String boolLabel(boolean on, String label) {
        return (on ? "§a✔ " : "§8✘ ") + "§7" + label;
    }

    private void sectionLabel(int x, int y, String text) {
        labels.add(new Label(x, y, "§e" + text));
    }
}
