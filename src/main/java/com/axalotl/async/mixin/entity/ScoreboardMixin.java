package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import net.minecraft.world.scores.PlayerScores;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {
	@Shadow
	private final Map<String, PlayerScores> playerScores = ConcurrentCollections.newHashMap();
}