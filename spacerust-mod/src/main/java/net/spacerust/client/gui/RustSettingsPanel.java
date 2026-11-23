package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.option.AoMode;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.config.RustSettings;
import net.spacerust.client.voice.RustVoiceEngine;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared CoilRust-authentic Settings Panel used in both Main Menu ("НАСТРОЙКИ")
 * and In-Game Settings ("Esc -> Настройки").
 */
public class RustSettingsPanel {

    public enum Category {
        GAMEPLAY("ИГРОВОЙ ПРОЦЕСС"),
        AUDIO("ЗВУК"),
        DISPLAY("ЭКРАН"),
        GRAPHICS("ГРАФИКА"),
        EFFECTS("ЭФФЕКТЫ ИЗОБРАЖЕНИЯ"),
        INTERFACE("ИНТЕРФЕЙС"),
        CONTROLS("УПРАВЛЕНИЕ"),
        EXPERIMENTAL("ЭКСПЕРИМЕНТАЛЬНОЕ"),
        VOICE("VOICE CHAT");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }

    private interface RowAction {
        void onClick();
    }

    private interface SliderChange {
        void onSlide(float ratio);
    }

    private enum RowKind {
        HEADER,
        SLIDER,
        TOGGLE,
        CYCLE,
        BUTTON,
        KEYBIND,
        METER,
        EMPTY_NOTE
    }

    private static final class SettingRow {
        final RowKind kind;
        final String label;
        final String valueText;
        final String description;
        final boolean boolVal;
        final float ratio;
        final RowAction action;
        final SliderChange slider;
        final KeyBinding keyBinding;

        SettingRow(RowKind kind, String label, String valueText, String description,
                   boolean boolVal, float ratio, RowAction action, SliderChange slider, KeyBinding keyBinding) {
            this.kind = kind;
            this.label = label;
            this.valueText = valueText;
            this.description = description;
            this.boolVal = boolVal;
            this.ratio = ratio;
            this.action = action;
            this.slider = slider;
            this.keyBinding = keyBinding;
        }
    }

    private Category selectedCategory = Category.GAMEPLAY;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private String hoveredTitle = "ПОЛЕ ЗРЕНИЯ";
    private String hoveredDesc = "Угол обзора камеры в игре (FOV). Более широкое значение дает лучший периферийный обзор.";
    private String lastHoverKey = "";
    private SliderChange activeSlider = null;
    private KeyBinding capturingKey = null;

    private List<String> cachedInputDevices;
    private List<String> cachedOutputDevices;
    private long lastDeviceScanMs = 0L;

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public boolean isCapturingInput() {
        return searchFocused || capturingKey != null;
    }

    public void onClosed() {
        searchFocused = false;
        capturingKey = null;
        activeSlider = null;
        RustVoiceEngine.get().setLoopbackTest(false);
    }

    private void playClickSound(MinecraftClient client) {
        RustUiHelper.playMenuClick(client);
    }

    private void playHoverSound(MinecraftClient client, String hoverKey) {
        if (hoverKey == null || hoverKey.equals(lastHoverKey)) {
            return;
        }
        lastHoverKey = hoverKey;
        RustUiHelper.playMenuHover(client);
    }

    private void ensureAudioDevicesFresh() {
        long now = System.currentTimeMillis();
        if (cachedInputDevices == null || (now - lastDeviceScanMs) > 4000L) {
            lastDeviceScanMs = now;
            cachedInputDevices = RustVoiceEngine.listInputDevices();
            cachedOutputDevices = RustVoiceEngine.listOutputDevices();
        }
    }

