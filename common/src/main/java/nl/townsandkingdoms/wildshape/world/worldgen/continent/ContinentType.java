package nl.townsandkingdoms.wildshape.world.worldgen.continent;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.advanced.AdvancedContinentGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.fancy.FancyContinentGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.infinite.InfiniteContinentGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.simple.MultiContinentGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.simple.SingleContinentGenerator;
import nl.townsandkingdoms.wildshape.world.worldgen.util.Seed;

public enum ContinentType implements StringRepresentable {
    MULTI {
        
    	@Override
        public MultiContinentGenerator create(Seed seed, GeneratorContext context) {
            return new MultiContinentGenerator(seed, context);
        }
    }, 
    SINGLE {
        
    	@Override
        public SingleContinentGenerator create(Seed seed, GeneratorContext context) {
            return new SingleContinentGenerator(seed, context);
        }
    }, 
    MULTI_IMPROVED {
        
    	@Override
        public AdvancedContinentGenerator create(Seed seed, GeneratorContext context) {
            return new AdvancedContinentGenerator(seed, context);
        }
    }, 
    EXPERIMENTAL {
        
    	@Override
        public FancyContinentGenerator create(Seed seed, GeneratorContext context) {
            return new FancyContinentGenerator(seed, context);
        }
    },
    INFINITE {
        
    	@Override
        public InfiniteContinentGenerator create(Seed seed, GeneratorContext context) {
            return new InfiniteContinentGenerator(context);
        }
    };
	
	public static final Codec<ContinentType> CODEC = StringRepresentable.fromEnum(ContinentType::values);
    
    public abstract Continent create(Seed seed, GeneratorContext context);

    @Override
	public String getSerializedName() {
		return this.name();
	}
}
