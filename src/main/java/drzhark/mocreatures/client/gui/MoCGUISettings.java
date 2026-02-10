/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfigCategory;
import drzhark.mocreatures.config.MoCConfiguration;
import drzhark.mocreatures.config.MoCProperty;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
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
        if (MoCreatures.proxy == null || MoCreatures.proxy.mocSettingsConfig == null) {
            lines.add(new Line("No config loaded", false, null));
            return;
        }
        MoCConfiguration config = MoCreatures.proxy.mocSettingsConfig;
        Set<String> categoryNames = config.getCategoryNames();
        for (String catName : new TreeSet<>(categoryNames)) {
            MoCConfigCategory category = config.getCategory(catName);
            if (category == null || category.isChild()) {
                continue;
            }
            lines.add(new Line("--- " + catName + " ---", true, null));
            Map<String, MoCProperty> values = category.getValues();
            if (values != null) {
                for (Map.Entry<String, MoCProperty> entry : values.entrySet()) {
                    MoCProperty prop = entry.getValue();
                    if (prop == null) continue;
                    String typeChar = prop.getTypeMoC() == null ? "S" : String.valueOf(prop.getTypeMoC().name().charAt(0));
                    String value = prop.isList() ? "[list]" : prop.getString();
                    lines.add(new Line("  " + prop.getName() + " = " + value + " (" + typeChar + ")", false, prop));
                }
            }
        }
    }

    @Override
    protected void init() {
        int buttonW = 100;
        int centerX = this.width / 2;
        int bottomY = this.height - BOTTOM_BUTTON_HEIGHT;
        this.addButton(new Button(centerX - buttonW * 2, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.done"), b -> this.minecraft.displayGuiScreen(null)));
        this.addButton(new Button(centerX - buttonW / 2, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.mocreatures.reload"), b -> {
            if (MoCreatures.proxy != null && MoCreatures.proxy.mocSettingsConfig != null) {
                MoCreatures.proxy.mocSettingsConfig.load();
                MoCreatures.proxy.readGlobalConfigValues();
                buildLines();
            }
        }));
        this.addButton(new Button(centerX + 2, bottomY, buttonW, 20,
                new TranslationTextComponent("gui.mocreatures.save"), b -> saveAndReload()));
        listTop = TITLE_HEIGHT + PADDING;
        listBottom = this.height - BOTTOM_BUTTON_HEIGHT - PADDING;
        totalContentHeight = lines.size() * LINE_HEIGHT;
        scrollOffset = Math.min(scrollOffset, Math.max(0, totalContentHeight - (listBottom - listTop)));
    }

    private void saveAndReload() {
        if (MoCreatures.proxy == null || MoCreatures.proxy.mocSettingsConfig == null) return;
        MoCreatures.proxy.mocSettingsConfig.save();
        MoCreatures.proxy.readGlobalConfigValues();
        buildLines();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.getTitle().getString(), this.width / 2, 12, 0xffffff);
        int y = listTop - scrollOffset;
        for (Line line : lines) {
            if (y + LINE_HEIGHT >= listTop && y <= listBottom) {
                int color = line.isHeader ? 0xffff00 : 0xcccccc;
                this.font.drawString(matrixStack, line.text, PADDING, y, color);
            }
            y += LINE_HEIGHT;
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= PADDING && mouseX <= this.width - PADDING && mouseY >= listTop && mouseY <= listBottom) {
            int lineIndex = (int) ((mouseY - listTop + scrollOffset) / LINE_HEIGHT);
            if (lineIndex >= 0 && lineIndex < lines.size()) {
                Line line = lines.get(lineIndex);
                if (line.property != null && line.property.getTypeMoC() == MoCProperty.Type.BOOLEAN) {
                    boolean current = line.property.getBoolean(false);
                    line.property.set(!current);
                    buildLines();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= PADDING && mouseX <= this.width - PADDING && mouseY >= listTop && mouseY <= listBottom) {
            int maxScroll = Math.max(0, totalContentHeight - (listBottom - listTop));
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

        Line(String text, boolean isHeader, MoCProperty property) {
            this.text = text;
            this.isHeader = isHeader;
            this.property = property;
        }
    }
}
