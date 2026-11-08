package nl.townsandkingdoms.wildshape.platform;

import java.io.IOException;
import java.nio.file.Path;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ConfigUtil {
	public static final Path WILDSHAPE_CONFIG_PATH = getConfigPath().resolve("wildshape");
	
	public static Path wildshape(String path) {
		return WILDSHAPE_CONFIG_PATH.resolve(path);
	}
	
	@ExpectPlatform
	public static Path getConfigPath() {
		throw new IllegalStateException();
	}
	
    static {
        try {
            ConfigMigration.migrate(getConfigPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize Wildshape configuration", e);
        }
    }
}
