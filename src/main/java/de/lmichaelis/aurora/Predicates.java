// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public final class Predicates {
	// Note: This set of blocks was adapted from GriefPrevention
	private static final EnumSet<Material> INTERACT_ACCESS_PROTECTED = EnumSet.of(
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
	private static final EnumSet<Material> INTERACT_BUILD_PROTECTED = EnumSet.of(
			Material.NOTE_BLOCK,
			Material.REPEATER,
			Material.DRAGON_EGG,
			Material.DAYLIGHT_DETECTOR,
			Material.COMPARATOR,
			Material.REDSTONE_WIRE
	);

	// Note: This set of blocks was adapted from GriefPrevention
	private static final EnumSet<Material> PLACE_BUILD_PROTECTED = EnumSet.of(
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

	private static final EnumSet<EntityType> BLOCK_ALTERING_ENTITIES = EnumSet.of(
			EntityType.ENDERMAN,
			EntityType.SILVERFISH,
			EntityType.RABBIT,
			EntityType.RAVAGER,

			EntityType.WITHER,
			EntityType.CREEPER
	);

	private static final Set<PotionEffectType> POSITIVE_EFFECT_TYPES = Set.of(
			PotionEffectType.SPEED,
			PotionEffectType.FAST_DIGGING,
			PotionEffectType.INCREASE_DAMAGE,
			PotionEffectType.HEAL,
			PotionEffectType.REGENERATION,
			PotionEffectType.DAMAGE_RESISTANCE,
			PotionEffectType.FIRE_RESISTANCE,
			PotionEffectType.WATER_BREATHING,
			PotionEffectType.INVISIBILITY,
			PotionEffectType.NIGHT_VISION,
			PotionEffectType.HEALTH_BOOST,
			PotionEffectType.ABSORPTION,
			PotionEffectType.SATURATION,
			PotionEffectType.GLOWING,
			PotionEffectType.LUCK,
			PotionEffectType.SLOW_FALLING,
			PotionEffectType.CONDUIT_POWER,
			PotionEffectType.DOLPHINS_GRACE,
			PotionEffectType.HERO_OF_THE_VILLAGE,
			PotionEffectType.JUMP
	);

	private static final EnumSet<Material> SPAWN_EGGS;
	private static final EnumSet<Material> DYES;

	static {
		SPAWN_EGGS = EnumSet.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_SPAWN_EGG")).toList());
		DYES = EnumSet.copyOf(Arrays.stream(Material.values()).filter(material -> material.name().endsWith("_DYE")).toList());
	}

	public static boolean isInteractAccessProtected(final Material material) {
		return INTERACT_ACCESS_PROTECTED.contains(material) ||
				Tag.CANDLES.isTagged(material) ||
				Tag.CANDLE_CAKES.isTagged(material) ||
				Tag.WOODEN_TRAPDOORS.isTagged(material) ||
				Tag.WOODEN_DOORS.isTagged(material) ||
				Tag.FENCE_GATES.isTagged(material) ||
				Tag.BUTTONS.isTagged(material);
	}

	public static boolean isInteractBuildProtected(final Material material) {
		return INTERACT_BUILD_PROTECTED.contains(material) ||
				Tag.FLOWER_POTS.isTagged(material);
	}

	public static boolean isPlaceBuildProtected(final Material material) {
		return PLACE_BUILD_PROTECTED.contains(material) ||
				DYES.contains(material) ||
				SPAWN_EGGS.contains(material) ||
				Tag.ITEMS_BOATS.isTagged(material);
	}

	public static boolean hasEntityContainer(final @NotNull Entity entity) {
		return entity.getType() == EntityType.ARMOR_STAND ||
				entity instanceof Hanging ||
				entity instanceof InventoryHolder || // includes villagers, minecarts and all other entities with attached storage
				entity instanceof PoweredMinecart;
	}

	public static boolean canEntityChangeBlock(final @NotNull Entity entity) {
		return BLOCK_ALTERING_ENTITIES.contains(entity.getType());
	}

	public static boolean isPositiveEffect(final @NotNull PotionEffect effect) {
		return POSITIVE_EFFECT_TYPES.contains(effect.getType());
	}

	public static boolean isProtectedEntity(final @NotNull Entity entity) {
		return entity.getType() == EntityType.VILLAGER || entity instanceof Animals;
	}
}
