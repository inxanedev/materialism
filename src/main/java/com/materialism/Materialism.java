package com.materialism;

import com.materialism.command.BalanceCommand;
import com.materialism.command.SellCommand;
import com.materialism.command.ShopCommand;
import com.materialism.economy.EconomyManager;
import com.materialism.menu.ModMenus;
import com.materialism.network.MaterialismNetworking;
import com.materialism.network.ShopNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Materialism implements ModInitializer {
    public static final String MOD_ID = "materialism";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModMenus.init();
        WorthData.get();
        EconomyManager.init();
        MaterialismNetworking.init();
        ShopNetworking.init();
        SellCommand.register();
        BalanceCommand.register();
        ShopCommand.register();
        LOGGER.info("Materialism loaded");
    }
}
