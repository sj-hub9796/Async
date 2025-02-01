package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends Entity {

    public AnimalEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * Utility method to acquire a consistent lock ordering for two animals.
     */
    @Unique
    private static Object[] getOrderedLocks(AnimalEntity a, AnimalEntity b) {
        if (System.identityHashCode(a) <= System.identityHashCode(b)) {
            return new Object[]{a, b};
        } else {
            return new Object[]{b, a};
        }
    }

    @WrapMethod(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V")
    private void breed(ServerWorld world, AnimalEntity other, PassiveEntity baby, Operation<Void> original) {
        Object[] locks = getOrderedLocks((AnimalEntity)(Object)this, other);
        synchronized (locks[0]) {
            synchronized (locks[1]) {
                original.call(world, other, baby);
            }
        }
    }

    @WrapMethod(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V")
    private void breed(ServerWorld world, AnimalEntity other, Operation<Void> original) {
        Object[] locks = getOrderedLocks((AnimalEntity)(Object)this, other);
        synchronized (locks[0]) {
            synchronized (locks[1]) {
                original.call(world, other);
            }
        }
    }
}
