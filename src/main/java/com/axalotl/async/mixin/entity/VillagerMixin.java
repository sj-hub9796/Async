package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Villager.class)
public class VillagerMixin {
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "pickUpItem")
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }

    @WrapMethod(method = "spawnGolemIfNeeded")
    private void spawnGolemIfNeeded(ServerLevel world, long time, int requiredCount, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(world, time, requiredCount);
        }
    }
}
