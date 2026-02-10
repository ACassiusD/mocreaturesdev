/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfigCategory;
import drzhark.mocreatures.config.MoCConfiguration;
import drzhark.mocreatures.config.MoCProperty;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@OnlyIn(Dist.CLIENT)
public class MoCGUISettings extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 30;
    private static final int BOTTOM_BUTTON_HEIGHT = 30;

    /**
     * Spawn limits to match vanilla/Forge (MobSpawnInfo.Spawners, DefaultBiomeFeatures).
     * Weight: 0 = disabled; upper bound 100 (vanilla e.g. creeper). Min/max group: 0–8 (vanilla uses 1–8).
     */
    private static final int FREQUENCY_MIN = 0;
    private static final int FREQUENCY_MAX = 100;
    private static final int SPAWN_GROUP_MIN = 0;
    private static final int SPAWN_GROUP_MAX = 8;

    public enum ViewMode { GLOBAL_SETTINGS, CREATURE_SPAWNS }

    private ViewMode viewMode = ViewMode.GLOBAL_SETTINGS;
    private final List<Line> lines = new ArrayList<>();
    private int scrollOffset;
    private int totalContentHeight;
    private int listTop;
    private int listBottom;

    public MoCGUISettings() {
        super(new TranslationTextComponent("gui.mocreatures.settings"));
        buildLines();
    }

    private void buildLines() {
        lines.clear();
        if (viewMode == ViewMode.CREATURE_SPAWNS) {
            buildSpawnConfigLines();
            return;
        }
        if (MoCreatures.proxy == null || MoCreatures.proxy.mocSettingsConfig == null) {
            lines.add(new Line("No config loaded", false, null, null, null));
            return;
        }
        MoCConfiguration config = MoCreatures.proxy.mocSettingsConfig;
        Set<String> categoryNames = config.getCategoryNames();
        for (String catName : new TreeSet<>(categoryNames)) {
            MoCConfigCategory category = config.getCategory(catName);
            if (category == null || category.isChild()) {
                continue;
            }
            lines.add(new Line("--- " + catName + " ---", true, null, null, null));
            Map<String, MoCProperty> values = category.getValues();
            if (values != null) {
                for (Map.Entry<String, MoCProperty> entry : values.entrySet()) {
                    MoCProperty prop = entry.getValue();
                    if (prop == null) continue;
                    String typeChar = prop.getTypeMoC() == null ? "S" : String.valueOf(prop.getTypeMoC().name().charAt(0));
                    String value = prop.isList() ? "[list]" : prop.getString();
                    lines.add(new Line("  " + prop.getName() + " = " + value + " (" + typeChar + ")", false, prop, null, null));
                }
            }
        }
        updateContentHeightAndScroll();
    }

    private void buildSpawnConfigLines() {
        if (MoCreatures.proxy == null || MoCreatures.proxy.mocEntityConfig == null) {
            lines.add(new Line("No entity spawn config loaded", false, null, null, null));
            return;
        }
        MoCConfiguration config = MoCreatures.proxy.mocEntityConfig;
        Set<String> categoryNames = config.getCategoryNames();
        if (categoryNames == null || categoryNames.isEmpty()) {
            lines.add(new Line("No entity spawn config loaded", false, null, null, null));
            return;
        }
        String[] spawnKeys = new String[] { "canSpawn", "frequency", "minSpawn", "maxSpawn" };
        for (String catName : new TreeSet<>(categoryNames)) {
            MoCConfigCategory category = config.getCategory(catName);
            if (category == null || category.isChild()) continue;
            if ("custom-id-settings".equalsIgnoreCase(catName)) continue;
            boolean hasAnySpawnProp = false;
            for (String key : spawnKeys) {
                if (category.get(key) != null) {
                    hasAnySpawnProp = true;
                    break;
                }
            }
            if (!hasAnySpawnProp) continue;
            String displayName = toDisplayName(catName);
            lines.add(new Line("--- " + displayName + " ---", true, null, null, null));
            for (String key : spawnKeys) {
                MoCProperty prop = category.get(key);
                if (prop == null) continue;
                String value = prop.isList() ? "[list]" : prop.getString();
                String rangeHint = "";
                if (prop.getTypeMoC() == MoCProperty.Type.INTEGER) {
                    if ("frequency".equals(key)) rangeHint = " [" + FREQUENCY_MIN + "-" + FREQUENCY_MAX + "]";
                    else if ("minSpawn".equals(key) || "maxSpawn".equals(key)) rangeHint = " [" + SPAWN_GROUP_MIN + "-" + SPAWN_GROUP_MAX + "]";
                }
                String hint = (prop.getTypeMoC() == MoCProperty.Type.INTEGER) ? " (L/R ±1)" + rangeHint : "";
                lines.add(new Line("  " + key + " = " + value + hint, false, prop, key, catName));
            }
        }
        updateContentHeightAndScroll();
    }

    private void updateContentHeightAndScroll() {
        totalContentHeight = lines.size() * LINE_HEIGHT;
        if (listBottom > listTop) {
            int visibleHeight = listBottom - listTop;
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    private static ITextComponent getSpawnKeyTooltip(String spawnKey) {
        if (spawnKey == null) return null;
        switch (spawnKey) {
            case "canSpawn":
                return new StringTextComponent("Whether this creature can spawn. Click to toggle.");
            case "frequency":
                return new StringTextComponent("Spawn weight (relative chance vs other mobs). Higher = more common. 0 = disabled.");
            case "minSpawn":
                return new StringTextComponent("Minimum group size per spawn attempt.");
            case "maxSpawn":
                return new StringTextComponent("Maximum group size per spawn attempt. Must be ≥ minSpawn.");
            default:
                return null;
        }
    }

    private static String toDisplayName(String configKey) {
        if (configKey == null || configKey.isEmpty()) return configKey;
        if (MoCreatures.mocEntityMap != null) {
            for (String entityName : MoCreatures.mocEntityMap.keySet()) {
                if (entityName.equalsIgnoreCase(configKey)) return entityName;
            }
        }
        return configKey.substring(0, 1).toUpperCase() + configKey.substring(1).toLowerCase();
    }

    @Override
    protected void init() {
        int buttonW = 100;
        int centerX = this.width / 2;
        int bottomY = this.height - BOTTOM_BUTTON_HEIGHT;
        int topY = 8;
        this.addButton(new Button(PADDING, topY, 110, 20,
                new TranslationTextComponent("gui.mocreatures.global_settings"), b -> {
            viewMode = ViewMode.GLOBAL_SETTINGS;
            scrollOffset = 0;
            buildLines();
        }));
        this.addButton(new Button(PADDING + 118, topY, 110, 20,
                new TranslationTextComponent("gui.mocreatures.creature_spawns"), b -> {
            viewMode = ViewMode.CREATURE_SPAWNS;
            scrollOffset = 0;
            if (MoCreatures.proxy != null && MoCreatures.proxy.mocEntityConfig != null) {
                MoCreatures.proxy.mocEntityConfig.load();
                MoCreatures.proxy.readMocConfigValues();
            }
            buildLines();
        }));
        listTop = TITLE_HEIGHT + PADDING + 4;
        int gap = 10;
        int totalBottomWidth = buttonW * 3 + gap * 2;
        int leftX = centerX - totalBottomWidth / 2;
        this.addButton(new Button(leftX, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.done"), b -> this.minecraft.displayGuiScreen(null)));
        this.addButton(new Button(leftX + buttonW + gap, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.mocreatures.reload"), b -> {
            if (viewMode == ViewMode.CREATURE_SPAWNS && MoCreatures.proxy != null && MoCreatures.proxy.mocEntityConfig != null) {
                MoCreatures.proxy.mocEntityConfig.load();
                MoCreatures.proxy.readMocConfigValues();
            } else if (MoCreatures.proxy != null && MoCreatures.proxy.mocSettingsConfig != null) {
                MoCreatures.proxy.mocSettingsConfig.load();
                MoCreatures.proxy.readGlobalConfigValues();
            }
            buildLines();
        }));
        this.addButton(new Button(leftX + (buttonW + gap) * 2, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.mocreatures.save"), b -> saveAndReload()));
        listBottom = this.height - BOTTOM_BUTTON_HEIGHT - PADDING;
        updateContentHeightAndScroll();
    }

    private void saveAndReload() {
        if (viewMode == ViewMode.CREATURE_SPAWNS) {
            if (MoCreatures.proxy == null || MoCreatures.proxy.mocEntityConfig == null) return;
            MoCreatures.proxy.mocEntityConfig.save();
            MoCreatures.proxy.readMocConfigValues();
        } else {
            if (MoCreatures.proxy == null || MoCreatures.proxy.mocSettingsConfig == null) return;
            MoCreatures.proxy.mocSettingsConfig.save();
            MoCreatures.proxy.readGlobalConfigValues();
        }
        buildLines();

        // Return to main menu so spawn/config changes take effect on next world load
        if (this.minecraft != null && this.minecraft.world != null) {
            this.minecraft.world.sendQuittingDisconnectingPacket();
            if (this.minecraft.isIntegratedServerRunning()) {
                this.minecraft.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
                this.minecraft.displayGuiScreen(new MainMenuScreen());
            } else {
                this.minecraft.unloadWorld();
                this.minecraft.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
            }
        }
    }

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PAD = 2;

    private static final int BG_ALT = 0x0AFFFFFF;   // alternating block (subtle white)
    private static final int BG_HEADER = 0x18FFFFFF; // header bar (slightly stronger)

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.getTitle().getString(), this.width / 2, 12, 0xffffff);
        int contentHeight = listBottom - listTop;
        boolean showScrollbar = totalContentHeight > contentHeight && contentHeight > 0;
        int listRight = getListRight();
        int y = listTop - scrollOffset;
        int headerCount = 0;
        Line hoveredSpawnLine = null;
        for (Line line : lines) {
            boolean visible = y + LINE_HEIGHT >= listTop && y <= listBottom;
            if (visible) {
                if (viewMode == ViewMode.CREATURE_SPAWNS) {
                    if (line.isHeader) {
                        fill(matrixStack, PADDING, y, listRight, y + LINE_HEIGHT, BG_HEADER);
                        headerCount++;
                    } else {
                        int blockIndex = headerCount - 1;
                        if (blockIndex >= 0 && (blockIndex % 2) == 1) {
                            fill(matrixStack, PADDING, y, listRight, y + LINE_HEIGHT, BG_ALT);
                        }
                        if (line.spawnKey != null && mouseX >= PADDING && mouseX <= listRight && mouseY >= y && mouseY < y + LINE_HEIGHT) {
                            hoveredSpawnLine = line;
                        }
                    }
                }
                int color = line.isHeader ? 0xFFE0E0A0 : 0xFFCCCCCC;
                this.font.drawString(matrixStack, line.text, PADDING, y, color);
            }
            if (line.isHeader) headerCount++;
            y += LINE_HEIGHT;
        }
        if (hoveredSpawnLine != null && hoveredSpawnLine.spawnKey != null) {
            ITextComponent tooltip = getSpawnKeyTooltip(hoveredSpawnLine.spawnKey);
            if (tooltip != null) {
                this.renderTooltip(matrixStack, tooltip, mouseX, mouseY);
            }
        }
        if (showScrollbar) {
            int scrollbarLeft = this.width - PADDING - SCROLLBAR_WIDTH;
            fill(matrixStack, scrollbarLeft, listTop, scrollbarLeft + SCROLLBAR_WIDTH, listBottom, 0xFF404040);
            int thumbHeight = Math.max(20, contentHeight * contentHeight / totalContentHeight);
            int maxScroll = totalContentHeight - contentHeight;
            int thumbY = maxScroll <= 0 ? listTop : listTop + (listBottom - listTop - thumbHeight) * scrollOffset / maxScroll;
            fill(matrixStack, scrollbarLeft + 1, thumbY, scrollbarLeft + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF808080);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private int getListRight() {
        int listRight = this.width - PADDING;
        if (totalContentHeight > listBottom - listTop && listBottom > listTop) {
            listRight -= (SCROLLBAR_WIDTH + SCROLLBAR_PAD);
        }
        return listRight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= PADDING && mouseX <= getListRight() && mouseY >= listTop && mouseY <= listBottom) {
            int lineIndex = (int) ((mouseY - listTop + scrollOffset) / LINE_HEIGHT);
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                Line line = lines.get(lineIndex);
                if (line.property != null) {
                    if (line.property.getTypeMoC() == MoCProperty.Type.BOOLEAN && button == 0) {
                        boolean current = line.property.getBoolean(false);
                        line.property.set(!current);
                        buildLines();
                        return true;
                    }
                    if (line.property.getTypeMoC() == MoCProperty.Type.INTEGER) {
                        int current;
                        try {
                            current = Integer.parseInt(line.property.value);
                        } catch (NumberFormatException e) {
                            current = 0;
                        }
                        int min, max;
                        if ("frequency".equals(line.spawnKey)) {
                            min = FREQUENCY_MIN;
                            max = FREQUENCY_MAX;
                        } else if ("minSpawn".equals(line.spawnKey) || "maxSpawn".equals(line.spawnKey)) {
                            min = SPAWN_GROUP_MIN;
                            max = SPAWN_GROUP_MAX;
                        } else {
                            min = 0;
                            max = 100;
                        }
                        if (button == 0) {
                            current = Math.min(current + 1, max);
                        } else if (button == 1) {
                            current = Math.max(current - 1, min);
                        } else {
                            return false;
                        }
                        line.property.set(String.valueOf(current));
                        if (line.spawnCategoryName != null && ("minSpawn".equals(line.spawnKey) || "maxSpawn".equals(line.spawnKey))) {
                            enforceMinMaxSpawnOrder(line.spawnCategoryName, line.spawnKey, current);
                        }
                        buildLines();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Minecraft requires maxCount >= minCount (groupSize = minCount + nextInt(1 + maxCount - minCount)). */
    private void enforceMinMaxSpawnOrder(String categoryName, String changedKey, int newValue) {
        if (MoCreatures.proxy == null || MoCreatures.proxy.mocEntityConfig == null) return;
        MoCConfigCategory category = MoCreatures.proxy.mocEntityConfig.getCategory(categoryName);
        if (category == null) return;
        MoCProperty minProp = category.get("minSpawn");
        MoCProperty maxProp = category.get("maxSpawn");
        if (minProp == null || maxProp == null) return;
        int minVal = parsePropInt(minProp, 0);
        int maxVal = parsePropInt(maxProp, 0);
        if ("minSpawn".equals(changedKey)) {
            if (newValue > maxVal) {
                maxProp.set(String.valueOf(newValue));
            }
        } else {
            if (newValue < minVal) {
                minProp.set(String.valueOf(newValue));
            }
        }
    }

    private static int parsePropInt(MoCProperty prop, int defaultValue) {
        try {
            return prop != null && prop.value != null ? Integer.parseInt(prop.value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= PADDING && mouseX <= getListRight() && mouseY >= listTop && mouseY <= listBottom) {
            int visibleHeight = listBottom - listTop;
            int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
            scrollOffset = (int) (scrollOffset - delta * LINE_HEIGHT);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return false;
    }

    private static class Line {
        final String text;
        final boolean isHeader;
        final MoCProperty property;
        /** For spawn config: "canSpawn", "frequency", "minSpawn", "maxSpawn" (for integer bounds) */
        final String spawnKey;
        /** For spawn config: entity category name (lowercase) so we can enforce min <= max. */
        final String spawnCategoryName;

        Line(String text, boolean isHeader, MoCProperty property, String spawnKey, String spawnCategoryName) {
            this.text = text;
            this.isHeader = isHeader;
            this.property = property;
            this.spawnKey = spawnKey;
            this.spawnCategoryName = spawnCategoryName;
        }
    }
}
