// Copyright (c) 2021. Luis Michaelis
// SPDX-License-Identifier: LGPL-3.0-only
package de.lmichaelis.aurora.config;

/**
 * A list of all messages used by Aurora.
 */
public final class MessagesConfig {
	public String tooFarAway = "§cThat's too far away!";
	public String notAClaim = "§cThere is no claim here.";
	public String alreadyClaimed = "§cThere is already a claim here.";
	public String blockClaimedBy = "§cThis block is claimed by %s.";
	public String claimCornerSet = "§aFirst claim corner set.";
	public String needMoreClaimBlocks = "§cYou need %d more claim blocks to be able to claim this area.";
	public String wouldOverlapAnotherClaim = "§cYou can't create a claim here because it would overlap another one.";
	public String claimCreated = "§aClaim created (%d x %d blocks).";
	public String claimResized = "§aClaim resized (%d x %d blocks).";
	public String resizingClaim = "§aResizing claim.";
	public String noPermission = "§cYou don't have permission to do that here.";
}
