// Copyright (c) 2022. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.task;

import de.lmichaelis.aurora.model.Claim;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimVisualizationTask implements Runnable {
	private static final Particle PARTICLE = Particle.REDSTONE.builder().force(false).particle();
	private final @NotNull Player player;

	private final Claim claim;

	private final int particleDensityX, particleDensityY, particleDensityZ;
	private final Particle.DustOptions options;


	public ClaimVisualizationTask(final @NotNull Claim claim, final @NotNull Player player, Color color) {
		this.player = player;
		this.claim = claim;
		this.options = new Particle.DustOptions(color, 3);

		this.particleDensityX = (claim.maxX - claim.minX) / 2;
		this.particleDensityY = (claim.maxY - claim.minY) / 2;
		this.particleDensityZ = (claim.maxZ - claim.minZ) / 2;
	}

	private void drawLineX(int x, int y, int z) {
		player.spawnParticle(PARTICLE, x, y, z, Math.max(particleDensityX / 2, 5), particleDensityX / 2., 0, 0, options);
	}

	private void drawLineY(int x, int y, int z) {
		player.spawnParticle(PARTICLE, x, y, z, Math.max(particleDensityY / 3, 5), 0, Math.max(particleDensityY / 3.f, 1), 0, options);

		// Don't show highlights for sub-claims.
		if (claim.parent == null)  {
			player.spawnParticle(PARTICLE, x, this.player.getLocation().getY(), z, 20, 0, 10, 0, options);
		}
	}

	private void drawLineZ(int x, int y, int z) {
		player.spawnParticle(PARTICLE, x, y, z, Math.max(particleDensityZ / 2, 5), 0, 0, particleDensityZ / 3.f, options);
	}

	@Override
	public void run() {
		// Draw the corners
		drawLineY(claim.minX, claim.minY + particleDensityY, claim.minZ);
		drawLineY(claim.minX, claim.minY + particleDensityY, claim.maxZ + 1);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY, claim.minZ);
		drawLineY(claim.maxX + 1, claim.minY + particleDensityY, claim.maxZ + 1);

		// Draw the bottom rectangle
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.minY, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.minY, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.minY, claim.minZ + particleDensityZ);

		// Draw the top rectangle
		drawLineX(claim.minX + particleDensityX, claim.maxY, claim.minZ);
		drawLineX(claim.minX + particleDensityX, claim.maxY, claim.maxZ + 1);
		drawLineZ(claim.minX, claim.maxY, claim.minZ + particleDensityZ);
		drawLineZ(claim.maxX + 1, claim.maxY, claim.minZ + particleDensityZ);

		// Draw a line at the player's head position only if we're not inspecting a subclaim
		if (claim.parent == null) {
			final var playerY = (int) player.getLocation().getY() + 1;
			drawLineX(claim.minX + particleDensityX, playerY, claim.minZ);
			drawLineX(claim.minX + particleDensityX, playerY, claim.maxZ + 1);
			drawLineZ(claim.minX, playerY, claim.minZ + particleDensityZ);
			drawLineZ(claim.maxX + 1, playerY, claim.minZ + particleDensityZ);
		}
	}
}
