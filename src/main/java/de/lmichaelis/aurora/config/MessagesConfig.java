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
	public String claimCornerSet = "§aFirst claim corner set (%d, %d, %d).";
	public String needMoreClaimBlocks = "§cYou need %d more claim blocks to be able to claim this area.";
	public String wouldOverlapAnotherClaim = "§cYou can't create a claim here because it would overlap another one.";
	public String claimCreated = "§aClaim created (%d x %d blocks). You have %d claim blocks left.";
	public String claimResized = "§aClaim resized (%d x %d blocks).";
	public String resizingClaim = "§aResizing claim.";
	public String noPermission = "§cYou don't have permission to do that here.";
	public String raidPrevented = "§cA raid has been prevented.";
	public String pvpDisabled = "§cPvP is disabled in this claim.";
	public String noClaimCreationPermission = "§cYou don't have permission to create claims.";
	public String notClaimOwner = "§cYou cannot do this because you are not the owner of this claim.";
	public String claimDeleted = "§aClaim deleted.";
	public String claimDeletedByOwner = "§aClaim deleted. You have %d claim blocks left.";
	public String unknownPlayer = "§cI've never seen a player by that name.";
	public String cannotSetOwnGroup = "§cYou can't change your own group.";
	public String tooManyClaims = "§cYou can't create any more claims.";

	public String claimListHeader = "These are your claims:";
	public String claimListEntry = "§7- %s at (%d, %d); %d blocks";
	public String claimListEntryAdmin = "§b- %s at (%d, %d); %d blocks (Admin)";
	public String claimListFooter = "You have used %d of your %d claim blocks. That means you have §a%d claim blocks as well as %d claims remaining.";
	public String claimListEmpty = "   You haven't created any claims yet.";

	public String leaveAdminMode = "§aYou've left administrative mode.";
	public String enterAdminMode = "§aYou've entered administrative mode.";
	public String creatingAdminClaim = "§aYou'll be creating an admin claim.";
}
