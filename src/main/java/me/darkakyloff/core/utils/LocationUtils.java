package me.darkakyloff.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.text.DecimalFormat;

public class LocationUtils
{
    public static Location get(String location)
    {
        if (location == null || location.isEmpty())
        {
            LoggerUtils.warning("Попытка получить локацию из пустой строки");
            return null;
        }

        String[] parts = location.split("\\s*\\|\\s*");

        if (parts.length != 6)
        {
            LoggerUtils.error("Неверный формат локации: " + location);
            return null;
        }

        String worldName = parts[0];
        double x, y, z;
        float yaw, pitch;

        try
        {
            x = Double.parseDouble(parts[1].replace(',', '.'));
            y = Double.parseDouble(parts[2].replace(',', '.'));
            z = Double.parseDouble(parts[3].replace(',', '.'));
            yaw = Float.parseFloat(parts[4].replace(',', '.'));
            pitch = Float.parseFloat(parts[5].replace(',', '.'));
        }
        catch (NumberFormatException exception)
        {
            LoggerUtils.error("Ошибка парсинга координат локации: " + location, exception);
            return null;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null)
        {
            LoggerUtils.warning("Мир не найден: " + worldName);
            return null;
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static String put(Location location)
    {
        if (location == null || location.getWorld() == null)
        {
            LoggerUtils.warning("Попытка сериализовать null локацию");
            return "";
        }

        DecimalFormat df = new DecimalFormat("#.##");

        String xFormatted = df.format(location.getX());
        String yFormatted = df.format(location.getY());
        String zFormatted = df.format(location.getZ());
        String yawFormatted = df.format(location.getYaw());
        String pitchFormatted = df.format(location.getPitch());

        return location.getWorld().getName() + " | " + xFormatted + " | " + yFormatted + " | " + zFormatted + " | " + yawFormatted + " | " + pitchFormatted;
    }

    public static String formatCoordinates(Location location)
    {
        if (location == null) return "неизвестно";
        
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", 
                location.getX(), location.getY(), location.getZ());
    }

    public static double distance(Location loc1, Location loc2)
    {
        if (loc1 == null || loc2 == null) return Double.MAX_VALUE;
        if (!loc1.getWorld().equals(loc2.getWorld())) return Double.MAX_VALUE;
        
        return loc1.distance(loc2);
    }

    public static Location center(Location location)
    {
        if (location == null) return null;
        
        Location centered = location.clone();
        centered.setX(Math.floor(location.getX()) + 0.5);
        centered.setZ(Math.floor(location.getZ()) + 0.5);
        
        return centered;
    }
}