package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Dolphin.class)
public abstract class DolphinMixin extends AgeableWaterCreature {

    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    protected DolphinMixin(EntityType<? extends AgeableWaterCreature> p_364107_, Level p_360956_) {
        super(p_364107_, p_360956_);
    }

    @WrapMethod(method = "pickUpItem")
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }
}
