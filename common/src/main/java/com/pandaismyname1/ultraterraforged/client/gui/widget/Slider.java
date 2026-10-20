package com.pandaismyname1.ultraterraforged.client.gui.widget;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

//TODO this should be cleaned up
public class Slider extends AbstractSliderButton {
    private double min;
    private double max;
	private Component name;
	private Format format;
	@Nullable
	private Callback callback;
    
    public Slider(int x, int y, int width, int height, float initialValue, float min, float max, Component name, Format format, @Nullable Callback callback) {
        super(x, y, width, height, CommonComponents.EMPTY, 0.0);
        this.name = name;
        this.format = format;
        this.min = min;
        this.max = max;
        this.value = this.getSliderValue(initialValue);
        this.callback = callback;
        this.updateMessage();
    }
    
    public void setValue(double value) {
        if (Double.isFinite(value)) this.value = Mth.clamp(value, 0.0D, 1.0D);
        this.updateMessage();
    }
    
    public double getValue() {
    	return this.value;
    }
    
	public double getMin() {
		return this.min;
	}

	public double getMax() {
		return this.max;
	}
    
    public double getSliderValue(float value) {
    	return this.max == this.min ? 0.0D : (Mth.clamp(value, this.min, this.max) - this.min) / (this.max - this.min);
    }
    
    public double getLerpedValue() {
    	return this.lerpValue(this.value);
    }
    
    public double lerpValue(double value) {
    	return Mth.lerp(value, this.min, this.max);
    }
    
    public double scaleValue(double value) {
        return this.format.scale(this.lerpValue(value));
    }
    
    @Override
    public void applyValue() {
        if (this.callback != null) {
            double accepted = this.callback.apply(this, this.value);
            if (Double.isFinite(accepted)) this.value = Mth.clamp(accepted, 0.0D, 1.0D);
        }
        this.updateMessage();
    }

    // Nested option lists may swallow the vanilla base slider's value update.
    // Always derive the new normalized value from the pointer ourselves.
    @Override
    public void onClick(double mouseX, double mouseY) {
        this.utf$fromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.utf$fromMouse(mouseX);
    }

    private void utf$fromMouse(double mouseX) {
        if (!this.active || !this.visible || this.width <= 8) return;
        double next = Mth.clamp((mouseX - (this.getX() + 4)) / (double)(this.width - 8), 0.0D, 1.0D);
        if (next != this.value) {
            this.value = next;
            this.applyValue();
        }
    }

    @Override
    protected void updateMessage() {
        this.setMessage(CommonComponents.optionNameValue(this.name, Component.literal(this.format.getMessage(this.scaleValue(this.value)))));
    }
    
    public static enum Format {
    	INT {
			@Override
			public double scale(double input) {
				// round rather than truncate, or 3000 comes back as 2999 after the float round trip
				return Math.round(input);
			}

			@Override
			public String getMessage(double input) {
				return String.valueOf(Math.round(input));
			}
		},
    	FLOAT {
			@Override
			public double scale(double input) {
				return input;
			}

			@Override
			public String getMessage(double input) {
				return String.format("%.3f", input);
			}
		};
    	
    	public abstract double scale(double input);
    	
    	public abstract String getMessage(double input);
    }
    
    public interface Callback {
    	double apply(Slider slider, double value);
    }
}