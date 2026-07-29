package com.github.bdev42.ioucreative;

import com.github.bdev42.ioucreative.mixin.LivingEntityAccessor;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SeparateInventoryData extends SavedData {
    private final Map<UUID, Map<SaveSlot, CompoundTag>> data;

    private static final Codec<SeparateInventoryData> CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC,
            Codec.unboundedMap(
                    SaveSlot.CODEC,
                    CompoundTag.CODEC
            )
    ).xmap(SeparateInventoryData::new, SeparateInventoryData::getData);

    private static final SavedDataType<SeparateInventoryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(IOUCreative.MOD_ID, "separate_inventory_data"),
            SeparateInventoryData::new,
            CODEC,
            DataFixTypes.PLAYER
    );

    public static SeparateInventoryData getSeparateInventoryData(ServerLevel level) {
        level = level.getServer().getLevel(ServerLevel.OVERWORLD);
        if (level == null) return new SeparateInventoryData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public SeparateInventoryData() {
        this.data = new HashMap<>();
    }

    public SeparateInventoryData(Map<UUID, Map<SaveSlot, CompoundTag>> data) {
        this.data = new HashMap<>(data);
        for (Map.Entry<UUID, Map<SaveSlot, CompoundTag>> entry : data.entrySet()) {
            this.data.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }

    private Map<UUID, Map<SaveSlot, CompoundTag>> getData() {
        return Collections.unmodifiableMap(data);
    }

    public void save(ServerPlayer player, SaveSlot saveSlot) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.getInventory().save(output.list("Inventory", ItemStackWithSlot.CODEC));
        EntityEquipment equipment = ((LivingEntityAccessor) player).ioucreative_getEquipment();
        if (!equipment.isEmpty()) {
            output.store("equipment", EntityEquipment.CODEC, equipment);
        }

        Map<SaveSlot, CompoundTag> playerMap = data.getOrDefault(player.getUUID(), new HashMap<>());
        playerMap.put(saveSlot, output.buildResult());
        data.put(player.getUUID(), playerMap);
        setDirty();
    }

    public void restore(ServerPlayer player, SaveSlot saveSlot) {
        CompoundTag tag = data.getOrDefault(player.getUUID(), new HashMap<>()).getOrDefault(saveSlot, new CompoundTag());
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag);

        player.getInventory().load(input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));

        EntityEquipment equipment = ((LivingEntityAccessor) player).ioucreative_getEquipment();
        equipment.setAll(input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));

        player.sendOverlayMessage(Component.literal("Restored inventory: ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(saveSlot.name()).withStyle(ChatFormatting.GREEN)));
    }

    public void reset(ServerPlayer player, SaveSlot saveSlot) {
        Map<SaveSlot, CompoundTag> playerMap = data.get(player.getUUID());
        if (playerMap == null) return;
        playerMap.remove(saveSlot);
        data.put(player.getUUID(), playerMap);
        setDirty();
    }

    public enum SaveSlot implements StringRepresentable {
        CREATIVE, SURVIVAL, BACKUP;

        @Override
        public @NonNull String getSerializedName() { return name().toLowerCase(); }

        public static final Codec<SaveSlot> CODEC = StringRepresentable.fromEnum(SaveSlot::values);
    }
}
