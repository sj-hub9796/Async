package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin extends AnimalEntity implements Angerable, Flutterer {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    protected BeeEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @WrapMethod(method = "canEnterHive")
    private boolean loot(Operation<Boolean> original) {
        synchronized (lock) {
            return original.call();
        }
    }
}