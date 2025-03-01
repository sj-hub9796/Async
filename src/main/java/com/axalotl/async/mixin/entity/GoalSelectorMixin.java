package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {
	@Unique
	private static final ReentrantLock async$lock = new ReentrantLock();
	@Mutable
	@Shadow
	@Final
	private Set<WrappedGoal> availableGoals;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		this.availableGoals = ConcurrentCollections.newHashSet();
	}

	@WrapMethod(method = "tickRunningGoals")
	private void tickGoals(boolean tickAll, Operation<Void> original) {
		synchronized (async$lock) {
			original.call(tickAll);
		}
	}

	@WrapMethod(method = "addGoal")
	private void add(int priority, Goal goal, Operation<Void> original) {
		synchronized (async$lock) {
			original.call(priority, goal);
		}
	}

	@WrapMethod(method = "removeGoal")
	private void remove(Goal goal, Operation<Void> original) {
		synchronized (async$lock) {
			original.call(goal);
		}
	}
}
