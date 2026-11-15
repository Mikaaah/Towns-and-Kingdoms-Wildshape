package nl.townsandkingdoms.wildshape.preset.option;

import nl.townsandkingdoms.wildshape.client.data.WildshapeTranslationKeys;

// Extra information surfaced next to an option, e.g. in its tooltip
public enum OptionTag {
	EXPERIMENTAL(WildshapeTranslationKeys.OPTION_TAG_EXPERIMENTAL),
	MEDIUM_PERFORMANCE_IMPACT(WildshapeTranslationKeys.OPTION_TAG_MEDIUM_PERFORMANCE_IMPACT),
	HEAVY_PERFORMANCE_IMPACT(WildshapeTranslationKeys.OPTION_TAG_HEAVY_PERFORMANCE_IMPACT);

	private final String translationKey;

	private OptionTag(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return this.translationKey;
	}
}
