package nl.townsandkingdoms.terraforged.compat.streams.mixin;

import com.pandaismyname1.ultraterraforged.platform.ModLoaderUtil;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Do not resolve Streams-dependent target classes unless the optional mod is present. */
public final class OptionalStreamsMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!ModLoaderUtil.isLoaded("streamsreflowing")) return false;
        var file = net.neoforged.fml.loading.FMLLoader.getLoadingModList().getModFileById("streamsreflowing");
        boolean modern = file.getMods().getFirst().getVersion().toString().startsWith("2.16.2");
        if (mixin.contains(".Modern")) return modern;
        if (mixin.endsWith("ActualGridMixin") || mixin.endsWith("ActualBakedFlowMixin")
                || mixin.endsWith("ActualParticleMixin")) return !modern;
        return true;
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
