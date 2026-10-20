package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;

/** Skip Alex-dependent Mixins completely if the optional port is not installed. */
public final class OptionalAlexMixinPlugin implements IMixinConfigPlugin {
    private static boolean active() { return ModLoaderUtil.isLoaded("alexscaves"); }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return active(); }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
