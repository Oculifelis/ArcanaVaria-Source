package com.arcanavaria.lens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a written Blueprint Scroll holds: blocks at positions relative to the copied region's lowest corner. A datapack
 * loot table can write one (set_components), which is how the Forsaken Laboratories hand out prebuilt scrolls.
 */
public record BlueprintData(String name, List<Entry> blocks) {
    public static final int MAX = 4096;

    public BlueprintData {
        blocks = List.copyOf(blocks);
        if (blocks.size() > MAX) {
            throw new IllegalArgumentException("Blueprint exceeds " + MAX + " blocks");
        }
    }

    public record Entry(BlockPos at, BlockState state) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("at").forGetter(Entry::at),
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state)).apply(i, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::at, ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::state, Entry::new);
    }

    public static final Codec<BlueprintData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(BlueprintData::name),
            Entry.CODEC.listOf(0, MAX).fieldOf("blocks").forGetter(BlueprintData::blocks)).apply(i, BlueprintData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BlueprintData::name, Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), BlueprintData::blocks,
            BlueprintData::new);

    public BlockPos size() {
        int x = 0, y = 0, z = 0;
        for (Entry e : blocks) {
            x = Math.max(x, e.at().getX() + 1);
            y = Math.max(y, e.at().getY() + 1);
            z = Math.max(z, e.at().getZ() + 1);
        }
        return new BlockPos(x, y, z);
    }

    /**
     * Adapted from Building Gadgets 2 v26.1.2.3 StatePos.rotate90Degrees and getItemList.
     * Copyright (c) 2023 Direwolf20-MC, MIT. Full notice in LICENSE-third-party.md.
     */
    public BlueprintData transformed(Rotation rotation, Mirror mirror) {
        List<Entry> transformed = blocks.stream().map(entry -> {
            BlockPos at = entry.at();
            if (mirror == Mirror.FRONT_BACK) {
                at = new BlockPos(-at.getX(), at.getY(), at.getZ());
            } else if (mirror == Mirror.LEFT_RIGHT) {
                at = new BlockPos(at.getX(), at.getY(), -at.getZ());
            }
            at = switch (rotation) {
                case NONE -> at;
                case CLOCKWISE_90 -> new BlockPos(-at.getZ(), at.getY(), at.getX());
                case CLOCKWISE_180 -> new BlockPos(-at.getX(), at.getY(), -at.getZ());
                case COUNTERCLOCKWISE_90 -> new BlockPos(at.getZ(), at.getY(), -at.getX());
            };
            return new Entry(at, com.arcanavaria.lens.bg2.BlockStates.cleanBlockState(entry.state()).mirror(mirror).rotate(rotation));
        }).toList();
        BlockPos min = transformed.stream().map(Entry::at).reduce(BlockPos::min).orElse(BlockPos.ZERO);
        return new BlueprintData(name, transformed.stream().map(entry -> new Entry(entry.at().subtract(min), entry.state())).toList());
    }

    public Map<Item, Integer> materials() {
        Map<Item, Integer> materials = new LinkedHashMap<>();
        for (Entry entry : blocks) {
            Item item = entry.state().getBlock().asItem();
            if (item != Items.AIR) {
                materials.merge(item, LensInventory.quantity(entry.state()), Integer::sum);
            }
        }
        return materials;
    }
}
