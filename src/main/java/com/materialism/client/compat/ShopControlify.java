package com.materialism.client.compat;

import com.materialism.client.shop.ShopScreen;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;

public final class ShopControlify implements ControlifyEntrypoint {
    @Override
    public void onControlifyPreInit(PreInitContext context) {
        ScreenProcessorProvider.registerProvider(ShopScreen.class, ShopScreenProcessor::new);
    }

    @Override
    public void onControlifyInit(InitContext context) {
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
    }

    private static final class ShopScreenProcessor extends ScreenProcessor<ShopScreen> {
        private ShopScreenProcessor(ShopScreen screen) {
            super(screen);
        }

        @Override
        protected void handleTabNavigation(ControllerEntity controller) {
            if (ControlifyBindings.GUI_NEXT_TAB.on(controller).justPressed()) {
                if (this.screen.showNextPage()) {
                    playClackSound();
                }
                return;
            }

            if (ControlifyBindings.GUI_PREV_TAB.on(controller).justPressed()) {
                if (this.screen.showPreviousPage()) {
                    playClackSound();
                }
                return;
            }

            super.handleTabNavigation(controller);
        }
    }
}
