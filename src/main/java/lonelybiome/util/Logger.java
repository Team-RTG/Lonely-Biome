package lonelybiome.util;

import lonelybiome.config.lonelybiome.ConfigLB;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;

public class Logger {

	public static void debug(String format, Object... data) {
		if (ConfigLB.enableDebugging) FMLLog.log(Level.DEBUG, "[LonelyBiome-DEBUG] " + format, data);
	}

	public static void info(String format, Object... data) {
		FMLLog.log(Level.INFO, "[LonelyBiome-INFO] " + format, data);
	}

	public static void warn(String format, Object... data) {
		FMLLog.log(Level.WARN, "[LonelyBiome-WARN] " + format, data);
	}

	public static void error(String format, Object... data) {
		FMLLog.log(Level.ERROR, "[LonelyBiome-ERROR] " + format, data);
	}

	public static void fatal(String format, Object... data) {
		FMLLog.log(Level.FATAL, "[LonelyBiome-FATAL] " + format, data);
	}
}