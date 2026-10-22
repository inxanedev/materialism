package com.materialism.client.shop;

import com.materialism.Money;
import com.materialism.client.ui.MaterialButton;
import com.materialism.client.ui.UiDraw;
import com.materialism.client.ui.UiTheme;
import com.materialism.network.BuyItemPayload;
import com.materialism.network.PurchaseResultPayload;
import com.materialism.shop.ShopCategory;
import com.materialism.shop.ShopEntry;
import com.materialism.shop.ShopSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShopScreen extends Screen {
    private static final int IMAGE_WIDTH = 400;
    private static final int IMAGE_HEIGHT = 228;

    private static final int LEFT_X = 8;
    private static final int LEFT_WIDTH = 62;
    private static final int CENTER_X = 76;
    private static final int CENTER_WIDTH = 182;
    private static final int RIGHT_X = 264;
    private static final int RIGHT_WIDTH = 128;
    private static final int CONTENT_Y = 34;

    private static final int BROWSE_X = 72;
    private static final int BROWSE_Y = 56;
    private static final int BROWSE_WIDTH = 190;
    private static final int BROWSE_HEIGHT = 164;
    private static final int GRID_X = 77;
    private static final int GRID_Y = 64;
    private static final int CELL = 20;
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 6;
    private static final int GRID_SIZE = GRID_COLUMNS * GRID_ROWS;
    private static final int PAGE_Y = 188;

    private static final int SEARCH_X = 76;
    private static final int SEARCH_Y = CONTENT_Y;
    private static final int SEARCH_WIDTH = 182;
    private static final int SEARCH_HEIGHT = 20;

    private static final int DETAIL_INNER_X = RIGHT_X + 10;
    private static final int DETAIL_INNER_RIGHT = RIGHT_X + RIGHT_WIDTH - 10;
    private static final int QUANTITY_X = 282;
    private static final int QUANTITY_FIELD_X = 304;
    private static final int QUANTITY_FIELD_WIDTH = 48;
    private static final int QUANTITY_PLUS_X = 356;

    private final ShopSnapshot snapshot;
    private final List<Offer> offers = new ArrayList<>();
    private final List<Offer> filtered = new ArrayList<>();
    private final Map<String, MaterialButton> categoryButtons = new HashMap<>();
    private final Map<String, MaterialButton> presetButtons = new HashMap<>();
    private final List<ShopSlotWidget> slotWidgets = new ArrayList<>();

    private int leftPos;
    private int topPos;
    private int page;
    private String selectedCategory = "all";
    private String search = "";
    private String selectedId;
    private int quantity = 1;
    private boolean boxed;
    private boolean manualDelivery;
    private boolean updatingQuantity;
    private boolean buyArmed;
    private boolean purchasePending;
    private String statusText = "";
    private int statusColor = UiTheme.MUTED;

    private EditBox searchBox;
    private EditBox quantityBox;
    private MaterialButton previousPage;
    private MaterialButton nextPage;
    private MaterialButton minusButton;
    private MaterialButton plusButton;
    private MaterialButton looseButton;
    private MaterialButton boxedButton;
    private MaterialButton buyButton;
    private MaterialButton sellButton;

    public ShopScreen(ShopSnapshot snapshot) {
        super(Component.translatable("materialism.shop.title"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.categoryButtons.clear();
        this.presetButtons.clear();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        this.offers.clear();
        for (ShopEntry entry : this.snapshot.entries) {
            Identifier id = Identifier.tryParse(entry.id);
            if (id == null) {
                continue;
            }

            var item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty()) {
                continue;
            }

            ItemStack stack = new ItemStack(item.get());
            if (stack.isEmpty()) {
                continue;
            }

            this.offers.add(new Offer(entry, stack, stack.getHoverName().getString()));
        }

        this.slotWidgets.clear();
        for (int i = 0; i < GRID_SIZE; i++) {
            ShopSlotWidget slot = new ShopSlotWidget();
            this.slotWidgets.add(slot);
            this.addRenderableWidget(slot);
        }

        this.searchBox = new EditBox(
                this.font,
                this.leftPos + SEARCH_X + 8,
                this.topPos + SEARCH_Y + 6,
                SEARCH_WIDTH - 16,
                SEARCH_HEIGHT - 4,
                Component.translatable("materialism.shop.search")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(UiTheme.TEXT);
        this.searchBox.setTextColorUneditable(UiTheme.DIM);
        this.searchBox.setHint(Component.translatable("materialism.shop.search"));
        this.searchBox.setResponder(value -> {
            this.search = value == null ? "" : value.toLowerCase(Locale.ROOT);
            this.page = 0;
            this.buyArmed = false;
            this.applyFilter();
        });
        this.addRenderableWidget(this.searchBox);

        int categoryY = this.topPos + CONTENT_Y;
        for (ShopCategory category : this.snapshot.categories) {
            MaterialButton button = new MaterialButton(
                    this.leftPos + LEFT_X,
                    categoryY,
                    LEFT_WIDTH,
                    16,
                    Component.translatable(category.title),
                    MaterialButton.Style.NAV,
                    () -> {
                        this.selectedCategory = category.id;
                        this.page = 0;
                        this.buyArmed = false;
                        this.applyFilter();
                    }
            );
            button.setLeftAligned(true);
            categoryY += 16;
            this.categoryButtons.put(category.id, button);
            this.addRenderableWidget(button);
        }

        this.previousPage = new MaterialButton(
                this.leftPos + GRID_X,
                this.topPos + PAGE_Y,
                18,
                16,
                Component.literal("<"),
                MaterialButton.Style.GHOST,
                this::showPreviousPage
        );
        this.nextPage = new MaterialButton(
                this.leftPos + GRID_X + GRID_COLUMNS * CELL - 18,
                this.topPos + PAGE_Y,
                18,
                16,
                Component.literal(">"),
                MaterialButton.Style.GHOST,
                this::showNextPage
        );
        this.addRenderableWidget(this.previousPage);
        this.addRenderableWidget(this.nextPage);

        this.quantityBox = new EditBox(
                this.font,
                this.leftPos + QUANTITY_FIELD_X,
                this.topPos + 96,
                QUANTITY_FIELD_WIDTH,
                16,
                Component.translatable("materialism.shop.quantity")
        );
        this.quantityBox.setBordered(false);
        this.quantityBox.setCentered(true);
        this.quantityBox.setTextColor(UiTheme.TEXT);
        this.quantityBox.setTextColorUneditable(UiTheme.DIM);
        this.quantityBox.setMaxLength(6);
        this.quantityBox.setValue("1");
        this.quantityBox.setResponder(this::onQuantityTyped);
        this.addRenderableWidget(this.quantityBox);

        this.minusButton = new MaterialButton(
                this.leftPos + QUANTITY_X,
                this.topPos + 90,
                18,
                20,
                Component.literal("-"),
                MaterialButton.Style.SECONDARY,
                () -> this.setQuantity(this.quantity - 1, null)
        );
        this.plusButton = new MaterialButton(
                this.leftPos + QUANTITY_PLUS_X,
                this.topPos + 90,
                18,
                20,
                Component.literal("+"),
                MaterialButton.Style.SECONDARY,
                () -> this.setQuantity(this.quantity + 1, null)
        );
        this.addRenderableWidget(this.minusButton);
        this.addRenderableWidget(this.plusButton);

        this.presetButtons.put("one", new MaterialButton(
                this.leftPos + DETAIL_INNER_X + 2,
                this.topPos + 114,
                32,
                16,
                Component.literal("1"),
                MaterialButton.Style.SEGMENT,
                () -> this.setQuantity(1, false)
        ));
        this.presetButtons.put("stack", new MaterialButton(
                this.leftPos + DETAIL_INNER_X + 38,
                this.topPos + 114,
                32,
                16,
                Component.translatable("materialism.shop.stack"),
                MaterialButton.Style.SEGMENT,
                () -> {
                    Offer offer = this.selectedOffer();
                    if (offer != null) {
                        this.setQuantity(offer.stack.getMaxStackSize(), false);
                    }
                }
        ));
        this.presetButtons.put("shulker", new MaterialButton(
                this.leftPos + DETAIL_INNER_X + 74,
                this.topPos + 114,
                32,
                16,
                Component.translatable("materialism.shop.shulker"),
                MaterialButton.Style.SEGMENT,
                () -> {
                    Offer offer = this.selectedOffer();
                    if (offer != null) {
                        this.setQuantity(capacity(offer.stack), true);
                    }
                }
        ));
        for (MaterialButton button : this.presetButtons.values()) {
            this.addRenderableWidget(button);
        }

        this.looseButton = new MaterialButton(
                this.leftPos + DETAIL_INNER_X + 2,
                this.topPos + 144,
                50,
                16,
                Component.translatable("materialism.shop.loose"),
                MaterialButton.Style.SEGMENT,
                () -> this.setBoxed(false)
        );
        this.boxedButton = new MaterialButton(
                this.leftPos + DETAIL_INNER_X + 56,
                this.topPos + 144,
                50,
                16,
                Component.translatable("materialism.shop.boxed"),
                MaterialButton.Style.SEGMENT,
                () -> this.setBoxed(true)
        );
        this.addRenderableWidget(this.looseButton);
        this.addRenderableWidget(this.boxedButton);

        this.buyButton = new MaterialButton(
                this.leftPos + DETAIL_INNER_X,
                this.topPos + 196,
                108,
                22,
                Component.translatable("materialism.shop.buy"),
                MaterialButton.Style.PRIMARY,
                this::onBuy
        );
        this.addRenderableWidget(this.buyButton);

        this.sellButton = new MaterialButton(
                this.leftPos + 8,
                this.topPos + 7,
                70,
                18,
                Component.translatable("materialism.shop.sell_short"),
                MaterialButton.Style.SECONDARY,
                this::openSell
        );
        this.addRenderableWidget(this.sellButton);

        this.applyFilter();
    }

    public boolean showPreviousPage() {
        if (this.page <= 0) {
            return false;
        }

        this.page--;
        this.buyArmed = false;
        this.updateControls();
        return true;
    }

    public boolean showNextPage() {
        if (this.page + 1 >= this.pageCount()) {
            return false;
        }

        this.page++;
        this.buyArmed = false;
        this.updateControls();
        return true;
    }

    private void applyFilter() {
        this.filtered.clear();
        Offer selected = this.selectedOffer();
        for (Offer offer : this.offers) {
            boolean categoryMatch = this.selectedCategory.equals("all")
                    || this.selectedCategory.equals(offer.entry.category);
            if (!categoryMatch) {
                continue;
            }

            if (!this.search.isEmpty() && !offer.name.toLowerCase(Locale.ROOT).contains(this.search)
                    && !offer.entry.id.toLowerCase(Locale.ROOT).contains(this.search)) {
                continue;
            }

            this.filtered.add(offer);
        }

        int maxPage = Math.max(0, this.pageCount() - 1);
        if (this.page > maxPage) {
            this.page = maxPage;
        }

        if (selected == null || !this.filtered.contains(selected)) {
            this.selectedId = this.filtered.isEmpty() ? null : this.filtered.getFirst().entry.id;
            this.quantity = 1;
            this.boxed = false;
            this.manualDelivery = false;
            this.buyArmed = false;
        }

        this.updateControls();
    }

    private void updateControls() {
        for (Map.Entry<String, MaterialButton> entry : this.categoryButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(this.selectedCategory));
        }

        Offer offer = this.selectedOffer();
        boolean hasItem = offer != null;
        boolean packable = hasItem && isPackable(offer.stack);
        int stackSize = hasItem ? Math.max(1, offer.stack.getMaxStackSize()) : 1;

        this.quantityBox.visible = hasItem;
        this.minusButton.visible = hasItem;
        this.plusButton.visible = hasItem;
        for (MaterialButton button : this.presetButtons.values()) {
            button.visible = hasItem;
            button.active = hasItem;
        }

        MaterialButton one = this.presetButtons.get("one");
        MaterialButton stack = this.presetButtons.get("stack");
        MaterialButton shulker = this.presetButtons.get("shulker");
        if (one != null) {
            one.setSelected(this.quantity == 1 && !this.boxed);
        }
        if (stack != null) {
            stack.active = hasItem && stackSize > 1;
            stack.setSelected(hasItem && stackSize > 1 && this.quantity == stackSize && !this.boxed);
        }
        if (shulker != null) {
            shulker.active = packable;
            shulker.setSelected(hasItem && this.boxed && this.quantity == this.capacity(offer.stack));
        }

        this.looseButton.visible = packable;
        this.boxedButton.visible = packable;
        this.looseButton.active = packable;
        this.boxedButton.active = packable;
        this.looseButton.setSelected(packable && !this.boxed);
        this.boxedButton.setSelected(packable && this.boxed);

        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page + 1 < this.pageCount();
        this.updateSlotWidgets();

        long total = hasItem ? this.total(offer) : 0L;
        boolean affordable = hasItem && total <= this.snapshot.balance;
        this.buyButton.visible = hasItem;
        this.buyButton.active = hasItem && affordable && !this.purchasePending;
        this.buyButton.setMessage(Component.translatable(
                this.buyArmed ? "materialism.shop.buy_anyway" : "materialism.shop.buy"
        ));

        this.updateQuantityValue();
    }

    private void updateQuantityValue() {
        if (this.quantityBox == null || this.quantityBox.isFocused()) {
            return;
        }

        String value = Integer.toString(this.quantity);
        if (!value.equals(this.quantityBox.getValue())) {
            this.updatingQuantity = true;
            this.quantityBox.setValue(value);
            this.updatingQuantity = false;
        }
    }

    private void onQuantityTyped(String value) {
        if (this.updatingQuantity) {
            return;
        }

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            int typed = Integer.parseInt(value);
            int max = this.maxQuantity();
            int clamped = Math.max(1, Math.min(max, typed));
            this.quantity = clamped;
            if (clamped != typed) {
                this.updatingQuantity = true;
                this.quantityBox.setValue(Integer.toString(clamped));
                this.updatingQuantity = false;
            }
            this.applyAutomaticDelivery();
            this.buyArmed = false;
            this.updateControls();
        } catch (NumberFormatException ignored) {
        }
    }

    private void setQuantity(int value, Boolean forceBoxed) {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            return;
        }

        int max = this.maxQuantity();
        this.quantity = Math.max(1, Math.min(max, value));
        if (forceBoxed != null) {
            this.boxed = forceBoxed && isPackable(offer.stack);
            this.manualDelivery = true;
        } else {
            this.manualDelivery = false;
            this.applyAutomaticDelivery();
        }

        this.buyArmed = false;
        this.updateControls();
    }

    private void applyAutomaticDelivery() {
        Offer offer = this.selectedOffer();
        if (offer == null || !isPackable(offer.stack)) {
            this.boxed = false;
            return;
        }

        int stackSize = Math.max(1, offer.stack.getMaxStackSize());
        if (this.quantity <= stackSize) {
            this.boxed = false;
        } else if (!this.manualDelivery) {
            this.boxed = this.quantity > 9L * stackSize;
        }
    }

    private void setBoxed(boolean value) {
        Offer offer = this.selectedOffer();
        if (offer == null || !isPackable(offer.stack)) {
            return;
        }

        this.boxed = value;
        this.manualDelivery = true;
        this.buyArmed = false;
        this.updateControls();
    }

    private void onBuy() {
        Offer offer = this.selectedOffer();
        if (offer == null || this.purchasePending) {
            return;
        }

        long total = this.total(offer);
        if (total > this.snapshot.balance) {
            return;
        }

        int dropped = this.estimatedDropped(offer);
        if (dropped > 0 && !this.buyArmed) {
            this.buyArmed = true;
            this.updateControls();
            return;
        }

        this.purchasePending = true;
        this.buyArmed = false;
        this.updateControls();
        ClientPlayNetworking.send(new BuyItemPayload(
                Identifier.parse(offer.entry.id),
                this.quantity,
                this.boxed
        ));
    }

    public void handlePurchaseResult(PurchaseResultPayload result) {
        this.snapshot.balance = result.balance();
        this.purchasePending = false;
        this.buyArmed = false;
        if (result.success()) {
            this.statusColor = UiTheme.SUCCESS;
            this.statusText = Component.translatable("materialism.shop.complete").getString();
        } else {
            this.statusColor = UiTheme.DANGER;
            this.statusText = Component.translatable("materialism.shop.failed").getString();
        }
        this.updateControls();
    }

    private void openSell() {
        if (this.minecraft.getConnection() == null) {
            return;
        }

        this.minecraft.gui.setScreen(null);
        this.minecraft.getConnection().sendCommand("sell");
    }

    private Offer selectedOffer() {
        if (this.selectedId == null) {
            return null;
        }

        for (Offer offer : this.filtered) {
            if (offer.entry.id.equals(this.selectedId)) {
                return offer;
            }
        }

        return null;
    }

    private int pageCount() {
        return Math.max(1, (this.filtered.size() + GRID_SIZE - 1) / GRID_SIZE);
    }

    private int capacity(ItemStack stack) {
        return 27 * Math.max(1, stack.getMaxStackSize());
    }

    private int maxQuantity() {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            return 1;
        }

        if (!isPackable(offer.stack)) {
            return 27;
        }

        return Math.max(1, this.snapshot.maxShulkerBoxes) * this.capacity(offer.stack);
    }

    private long total(Offer offer) {
        long itemCost = offer.entry.price * (long) this.quantity;
        int boxes = this.boxed ? (int) Math.ceil(this.quantity / (double) this.capacity(offer.stack)) : 0;
        return itemCost + boxes * this.snapshot.shulkerBoxPrice;
    }

    private int boxCount(Offer offer) {
        return this.boxed ? (int) Math.ceil(this.quantity / (double) this.capacity(offer.stack)) : 0;
    }

    private int estimatedDropped(Offer offer) {
        if (this.boxed) {
            int free = 0;
            for (int slot = 0; slot < this.minecraft.player.getInventory().getContainerSize(); slot++) {
                if (this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                    free++;
                }
            }
            return Math.max(0, this.boxCount(offer) - free);
        }

        int stackSize = Math.max(1, offer.stack.getMaxStackSize());
        long room = 0L;
        for (int slot = 0; slot < this.minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                room += stackSize;
            } else if (ItemStack.isSameItemSameComponents(stack, offer.stack)) {
                room += Math.max(0, stackSize - stack.getCount());
            }
        }

        return (int) Math.max(0L, this.quantity - room);
    }

    private static boolean isPackable(ItemStack stack) {
        Item item = stack.getItem();
        return !(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, UiTheme.OVERLAY);
        UiDraw.panel(graphics, this.leftPos, this.topPos, IMAGE_WIDTH, IMAGE_HEIGHT, 10);

        int x = this.leftPos;
        int y = this.topPos;

        UiDraw.roundedRect(graphics, x + 266, y + 6, 126, 20, 9, UiTheme.BORDER_DARK);
        UiDraw.roundedRect(graphics, x + 267, y + 7, 124, 18, 8, UiTheme.BORDER);
        UiDraw.roundedRect(graphics, x + 268, y + 8, 122, 16, 7, UiTheme.ACCENT_SOFT);
        UiDraw.field(
                graphics,
                x + SEARCH_X,
                y + SEARCH_Y,
                SEARCH_WIDTH,
                SEARCH_HEIGHT,
                this.searchBox != null && this.searchBox.isFocused()
        );
        UiDraw.card(graphics, x + BROWSE_X, y + BROWSE_Y, BROWSE_WIDTH, BROWSE_HEIGHT, 8);
        UiDraw.card(graphics, x + RIGHT_X, y + CONTENT_Y, RIGHT_WIDTH, 186, 8);

        if (this.selectedOffer() != null && this.quantityBox != null) {
            UiDraw.field(
                    graphics,
                    x + QUANTITY_FIELD_X,
                    y + 90,
                    QUANTITY_FIELD_WIDTH,
                    20,
                    this.quantityBox.isFocused()
            );
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.drawGrid(graphics, mouseX, mouseY);
        this.drawPageText(graphics);
        this.drawDetail(graphics);
        this.drawHeader(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.drawHoverTooltip(graphics, mouseX, mouseY);
    }

    private void drawHoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.previousPage.isMouseOver(mouseX, mouseY)) {
            this.drawTextTooltip(
                    graphics,
                    Component.translatable("materialism.shop.prev_page"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (this.nextPage.isMouseOver(mouseX, mouseY)) {
            this.drawTextTooltip(
                    graphics,
                    Component.translatable("materialism.shop.next_page"),
                    mouseX,
                    mouseY
            );
            return;
        }

        int hovered = this.gridIndexAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < this.filtered.size()) {
            this.drawItemTooltip(graphics, this.filtered.get(hovered), mouseX, mouseY);
        }
    }

    private void drawTextTooltip(
            GuiGraphicsExtractor graphics,
            Component text,
            int mouseX,
            int mouseY
    ) {
        int width = this.font.width(text) + 14;
        int x = mouseX + 10;
        int y = mouseY + 10;
        if (x + width > this.leftPos + IMAGE_WIDTH - 8) {
            x = mouseX - width - 10;
        }
        if (y + 20 > this.topPos + IMAGE_HEIGHT - 8) {
            y = mouseY - 20 - 10;
        }
        UiDraw.panel(graphics, x, y, width, 20, 6);
        graphics.text(this.font, text, x + 7, y + 6, UiTheme.MUTED, false);
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        int center = x + IMAGE_WIDTH / 2;
        graphics.centeredText(this.font, this.title, center, y + 7, UiTheme.TEXT);
        graphics.fill(center - 14, y + 17, center + 14, y + 19, UiTheme.ACCENT);
        graphics.centeredText(
                this.font,
                Component.translatable("materialism.shop.subtitle"),
                center,
                y + 21,
                UiTheme.MUTED
        );

        Component balance = Component.literal(Money.currency(this.snapshot.balance));
        int balanceWidth = 18 + this.font.width(balance);
        int balanceX = x + 266 + (126 - balanceWidth) / 2;
        graphics.item(new ItemStack(Items.GOLD_INGOT), balanceX, y + 8);
        graphics.text(this.font, balance, balanceX + 18, y + 12, UiTheme.ACCENT, false);
    }

    private void drawGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.filtered.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.no_items"),
                    this.leftPos + CENTER_X + CENTER_WIDTH / 2,
                    this.topPos + 120,
                    UiTheme.MUTED
            );
        }
    }

    private void drawItemTooltip(GuiGraphicsExtractor graphics, Offer offer, int mouseX, int mouseY) {
        Component name = offer.stack.getHoverName();
        Component price = Component.translatable("materialism.shop.price", Money.currency(offer.entry.price));
        int width = Math.max(this.font.width(name), this.font.width(price)) + 16;
        int height = 30;
        int x = mouseX + 12;
        int y = mouseY + 12;
        int gridRight = this.leftPos + BROWSE_X + BROWSE_WIDTH - 4;
        if (x + width > gridRight) {
            x = Math.max(this.leftPos + BROWSE_X + 4, mouseX - width - 12);
        }
        if (y + height > this.height - 8) {
            y = mouseY - height - 12;
        }

        UiDraw.panel(graphics, x, y, width, height, 6);
        graphics.text(this.font, name, x + 8, y + 7, UiTheme.TEXT, false);
        graphics.text(this.font, price, x + 8, y + 19, UiTheme.ACCENT, false);
    }

    private void drawPageText(GuiGraphicsExtractor graphics) {
        graphics.centeredText(
                this.font,
                Component.translatable(
                        "materialism.shop.page",
                        this.page + 1,
                        this.pageCount()
                ),
                this.leftPos + CENTER_X + CENTER_WIDTH / 2,
                this.topPos + 194,
                UiTheme.MUTED
        );
    }

    private void drawDetail(GuiGraphicsExtractor graphics) {
        Offer offer = this.selectedOffer();
        if (offer == null) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.select"),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    this.topPos + 120,
                    UiTheme.MUTED
            );
            return;
        }

        int x = this.leftPos + DETAIL_INNER_X;
        int y = this.topPos;

        graphics.item(offer.stack, x, y + 42);
        graphics.itemDecorations(this.font, offer.stack, x, y + 42);
        List<FormattedCharSequence> nameLines = this.font.split(offer.stack.getHoverName(), 78);
        for (int i = 0; i < Math.min(2, nameLines.size()); i++) {
            graphics.text(this.font, nameLines.get(i), x + 22, y + 44 + i * 10, UiTheme.TEXT, false);
        }

        graphics.centeredText(
                this.font,
                Component.translatable("materialism.shop.price", Money.currency(offer.entry.price)),
                this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                y + 64,
                UiTheme.ACCENT
        );
        UiDraw.divider(graphics, x, y + 76, DETAIL_INNER_RIGHT - x);

        graphics.centeredText(
                this.font,
                Component.translatable("materialism.shop.quantity"),
                this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                y + 78,
                UiTheme.DIM
        );

        if (isPackable(offer.stack)) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.delivery"),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 132,
                    UiTheme.DIM
            );
        }

        int boxes = this.boxCount(offer);
        if (this.boxed) {
            graphics.centeredText(
                    this.font,
                    Component.translatable(
                            "materialism.shop.boxes_cost",
                            boxes,
                            this.snapshot.maxShulkerBoxes,
                            Money.currency(this.snapshot.shulkerBoxPrice * boxes)
                    ),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 162,
                    UiTheme.MUTED
            );
        } else if (isPackable(offer.stack)) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.loose_hint"),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 162,
                    UiTheme.MUTED
            );
        }

        graphics.centeredText(
                this.font,
                Component.translatable("materialism.shop.total", Money.currency(this.total(offer))),
                this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                y + 172,
                UiTheme.ACCENT
        );

        int dropped = this.estimatedDropped(offer);
        if (dropped > 0) {
            Component warning = Component.translatable(
                    this.boxed ? "materialism.shop.drop_boxes" : "materialism.shop.drop_items",
                    dropped
            );
            graphics.centeredText(
                    this.font,
                    warning,
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 184,
                    UiTheme.DANGER
            );
        } else if (this.buyArmed) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.confirm_drop"),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 184,
                    UiTheme.DANGER
            );
        } else if (!this.statusText.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.literal(this.statusText),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 176,
                    this.statusColor
            );
        } else if (this.total(offer) > this.snapshot.balance) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("materialism.shop.no_money"),
                    this.leftPos + RIGHT_X + RIGHT_WIDTH / 2,
                    y + 184,
                    UiTheme.DANGER
            );
        }
    }

    private int gridIndexAt(double mouseX, double mouseY) {
        int localX = (int) (mouseX - (this.leftPos + GRID_X));
        int localY = (int) (mouseY - (this.topPos + GRID_Y));
        if (localX < 0 || localY < 0) {
            return -1;
        }

        int column = localX / CELL;
        int row = localY / CELL;
        if (column >= GRID_COLUMNS || row >= GRID_ROWS) {
            return -1;
        }

        return this.page * GRID_SIZE + row * GRID_COLUMNS + column;
    }


    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.getFocused() != null) {
            return super.keyPressed(event);
        }

        if (super.keyPressed(event)) {
            return true;
        }

        int delta;
        switch (event.key()) {
            case 263 -> delta = -1;
            case 262 -> delta = 1;
            case 265 -> delta = -GRID_COLUMNS;
            case 264 -> delta = GRID_COLUMNS;
            default -> {
                return false;
            }
        }

        this.moveGridSelection(delta);
        return true;
    }

    private void moveGridSelection(int delta) {
        if (this.filtered.isEmpty()) {
            return;
        }

        int current = -1;
        for (int index = 0; index < this.filtered.size(); index++) {
            if (this.filtered.get(index).entry.id.equals(this.selectedId)) {
                current = index;
                break;
            }
        }

        int next = current < 0 ? 0 : Math.max(0, Math.min(this.filtered.size() - 1, current + delta));
        if (next == current) {
            return;
        }

        Offer offer = this.filtered.get(next);
        this.selectedId = offer.entry.id;
        this.page = next / GRID_SIZE;
        this.quantity = 1;
        this.boxed = false;
        this.manualDelivery = false;
        this.buyArmed = false;
        this.statusText = "";
        this.updateControls();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int index = this.gridIndexAt(event.x(), event.y());
        if (index >= 0 && index < this.filtered.size()) {
            Offer offer = this.filtered.get(index);
            if (!offer.entry.id.equals(this.selectedId)) {
                this.selectedId = offer.entry.id;
                this.quantity = 1;
                this.boxed = false;
                this.manualDelivery = false;
                this.buyArmed = false;
                this.statusText = "";
                this.updateControls();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && this.gridIndexAt(mouseX, mouseY) >= 0) {
            if (scrollY > 0.0D && this.page > 0) {
                this.page--;
            } else if (scrollY < 0.0D && this.page + 1 < this.pageCount()) {
                this.page++;
            }
            this.buyArmed = false;
            this.updateControls();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateSlotWidgets() {
        int start = this.page * GRID_SIZE;
        for (int local = 0; local < this.slotWidgets.size(); local++) {
            ShopSlotWidget widget = this.slotWidgets.get(local);
            int column = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            widget.setX(this.leftPos + GRID_X + column * CELL + 1);
            widget.setY(this.topPos + GRID_Y + row * CELL + 1);

            int index = start + local;
            Offer offer = index < this.filtered.size() ? this.filtered.get(index) : null;
            widget.setOffer(offer);
        }
    }

    private void selectOffer(Offer offer) {
        if (offer == null || offer.entry.id.equals(this.selectedId)) {
            return;
        }

        this.selectedId = offer.entry.id;
        this.quantity = 1;
        this.boxed = false;
        this.manualDelivery = false;
        this.buyArmed = false;
        this.statusText = "";
        this.updateControls();
    }

    private final class ShopSlotWidget extends AbstractWidget {
        private Offer offer;

        private ShopSlotWidget() {
            super(0, 0, 18, 18, Component.empty());
        }

        private void setOffer(Offer offer) {
            this.offer = offer;
            this.visible = offer != null;
            this.active = offer != null;
            this.setMessage(offer == null ? Component.empty() : offer.stack.getHoverName());
            if (offer == null && this.isFocused()) {
                this.setFocused(false);
            }
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float delta
        ) {
            if (this.offer == null) {
                return;
            }

            boolean selected = this.offer.entry.id.equals(ShopScreen.this.selectedId);
            boolean focused = this.isFocused();
            boolean hovered = this.isHovered();
            if (focused) {
                UiDraw.roundedRect(graphics, this.getX() - 1, this.getY() - 1, 20, 20, 5, UiTheme.ACCENT_HOVER);
            }

            int fill = selected ? UiTheme.ACCENT_SOFT : hovered ? UiTheme.CARD_HOVER : UiTheme.CARD;
            int border = selected ? UiTheme.ACCENT : hovered || focused ? UiTheme.ACCENT_HOVER : UiTheme.BORDER_SOFT;
            UiDraw.roundedRect(graphics, this.getX(), this.getY(), 18, 18, 4, border);
            UiDraw.roundedRect(graphics, this.getX() + 1, this.getY() + 1, 16, 16, 3, fill);
            graphics.item(this.offer.stack, this.getX() + 1, this.getY() + 1);
            graphics.itemDecorations(ShopScreen.this.font, this.offer.stack, this.getX() + 1, this.getY() + 1);
        }

        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (this.active && this.visible && this.offer != null) {
                this.playDownSound(ShopScreen.this.minecraft.getSoundManager());
                ShopScreen.this.selectOffer(this.offer);
            }
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            if (this.active && this.visible && event.isSelection() && this.offer != null) {
                this.playDownSound(ShopScreen.this.minecraft.getSoundManager());
                ShopScreen.this.selectOffer(this.offer);
                return true;
            }

            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private record Offer(ShopEntry entry, ItemStack stack, String name) {
    }
}
