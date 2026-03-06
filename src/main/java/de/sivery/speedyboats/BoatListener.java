package de.sivery.speedyboats;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.List;

public class BoatListener implements Listener {
    private final SpeedyBoats plugin;

    public BoatListener(SpeedyBoats plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleDrive(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        List<Entity> passengers = event.getVehicle().getPassengers();
        if (passengers.isEmpty()) {
            return;
        }

        Entity passenger = passengers.get(0);
        if (vehicle instanceof Boat boat && passenger instanceof Player player) {
            if (boat.getLocation().getBlock().getType() != Material.WATER) {
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            String key = Engine.getEngineKey(plugin, item);
            if (key == null) {
                return;
            }

            // Find Section
            ConfigurationSection section = plugin.config.getConfigurationSection("engines." + key);
            if (section == null) return;

            // Update Velocity
            double multiplier = section.getDouble("multiplier");
            Vector direction = boat.getLocation().getDirection().multiply(multiplier);

            boat.setVelocity(new Vector(direction.getX(), 0.0, direction.getZ()));
        }
    }

    @EventHandler
    public void onEnginePlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        if (!Engine.isEngine(plugin, itemInHand)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("You cannot place boat engines."));

        PlayerInventory inventory = event.getPlayer().getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand.getAmount() == 0) {
            inventory.setItemInMainHand(itemInHand.clone());
        }
    }
}
