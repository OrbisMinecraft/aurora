// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.listener;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.Predicates;
import de.lmichaelis.aurora.model.Claim;
import de.lmichaelis.aurora.model.Group;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.projectiles.BlockProjectileSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Event handlers for entity events.
 */
public final class EntityEventListener extends BaseListener {
	public EntityEventListener(final Aurora plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityBreakDoor(final @NotNull EntityBreakDoorEvent event) {
		final var claim = Claim.getClaim(event.getBlock().getLocation());

		// Rule: Zombies can always break doors outside of claims
		if (claim == null) return;

		// Rule: If mob griefing is enabled in the claim, zombies can break doors
		if (claim.mobGriefing) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(final @NotNull EntityDamageEvent event) {
		final var entity = event.getEntity();
		final var cause = event.getCause();

		if (Predicates.isHostileEntity(entity)) {
			// Rule: Allow damaging all unnamed hostile entities everywhere
			if (entity.customName() == null) return;

			// Rule: Don't allow damaging named hostile entities in claims
			// TODO: Disable this check when a player has taken damage from the entity or disable
			//       named hostile entities tracking players
			final var claim = Claim.getClaim(entity.getLocation());
			if (claim != null) event.setCancelled(true);

			return;
		}

		// Rule: Allow damaging only tamable entities that haven't been tamed yet.
		// TODO: Disable this check when a player has taken damage from the entity or disable
		//       player tracking on tamed animals
		if (entity instanceof final Tameable tameable && !tameable.isTamed()) return;

		// Rule: Allow damage from block explosions only in claims with explosions enabled
		if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
			final var claim = Claim.getClaim(entity.getLocation());
			if (claim != null && claim.allowsExplosions) return;
		}

		// Only entities damaging entities is handled from now on
		if (!(event instanceof final EntityDamageByEntityEvent entityEvent)) {
			event.setCancelled(true);
			return;
		}

		final var claim = Claim.getClaim(entity.getLocation());
		var damager = entityEvent.getDamager();

		// Rule: Always allow any other entities to be damaged outside of claims
		if (claim == null) return;

		// TODO: Check fireworks fired from a crossbow
		if (damager instanceof final Projectile projectile) {
			if (projectile.getShooter() instanceof final Entity shooter) damager = shooter;

			// Rule: Allow all damage from non-player projectiles originating from a dispenser inside the claim
			if (projectile.getShooter() instanceof final BlockProjectileSource source) {
				final var sourceClaim = Claim.getClaim(source.getBlock().getLocation());
				if (sourceClaim != null && Objects.equals(sourceClaim.owner, claim.owner)) return;
			}
		}

		// TODO: Make sure a player thrown trident's lightning strike cannot damage entities

		// Rule: Allow entity explosions to damage entities only when
		//       explosions are enabled in the claim
		if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
			if (claim.allowsExplosions) return;
		}

		// Rule: Only allow the lingering effect to damage players within PvP-enabled claims
		if (damager.getType() == EntityType.AREA_EFFECT_CLOUD && entity.getType() == EntityType.PLAYER) {
			if (!claim.pvpEnabled) event.setCancelled(true);
			return;
		}

		// Rule: Allow PvP combat only in PvP-enabled claims
		if (damager.getType() == EntityType.PLAYER && entity.getType() == EntityType.PLAYER) {
			if (damager != entity && !claim.pvpEnabled) {
				damager.sendMessage(plugin.config.messages.noPermission);
				event.setCancelled(true);
			}

			return;
		}

		// Rule: Allow taking damage from tame-ables only if they have not been tamed yet
		//       or PvP is enabled in the claim
		if (entity.getType() == EntityType.PLAYER && (damager instanceof final Tameable pet)) {
			if (pet.isTamed() && !claim.pvpEnabled) event.setCancelled(true);
			return;
		}

		// Rule: Allow all players with BUILD permission to damage all other entities in claims
		if (damager.getType() == EntityType.PLAYER) {
			if (!claim.isAllowed((Player) damager, Group.BUILD)) {
				damager.sendMessage(plugin.config.messages.noPermission);
				event.setCancelled(true);
			}

			return;
		}

		// Rule: Allow all other sources to damage players (ie. mobs)
		if (entity.getType() == EntityType.PLAYER) return;

		// Rule: Allow mobs to damage other entities only if mob griefing is enabled
		if (claim.mobGriefing) return;

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(final @NotNull EntityExplodeEvent event) {
		final var iter = event.blockList().iterator();
		while (iter.hasNext()) {
			final var block = iter.next();

			// Ignore air blocks
			if (block.getType().isAir()) continue;

			// TODO: Cache the claim for better efficiency
			final var claim = Claim.getClaim(block.getLocation());

			// Rule: Explosions can affect all blocks outside of claims
			if (claim == null) continue;

			// Rule: If explosions are turned on in the claim, all blocks can be destroyed
			if (claim.allowsExplosions) continue;

			// Otherwise, prevent the block from breaking
			iter.remove();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(final @NotNull EntityInteractEvent event) {
		final var block = event.getBlock();

		// We're only interested in farmland at the moment
		if (block.getType() == Material.FARMLAND) {
			final var claim = Claim.getClaim(block.getLocation());

			// Rule: Entities can always trample crops outside of claims
			if (claim == null) return;

			// Rule: Inside of claims, entities can only trample crops if mob griefing is enabled
			if (claim.mobGriefing) return;

			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(final @NotNull EntityChangeBlockEvent event) {
		final var block = event.getBlock();
		final var entity = event.getEntity();
		final var claim = Claim.getClaim(block.getLocation());

		// Rule: Entities can always change blocks outside of claims
		if (claim == null) return;

		if (Predicates.canEntityChangeBlock(entity)) {
			// Rule: Mobs can only change blocks if mob griefing is enabled in the claim
			if (claim.mobGriefing) return;
		} else if (entity instanceof final Projectile projectile) {
			if (projectile.getShooter() instanceof final Player player) {
				// Rule: Only players with build permissions may knock down dripstone or ignite TNT
				if (claim.isAllowed(player, Group.BUILD)) return;
			} else if (projectile.getShooter() instanceof final BlockProjectileSource source) {
				// Rule: Dispensers shooting arrows from within a claim with the same
				//       owner can also change blocks
				final var sourceClaim = Claim.getClaim(source.getBlock().getLocation());
				if (sourceClaim != null && sourceClaim.owner == claim.owner) return;
			}
		} else if (entity instanceof Vehicle && !entity.getPassengers().isEmpty()) {
			// Rule: Players in boats cannot break lily pads (for example) unless
			//       they are in the BUILD group
			if (entity.getPassengers().get(0) instanceof final Player player && claim.isAllowed(player, Group.BUILD))
				return;
		}

		// FIXME: Prevent sand cannons from working. This requires checking for a falling
		//        gravity block and setting some metadata on it.

		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(final @NotNull ProjectileHitEvent event) {
		final var block = event.getHitBlock();
		final var projectile = event.getEntity();

		// Quirk: Shooting chorus flowers does not emit a `EntityChangeBlockEvent`
		// Rule: Only players with the BUILD group may break chorus flowers in claims
		if (block != null && block.getType() == Material.CHORUS_FLOWER) {
			final var claim = Claim.getClaim(block.getLocation());

			if (claim == null) return;
			if (projectile.getShooter() instanceof final Player player) {
				if (claim.isAllowed(player, Group.BUILD)) return;
				player.sendMessage(plugin.config.messages.noPermission);
			}

			// TODO: Find out whether we should check for mob griefing or projectiles
			//       fired by dispensers here
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(final @NotNull PotionSplashEvent event) {
		final var potion = event.getPotion();
		final var thrower = potion.getShooter();
		final var player = thrower instanceof Player ? (Player) thrower : null;

		// I don't know when it can be null. Just ignore potions thrown by nobody.
		if (thrower == null) return;

		for (final var effect : potion.getEffects()) {
			// Rule: Always allow all positive potion effects
			if (Predicates.isPositiveEffect(effect)) continue;

			for (final var affected : event.getAffectedEntities()) {
				// Rule: Always affect the thrower with negative effects
				if (affected == thrower) continue;
				if (!Predicates.isProtectedEntity(affected)) continue;

				// TODO: Cache the claim for better efficiency
				final var claim = Claim.getClaim(affected.getEyeLocation());

				// Rule: Players can apply all effects to all entities outside of claims
				if (claim == null) return;

				if (player != null) {
					// Rule: Players cannot be damaged by potions thrown by other players
					//       in PvP-protected claims
					if (affected instanceof Player) {
						if (!claim.pvpEnabled) {
							event.setIntensity(affected, 0);
							player.sendMessage(plugin.config.messages.pvpDisabled);
						}

						continue;
					}

					// Rule: Players can only apply negative effects to entities inside
					//       claims if they have the BUILD group
					if (!claim.isAllowed(player, Group.BUILD)) {
						event.setIntensity(affected, 0);
						player.sendMessage(plugin.config.messages.noPermission);
					}
				} else if (thrower instanceof final BlockProjectileSource source) {
					// Rule: Dispensers in a claim owned by the same player can apply negative
					//       effects to entities inside it.
					final var sourceClaim = Claim.getClaim(source.getBlock().getLocation());
					if (sourceClaim == null || sourceClaim.owner != claim.owner) event.setIntensity(affected, 0);
				}
			}
		}
	}
}
