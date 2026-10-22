package com.materialism.client;

import com.materialism.client.ui.MaterialButton;
import com.materialism.client.ui.UiDraw;
import com.materialism.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class MaterialismConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 164;

    private final Screen parent;

    public MaterialismConfigScreen(Screen parent) {
        super(Component.translatable("materialism.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        MaterialButton confirm = new MaterialButton(
                left + 50,
                top + 76,
                200,
                24,
                confirmLabel(),
                MaterialButton.Style.SEGMENT,
                () -> {
                    MaterialismConfig.setConfirmBeforeSelling(!MaterialismConfig.confirmBeforeSelling());
                    this.rebuildWidgets();
                }
        );
        confirm.setSelected(MaterialismConfig.confirmBeforeSelling());

        MaterialButton done = new MaterialButton(
                left + 50,
                top + 112,
                200,
                24,
                Component.translatable("materialism.config.done"),
                MaterialButton.Style.PRIMARY,
                () -> this.minecraft.gui.setScreen(this.parent)
        );

        this.addRenderableWidget(confirm);
        this.addRenderableWidget(done);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, UiTheme.OVERLAY);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        UiDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, 10);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.centeredText(this.font, this.title, left + PANEL_WIDTH / 2, top + 18, UiTheme.TEXT);
        graphics.fill(left + 138, top + 34, left + 162, top + 36, UiTheme.ACCENT);

        List<FormattedCharSequence> description = this.font.split(
                Component.translatable("materialism.config.confirm.tooltip"),
                PANEL_WIDTH - 40
        );
        for (int index = 0; index < Math.min(2, description.size()); index++) {
            graphics.centeredText(
                    this.font,
                    description.get(index),
                    left + PANEL_WIDTH / 2,
                    top + 46 + index * 12,
                    UiTheme.MUTED
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private static Component confirmLabel() {
        return Component.translatable("materialism.config.confirm")
                .append(": ")
                .append(Component.translatable(
                        MaterialismConfig.confirmBeforeSelling()
                                ? "materialism.config.on"
                                : "materialism.config.off"
                ));
    }
}