    public void render(MatrixStack matrices, Screen parent, MinecraftClient client,
                       int mouseX, int mouseY, int x, int y, int w, int h) {
        if (client == null || client.options == null) return;

        int catW = 136;
        int catX = x;
        int topY = y;

        // 1. Search box at top-left of Settings sidebar ("ПОИСК...")
        int searchH = 18;
        DrawableHelper.fill(matrices, catX, topY, catX + catW, topY + searchH, 0xCC12151B);
        DrawableHelper.fill(matrices, catX, topY + searchH - 1, catX + catW, topY + searchH,
                searchFocused ? 0xFFCD412B : 0x22FFFFFF);
        RustFontRenderer rf = RustFontRenderer.regular();
        if (searchQuery.isEmpty()) {
            rf.drawCenteredY(matrices, searchFocused ? "|" : "ПОИСК...", catX + 7, topY, searchH, 0x66B8B8B8);
        } else {
            rf.drawCenteredY(matrices, rf.trim(searchQuery, catW - 14) + (searchFocused ? "|" : ""),
                    catX + 7, topY, searchH, 0xFFFFFFFF);
        }

        // 2. Left 9 Categories
        int curCatY = topY + searchH + 7;
        int itemH = 19;
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        for (Category cat : Category.values()) {
            boolean active = (cat == selectedCategory) && searchQuery.trim().isEmpty();
            boolean hov = inRect(mouseX, mouseY, catX, curCatY, catW, itemH);
            if (hov) {
                playHoverSound(client, "cat:" + cat.name());
            }
            int bg = active ? 0xFFCD412B : (hov ? 0x33FFFFFF : 0x4415181E);
            DrawableHelper.fill(matrices, catX, curCatY, catX + catW, curCatY + itemH, bg);
            int txtColor = active ? 0xFFFFFFFF : (hov ? 0xFFEAECEF : 0xFF9EA5AE);
            bfSmall.drawCenteredY(matrices, cat.title, catX + 8, curCatY, itemH, txtColor);
            curCatY += itemH + 3;
        }

        // 3. Center Settings List + Right Description Card
        int centerColX = catX + catW + 14;
        int descColW = Math.min(205, Math.max(150, (int) (w * 0.25f)));
        int centerColW = w - (centerColX - x) - descColW - 14;
        int listTop = topY + 22;
        int listBottom = y + h - 4;
        int listHeight = Math.max(80, listBottom - listTop);

        String headerTitle = searchQuery.trim().isEmpty() ? selectedCategory.title : "РЕЗУЛЬТАТЫ ПОИСКА";
        RustFontRenderer.boldLarge().draw(matrices, headerTitle, centerColX, topY, 0xFFF2EDE4);

        List<SettingRow> rows = buildVisibleRows(parent, client);
        int totalH = 0;
        for (SettingRow r : rows) {
            totalH += rowHeight(r);
        }
        maxScroll = Math.max(0, totalH - listHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int drawY = listTop - scrollOffset;
        for (int i = 0; i < rows.size(); i++) {
            SettingRow r = rows.get(i);
            int rh = rowHeight(r);
            if (drawY + rh >= listTop && drawY <= listBottom) {
                renderRow(matrices, client, r, mouseX, mouseY, centerColX, drawY, centerColW, i);
            }
            drawY += rh;
        }

        // Scrollbar if needed
        if (maxScroll > 0) {
            int barX = centerColX + centerColW + 3;
            DrawableHelper.fill(matrices, barX, listTop, barX + 3, listBottom, 0x33000000);
            int thumbH = Math.max(20, (int) ((float) listHeight * listHeight / Math.max(1, totalH)));
            int thumbY = listTop + (int) ((listHeight - thumbH) * ((float) scrollOffset / maxScroll));
            DrawableHelper.fill(matrices, barX, thumbY, barX + 3, thumbY + thumbH, 0xAACD412B);
        }

        // 4. Right Info Panel (shows hovered setting title & help text)
        int descX = x + w - descColW;
        int descY = topY + 22;
        DrawableHelper.fill(matrices, descX, descY, descX + descColW, descY + 126, 0x6616191E);
        DrawableHelper.fill(matrices, descX, descY, descX + 2, descY + 126, 0xFFCD412B);
        RustFontRenderer.bold().draw(matrices, hoveredTitle, descX + 9, descY + 8, 0xFFF4EFE7);
        int dy = descY + 25;
        for (String line : wrapText(hoveredDesc, descColW - 18, rf)) {
            rf.draw(matrices, line, descX + 9, dy, 0xFF9EA5AE);
            dy += 12;
        }

        // Key capture hint banner
        if (capturingKey != null) {
            int bannerY = y + h - 22;
            DrawableHelper.fill(matrices, centerColX, bannerY, centerColX + centerColW, bannerY + 18, 0xEECD412B);
            String msg = "НАЖМИТЕ ЛЮБУЮ КЛАВИШУ ДЛЯ: " + capturingKey.getTranslationKey() + "  (ESC — ОТМЕНА)";
            bfSmall.drawCenteredY(matrices, bfSmall.trim(msg, centerColW - 12), centerColX + 6, bannerY, 18, 0xFFFFFFFF);
        }
    }

    private int rowHeight(SettingRow r) {
        if (r.kind == RowKind.HEADER) return 19;
        if (r.kind == RowKind.EMPTY_NOTE) return 32;
        return 22;
    }

    private void renderRow(MatrixStack matrices, MinecraftClient client, SettingRow r,
                           int mouseX, int mouseY, int x, int y, int w, int rowIdx) {
        if (r.kind == RowKind.HEADER) {
            RustFontRenderer.boldSmall().drawCenteredY(matrices, r.label, x, y + 3, 14, 0xFF9EA5AE);
            return;
        }
        if (r.kind == RowKind.EMPTY_NOTE) {
            DrawableHelper.fill(matrices, x, y, x + w, y + 26, 0x4414171D);
            RustFontRenderer.regular().drawCenteredY(matrices, r.label, x + 8, y, 26, 0xFF8C929B);
            return;
        }

        int h = 19;
        boolean rowHov = inRect(mouseX, mouseY, x, y, w, h);
        if (rowHov) {
            hoveredTitle = r.label;
            hoveredDesc = r.description;
            playHoverSound(client, "row:" + r.label + ":" + rowIdx);
        }

        DrawableHelper.fill(matrices, x, y, x + w, y + h, rowHov ? 0x88242930 : 0x55181C21);
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        rf.drawCenteredY(matrices, rf.trim(r.label, w - 150), x + 8, y, h, 0xFFEAECEF);

        if (r.kind == RowKind.TOGGLE) {
            int btnW = 66;
            int btnH = 15;
            int btnX = x + w - btnW - 4;
            int btnY = y + 2;
            boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
            int bg = r.boolVal ? (hov ? 0xFFE04E36 : 0xFFCD412B) : (hov ? 0x44FFFFFF : 0xAA252A31);
            DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bg);
            String txt = r.boolVal ? "ВКЛ" : "ВЫКЛ";
            int fg = r.boolVal ? 0xFFFFFFFF : 0xFFA2A8B0;
            bf.drawCenteredY(matrices, txt, btnX + (btnW - bf.getWidth(txt)) / 2f, btnY, btnH, fg);

        } else if (r.kind == RowKind.CYCLE || r.kind == RowKind.BUTTON || r.kind == RowKind.KEYBIND) {
            int btnW = Math.min(140, Math.max(96, (int) (w * 0.34f)));
            int btnH = 15;
            int btnX = x + w - btnW - 4;
            int btnY = y + 2;
            boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
            boolean capturing = (r.kind == RowKind.KEYBIND && r.keyBinding != null && r.keyBinding == capturingKey);
            int bg = capturing ? 0xFFE04E36 : (r.kind == RowKind.BUTTON && r.boolVal
                    ? (hov ? 0xFFE04E36 : 0xFFCD412B)
                    : (hov ? 0xFF343B44 : 0xAA22272E));
            DrawableHelper.fill(matrices, btnX, btnY, btnX + btnW, btnY + btnH, bg);
            String txt = capturing ? "[НАЖМИТЕ КЛАВИШУ]" : bf.trim(r.valueText, btnW - 10);
            bf.drawCenteredY(matrices, txt, btnX + (btnW - bf.getWidth(txt)) / 2f, btnY, btnH, 0xFFFFFFFF);

        } else if (r.kind == RowKind.SLIDER) {
            int trackW = 104;
            int trackH = 5;
            int trackX = x + w - trackW - 6;
            int trackY = y + 7;

            float valW = rf.getWidth(r.valueText);
            rf.drawCenteredY(matrices, r.valueText, trackX - valW - 8, y, h, 0xFFFFFFFF);
            DrawableHelper.fill(matrices, trackX, trackY, trackX + trackW, trackY + trackH, 0xAA101317);
            int fill = Math.round(trackW * Math.max(0f, Math.min(1f, r.ratio)));
            if (fill > 0) {
                DrawableHelper.fill(matrices, trackX, trackY, trackX + fill, trackY + trackH, 0xFFCD412B);
            }
            int knobX = trackX + Math.max(0, Math.min(trackW - 4, fill - 2));
            DrawableHelper.fill(matrices, knobX, trackY - 2, knobX + 4, trackY + trackH + 2, 0xFFFFFFFF);

        } else if (r.kind == RowKind.METER) {
            int barW = 136;
            int barH = 7;
            int barX = x + w - barW - 6;
            int barY = y + 6;
            DrawableHelper.fill(matrices, barX, barY, barX + barW, barY + barH, 0xAA101317);
            int fill = Math.round(barW * Math.max(0f, Math.min(1f, r.ratio)));
            if (fill > 0) {
                int col = r.ratio > 0.7f ? 0xFFE66A2C : 0xFF7EC845;
                DrawableHelper.fill(matrices, barX, barY, barX + fill, barY + barH, col);
            }
        }
    }

