package dev.engine_room.flywheel.lib.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;

public final class FlwUtil {
	private FlwUtil() {
	}

	public static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

	public static <T> Set<T> createWeakHashSet() {
		return Collections.newSetFromMap(new WeakHashMap<>());
	}

	public static int[] initArray(int size, int fill) {
		var out = new int[size];
		Arrays.fill(out, fill);
		return out;
	}
}
