package pl.socketbyte.opensectors.linker.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import pl.socketbyte.opensectors.linker.json.controllers.ServerController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Util {

    public static String fixColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static long getRandomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    public static List<String> fixColors(List<String> texts) {
        List<String> strings = new ArrayList<>();
        for (String str : texts)
            strings.add(Util.fixColors(str));
        return strings;
    }

    private static boolean isLocationValid(Location loc) {
        Material mat = loc.getBlock().getType();
        return (mat == Material.GRASS) || (mat == Material.SAND) || (mat == Material.DIRT)
                || (mat == Material.GRAVEL) || (mat == Material.STONE) || (mat == Material.WATER);
    }

    public static Location getValidLocation(Location loc, int y) {
        for (int i = 0; i < 256; i++) {
            loc.setY(loc.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()));
            if (isLocationValid(new Location(loc.getWorld(), loc.getX(), loc.getY() - 1, loc.getZ()))) {
                loc.add(0, 3, 0);
                if (y > (loc.getBlockY() + 2))
                    loc.setY(y);

                return loc;
            }
        }
        return loc;
    }

    public static int[] getDestinationWithOffset(ServerController current, ServerController next, int x, int z) {
        int distWest = Math.abs((next.maxX) - current.maxX);
        int distEast = Math.abs((next.minX) - current.minX);
        int distNorth = Math.abs((next.minZ) - current.minZ);
        int distSouth = Math.abs((next.maxZ) - current.maxZ);
        int dirX = (distWest < distEast) ? -8 : 8;
        int dirZ = (distNorth < distSouth) ? -8 : 8;
        int distX = (distWest < distEast) ? distWest : distEast;
        int distZ = (distNorth < distSouth) ? distNorth : distSouth;
        return distX < distZ ? new int[] { x, z + dirZ } : new int[] { x + dirX, z };
    }
}