    public boolean mouseClicked(Screen parent, MinecraftClient client, double mouseX, double mouseY, int button,
                                int x, int y, int w, int h) {
        if (button != 0 || client == null || client.options == null) {
            return false;
        }
        int catW = 136;
        int catX = x;
        int topY = y;
        int searchH = 18;

        // Search bar focus
        boolean clickedSearch = inRect(mouseX, mouseY, catX, topY, catW, searchH);
        if (clickedSearch) {
            searchFocused = true;
            capturingKey = null;
            playClickSound(client);
            return true;
        }
        searchFocused = false;

        // Left sidebar category clicks
        int curCatY = topY + searchH + 7;
        int itemH = 19;
        for (Category cat : Category.values()) {
            if (inRect(mouseX, mouseY, catX, curCatY, catW, itemH)) {
                selectedCategory = cat;
                searchQuery = "";
                scrollOffset = 0;
                capturingKey = null;
                playClickSound(client);
                return true;
            }
            curCatY += itemH + 3;
        }

        int centerColX = catX + catW + 14;
        int descColW = Math.min(205, Math.max(150, (int) (w * 0.25f)));
        int centerColW = w - (centerColX - x) - descColW - 14;
        int listTop = topY + 22;
        int listBottom = y + h - 4;

        List<SettingRow> rows = buildVisibleRows(parent, client);
        int drawY = listTop - scrollOffset;
        for (SettingRow r : rows) {
            int rh = rowHeight(r);
            if (mouseY >= listTop && mouseY <= listBottom && drawY + rh >= listTop && drawY <= listBottom) {
                int rowH = 19;
                if (r.kind == RowKind.SLIDER) {
                    int trackW = 104;
                    int trackX = centerColX + centerColW - trackW - 6;
                    if (inRect(mouseX, mouseY, trackX - 4, drawY, trackW + 8, rowH)) {
                        activeSlider = r.slider;
                        playClickSound(client);
                        applySliderAt(mouseX, trackX, trackW, r.slider);
                        return true;
                    }
                } else if (r.kind == RowKind.TOGGLE && r.action != null) {
                    int btnW = 66;
                    int btnX = centerColX + centerColW - btnW - 4;
                    if (inRect(mouseX, mouseY, btnX, drawY + 1, btnW, rowH - 2)) {
                        playClickSound(client);
                        r.action.onClick();
                        saveAll(client);
                        return true;
                    }
                } else if ((r.kind == RowKind.CYCLE || r.kind == RowKind.BUTTON || r.kind == RowKind.KEYBIND) && r.action != null) {
                    int btnW = Math.min(140, Math.max(96, (int) (centerColW * 0.34f)));
                    int btnX = centerColX + centerColW - btnW - 4;
                    if (inRect(mouseX, mouseY, btnX, drawY + 1, btnW, rowH - 2)) {
                        playClickSound(client);
                        r.action.onClick();
                        saveAll(client);
                        return true;
                    }
                }
            }
            drawY += rh;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, int x, int w) {
        if (activeSlider == null) return false;
        int catW = 136;
        int centerColX = x + catW + 14;
        int descColW = Math.min(205, Math.max(150, (int) (w * 0.25f)));
        int centerColW = w - (centerColX - x) - descColW - 14;
        int trackW = 104;
        int trackX = centerColX + centerColW - trackW - 6;
        applySliderAt(mouseX, trackX, trackW, activeSlider);
        return true;
    }

    public boolean mouseReleased(MinecraftClient client) {
        if (activeSlider != null) {
            activeSlider = null;
            saveAll(client);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double amount) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (amount * 22)));
        return true;
    }

    public boolean charTyped(char chr) {
        if (searchFocused && chr >= 32 && chr != 127 && searchQuery.length() < 32) {
            searchQuery += chr;
            scrollOffset = 0;
            return true;
        }
        return false;
    }

    public boolean keyPressed(MinecraftClient client, int keyCode, int scanCode) {
        if (capturingKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingKey = null;
                return true;
            }
            capturingKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            KeyBinding.updateKeysByCode();
            saveAll(client);
            capturingKey = null;
            playClickSound(client);
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    scrollOffset = 0;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return false;
    }

    private void applySliderAt(double mouseX, int trackX, int trackW, SliderChange slider) {
        if (slider == null) return;
        float ratio = (float) ((mouseX - trackX) / (double) trackW);
        ratio = Math.max(0f, Math.min(1f, ratio));
        slider.onSlide(ratio);
    }

    private static void saveAll(MinecraftClient client) {
        RustSettings.get().save();
        if (client != null && client.options != null) {
            client.options.write();
        }
    }

    private List<SettingRow> buildVisibleRows(Screen parent, MinecraftClient client) {
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return buildRowsForCategory(selectedCategory, parent, client);
        }
        List<SettingRow> matches = new ArrayList<>();
        for (Category cat : Category.values()) {
            for (SettingRow r : buildRowsForCategory(cat, parent, client)) {
                if (r.kind != RowKind.HEADER && r.kind != RowKind.EMPTY_NOTE
                        && r.label.toLowerCase(Locale.ROOT).contains(q)) {
                    matches.add(r);
                }
            }
        }
        if (matches.isEmpty()) {
            matches.add(new SettingRow(RowKind.EMPTY_NOTE, "По вашему запросу ничего не найдено.", "", "",
                    false, 0f, null, null, null));
        }
        return matches;
    }

    private List<SettingRow> buildRowsForCategory(Category cat, Screen parent, MinecraftClient client) {
        List<SettingRow> out = new ArrayList<>();
        RustSettings cfg = RustSettings.get();

        if (cat == Category.GAMEPLAY) {
            out.add(hdr("ИГРА"));
            out.add(slider("ПОЛЕ ЗРЕНИЯ", String.valueOf((int) Math.round(client.options.fov)),
                    (float) ((client.options.fov - 30.0) / 80.0),
                    "Угол обзора камеры в игре (FOV). Рекомендуемое значение в Rust — 90.",
                    r -> client.options.fov = Math.round(30.0 + r * 80.0)));
            out.add(toggle("ПОКАЧИВАНИЕ КАМЕРЫ", client.options.bobView,
                    "Реалистичное покачивание рук и оружия при движении.",
                    () -> client.options.bobView = !client.options.bobView));
            out.add(toggle("ХИТМАРКЕР", cfg.showHitmarker,
                    "Визуальное подтверждение попадания по цели.",
                    () -> cfg.showHitmarker = !cfg.showHitmarker));
            out.add(slider("ТРЯСКА ОТ ВЗРЫВОВ", Math.round(cfg.explosionShake * 100) + "%", cfg.explosionShake,
                    "Сила тряски экрана при близких взрывах C4 и бочек.",
                    r -> cfg.explosionShake = r));
            out.add(toggle("МУЗЫКА В МЕНЮ", cfg.menuMusicEnabled,
                    "Фоновая музыка главного меню SpaceRust.",
                    () -> cfg.menuMusicEnabled = !cfg.menuMusicEnabled));
            out.add(cycle("ТЕМА МЕНЮ", cfg.menuMusicTrack == 1 ? "SAFE ZONE" : "CLASSIC",
                    "Переключение между саундтреками главного меню Rust.",
                    () -> cfg.menuMusicTrack = (cfg.menuMusicTrack + 1) % 2));
            out.add(toggle("МУЗЫКА В ИГРЕ (RUST)", cfg.inGameMusicEnabled,
                    "Заменяет стандартную музыку Minecraft на саундтрек Rust во время игры.",
                    () -> cfg.inGameMusicEnabled = !cfg.inGameMusicEnabled));

        } else if (cat == Category.AUDIO) {
            out.add(hdr("ГРОМКОСТЬ"));
            addSoundSlider(out, client, "ОБЩАЯ ГРОМКОСТЬ", SoundCategory.MASTER,
                    "Главная громкость всех звуков, музыки и голосового чата.");
            addSoundSlider(out, client, "МУЗЫКА", SoundCategory.MUSIC,
                    "Громкость фоновой музыки в главном меню и игре.");
            out.add(slider("ГРОМКОСТЬ ИНТЕРФЕЙСА", Math.round(cfg.uiVolume * 100) + "%", cfg.uiVolume,
                    "Громкость кликов и наведения на кнопки в меню и настройках (0% — полностью без звука).",
                    r -> cfg.uiVolume = r));
            addSoundSlider(out, client, "ЗВУКИ МИРА И БЛОКОВ", SoundCategory.BLOCKS,
                    "Звуки строительства, добычи руды, костров и дверей.");
            addSoundSlider(out, client, "ИГРОКИ", SoundCategory.PLAYERS,
                    "Звуки шагов, экипировки, выстрелов и перезарядки.");
            addSoundSlider(out, client, "ОКРУЖАЮЩАЯ СРЕДА", SoundCategory.AMBIENT,
                    "Фоновая атмосфера карты, ветра и радиационных зон.");
            addSoundSlider(out, client, "ПОГОДА", SoundCategory.WEATHER,
                    "Звуки дождя, ветра и грозы.");
            addSoundSlider(out, client, "ГОЛОС И УВЕДОМЛЕНИЯ", SoundCategory.VOICE,
                    "Дополнительный канал голосовых эффектов и оповещений.");

        } else if (cat == Category.DISPLAY) {
            out.add(hdr("ЭКРАН"));
            out.add(toggle("ПОЛНЫЙ ЭКРАН", client.options.fullscreen,
                    "Полноэкранный режим отображения игры.",
                    () -> {
                        client.options.fullscreen = !client.options.fullscreen;
                        if (client.getWindow().isFullscreen() != client.options.fullscreen) {
                            client.getWindow().toggleFullscreen();
                        }
                    }));
            String[] resLabels = {"ПО УМОЛЧАНИЮ", "800 x 600", "1280 x 720", "1600 x 900", "1920 x 1080"};
            final int[][] resDims = {{1280, 720}, {800, 600}, {1280, 720}, {1600, 900}, {1920, 1080}};
            out.add(cycle("РАЗРЕШЕНИЕ", resLabels[cfg.resolutionIndex],
                    "Разрешение игрового окна (применяется в оконном режиме).",
                    () -> {
                        cfg.resolutionIndex = (cfg.resolutionIndex + 1) % resLabels.length;
                        if (!client.getWindow().isFullscreen() && cfg.resolutionIndex > 0) {
                            long handle = client.getWindow().getHandle();
                            GLFW.glfwSetWindowSize(handle, resDims[cfg.resolutionIndex][0], resDims[cfg.resolutionIndex][1]);
                        }
                    }));

            out.add(hdr("ЧАСТОТА КАДРОВ"));
            int fps = client.options.maxFps;
            String fpsTxt = fps >= 255 ? "НЕОГРАНИЧЕННО" : (fps + " FPS");
            out.add(slider("ЛИМИТ КАДРОВ", fpsTxt, (float) ((fps - 10) / 250.0),
                    "Ограничение максимального числа кадров в секунду.",
                    r -> {
                        int val = Math.round(10 + r * 250);
                        client.options.maxFps = val >= 255 ? 260 : val;
                    }));
            out.add(toggle("ВЕРТИКАЛЬНАЯ СИНХРОНИЗАЦИЯ", client.options.enableVsync,
                    "Синхронизирует частоту кадров с частотой обновления монитора (VSync).",
                    () -> {
                        client.options.enableVsync = !client.options.enableVsync;
                        client.getWindow().setVsync(client.options.enableVsync);
                    }));

        } else if (cat == Category.GRAPHICS) {
            out.add(hdr("МИР"));
            String[] grassNames = {"ВЫКЛ", "БЫСТРАЯ", "ГУСТАЯ"};
            out.add(cycle("ТРАВА", grassNames[cfg.grassMode],
                    "Плотность и детализация травы и растительности в мире.",
                    () -> cfg.grassMode = (cfg.grassMode + 1) % 3));
            out.add(slider("ДИСТАНЦИЯ ПРОРИСОВКИ", client.options.viewDistance + " ЧАНКОВ",
                    (float) ((client.options.viewDistance - 2) / 30.0),
                    "Дальность прорисовки карты и объектов.",
                    r -> client.options.viewDistance = Math.max(2, Math.min(32, Math.round(2 + r * 30)))));
            String gfxLabel = client.options.graphicsMode == GraphicsMode.FAST ? "БЫСТРАЯ"
                    : (client.options.graphicsMode == GraphicsMode.FABULOUS ? "НЕВЕРОЯТНАЯ" : "КРАСИВАЯ");
            out.add(cycle("КАЧЕСТВО ГРАФИКИ", gfxLabel,
                    "Общее качество листвы, воды и прозрачности.",
                    () -> {
                        GraphicsMode[] vals = GraphicsMode.values();
                        client.options.graphicsMode = vals[(client.options.graphicsMode.ordinal() + 1) % vals.length];
                        client.worldRenderer.reload();
                    }));
            String mipTxt = client.options.mipmapLevels == 0 ? "ВЫКЛ" : (client.options.mipmapLevels >= 4 ? "ИСХОДНОЕ" : (client.options.mipmapLevels + "x"));
            out.add(cycle("КАЧЕСТВО ТЕКСТУР", mipTxt,
                    "Четкость текстур на расстоянии (уровни mipmap).",
                    () -> client.options.mipmapLevels = (client.options.mipmapLevels + 1) % 5));
            String aoTxt = client.options.ao == AoMode.OFF ? "ВЫКЛ"
                    : (client.options.ao == AoMode.MIN ? "МИНИМУМ" : "МАКСИМУМ");
            out.add(cycle("МЯГКОЕ ОСВЕЩЕНИЕ", aoTxt,
                    "Сглаживание теней в углах и углублениях блоков.",
                    () -> client.options.ao = AoMode.byId((client.options.ao.getId() + 1) % 3)));
            String cloudTxt = client.options.cloudRenderMode == CloudRenderMode.OFF ? "ВЫКЛ"
                    : (client.options.cloudRenderMode == CloudRenderMode.FAST ? "БЫСТРЫЕ" : "КРАСИВЫЕ");
            out.add(cycle("ОБЛАКА", cloudTxt,
                    "Стиль и объемность неба и облаков.",
                    () -> client.options.cloudRenderMode = CloudRenderMode.byId((client.options.cloudRenderMode.getId() + 1) % 3)));
            String partTxt = client.options.particles == ParticlesMode.ALL ? "ВСЕ"
                    : (client.options.particles == ParticlesMode.DECREASED ? "МЕНЬШЕ" : "МИНИМУМ");
            out.add(cycle("ЧАСТИЦЫ", partTxt,
                    "Количество частиц от выстрелов, дыма и костров.",
                    () -> client.options.particles = ParticlesMode.byId((client.options.particles.getId() + 1) % 3)));
            out.add(slider("УРОВЕНЬ ДЕТАЛИЗАЦИИ", String.valueOf(Math.max(1, client.options.biomeBlendRadius)),
                    (float) (client.options.biomeBlendRadius / 7.0),
                    "Плавность переходов биомов и дистанция отображения деталей.",
                    r -> client.options.biomeBlendRadius = Math.round(r * 7)));
            out.add(toggle("БУФЕРЫ ВИДЕОПАМЯТИ", client.options.entityShadows,
                    "Аппаратная буферизация геометрии и теней сущностей.",
                    () -> client.options.entityShadows = !client.options.entityShadows));

        } else if (cat == Category.EFFECTS) {
            out.add(hdr("ИЗОБРАЖЕНИЕ"));
            out.add(slider("НАСЫЩЕННОСТЬ", String.format(Locale.US, "%.1f", cfg.saturation), cfg.saturation / 2.0f,
                    "Интенсивность и сочность цветов картинки.",
                    r -> cfg.saturation = r * 2.0f));
            out.add(slider("ЯРКОСТЬ", Math.round(client.options.gamma * 100) + "%", (float) client.options.gamma,
                    "Гамма и видимость в темное время суток.",
                    r -> client.options.gamma = r));

            out.add(hdr("ПОСТОБРАБОТКА"));
            out.add(toggle("ПОСТОБРАБОТКА", cfg.postProcessing,
                    "Общий переключатель шейдерной постобработки SpaceRust.",
                    () -> cfg.postProcessing = !cfg.postProcessing));
            out.add(toggle("ЗАТЕМНЕНИЕ УГЛОВ", cfg.vignette,
                    "Мягкая виньетка по краям экрана.",
                    () -> cfg.vignette = !cfg.vignette));
            out.add(slider("СИЛА ЗАТЕМНЕНИЯ", Math.round(cfg.vignetteStrength * 100) + "%", cfg.vignetteStrength,
                    "Интенсивность затемнения краев изображения.",
                    r -> cfg.vignetteStrength = r));
            out.add(toggle("ЦВЕТОКОРРЕКЦИЯ", cfg.colorCorrection,
                    "Кинематографичная цветовая палитра (LUT).",
                    () -> cfg.colorCorrection = !cfg.colorCorrection));
            out.add(slider("СИЛА ЦВЕТОКОРРЕКЦИИ", Math.round(cfg.colorCorrectionStrength * 100) + "%", cfg.colorCorrectionStrength,
                    "Степень применения LUT цветокоррекции.",
                    r -> cfg.colorCorrectionStrength = r));
            out.add(toggle("ЭКСПОНЕНЦИАЛЬНЫЙ ТУМАН", cfg.expFog,
                    "Атмосферный туман на горизонте карты.",
                    () -> cfg.expFog = !cfg.expFog));
            out.add(toggle("ПЛАВНОЕ ОСВЕЩЕНИЕ МОДЕЛЕЙ", cfg.modelLighting,
                    "Плавное освещение 3D моделей руды, ящиков и оружия.",
                    () -> cfg.modelLighting = !cfg.modelLighting));
            String[] styles = {"Производительность", "Баланс", "Качество"};
            out.add(cycle("СТИЛЬ МИРА", styles[cfg.worldStyle],
                    "Предустановка визуального баланса освещения и теней.",
                    () -> cfg.worldStyle = (cfg.worldStyle + 1) % styles.length));
            out.add(toggle("ВЕТЕР", cfg.wind,
                    "Атмосферное колыхание растительности.",
                    () -> cfg.wind = !cfg.wind));
            out.add(toggle("СЧЁТЧИК КАДРОВ", cfg.showFps,
                    "Отображать FPS в главном меню и интерфейсе.",
                    () -> cfg.showFps = !cfg.showFps));

        } else if (cat == Category.INTERFACE) {
            out.add(hdr("ИНТЕРФЕЙС"));
            String[] guiNames = {"АВТО", "МАЛЕНЬКИЙ (1)", "СРЕДНИЙ (RUST 2)", "БОЛЬШОЙ (3)"};
            out.add(cycle("РАЗМЕР ИНТЕРФЕЙСА", guiNames[cfg.guiScaleOverride],
                    "Масштаб HUD, инвентаря и меню настроек (по умолчанию 2).",
                    () -> {
                        cfg.guiScaleOverride = (cfg.guiScaleOverride + 1) % 4;
                        client.onResolutionChanged();
                    }));
            out.add(toggle("ПОДСКАЗКИ ПРЕДМЕТОВ", client.options.heldItemTooltips,
                    "Показывать всплывающие названия предметов при переключении слотов.",
                    () -> client.options.heldItemTooltips = !client.options.heldItemTooltips));
            out.add(toggle("СУБТИТРЫ", client.options.showSubtitles,
                    "Текстовые субтитры окружающих звуков.",
                    () -> client.options.showSubtitles = !client.options.showSubtitles));
            out.add(button("ЯЗЫК ИНТЕРФЕЙСА", "ВЫБРАТЬ ЯЗЫК", false,
                    "Открыть стандартное меню выбора языка игры.",
                    () -> client.openScreen(new LanguageOptionsScreen(parent, client.options, client.getLanguageManager()))));

        } else if (cat == Category.CONTROLS) {
            out.add(hdr("ОБЩЕЕ"));
            int sens = (int) Math.round(client.options.mouseSensitivity * 200.0);
            out.add(slider("ЧУВСТВИТЕЛЬНОСТЬ МЫШИ", sens + "%", (float) client.options.mouseSensitivity,
                    "Скорость поворота камеры и прицеливания.",
                    r -> client.options.mouseSensitivity = r));
            out.add(toggle("ИНВЕРСИЯ МЫШИ", client.options.invertYMouse,
                    "Инвертировать вертикальную ось обзора мыши.",
                    () -> client.options.invertYMouse = !client.options.invertYMouse));
            out.add(toggle("АВТОПРЫЖОК", client.options.autoJump,
                    "Автоматический прыжок перед препятствиями в 1 блок.",
                    () -> client.options.autoJump = !client.options.autoJump));

            out.add(hdr("НАЗНАЧЕНИЕ КЛАВИШ"));
            addKeyRow(out, "БЫСТРЫЙ ЛУТ (УДЕРЖИВАТЬ)", SpaceRustClient.keyQuickLoot,
                    "Удерживайте и наводите на предметы в ящике или инвентаре для быстрого перемещения (по умолчанию H).");
            addKeyRow(out, "ГОЛОСОВОЙ ЧАТ (УДЕРЖИВАТЬ)", SpaceRustClient.keyVoice,
                    "Активация микрофона в ближайшем голосовом чате (по умолчанию V).");
            addKeyRow(out, "ОТКРЫТЬ КАРТУ РТ", SpaceRustClient.keyMap,
                    "Открыть глобальную карту монументов SpaceRust (по умолчанию G).");
            addKeyRow(out, "ПЕРЕЗАРЯДКА ОРУЖИЯ", SpaceRustClient.keyReload,
                    "Перезарядить текущее огнестрельное оружие (по умолчанию R).");
            addKeyRow(out, "ВПЕРЁД", client.options.keyForward, "Движение персонажа вперед.");
            addKeyRow(out, "НАЗАД", client.options.keyBack, "Движение персонажа назад.");
            addKeyRow(out, "ВЛЕВО", client.options.keyLeft, "Шаг влево.");
            addKeyRow(out, "ВПРАВО", client.options.keyRight, "Шаг вправо.");
            addKeyRow(out, "ПРЫЖОК", client.options.keyJump, "Прыжок персонажа.");
            addKeyRow(out, "ПРИСЕСТЬ", client.options.keySneak, "Подкрадывание и приседание.");
            addKeyRow(out, "БЕГ / СПРИНТ", client.options.keySprint, "Ускоренный бег.");
            addKeyRow(out, "ИНВЕНТАРЬ", client.options.keyInventory, "Открыть инвентарь Rust.");
            out.add(button("ВСЕ КЛАВИШИ MINECRAFT", "ОТКРЫТЬ КЛАВИШИ", false,
                    "Полный список всех стандартных клавиш Minecraft.",
                    () -> client.openScreen(new ControlsOptionsScreen(parent, client.options))));

        } else if (cat == Category.EXPERIMENTAL) {
            out.add(hdr("ЭКСПЕРИМЕНТАЛЬНОЕ"));
            out.add(new SettingRow(RowKind.EMPTY_NOTE, "Настройки появятся позже.", "",
                    "Экспериментальные функции и бета-шейдеры появятся в будущих обновлениях.",
                    false, 0f, null, null, null));

        } else if (cat == Category.VOICE) {
            ensureAudioDevicesFresh();
            out.add(hdr("VOICE CHAT"));
            out.add(toggle("ГОЛОСОВОЙ ЧАТ", cfg.voiceEnabled,
                    "Включить или выключить 3D голосовой чат SpaceRust.",
                    () -> {
                        cfg.voiceEnabled = !cfg.voiceEnabled;
                        RustVoiceEngine.get().invalidateDevices();
                    }));
            out.add(cycle("МИКРОФОН", RustVoiceEngine.displayDeviceName(cfg.voiceInputDevice),
                    "Выбор устройства записи речи (микрофона) Windows.",
                    () -> {
                        List<String> devs = cachedInputDevices;
                        if (devs != null && !devs.isEmpty()) {
                            int idx = devs.indexOf(cfg.voiceInputDevice);
                            cfg.voiceInputDevice = devs.get((idx + 1) % devs.size());
                            RustVoiceEngine.get().invalidateDevices();
                        }
                    }));
            out.add(cycle("НАУШНИКИ / УСТРОЙСТВО ВЫВОДА", RustVoiceEngine.displayDeviceName(cfg.voiceOutputDevice),
                    "Устройство воспроизведения голосов других игроков.",
                    () -> {
                        List<String> devs = cachedOutputDevices;
                        if (devs != null && !devs.isEmpty()) {
                            int idx = devs.indexOf(cfg.voiceOutputDevice);
                            cfg.voiceOutputDevice = devs.get((idx + 1) % devs.size());
                            RustVoiceEngine.get().invalidateDevices();
                        }
                    }));
            out.add(slider("ГРОМКОСТЬ ИГРОКОВ", Math.round(cfg.voicePlayerVolume * 100) + "%", cfg.voicePlayerVolume / 2.0f,
                    "Общая громкость речи игроков вокруг вас (до 200%).",
                    r -> cfg.voicePlayerVolume = r * 2.0f));
            out.add(slider("УСИЛЕНИЕ МИКРОФОНА", Math.round(cfg.voiceMicGain * 100) + "%",
                    (cfg.voiceMicGain - 0.2f) / 2.8f,
                    "Чувствительность и предусиление вашего микрофона.",
                    r -> cfg.voiceMicGain = 0.2f + r * 2.8f));
            out.add(slider("ШУМОДАВ (ПОРОГ)", Math.round((cfg.voiceNoiseGate / 0.12f) * 100) + "%",
                    cfg.voiceNoiseGate / 0.12f,
                    "Порог отсечения фонового шума клавиатуры и дыхания.",
                    r -> cfg.voiceNoiseGate = r * 0.12f));

            boolean loop = RustVoiceEngine.get().isLoopbackTest();
            out.add(button("ТЕСТ МИКРОФОНА (ПЕТЛЯ)", loop ? "ОСТАНОВИТЬ" : "ПРОВЕРИТЬ", loop,
                    "Включить прослушивание собственного микрофона в наушниках прямо здесь.",
                    () -> RustVoiceEngine.get().setLoopbackTest(!RustVoiceEngine.get().isLoopbackTest())));

            float lvl = Math.min(1f, RustVoiceEngine.get().getLiveMicLevel() * 4.5f);
            out.add(new SettingRow(RowKind.METER, "УРОВЕНЬ МИКРОФОНА", "",
                    "Индикатор входного сигнала вашего микрофона в реальном времени.",
                    false, lvl, null, null, null));

            addKeyRow(out, "КЛАВИША ПЕРЕДАЧИ ГОЛОСА", SpaceRustClient.keyVoice,
                    "Удерживайте эту клавишу в игре для общения (по умолчанию V).");
        }
        return out;
    }

    private void addSoundSlider(List<SettingRow> out, MinecraftClient client, String title, SoundCategory sc, String desc) {
        float vol = client.options.getSoundVolume(sc);
        out.add(slider(title, Math.round(vol * 100) + "%", vol, desc, r -> {
            client.options.setSoundVolume(sc, r);
            if (client.getSoundManager() != null) {
                client.getSoundManager().updateSoundVolume(sc, r);
            }
        }));
    }

    private void addKeyRow(List<SettingRow> out, String label, KeyBinding kb, String desc) {
        if (kb == null) return;
        String bound = kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT);
        out.add(new SettingRow(RowKind.KEYBIND, label, bound, desc, false, 0f,
                () -> capturingKey = (capturingKey == kb ? null : kb), null, kb));
    }

    private static SettingRow hdr(String title) {
        return new SettingRow(RowKind.HEADER, title, "", "", false, 0f, null, null, null);
    }

    private static SettingRow toggle(String label, boolean val, String desc, RowAction act) {
        return new SettingRow(RowKind.TOGGLE, label, "", desc, val, 0f, act, null, null);
    }

    private static SettingRow cycle(String label, String valText, String desc, RowAction act) {
        return new SettingRow(RowKind.CYCLE, label, valText, desc, false, 0f, act, null, null);
    }

    private static SettingRow button(String label, String btnText, boolean activeHighlight, String desc, RowAction act) {
        return new SettingRow(RowKind.BUTTON, label, btnText, desc, activeHighlight, 0f, act, null, null);
    }

    private static SettingRow slider(String label, String valText, float ratio, String desc, SliderChange sc) {
        return new SettingRow(RowKind.SLIDER, label, valText, desc, false, ratio, null, sc, null);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static List<String> wrapText(String text, float maxWidth, RustFontRenderer font) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() > 0 && font.getWidth(cur + " " + w) > maxWidth) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                if (cur.length() > 0) cur.append(" ");
                cur.append(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
