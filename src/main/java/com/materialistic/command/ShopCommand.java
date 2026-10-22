package com.materialistic.command;

import com.materialistic.shop.ShopService;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class ShopCommand {
    private ShopCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ShopService.open(player);
            return 1;
        }));

        dispatcher.register(Commands.literal("buy").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ShopService.open(player);
            return 1;
        }));
    }
}
