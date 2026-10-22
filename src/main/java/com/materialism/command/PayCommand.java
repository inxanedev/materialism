package com.materialism.command;

import com.materialism.Money;
import com.materialism.economy.EconomyManager;
import com.materialism.economy.EconomyStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PayCommand {
    private PayCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher)
        );
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> pay(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        DoubleArgumentType.getDouble(context, "amount")
                                )))));
    }

    private static int pay(CommandSourceStack source, ServerPlayer target, double amount)
            throws CommandSyntaxException {
        ServerPlayer payer = source.getPlayerOrException();

        if (payer == target) {
            source.sendFailure(Component.translatable("materialism.command.pay.self"));
            return 0;
        }

        long units;
        try {
            units = BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(4)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            source.sendFailure(Component.translatable("materialism.command.pay.invalid"));
            return 0;
        }

        EconomyStore economy = EconomyManager.get(source.getServer());
        if (!economy.transfer(payer.getUUID(), target.getUUID(), units)) {
            source.sendFailure(Component.translatable(
                    "materialism.command.pay.insufficient",
                    Money.currency(economy.get(payer.getUUID()))
            ));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        "materialism.command.pay.sent",
                        target.getDisplayName(),
                        Money.currency(units)
                ),
                false
        );
        target.sendSystemMessage(Component.translatable(
                "materialism.command.pay.received",
                Money.currency(units),
                payer.getDisplayName()
        ));
        return 1;
    }
}
