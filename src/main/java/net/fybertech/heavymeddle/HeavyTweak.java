package net.fybertech.heavymeddle;

import java.io.File;
import java.util.List;

import net.fybertech.meddle.MeddleMod;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="heavymeddle", name="Heavy Meddle", author="FyberOptic", version="1.0.2")
public class HeavyTweak implements ITweaker
{

	@Override
	public void acceptOptions(List<String> arg0, File arg1, File arg2, String arg3) {
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader arg0) {
		arg0.registerTransformer(HeavyTransformer.class.getName());
	}

}
