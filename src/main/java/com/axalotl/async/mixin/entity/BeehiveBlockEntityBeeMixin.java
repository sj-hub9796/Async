package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(BeehiveBlockEntity.BeeData.class)
public class BeehiveBlockEntityBeeMixin {
	@Unique
	private static final ReentrantLock async$lock = new ReentrantLock();

	@WrapMethod(method = "tick")
	private boolean canExitHive(Operation<Boolean> original) {
		synchronized (async$lock) {
			return original.call();
		}
	}
}