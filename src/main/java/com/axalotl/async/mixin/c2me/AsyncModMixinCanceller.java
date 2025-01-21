package com.axalotl.async.mixin.c2me;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import net.neoforged.fml.ModList;

import java.util.List;

public class AsyncModMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (ModList.get().isLoaded("c2me")) {
            return mixinClassName.equals("com.ishland.c2me.fixes.general.threading_issues.mixin.asynccatchers.MixinThreadedAnvilChunkStorage") ||
                    mixinClassName.equals("com.ishland.c2me.fixes.general.threading_issues.mixin.threading_detections.random_instances.MixinWorld");
        } else return false;
    }
}

