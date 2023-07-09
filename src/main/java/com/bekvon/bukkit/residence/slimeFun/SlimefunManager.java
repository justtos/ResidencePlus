package com.bekvon.bukkit.residence.slimeFun;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import com.bekvon.bukkit.residence.Residence;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.ProtectionManager;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.ProtectionModule;

public class SlimefunManager {

    // Fail safe to avoid infinite checks
    private final static int TRIES = 60;
    private static int times = 0;

    public static void register(Residence residence) {
	Bukkit.getServer().getScheduler().runTaskTimer(residence, task -> {
	    ++times;
	    if (times >= TRIES) {
		residence.consoleMessage("&cFailed to initialize SlimeFun support");
		task.cancel();
	    }

	    ProtectionManager manager = Slimefun.getProtectionManager();

	    // Wait for protectionManager to load (loaded on first server tick)
	    if (manager != null) {
		try {
		    Method method = manager.getClass().getMethod("registerModule", Server.class, String.class, Function.class);
		    if (method != null) {
			Function<Plugin, ProtectionModule> m = plugin -> new SlimeFunResidenceModule(plugin);
			method.invoke(manager, Bukkit.getServer(), "Residence", m);
			residence.consoleMessage("Enabled compatability with SlimeFun plugin");
			task.cancel();
		    }
		} catch (Throwable e) {
		    // Going with latest dev build supported approach
		    manager.registerModule(Bukkit.getServer().getPluginManager(), "Residence", plugin -> new SlimeFunResidenceModule(plugin));
		    residence.consoleMessage("Enabled compatability with SlimeFun plugin");
		    task.cancel();
		}
	    }
	}, 20, 20);
    }
}
