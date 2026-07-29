package com.github.bdev42.ioucreative;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public class IOUCreativeCommands {

    public static int executeListCommand(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        MutableComponent subject;
        if (context.getSource().isPlayer() && player.is(context.getSource().getPlayerOrException())) {
            subject = Component.literal("Your");
        } else {
            subject = Component.literal("").append(player.getName()).append("'s");
        }

        context.getSource().sendSuccess(() -> subject.withStyle(ChatFormatting.GREEN)
                .append(" current debt is:\n").append(IOUCreative.getPlayerDebtList(player)), false);
        return 1;
    }

    public static int executePayCommand(CommandContext<CommandSourceStack> context, int maxQuantity) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (player.isCreative()) {
            context.getSource().sendFailure(Component.literal("You cannot use this command in Creative"));
            return 0;
        }

        Item item = ItemArgument.getItem(context, "owed_item").item().value();

        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(context.getSource().getLevel());
        int amountOwed = debtData.getPlayerDebt(player, item);
        if (amountOwed < 1) {
            context.getSource().sendFailure(Component.literal("You do not owe any " + item));
            return 0;
        }
        maxQuantity = maxQuantity == -1 ? amountOwed : Math.min(amountOwed, maxQuantity);

        int actuallyRemoved = player.getInventory().clearOrCountMatchingItems(itemStack -> itemStack.is(item), maxQuantity, player.inventoryMenu.getCraftSlots());
        if (actuallyRemoved == 0) {
            context.getSource().sendFailure(Component.literal("You do not have any of this item on you"));
            return 0;
        }

        debtData.decreasePlayerDebt(player, item, actuallyRemoved);
        context.getSource().sendSuccess(() -> {
            MutableComponent msg = Component.literal("Successfully paid off " + actuallyRemoved + " ").withStyle(ChatFormatting.GREEN).append(Component.translatable(item.getDescriptionId()));
            int stillOwed = debtData.getPlayerDebt(player, item);
            if (stillOwed > 0) msg.append(". You still owe " + stillOwed);
            return msg;
        }, false);
        return 1;
    }

    public static int executeAdminDebtResetCommand(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, @Nullable Item owed_item) {
        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(context.getSource().getLevel());
        int numSuccessful = 0;
        boolean individualFeedback = players.size() <= 5;

        for (ServerPlayer player : players) {
            if (owed_item == null) {
                int itemsReset = 0;
                for (Map.Entry<String, Integer> entry : debtData.getPlayerDebtMap(player).entrySet()) {
                    debtData.setPlayerDebt(player, BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getKey())), 0);
                    itemsReset++;
                }

                if (itemsReset < 1) {
                    if (individualFeedback)
                        context.getSource().sendFailure(Component.literal("No debt to reset for ").append(player.getName()));
                } else {
                    if (individualFeedback)
                        context.getSource().sendSuccess(() -> Component.literal("Reset all debt for ").append(player.getName()), true);
                    numSuccessful++;
                }
            } else {
                if (debtData.getPlayerDebt(player, owed_item) == 0) {
                    if (individualFeedback)
                        context.getSource().sendFailure(Component.literal("No " + owed_item + " debt to reset for ").append(player.getName()));
                    continue;
                }
                debtData.setPlayerDebt(player, owed_item, 0);
                if (individualFeedback)
                    context.getSource().sendSuccess(() -> Component.literal("Reset " + owed_item + " debt for ").append(player.getName()), true);
                numSuccessful++;
            }
        }

        if (!individualFeedback) {
            if (numSuccessful < 1) {
                context.getSource().sendFailure(Component.literal("Failed to reset the debt of any players"));
            } else {
                int finalNumSuccessful = numSuccessful;
                context.getSource().sendSuccess(() -> Component.literal("Reset the debt of " + finalNumSuccessful + " out of " + players.size() + " players"), true);
            }
        }
        return numSuccessful;
    }

    public static int executeAdminDebtSetCommand(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, Item item, int value) {
        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(context.getSource().getLevel());
        for (ServerPlayer player : players) {
            debtData.setPlayerDebt(player, item, value);
        }

        context.getSource().sendSuccess(() -> Component.literal("Set "+item+" debt to "+value+" for ")
                .append(players.size() == 1 ? players.iterator().next().getName() : Component.literal(players.size()+" players")), true);
        return players.size();
    }

    public static int executeAdminRestoreInventorySlot(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, SeparateInventoryData.SaveSlot saveSlot) {
        SeparateInventoryData inventoryData = SeparateInventoryData.getSeparateInventoryData(context.getSource().getLevel());
        for (ServerPlayer player : players) {
            inventoryData.restore(player, saveSlot);
        }

        context.getSource().sendSuccess(() -> Component.literal("Restored inventory from slot "+saveSlot.name()+" for ")
                .append(players.size() == 1 ? players.iterator().next().getName() : Component.literal(players.size()+" players")), true);
        return players.size();
    }

    public static int executeAdminStoreInventorySlot(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, SeparateInventoryData.SaveSlot saveSlot) {
        SeparateInventoryData inventoryData = SeparateInventoryData.getSeparateInventoryData(context.getSource().getLevel());
        for (ServerPlayer player : players) {
            inventoryData.save(player, saveSlot);
        }

        context.getSource().sendSuccess(() -> Component.literal("Stored current inventory to slot "+saveSlot.name()+" for ")
                .append(players.size() == 1 ? players.iterator().next().getName() : Component.literal(players.size()+" players")), true);
        return players.size();
    }

    public static int executeAdminResetInventorySlot(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, SeparateInventoryData.@Nullable SaveSlot saveSlot) {
        SeparateInventoryData inventoryData = SeparateInventoryData.getSeparateInventoryData(context.getSource().getLevel());
        for (ServerPlayer player : players) {
            if (saveSlot == null) {
                for (SeparateInventoryData.SaveSlot eachSlot : SeparateInventoryData.SaveSlot.values()) {
                    inventoryData.reset(player, eachSlot);
                }
            } else {
                inventoryData.reset(player, saveSlot);
            }
        }

        context.getSource().sendSuccess(() -> Component.literal("Reset ").append(saveSlot == null ? "all" : saveSlot.name())
                .append(" inventory slot(s) for ")
                .append(players.size() == 1 ? players.iterator().next().getName() : Component.literal(players.size()+" players")), true);
        return players.size();
    }
}
