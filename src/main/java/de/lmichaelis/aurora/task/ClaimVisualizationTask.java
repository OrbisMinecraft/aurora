// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.task;

import de.lmichaelis.aurora.Aurora;
import de.lmichaelis.aurora.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimVisualizationTask implements Runnable {
	private static final Particle PARTICLE = Particle.REDSTONE.builder().force(false).particle();
	private final @NotNull Player player;

	private final Claim claim;
	private final double particleDensityX, particleDensityY, particleDensityZ;
	private final Particle.DustOptions options;
	private int id;


	public ClaimVisualizationTask(final @NotNull Claim claim, final @NotNull Player player, Color color) {
		this.player = player;
		this.claim = claim;
		this.options = new Particle.DustOptions(color, 3);

		this.particleDensityX = (claim.maxX - claim.minX) / 2.d;
		this.particleDensityY = (claim.maxY - claim.minY) / 2.d;
		this.particleDensityZ = (claim.maxZ - claim.minZ) / 2.d;
	}

	public static @NotNull ClaimVisualizationTask spawn(final @NotNull Claim claim, final @NotNull Player player, Color color, final int cancelAfterTicks) {
		final var task = new ClaimVisualizationTask(claim, player, color);
		task.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(Aurora.instance, task, 0, 5);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Aurora.instance, task::cancel, cancelAfterTicks);
		return task;
	}

	private void drawLineX(double x, double y, double z) {
		player.spawnParticle(PARTICLE, x, y, z, (int) Math.ceil(particleDensityX / 2), particleDensityX / 2.d, 0, 0, options);
	}

	private void drawLineY(double x, double y, double z) {
		player.spawnParticle(PARTICLE, x, y, z, (int) Math.max(particleDensityY / 3, 5), 0, particleDensityY / 2.d, 0, options);

		// Don't show highlights for sub-claims.
		if (claim.parent == null) {
			player.spawnParticle(PARTICLE, x, this.player.getLocation().getY(), z, 20, 0, 10, 0, options);
		}
	}

	private void drawLineZ(double x, double y, double z) {
		player.spawnParticle(PARTICLE, x, y, z, (int) Math.ceil(particleDensityZ / 2), 0, 0, particleDensityZ / 2.d, options);
	}

	@Override
	public void run() {
		// Draw the corners
		drawLineY(claim.minX, claim.minY + particleDensityY + 0.5, claim.minZ);
		drawLineY(claim.minX, claim.minY + particleDensityY + 0.5, claim.maxZ + 1);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY + 0.5, claim.minZ);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY + 0.5, claim.maxZ + 1);

		// Draw the bottom rectangle
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.minY, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.minY, claim.minZ + particleDensityZ);

		// Draw the top rectangle
		drawLineX(claim.minX + particleDensityX, claim.maxY + 1, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.maxY + 1, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.maxY + 1, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.maxY + 1, claim.minZ + particleDensityZ);

		// Draw a line at the player's head position only if we're not inspecting a subclaim
		if (claim.parent == null) {
			final var playerY = (int) player.getLocation().getY() + 1;
			drawLineX(claim.minX + particleDensityX, playerY, claim.minZ);
			drawLineX(claim.minX + particleDensityX, playerY, claim.maxZ + 1);
			drawLineZ(claim.minX, playerY, claim.minZ + particleDensityZ);
			drawLineZ(claim.maxX + 1, playerY, claim.minZ + particleDensityZ);
		}
	}

	public void cancel() {
		if (id != -1) {
			Bukkit.getScheduler().cancelTask(id);
			id = -1;
		}
	}
}
