// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Arrays;
import java.util.EnumSet;

public final class Predicates {
	// Note: This set of blocks was adapted from GriefPrevention
	private static final EnumSet<Material> INTERACT_PROTECTED_BLOCKS = EnumSet.of(
			Material.ANVIL,
			Material.BEACON,
			Material.BEE_NEST,
			Material.BEEHIVE,
			Material.BELL,
			Material.CAKE,
			Material.CAULDRON,
			Material.COMPOSTER,
			Material.CAVE_VINES,
			Material.CAVE_VINES_PLANT,
			Material.CHIPPED_ANVIL,
			Material.DAMAGED_ANVIL,
			Material.JUKEBOX,
			Material.PUMPKIN,
			Material.RESPAWN_ANCHOR,
			Material.ROOTED_DIRT,
			Material.SWEET_BERRY_BUSH,
			Material.LEVER
	);

	// Note: This set of blocks was adapted from GriefPrevention
	private static final EnumSet<Material> BUILD_PROTECTED_BLOCKS = EnumSet.of(
			Material.NOTE_BLOCK,
			Material.REPEATER,
			Material.DRAGON_EGG,
			Material.DAYLIGHT_DETECTOR,
			Material.COMPARATOR,
			Material.REDSTONE_WIRE
	);

	// Note: This set of blocks was adapted from GriefPrevention
	private static final EnumSet<Material> BUILD_PROTECTED_ITEMS = EnumSet.of(
			Material.BONE_MEAL,
			Material.ARMOR_STAND,
			Material.END_CRYSTAL,
			Material.FLINT_AND_STEEL,
			Material.INK_SAC,
			Material.GLOW_INK_SAC,
			Material.MINECART,
			Material.CHEST_MINECART,
			Material.TNT_MINECART,
			Material.HOPPER_MINECART,
			Material.FURNACE_MINECART
	);

	private static final EnumSet<Material> SPAWN_EGGS;
	private static final EnumSet<Material> DYES;

	static {
		SPAWN_EGGS = EnumSet.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_SPAWN_EGG")).toList());
		DYES = EnumSet.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_DYE")).toList());
	}

	public static boolean isInteractInteractProtected(final Material material) {
		return INTERACT_PROTECTED_BLOCKS.contains(material) ||
				Tag.CANDLES.isTagged(material) ||
				Tag.CANDLE_CAKES.isTagged(material) ||
				Tag.WOODEN_TRAPDOORS.isTagged(material) ||
				Tag.WOODEN_DOORS.isTagged(material) ||
				Tag.FENCE_GATES.isTagged(material) ||
				Tag.BUTTONS.isTagged(material);
	}

	public static boolean isInteractBuildProtected(final Material material) {
		return BUILD_PROTECTED_BLOCKS.contains(material) ||
				Tag.FLOWER_POTS.isTagged(material);
	}

	public static boolean isUseBuildProtected(final Material material) {
		return BUILD_PROTECTED_ITEMS.contains(material) ||
				DYES.contains(material) ||
				SPAWN_EGGS.contains(material) ||
				Tag.ITEMS_BOATS.isTagged(material);
	}
}
