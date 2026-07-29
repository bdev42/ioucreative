package com.github.bdev42.ioucreative;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class IOUCreative implements ModInitializer {
    public static final String MOD_ID = "ioucreative";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("This mod is a Proof of Concept. Thanks for trying it :)");
        PlayerBlockBreakEvents.AFTER.register(IOUCreative::onBlockBroken);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            dispatcher.register(Commands.literal("iouc")
                    .then(Commands.literal("list")
                            .executes(ctx -> IOUCreativeCommands.executeListCommand(ctx, ctx.getSource().getPlayerOrException())).requires(CommandSourceStack::isPlayer)
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> IOUCreativeCommands.executeListCommand(ctx, EntityArgument.getPlayer(ctx, "player")))))
                    .then(Commands.literal("pay").requires(CommandSourceStack::isPlayer)
                            .then(Commands.argument("owed_item", ItemArgument.item(buildContext))
                                    .executes(ctx -> IOUCreativeCommands.executePayCommand(ctx, 64))
                                    .then(Commands.literal("all")
                                            .executes(ctx -> IOUCreativeCommands.executePayCommand(ctx, -1)))
                                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                            .executes(ctx -> IOUCreativeCommands.executePayCommand(ctx, IntegerArgumentType.getInteger(ctx, "quantity"))))))
                    .then(Commands.literal("admin").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                            .then(Commands.literal("debt-reset")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .executes(ctx -> IOUCreativeCommands.executeAdminDebtResetCommand(ctx, EntityArgument.getPlayers(ctx, "players"), null))
                                            .then(Commands.argument("owed_item", ItemArgument.item(buildContext))
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminDebtResetCommand(ctx, EntityArgument.getPlayers(ctx, "players"), ItemArgument.getItem(ctx, "owed_item").item().value())))))
                            .then(Commands.literal("debt-set")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                    .then(Commands.argument("value", IntegerArgumentType.integer())
                                                            .executes(ctx -> IOUCreativeCommands.executeAdminDebtSetCommand(ctx, EntityArgument.getPlayers(ctx, "players"), ItemArgument.getItem(ctx, "item").item().value(), IntegerArgumentType.getInteger(ctx, "value")))))))
                            .then(Commands.literal("inv-restore")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.literal("CREATIVE")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminRestoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.CREATIVE)))
                                            .then(Commands.literal("SURVIVAL")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminRestoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.SURVIVAL)))
                                            .then(Commands.literal("BACKUP")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminRestoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.BACKUP)))))
                            .then(Commands.literal("inv-store")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.literal("CREATIVE")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminStoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.CREATIVE)))
                                            .then(Commands.literal("SURVIVAL")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminStoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.SURVIVAL)))
                                            .then(Commands.literal("BACKUP")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminStoreInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.BACKUP)))))
                            .then(Commands.literal("inv-reset")
                                    .then(Commands.argument("players", EntityArgument.players())
                                            .then(Commands.literal("all")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminResetInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), null)))
                                            .then(Commands.literal("CREATIVE")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminResetInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.CREATIVE)))
                                            .then(Commands.literal("SURVIVAL")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminResetInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.SURVIVAL)))
                                            .then(Commands.literal("BACKUP")
                                                    .executes(ctx -> IOUCreativeCommands.executeAdminResetInventorySlot(ctx, EntityArgument.getPlayers(ctx, "players"), SeparateInventoryData.SaveSlot.BACKUP))))))
            );
        });
    }

    public static void onBlockPlaced(BlockPlaceContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel) || context.getPlayer() == null) return;
        if (!context.getPlayer().isCreative()) return;
        Item placedItem = new BlockInWorld(context.getLevel(), context.getClickedPos(), false).getState().getBlock().asItem();

        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(serverLevel);
        debtData.increasePlayerDebt(context.getPlayer(), placedItem, 1);

        context.getPlayer().sendOverlayMessage(Component.literal("+1 ").withStyle(ChatFormatting.RED).append(Component.literal(placedItem.toString()).withStyle(ChatFormatting.AQUA)));
    }

    public static void onBlockBroken(Level level, Player player, BlockPos pos, BlockState blockState, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!player.isCreative()) return;
        Item brokenItem = blockState.getBlock().asItem();

        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(serverLevel);
        debtData.decreasePlayerDebt(player, brokenItem, 1);

        player.sendOverlayMessage(Component.literal("-1 ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(brokenItem.toString()).withStyle(ChatFormatting.AQUA)));
    }

    public static void onGameModeChanged(ServerPlayer player, GameType gameModeForPlayer, @Nullable GameType previousGameModeForPlayer) {
        SeparateInventoryData inventoryData = SeparateInventoryData.getSeparateInventoryData(player.level());
        if (gameModeForPlayer.isCreative()) {
            inventoryData.save(player, SeparateInventoryData.SaveSlot.SURVIVAL);
            inventoryData.restore(player, SeparateInventoryData.SaveSlot.CREATIVE);

            player.sendSystemMessage(wrapMessage(Component.literal("Tracking creative mode activity.")));
        } else if (previousGameModeForPlayer != null && previousGameModeForPlayer.isCreative()) {
            inventoryData.save(player, SeparateInventoryData.SaveSlot.CREATIVE);
            inventoryData.restore(player, SeparateInventoryData.SaveSlot.SURVIVAL);

            player.sendSystemMessage(wrapMessage(Component.literal("Your updated debt is:\n"))
                    .append(getPlayerDebtList(player))
            );
        }
    }

    public static MutableComponent wrapMessage(MutableComponent message) {
        return Component.literal("IOUCreative: ").withStyle(ChatFormatting.AQUA).append(message.withStyle(ChatFormatting.GREEN));
    }

    public static MutableComponent getPlayerDebtList(ServerPlayer player) {
        PlayersDebtData debtData = PlayersDebtData.getPlayersDebtData(player.level());
        List<Map.Entry<String, Integer>> sortedEntryList = new ArrayList<>(debtData.getPlayerDebtMap(player).entrySet());
        sortedEntryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        MutableComponent list = Component.literal("");
        for (Map.Entry<String, Integer> debtEntry : sortedEntryList) {
            int val = debtEntry.getValue();
            if (val == 0) continue;
            String valDigits = "" + Math.abs(val);

            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(debtEntry.getKey()));

            MutableComponent line = Component.literal(val > 0 ? "+ " : "- ");
            if (val > 0)
                line.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(new ItemStackTemplate(item))).withClickEvent(new ClickEvent.SuggestCommand("/iouc pay " + item + " all")));
            line.append(". ".repeat(Math.max(6 - valDigits.length(), 0))).append(valDigits).append("  ");
            line.withStyle(debtEntry.getValue() > 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
            line.append(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.AQUA));

            list.append(line.append("\n"));
        }
        return list;
    }
}
