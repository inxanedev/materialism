package com.materialistic.client;

import com.materialistic.Money;
import com.materialistic.client.ui.MaterialButton;
import com.materialistic.client.ui.UiDraw;
import com.materialistic.client.ui.UiTheme;
import com.materialistic.menu.SellMenu;
import com.materialistic.network.CancelPayload;
import com.materialistic.network.SellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SellScreen extends AbstractContainerScreen<SellMenu> {
    private static final int PANEL_WIDTH = 304;
    private static final int PANEL_HEIGHT = 228;
    private static final int SIDEBAR_X = 194;
    private static final int SIDEBAR_TEXT_X = 202;
    private static final int SIDEBAR_INNER_WIDTH = 88;

    private MaterialButton sellButton;
    private MaterialButton cancelButton;
    private MaterialButton confirmButton;
    private boolean confirming;
    private boolean sold;
    private boolean cancelled;

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.titleLabelX = 12;
        this.titleLabelY = 7;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 138;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos + SIDEBAR_TEXT_X;
        int y = this.topPos;

        this.confirmButton = new MaterialButton(
                x,
                y + 88,
                SIDEBAR_INNER_WIDTH,
                20,
                Component.empty(),
                MaterialButton.Style.SEGMENT,
                () -> {
                    MaterialisticConfig.setConfirmBeforeSelling(!MaterialisticConfig.confirmBeforeSelling());
                    this.updateButtonLabels();
                }
        );
        this.sellButton = new MaterialButton(
                x,
                y + 124,
                SIDEBAR_INNER_WIDTH,
                26,
                Component.translatable("materialistic.screen.sell"),
                MaterialButton.Style.PRIMARY,
                this::requestSell
        );
        this.cancelButton = new MaterialButton(
                x,
                y + 158,
                SIDEBAR_INNER_WIDTH,
                20,
                Component.translatable("materialistic.screen.cancel"),
                MaterialButton.Style.SECONDARY,
                this::sendCancel
        );

        this.addRenderableWidget(this.confirmButton);
        this.addRenderableWidget(this.sellButton);
        this.addRenderableWidget(this.cancelButton);
        this.updateButtonLabels();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, UiTheme.OVERLAY);
        UiDraw.panel(graphics, this.leftPos, this.topPos, PANEL_WIDTH, PANEL_HEIGHT, 10);
        UiDraw.card(graphics, this.leftPos + 6, this.topPos + 6, 182, 216, 8);
        UiDraw.card(graphics, this.leftPos + SIDEBAR_X, this.topPos + 6, 104, 216, 8);
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
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        int x = slot.x;
        int y = slot.y;
        boolean hovered = slot == this.hoveredSlot;
        int border = hovered ? UiTheme.ACCENT : UiTheme.BORDER_SOFT;
        UiDraw.roundedRect(graphics, x - 1, y - 1, 18, 18, 3, UiTheme.BORDER_DARK);
        UiDraw.roundedRect(graphics, x, y, 16, 16, 2, border);
        UiDraw.roundedRect(graphics, x + 1, y + 1, 14, 14, 1, hovered ? UiTheme.CARD_HOVER : UiTheme.FIELD);

        ItemStack stack = slot.getItem();
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(this.font, stack, x, y);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, UiTheme.TEXT, true);
        graphics.fill(this.titleLabelX, 18, this.titleLabelX + 30, 20, UiTheme.ACCENT);
        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.subtitle"),
                this.titleLabelX,
                21,
                UiTheme.MUTED,
                false
        );
        graphics.text(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                UiTheme.MUTED,
                false
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractContents(graphics, mouseX, mouseY, delta);

        SellMenu.Summary summary = this.menu.summary();
        int x = this.leftPos + SIDEBAR_TEXT_X;
        int y = this.topPos;

        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.summary"),
                x,
                y + 18,
                UiTheme.TEXT,
                false
        );
        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.items", summary.itemCount()),
                x,
                y + 38,
                UiTheme.MUTED,
                false
        );
        graphics.text(
                this.font,
                Component.translatable("materialistic.screen.total", Money.currency(summary.units())),
                x,
                y + 54,
                UiTheme.ACCENT,
                false
        );
        UiDraw.divider(graphics, x, y + 74, SIDEBAR_INNER_WIDTH);

        if (this.confirming) {
            graphics.fill(
                    this.leftPos + 6,
                    this.topPos + 6,
                    this.leftPos + 188,
                    this.topPos + 222,
                    0xAA000000
            );
            UiDraw.card(graphics, this.leftPos + 20, this.topPos + 72, 154, 72, 8);
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialistic.screen.confirm_title"),
                    this.leftPos + 97,
                    this.topPos + 84,
                    UiTheme.ACCENT
            );
            graphics.centeredText(
                    this.font,
                    Component.translatable(
                            "materialistic.screen.confirm_body",
                            Money.currency(summary.units())
                    ),
                    this.leftPos + 97,
                    this.topPos + 102,
                    UiTheme.TEXT
            );
        }

        this.sellButton.active = !this.sold && summary.units() > 0L;
        this.cancelButton.active = !this.sold;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = this.hoveredSlot.getItem();
        List<Component> lines = getTooltipFromItem(this.minecraft, stack);
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        int height = lines.size() * 10 + 8;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + width + 14 > this.width - 8) {
            x = mouseX - width - 20;
        }
        if (y + height > this.height - 8) {
            y = mouseY - height - 12;
        }

        UiDraw.panel(graphics, x, y, width + 14, height, 6);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(
                    this.font,
                    lines.get(index),
                    x + 7,
                    y + 5 + index * 10,
                    index == 0 ? UiTheme.TEXT : UiTheme.MUTED,
                    false
            );
        }
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
        this.confirmButton.setSelected(MaterialisticConfig.confirmBeforeSelling());
        this.confirmButton.visible = !this.confirming;
    }
}
