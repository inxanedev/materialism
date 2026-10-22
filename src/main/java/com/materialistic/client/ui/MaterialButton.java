package com.materialistic.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class MaterialButton extends AbstractWidget {
    public enum Style {
        PRIMARY,
        SECONDARY,
        GHOST,
        NAV,
        SEGMENT
    }

    private final Runnable onPress;
    private final Style style;
    private boolean selected;
    private boolean leftAligned;

    public MaterialButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Style style,
            Runnable onPress
    ) {
        super(x, y, width, height, message);
        this.style = style;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setLeftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = this.active && (this.isHovered() || this.isFocused());
        int background;
        int border;
        int textColor;

        if (!this.active) {
            background = UiTheme.CARD;
            border = UiTheme.BORDER_SOFT;
            textColor = UiTheme.DIM;
        } else if (this.style == Style.PRIMARY) {
            background = hovered ? 0xFF4A3A1D : UiTheme.ACCENT_SOFT;
            border = hovered ? UiTheme.ACCENT_HOVER : UiTheme.ACCENT;
            textColor = hovered ? UiTheme.ACCENT_HOVER : UiTheme.ACCENT;
        } else if (this.style == Style.NAV) {
            background = this.selected ? UiTheme.ACCENT_SOFT : hovered ? UiTheme.CARD : UiTheme.PANEL;
            border = this.selected ? UiTheme.ACCENT : UiTheme.PANEL;
            textColor = this.selected ? UiTheme.ACCENT : hovered ? UiTheme.TEXT : UiTheme.MUTED;
        } else if (this.style == Style.SEGMENT) {
            background = this.selected ? UiTheme.ACCENT_SOFT : hovered ? UiTheme.CARD_HOVER : UiTheme.FIELD;
            border = this.selected ? UiTheme.ACCENT : UiTheme.BORDER;
            textColor = this.selected ? UiTheme.ACCENT : hovered ? UiTheme.TEXT : UiTheme.MUTED;
        } else if (this.style == Style.GHOST) {
            background = hovered ? UiTheme.CARD : UiTheme.PANEL;
            border = hovered ? UiTheme.BORDER : UiTheme.PANEL;
            textColor = hovered ? UiTheme.TEXT : UiTheme.MUTED;
        } else {
            background = hovered ? UiTheme.CARD_HOVER : UiTheme.CARD;
            border = hovered ? UiTheme.BORDER : UiTheme.BORDER_SOFT;
            textColor = this.active ? UiTheme.TEXT : UiTheme.DIM;
        }

        if (this.isFocused()) {
            UiDraw.roundedRect(graphics, this.getX() - 1, this.getY() - 1, this.width + 2, this.height + 2, 7, UiTheme.ACCENT);
        }

        UiDraw.roundedRect(graphics, this.getX(), this.getY(), this.width, this.height, 6, UiTheme.BORDER_DARK);
        UiDraw.roundedRect(graphics, this.getX() + 1, this.getY() + 1, this.width - 2, this.height - 2, 5, border);
        UiDraw.roundedRect(graphics, this.getX() + 2, this.getY() + 2, this.width - 4, this.height - 4, 4, background);

        if (this.active && this.style == Style.PRIMARY) {
            UiDraw.roundedRect(
                    graphics,
                    this.getX() + 4,
                    this.getY() + 2,
                    this.width - 8,
                    1,
                    0,
                    0x66FFF4D6
            );
        }

        if (this.style == Style.NAV && this.selected) {
            UiDraw.roundedRect(graphics, this.getX() + 1, this.getY() + 5, 2, this.height - 10, 1, UiTheme.ACCENT);
        }

        if (!this.getMessage().getString().isEmpty()) {
            int textY = this.getY() + (this.height - 8) / 2;
            if (this.leftAligned) {
                graphics.text(this.font(), this.getMessage(), this.getX() + 9, textY, textColor, false);
            } else {
                int textX = this.getX() + (this.width - this.font().width(this.getMessage())) / 2;
                graphics.text(this.font(), this.getMessage(), textX, textY, textColor, false);
            }
        }
    }

    private net.minecraft.client.gui.Font font() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.font;
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.run();
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.active && this.visible
                && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_SPACE)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.run();
            return true;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
