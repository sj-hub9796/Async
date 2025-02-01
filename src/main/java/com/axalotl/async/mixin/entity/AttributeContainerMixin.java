package com.axalotl.async.mixin.entity;

import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AttributeContainer.class)
public class AttributeContainerMixin {
    @Shadow
    private final Set<EntityAttributeInstance> pendingUpdate = ConcurrentHashMap.newKeySet();
}
