package com.materialism.command;

import com.materialism.menu.SellMenu;
import com.materialism.shop.ShopService;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class SellCommand {
    private SellCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sell").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ShopService.close(player);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new SellMenu(containerId, inventory),
                    Component.translatable("materialism.screen.title")
            ));
            return 1;
        }));
    }
}
