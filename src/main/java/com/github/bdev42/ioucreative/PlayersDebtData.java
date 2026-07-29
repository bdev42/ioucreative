package com.github.bdev42.ioucreative;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayersDebtData extends SavedData {
    private final Map<UUID, Map<String, Integer>> globalDebtMap;

    private static final Codec<PlayersDebtData> CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC,
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.INT
            )
    ).xmap(PlayersDebtData::new, PlayersDebtData::getGlobalDebtMap);

    private static final SavedDataType<PlayersDebtData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(IOUCreative.MOD_ID, "players_debt_data"),
            PlayersDebtData::new,
            CODEC,
            DataFixTypes.PLAYER
    );

    public static PlayersDebtData getPlayersDebtData(ServerLevel level) {
        level = level.getServer().getLevel(ServerLevel.OVERWORLD);
        if (level == null) return new PlayersDebtData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayersDebtData() {
        this.globalDebtMap = new HashMap<>();
    }

    public PlayersDebtData(Map<UUID, Map<String, Integer>> globalDebtMap) {
        this.globalDebtMap = new HashMap<>(globalDebtMap);
        for (Map.Entry<UUID, Map<String, Integer>> entry : globalDebtMap.entrySet()) {
            this.globalDebtMap.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }

    private Map<UUID, Map<String, Integer>> getGlobalDebtMap() {
        return Collections.unmodifiableMap(globalDebtMap);
    }

    public Map<String, Integer> getPlayerDebtMap(Player player) {
        return Collections.unmodifiableMap(globalDebtMap.getOrDefault(player.getUUID(), new HashMap<>()));
    }

    public int getPlayerDebt(Player player, Item item) {
        return getPlayerDebtMap(player).getOrDefault(item.toString(), 0);
    }

    public void setPlayerDebt(Player player, Item item, int amount) {
        Map<String, Integer> playerDebtMap = globalDebtMap.getOrDefault(player.getUUID(), new HashMap<>());

        if (amount == 0) {
            playerDebtMap.remove(item.toString());
        } else {
            playerDebtMap.put(item.toString(), amount);
        }
        globalDebtMap.put(player.getUUID(), playerDebtMap);
        setDirty();
    }

    public void increasePlayerDebt(Player player, Item item, int increaseBy) {
        assert increaseBy > 0;
        setPlayerDebt(player, item, getPlayerDebt(player, item) + increaseBy);
    }

    public void decreasePlayerDebt(Player player, Item item, int decreaseBy) {
        assert decreaseBy > 0;
        setPlayerDebt(player, item, getPlayerDebt(player, item) - decreaseBy);
    }
}
