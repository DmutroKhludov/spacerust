package net.spacerust.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.sound.SoundCategory;
import net.spacerust.client.SpaceRustClient;
import net.spacerust.client.config.RustSettings;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared SpaceRust Settings Panel for 1.21.11 used in Main Menu and Escape Pause Menu.
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
        EXPERIMENTAL("ЭКСПЕРИМЕНТАЛЬНОЕ");

        public final String title;
        Category(String title) { this.title = title; }
    }

    private interface RowAction { void onClick(); }
    private interface SliderChange { void onSlide(float ratio); }

    private enum RowKind { HEADER, SLIDER, TOGGLE, CYCLE, BUTTON, KEYBIND, EMPTY_NOTE }

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
    private String hoveredDesc = "Угол обзора камеры в игре (FOV). Рекомендуемое значение в Rust — 90.";
    private String lastHoverKey = "";
    private SliderChange activeSlider = null;
    private KeyBinding capturingKey = null;

    public Category getSelectedCategory() { return selectedCategory; }
    public boolean isCapturingInput() { return searchFocused || capturingKey != null; }

    public void onClosed() {
        searchFocused = false;
        capturingKey = null;
        activeSlider = null;
    }

    private void playClickSound(MinecraftClient client) { RustUiHelper.playMenuClick(client); }

    private void playHoverSound(MinecraftClient client, String hoverKey) {
        if (hoverKey == null || hoverKey.equals(lastHoverKey)) return;
        lastHoverKey = hoverKey;
        RustUiHelper.playMenuHover(client);
    }

    public void render(DrawContext ctx, Screen parent, MinecraftClient client,
                       int mouseX, int mouseY, int x, int y, int w, int h) {
        if (client == null || client.options == null) return;

        int catW = 136;
        int catX = x;
        int topY = y;

        // 1. Search box
        int searchH = 18;
        ctx.fill(catX, topY, catX + catW, topY + searchH, 0xCC12151B);
        ctx.fill(catX, topY + searchH - 1, catX + catW, topY + searchH, searchFocused ? 0xFFCD412B : 0x22FFFFFF);
        RustFontRenderer rf = RustFontRenderer.regular();
        if (searchQuery.isEmpty()) {
            rf.drawCenteredY(ctx, searchFocused ? "|" : "ПОИСК...", catX + 7, topY, searchH, 0x66B8B8B8);
        } else {
            rf.drawCenteredY(ctx, rf.trim(searchQuery, catW - 14) + (searchFocused ? "|" : ""),
                    catX + 7, topY, searchH, 0xFFFFFFFF);
        }

        // 2. Categories
        int curCatY = topY + searchH + 7;
        int itemH = 19;
        RustFontRenderer bfSmall = RustFontRenderer.boldSmall();
        for (Category cat : Category.values()) {
            boolean active = (cat == selectedCategory) && searchQuery.trim().isEmpty();
            boolean hov = inRect(mouseX, mouseY, catX, curCatY, catW, itemH);
            if (hov) playHoverSound(client, "cat:" + cat.name());
            int bg = active ? 0xFFCD412B : (hov ? 0x33FFFFFF : 0x4415181E);
            ctx.fill(catX, curCatY, catX + catW, curCatY + itemH, bg);
            int txtColor = active ? 0xFFFFFFFF : (hov ? 0xFFEAECEF : 0xFF9EA5AE);
            bfSmall.drawCenteredY(ctx, cat.title, catX + 8, curCatY, itemH, txtColor);
            curCatY += itemH + 3;
        }

        // 3. Center Settings List
        int centerColX = catX + catW + 14;
        int descColW = Math.min(205, Math.max(150, (int) (w * 0.25f)));
        int centerColW = w - (centerColX - x) - descColW - 14;
        int listTop = topY + 22;
        int listBottom = y + h - 4;
        int listHeight = Math.max(80, listBottom - listTop);

        String headerTitle = searchQuery.trim().isEmpty() ? selectedCategory.title : "РЕЗУЛЬТАТЫ ПОИСКА";
        RustFontRenderer.boldLarge().draw(ctx, headerTitle, centerColX, topY, 0xFFF2EDE4);

        List<SettingRow> rows = buildVisibleRows(parent, client);
        int totalH = 0;
        for (SettingRow r : rows) totalH += rowHeight(r);
        maxScroll = Math.max(0, totalH - listHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int drawY = listTop - scrollOffset;
        for (int i = 0; i < rows.size(); i++) {
            SettingRow r = rows.get(i);
            int rh = rowHeight(r);
            if (drawY + rh >= listTop && drawY <= listBottom) {
                renderRow(ctx, client, r, mouseX, mouseY, centerColX, drawY, centerColW, i);
            }
            drawY += rh;
        }

        if (maxScroll > 0) {
            int barX = centerColX + centerColW + 3;
            ctx.fill(barX, listTop, barX + 3, listBottom, 0x33000000);
            int thumbH = Math.max(20, (int) ((float) listHeight * listHeight / Math.max(1, totalH)));
            int thumbY = listTop + (int) ((listHeight - thumbH) * ((float) scrollOffset / maxScroll));
            ctx.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xAACD412B);
        }

        // 4. Right Info Panel
        int descX = x + w - descColW;
        int descY = topY + 22;
        ctx.fill(descX, descY, descX + descColW, descY + 126, 0x6616191E);
        ctx.fill(descX, descY, descX + 2, descY + 126, 0xFFCD412B);
        RustFontRenderer.bold().draw(ctx, hoveredTitle, descX + 9, descY + 8, 0xFFF4EFE7);
        int dy = descY + 25;
        for (String line : wrapText(hoveredDesc, descColW - 18, rf)) {
            rf.draw(ctx, line, descX + 9, dy, 0xFF9EA5AE);
            dy += 12;
        }

        if (capturingKey != null) {
            int bannerY = y + h - 22;
            ctx.fill(centerColX, bannerY, centerColX + centerColW, bannerY + 18, 0xEECD412B);
            String msg = "НАЖМИТЕ ЛЮБУЮ КЛАВИШУ ДЛЯ: " + capturingKey.getId() + "  (ESC — ОТМЕНА)";
            bfSmall.drawCenteredY(ctx, bfSmall.trim(msg, centerColW - 12), centerColX + 6, bannerY, 18, 0xFFFFFFFF);
        }
    }

    private int rowHeight(SettingRow r) {
        if (r.kind == RowKind.HEADER) return 19;
        if (r.kind == RowKind.EMPTY_NOTE) return 32;
        return 22;
    }

    private void renderRow(DrawContext ctx, MinecraftClient client, SettingRow r,
                           int mouseX, int mouseY, int x, int y, int w, int rowIdx) {
        if (r.kind == RowKind.HEADER) {
            RustFontRenderer.boldSmall().drawCenteredY(ctx, r.label, x, y + 3, 14, 0xFF9EA5AE);
            return;
        }
        if (r.kind == RowKind.EMPTY_NOTE) {
            ctx.fill(x, y, x + w, y + 26, 0x4414171D);
            RustFontRenderer.regular().drawCenteredY(ctx, r.label, x + 8, y, 26, 0xFF8C929B);
            return;
        }

        int h = 19;
        boolean rowHov = inRect(mouseX, mouseY, x, y, w, h);
        if (rowHov) {
            hoveredTitle = r.label;
            hoveredDesc = r.description;
            playHoverSound(client, "row:" + r.label + ":" + rowIdx);
        }

        ctx.fill(x, y, x + w, y + h, rowHov ? 0x88242930 : 0x55181C21);
        RustFontRenderer rf = RustFontRenderer.regular();
        RustFontRenderer bf = RustFontRenderer.boldSmall();
        rf.drawCenteredY(ctx, rf.trim(r.label, w - 150), x + 8, y, h, 0xFFEAECEF);

        if (r.kind == RowKind.TOGGLE) {
            int btnW = 66;
            int btnH = 15;
            int btnX = x + w - btnW - 4;
            int btnY = y + 2;
            boolean hov = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
            int bg = r.boolVal ? (hov ? 0xFFE04E36 : 0xFFCD412B) : (hov ? 0x44FFFFFF : 0xAA252A31);
            ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);
            String txt = r.boolVal ? "ВКЛ" : "ВЫКЛ";
            int fg = r.boolVal ? 0xFFFFFFFF : 0xFFA2A8B0;
            bf.drawCenteredY(ctx, txt, btnX + (btnW - bf.getWidth(txt)) / 2f, btnY, btnH, fg);

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
            ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);
            String txt = capturing ? "[НАЖМИТЕ КЛАВИШУ]" : bf.trim(r.valueText, btnW - 10);
            bf.drawCenteredY(ctx, txt, btnX + (btnW - bf.getWidth(txt)) / 2f, btnY, btnH, 0xFFFFFFFF);

        } else if (r.kind == RowKind.SLIDER) {
            int trackW = 104;
            int trackH = 5;
            int trackX = x + w - trackW - 6;
            int trackY = y + 7;

            float valW = rf.getWidth(r.valueText);
            rf.drawCenteredY(ctx, r.valueText, trackX - valW - 8, y, h, 0xFFFFFFFF);
            ctx.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xAA101317);
            int fill = Math.round(trackW * Math.max(0f, Math.min(1f, r.ratio)));
            if (fill > 0) ctx.fill(trackX, trackY, trackX + fill, trackY + trackH, 0xFFCD412B);
            int knobX = trackX + Math.max(0, Math.min(trackW - 4, fill - 2));
            ctx.fill(knobX, trackY - 2, knobX + 4, trackY + trackH + 2, 0xFFFFFFFF);
        }
    }

    public boolean mouseClicked(Screen parent, MinecraftClient client, double mouseX, double mouseY, int button,
                                int x, int y, int w, int h) {
        if (button != 0 || client == null || client.options == null) return false;
        int catW = 136;
        int catX = x;
        int topY = y;
        int searchH = 18;

        if (inRect(mouseX, mouseY, catX, topY, catW, searchH)) {
            searchFocused = true;
            capturingKey = null;
            playClickSound(client);
            return true;
        }
        searchFocused = false;

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

    public boolean keyPressed(MinecraftClient client, KeyInput input) {
        int keyCode = input.key();
        if (capturingKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingKey = null;
                return true;
            }
            capturingKey.setBoundKey(InputUtil.fromKeyCode(input));
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
        float ratio = Math.max(0f, Math.min(1f, (float) ((mouseX - trackX) / (double) trackW)));
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
        if (q.isEmpty()) return buildRowsForCategory(selectedCategory, parent, client);
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
            matches.add(new SettingRow(RowKind.EMPTY_NOTE, "По вашему запросу ничего не найдено.", "", "", false, 0f, null, null, null));
        }
        return matches;
    }

    private List<SettingRow> buildRowsForCategory(Category cat, Screen parent, MinecraftClient client) {
        List<SettingRow> out = new ArrayList<>();
        RustSettings cfg = RustSettings.get();

        if (cat == Category.GAMEPLAY) {
            out.add(hdr("ИГРА"));
            int fov = client.options.getFov().getValue();
            out.add(slider("ПОЛЕ ЗРЕНИЯ", String.valueOf(fov), (fov - 30.0f) / 80.0f,
                    "Угол обзора камеры в игре (FOV). Рекомендуемое значение в Rust — 90.",
                    r -> client.options.getFov().setValue(Math.round(30.0f + r * 80.0f))));
            boolean bob = client.options.getBobView().getValue();
            out.add(toggle("ПОКАЧИВАНИЕ КАМЕРЫ", bob,
                    "Реалистичное покачивание рук и оружия при движении.",
                    () -> client.options.getBobView().setValue(!bob)));
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
            addSoundSlider(out, client, "ОБЩАЯ ГРОМКОСТЬ", SoundCategory.MASTER, "Главная громкость всех звуков.");
            addSoundSlider(out, client, "МУЗЫКА", SoundCategory.MUSIC, "Громкость фоновой музыки в меню и игре.");
            out.add(slider("ГРОМКОСТЬ ИНТЕРФЕЙСА", Math.round(cfg.uiVolume * 100) + "%", cfg.uiVolume,
                    "Громкость кликов и наведения на кнопки в меню и настройках.",
                    r -> cfg.uiVolume = r));
            addSoundSlider(out, client, "ЗВУКИ МИРА И БЛОКОВ", SoundCategory.BLOCKS, "Звуки строительства, добычи руды и дверей.");
            addSoundSlider(out, client, "ИГРОКИ", SoundCategory.PLAYERS, "Звуки шагов, выстрелов и перезарядки.");
            addSoundSlider(out, client, "ОКРУЖАЮЩАЯ СРЕДА", SoundCategory.AMBIENT, "Фоновая атмосфера карты.");
            addSoundSlider(out, client, "ПОГОДА", SoundCategory.WEATHER, "Звуки дождя и ветра.");
            addSoundSlider(out, client, "ГОЛОС И УВЕДОМЛЕНИЯ", SoundCategory.VOICE, "Канал голосовых эффектов.");

        } else if (cat == Category.DISPLAY) {
            out.add(hdr("ЭКРАН"));
            boolean fs = client.options.getFullscreen().getValue();
            out.add(toggle("ПОЛНЫЙ ЭКРАН", fs, "Полноэкранный режим отображения игры.", () -> {
                client.options.getFullscreen().setValue(!fs);
                if (client.getWindow().isFullscreen() != !fs) client.getWindow().toggleFullscreen();
            }));
            String[] resLabels = {"ПО УМОЛЧАНИЮ", "800 x 600", "1280 x 720", "1600 x 900", "1920 x 1080"};
            final int[][] resDims = {{1280, 720}, {800, 600}, {1280, 720}, {1600, 900}, {1920, 1080}};
            out.add(cycle("РАЗРЕШЕНИЕ", resLabels[cfg.resolutionIndex],
                    "Разрешение игрового окна (в оконном режиме).",
                    () -> {
                        cfg.resolutionIndex = (cfg.resolutionIndex + 1) % resLabels.length;
                        if (!client.getWindow().isFullscreen() && cfg.resolutionIndex > 0) {
                            GLFW.glfwSetWindowSize(client.getWindow().getHandle(), resDims[cfg.resolutionIndex][0], resDims[cfg.resolutionIndex][1]);
                        }
                    }));
            out.add(hdr("ЧАСТОТА КАДРОВ"));
            int fps = client.options.getMaxFps().getValue();
            String fpsTxt = fps >= 260 ? "НЕОГРАНИЧЕННО" : (fps + " FPS");
            out.add(slider("ЛИМИТ КАДРОВ", fpsTxt, (fps - 10) / 250.0f, "Максимальный FPS.", r -> {
                int val = Math.round(10 + r * 250);
                client.options.getMaxFps().setValue(val >= 255 ? 260 : val);
            }));
            boolean vs = client.options.getEnableVsync().getValue();
            out.add(toggle("ВЕРТИКАЛЬНАЯ СИНХРОНИЗАЦИЯ", vs, "Синхронизация с монитором (VSync).", () -> {
                client.options.getEnableVsync().setValue(!vs);
                client.getWindow().setVsync(!vs);
            }));

        } else if (cat == Category.GRAPHICS) {
            out.add(hdr("МИР"));
            int vd = client.options.getViewDistance().getValue();
            out.add(slider("ДИСТАНЦИЯ ПРОРИСОВКИ", vd + " ЧАНКОВ", (vd - 2) / 30.0f,
                    "Дальность прорисовки карты и построек.",
                    r -> client.options.getViewDistance().setValue(Math.max(2, Math.min(32, Math.round(2 + r * 30))))));
            GraphicsMode gm = client.options.getPreset().getValue();
            String gfxLabel = gm == GraphicsMode.FAST ? "БЫСТРАЯ" : (gm == GraphicsMode.FABULOUS ? "НЕВЕРОЯТНАЯ" : "КРАСИВАЯ");
            out.add(cycle("КАЧЕСТВО ГРАФИКИ", gfxLabel, "Общее качество графики.", () -> {
                GraphicsMode[] vals = GraphicsMode.values();
                client.options.getPreset().setValue(vals[(gm.ordinal() + 1) % vals.length]);
                client.worldRenderer.reload();
            }));
            int mip = client.options.getMipmapLevels().getValue();
            out.add(cycle("КАЧЕСТВО ТЕКСТУР", mip == 0 ? "ВЫКЛ" : (mip + "x"), "Уровни mipmap.",
                    () -> client.options.getMipmapLevels().setValue((mip + 1) % 5)));
            boolean ao = client.options.getAo().getValue();
            out.add(toggle("МЯГКОЕ ОСВЕЩЕНИЕ", ao, "Сглаживание теней блоков.",
                    () -> client.options.getAo().setValue(!ao)));
            CloudRenderMode cm = client.options.getCloudRenderMode().getValue();
            out.add(cycle("ОБЛАКА", cm.getText().getString().toUpperCase(Locale.ROOT), "Стиль облаков.", () -> {
                CloudRenderMode[] vals = CloudRenderMode.values();
                client.options.getCloudRenderMode().setValue(vals[(cm.ordinal() + 1) % vals.length]);
            }));
            ParticlesMode pm = client.options.getParticles().getValue();
            out.add(cycle("ЧАСТИЦЫ", pm.getText().getString().toUpperCase(Locale.ROOT), "Детализация частиц.", () -> {
                ParticlesMode[] vals = ParticlesMode.values();
                client.options.getParticles().setValue(vals[(pm.ordinal() + 1) % vals.length]);
            }));
            boolean es = client.options.getEntityShadows().getValue();
            out.add(toggle("ТЕНИ СУЩНОСТЕЙ", es, "Отображать тени под игроками и предметами.",
                    () -> client.options.getEntityShadows().setValue(!es)));

        } else if (cat == Category.EFFECTS) {
            out.add(hdr("ИЗОБРАЖЕНИЕ"));
            double gamma = client.options.getGamma().getValue();
            out.add(slider("ЯРКОСТЬ", Math.round(gamma * 100) + "%", (float) Math.min(1.0, gamma),
                    "Яркость и видимость ночью.", r -> client.options.getGamma().setValue((double) r)));
            out.add(toggle("СЧЁТЧИК КАДРОВ", cfg.showFps,
                    "Отображать FPS в главном меню.", () -> cfg.showFps = !cfg.showFps));

        } else if (cat == Category.INTERFACE) {
            out.add(hdr("ИНТЕРФЕЙС"));
            String[] guiNames = {"АВТО", "МАЛЕНЬКИЙ (1)", "СРЕДНИЙ (RUST 2)", "БОЛЬШОЙ (3)"};
            out.add(cycle("РАЗМЕР ИНТЕРФЕЙСА (GUI)", guiNames[cfg.guiScaleOverride],
                    "Уменьшает или увеличивает меню, чат и стандартный интерфейс.",
                    () -> {
                        cfg.guiScaleOverride = (cfg.guiScaleOverride + 1) % 4;
                        client.options.getGuiScale().setValue(cfg.guiScaleOverride);
                        client.onResolutionChanged();
                    }));
            out.add(slider("МАСШТАБ HUD И ИНВЕНТАРЯ", Math.round(cfg.rustHudScale * 100) + "%",
                    (cfg.rustHudScale - 0.60f) / 0.40f,
                    "Уменьшает инвентарь, крафт, хотбар и полосы здоровья/еды (60% – 100%).",
                    r -> {
                        float snapped = Math.round((0.60f + r * 0.40f) * 20.0f) / 20.0f;
                        cfg.rustHudScale = Math.max(0.60f, Math.min(1.0f, snapped));
                        cfg.syncSystemProperties();
                    }));
            boolean tips = client.options.advancedItemTooltips;
            out.add(toggle("ПОДСКАЗКИ ПРЕДМЕТОВ", tips, "Расширенные подсказки предметов в инвентаре.",
                    () -> client.options.advancedItemTooltips = !tips));
            boolean sub = client.options.getShowSubtitles().getValue();
            out.add(toggle("СУБТИТРЫ", sub, "Текстовые субтитры звуков.",
                    () -> client.options.getShowSubtitles().setValue(!sub)));
            out.add(button("ЯЗЫК ИНТЕРФЕЙСА", "ВЫБРАТЬ ЯЗЫК", false,
                    "Стандартное меню выбора языка.",
                    () -> client.setScreen(new LanguageOptionsScreen(parent, client.options, client.getLanguageManager()))));
            out.add(button("ВСЕ НАСТРОЙКИ MINECRAFT", "ОТКРЫТЬ", true,
                    "Открыть стандартные настройки Minecraft / модов.",
                    () -> {
                        SpaceRustClient.allowVanillaOptionsOnce = true;
                        client.setScreen(new OptionsScreen(parent, client.options));
                    }));

        } else if (cat == Category.CONTROLS) {
            out.add(hdr("ОБЩЕЕ"));
            double ms = client.options.getMouseSensitivity().getValue();
            out.add(slider("ЧУВСТВИТЕЛЬНОСТЬ МЫШИ", Math.round(ms * 200) + "%", (float) ms,
                    "Скорость поворота камеры.", r -> client.options.getMouseSensitivity().setValue((double) r)));
            boolean invY = client.options.getInvertMouseY().getValue();
            out.add(toggle("ИНВЕРСИЯ МЫШИ", invY, "Инвертировать вертикальную ось.",
                    () -> client.options.getInvertMouseY().setValue(!invY)));
            boolean aj = client.options.getAutoJump().getValue();
            out.add(toggle("АВТОПРЫЖОК", aj, "Автоматический прыжок перед блоками.",
                    () -> client.options.getAutoJump().setValue(!aj)));

            out.add(hdr("НАЗНАЧЕНИЕ КЛАВИШ"));
            addKeyRow(out, "ВПЕРЁД", client.options.forwardKey, "Движение вперед.");
            addKeyRow(out, "НАЗАД", client.options.backKey, "Движение назад.");
            addKeyRow(out, "ВЛЕВО", client.options.leftKey, "Шаг влево.");
            addKeyRow(out, "ВПРАВО", client.options.rightKey, "Шаг вправо.");
            addKeyRow(out, "ПРЫЖОК", client.options.jumpKey, "Прыжок.");
            addKeyRow(out, "ПРИСЕСТЬ", client.options.sneakKey, "Приседание.");
            addKeyRow(out, "БЕГ / СПРИНТ", client.options.sprintKey, "Ускоренный бег.");
            addKeyRow(out, "ИНВЕНТАРЬ", client.options.inventoryKey, "Открыть инвентарь.");
            out.add(button("ВСЕ КЛАВИШИ MINECRAFT", "ОТКРЫТЬ КЛАВИШИ", true,
                    "Полный список клавиш всех модов (включая SpaceRust 1.21.11).",
                    () -> client.setScreen(new KeybindsScreen(parent, client.options))));

        } else if (cat == Category.EXPERIMENTAL) {
            out.add(hdr("ЭКСПЕРИМЕНТАЛЬНОЕ"));
            out.add(new SettingRow(RowKind.EMPTY_NOTE, "Настройки появятся позже.", "",
                    "Дополнительные функции появятся в будущих обновлениях.", false, 0f, null, null, null));
        }
        return out;
    }

    private void addSoundSlider(List<SettingRow> out, MinecraftClient client, String title, SoundCategory sc, String desc) {
        double vol = client.options.getSoundVolumeOption(sc).getValue();
        out.add(slider(title, Math.round(vol * 100) + "%", (float) vol, desc,
                r -> client.options.getSoundVolumeOption(sc).setValue((double) r)));
    }

    private void addKeyRow(List<SettingRow> out, String label, KeyBinding kb, String desc) {
        if (kb == null) return;
        String bound = kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT);
        out.add(new SettingRow(RowKind.KEYBIND, label, bound, desc, false, 0f,
                () -> capturingKey = (capturingKey == kb ? null : kb), null, kb));
    }

    private static SettingRow hdr(String title) { return new SettingRow(RowKind.HEADER, title, "", "", false, 0f, null, null, null); }
    private static SettingRow toggle(String l, boolean v, String d, RowAction a) { return new SettingRow(RowKind.TOGGLE, l, "", d, v, 0f, a, null, null); }
    private static SettingRow cycle(String l, String vt, String d, RowAction a) { return new SettingRow(RowKind.CYCLE, l, vt, d, false, 0f, a, null, null); }
    private static SettingRow button(String l, String bt, boolean hl, String d, RowAction a) { return new SettingRow(RowKind.BUTTON, l, bt, d, hl, 0f, a, null, null); }
    private static SettingRow slider(String l, String vt, float r, String d, SliderChange sc) { return new SettingRow(RowKind.SLIDER, l, vt, d, false, r, null, sc, null); }
    private static boolean inRect(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

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
