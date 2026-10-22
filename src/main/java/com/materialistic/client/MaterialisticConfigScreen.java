package com.materialistic.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MaterialisticConfigScreen extends Screen {
    private final Screen parent;

    public MaterialisticConfigScreen(Screen parent) {
        super(Component.translatable("materialistic.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button confirm = Button.builder(confirmLabel(), button -> {
            MaterialisticConfig.setConfirmBeforeSelling(!MaterialisticConfig.confirmBeforeSelling());
            button.setMessage(confirmLabel());
        }).bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build();

        this.addRenderableWidget(confirm);
        this.addRenderableWidget(Button.builder(
                Component.translatable("materialistic.config.done"),
                button -> this.minecraft.gui.setScreen(this.parent)
        ).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 40, 0xFFFFFFFF);
        graphics.centeredText(
                this.font,
                Component.translatable("materialistic.config.confirm.tooltip"),
                this.width / 2,
                this.height / 2 - 34,
                0xFFB8B8B8
        );
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private static Component confirmLabel() {
        return Component.translatable("materialistic.config.confirm")
                .append(": ")
                .append(Component.translatable(
                        MaterialisticConfig.confirmBeforeSelling()
                                ? "materialistic.config.on"
                                : "materialistic.config.off"
                ));
    }
}
