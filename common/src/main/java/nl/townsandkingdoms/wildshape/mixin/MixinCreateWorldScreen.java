package nl.townsandkingdoms.wildshape.mixin;

import java.io.IOException;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.client.ClientConfig;
import nl.townsandkingdoms.wildshape.client.data.WildshapeTranslationKeys;
import nl.townsandkingdoms.wildshape.client.gui.createworld.PresetApplier;
import nl.townsandkingdoms.wildshape.client.gui.createworld.TerrainState;
import nl.townsandkingdoms.wildshape.client.gui.createworld.TerrainTab;

@Mixin(CreateWorldScreen.class)
abstract class MixinCreateWorldScreen extends Screen implements TerrainState.Holder {
	@Shadow
	@Final
	WorldCreationUiState uiState;
	@Shadow
	private boolean recreated;
	@Shadow
	@Final
	private TabManager tabManager;

	@Unique
	private TerrainState wildshape$terrainState;

	protected MixinCreateWorldScreen(Component title) {
		super(title);
	}

	@Shadow
	protected abstract void onCreate();

	@Override
	public TerrainState wildshape$getTerrainState() {
		if (this.wildshape$terrainState == null) {
			this.wildshape$terrainState = new TerrainState((CreateWorldScreen) (Object) this);
		}
		return this.wildshape$terrainState;
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void wildshape$selectDefaultWorldType(CallbackInfo callback) {
		// only for a new world, and only the first time the screen is set up
		if (this.wildshape$terrainState == null) {
			TerrainState state = this.wildshape$getTerrainState();
			if (!this.recreated && ClientConfig.load().defaultWorldType()) {
				state.selectWildshape();
			}
			// set only by the dev launch configurations, so test worlds can be flown around
			if (!this.recreated && Boolean.getBoolean("wildshape.dev.creative")) {
				state.uiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				state.uiState().setAllowCommands(true);
			}
		}
	}

	@ModifyArg(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"
		)
	)
	private Tab[] wildshape$addTerrainTab(Tab[] tabs) {
		Tab[] withTerrain = Arrays.copyOf(tabs, tabs.length + 1);
		withTerrain[tabs.length] = new TerrainTab((CreateWorldScreen) (Object) this);
		return withTerrain;
	}

	// make the world's datapacks match the chosen world type before creating it
	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void wildshape$applyPreset(CallbackInfo callback) {
		TerrainState state = this.wildshape$getTerrainState();
		try {
			if (!PresetApplier.prepareForCreate((CreateWorldScreen) (Object) this, state)) {
				// the datapacks are reloading; tick continues once they're loaded
				callback.cancel();
			}
		} catch (IOException | RuntimeException e) {
			WildshapeCommon.LOGGER.error("Couldn't prepare the Wildshape preset", e);
			state.setPendingSelection(null);
			SystemToast.addOrUpdate(this.minecraft.getToasts(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.translatable(WildshapeTranslationKeys.GUI_TERRAIN_TAB_APPLY_FAILED), Component.literal(String.valueOf(e.getMessage())));
			callback.cancel();
		}
	}

	// since 1.20.5 neither the screen nor its tabs tick on their own
	@Override
	public void tick() {
		super.tick();
		if (this.tabManager.getCurrentTab() instanceof TerrainTab terrainTab) {
			terrainTab.tick();
		}
		this.wildshape$createOncePresetLoaded();
	}

	@Unique
	private void wildshape$createOncePresetLoaded() {
		TerrainState state = this.wildshape$getTerrainState();
		if (PresetApplier.isReadyToCreate(state)) {
			state.setPendingSelection(null);
			// creating the world replaces this screen, which mustn't happen while it's ticking
			this.minecraft.tell(() -> {
				if (this.minecraft.screen == (Object) this) {
					this.onCreate();
				}
			});
		}
	}
}
