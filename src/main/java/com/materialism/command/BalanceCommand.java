package com.materialism.command;

import com.materialism.Money;
import com.materialism.economy.EconomyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BalanceCommand {
    private BalanceCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var command = Commands.literal("balance")
                .executes(context -> showBalance(context.getSource()));

        dispatcher.register(command);
        dispatcher.register(Commands.literal("bal")
                .executes(context -> showBalance(context.getSource())));
    }

    private static int showBalance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long balance = EconomyManager.get(source.getServer()).get(player.getUUID());
        source.sendSuccess(
                () -> Component.translatable("materialism.command.balance", Money.currency(balance)),
                false
        );
        return 1;
    }
}
