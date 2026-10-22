package com.materialistic.client;

import com.materialistic.Money;
import com.materialistic.menu.SellMenu;
import com.materialistic.network.CancelPayload;
import com.materialistic.network.SellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public final class SellScreen extends AbstractContainerScreen<SellMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "materialistic",
            "textures/gui/sell.png"
    );
    private static final int SIDEBAR_X = 184;
    private static final int SIDEBAR_TEXT_X = SIDEBAR_X + 10;
    private static final int SIDEBAR_WIDTH = 100;
    private static final int PANEL_WIDTH = 176;

    private Button sellButton;
    private Button cancelButton;
    private Button confirmButton;
    private boolean confirming;
    private boolean sold;
    private boolean cancelled;

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 288, 222);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos + SIDEBAR_X;
        this.sellButton = Button.builder(Component.translatable("materialistic.screen.sell"), button -> this.requestSell())
                .bounds(x, this.topPos + 84, SIDEBAR_WIDTH, 20)
                .build();
        this.cancelButton = Button.builder(Component.translatable("materialistic.screen.cancel"), button -> this.sendCancel())
                .bounds(x, this.topPos + 108, SIDEBAR_WIDTH, 20)
                .build();
        this.confirmButton = Button.builder(Component.empty(), button -> {
            MaterialisticConfig.setConfirmBeforeSelling(!MaterialisticConfig.confirmBeforeSelling());
            this.updateButtonLabels();
        }).bounds(x, this.topPos + 132, SIDEBAR_WIDTH, 20).build();

        this.addRenderableWidget(this.sellButton);
        this.addRenderableWidget(this.cancelButton);
        this.addRenderableWidget(this.confirmButton);
        this.updateButtonLabels();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                512,
                256
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.sellButton != null) {
            SellMenu.Summary summary = this.menu.summary();
            this.sellButton.active = !this.sold && summary.units() > 0L;
            this.cancelButton.active = !this.sold;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFD166, true);
        graphics.text(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                0xFFC9D4E0,
                true
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractContents(graphics, mouseX, mouseY, delta);

        SellMenu.Summary summary = this.menu.summary();
        int x = this.leftPos + SIDEBAR_TEXT_X;

        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.summary"),
                x,
                this.topPos + 18,
                0xFFFFD166,
                false
        );
        graphics.text(
                this.font,
                Component.literal(summary.itemCount() + " items"),
                x,
                this.topPos + 38,
                0xFFD8D8D8,
                false
        );
        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.total", Money.currency(summary.units())),
                x,
                this.topPos + 54,
                0xFFFFFFFF,
                false
        );

        if (this.confirming) {
            graphics.fill(
                    this.leftPos,
                    this.topPos,
                    this.leftPos + PANEL_WIDTH,
                    this.topPos + this.imageHeight,
                    0xB0000000
            );
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.screen.confirm_title"),
                    this.leftPos + PANEL_WIDTH / 2,
                    this.topPos + 92,
                    0xFFFFD166
            );
            graphics.centeredText(
                    this.font,
                    Component.translatable(
                            "materialistic.screen.confirm_body",
                            Money.currency(summary.units())
                    ),
                    this.leftPos + PANEL_WIDTH / 2,
                    this.topPos + 108,
                    0xFFFFFFFF
            );
        }

        this.sellButton.active = !this.sold && summary.units() > 0L;
        this.cancelButton.active = !this.sold;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
        if (this.confirming) {
            return;
        }

        super.slotClicked(slot, slotId, buttonNum, input);
    }

    @Override
    public void onClose() {
        if (this.confirming) {
            this.confirming = false;
            this.updateButtonLabels();
            return;
        }

        this.requestSell();
    }

    private void requestSell() {
        if (this.sold || this.cancelled) {
            return;
        }

        if (this.menu.summary().units() <= 0L) {
            this.sendCancel();
            return;
        }

        if (MaterialisticConfig.confirmBeforeSelling() && !this.confirming) {
            this.confirming = true;
            this.updateButtonLabels();
            return;
        }

        this.sold = true;
        this.sellButton.active = false;
        this.cancelButton.active = false;
        ClientPlayNetworking.send(SellPayload.INSTANCE);

        // Closing the menu is the authoritative sale trigger on the server.
        // This also guarantees the screen cannot get stuck if a custom payload is missed.
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
    }

    private void sendCancel() {
        if (this.sold || this.cancelled) {
            return;
        }

        this.cancelled = true;
        ClientPlayNetworking.send(CancelPayload.INSTANCE);
    }

    private void updateButtonLabels() {
        this.sellButton.setMessage(Component.translatable(
                this.confirming ? "materialistic.screen.confirm_yes" : "materialistic.screen.sell"
        ));
        this.cancelButton.setMessage(Component.translatable(
                this.confirming ? "materialistic.screen.confirm_no" : "materialistic.screen.cancel"
        ));
        this.confirmButton.setMessage(Component.translatable(
                MaterialisticConfig.confirmBeforeSelling()
                        ? "materialistic.screen.confirm_on"
                        : "materialistic.screen.confirm_off"
        ));
        this.confirmButton.visible = !this.confirming;
    }
}
