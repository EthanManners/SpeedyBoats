package de.sivery.speedyboats;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BoatsCommand implements CommandExecutor, Listener {
    private final SpeedyBoats plugin;

    public BoatsCommand(SpeedyBoats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        openEnginesMenu(player);
        return true;
    }

    private void openEnginesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new EnginesMenuHolder(), 27, Component.text("Boat Engines"));
        int slot = 0;
        for (Engine engine : Engine.REGISTERED) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, engine.getItem());
        }

        player.openInventory(inventory);
    }

    private void openRecipeMenu(Player player, Engine engine) {
        Inventory inventory = Bukkit.createInventory(new RecipeMenuHolder(engine.key), 27, Component.text("Recipe: " + engine.key));

        if (engine.getRecipe() instanceof ShapedRecipe shapedRecipe) {
            String[] shape = shapedRecipe.getShape();
            Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
            Map<Character, ItemStack> ingredientMap = new HashMap<>();

            for (Map.Entry<Character, RecipeChoice> entry : choiceMap.entrySet()) {
                RecipeChoice choice = entry.getValue();
                if (choice instanceof RecipeChoice.ExactChoice exactChoice && !exactChoice.getChoices().isEmpty()) {
                    ingredientMap.put(entry.getKey(), exactChoice.getChoices().get(0).clone());
                } else if (choice instanceof RecipeChoice.MaterialChoice materialChoice && !materialChoice.getChoices().isEmpty()) {
                    ingredientMap.put(entry.getKey(), new ItemStack(materialChoice.getChoices().get(0)));
                }
            }

            int[] slots = {10, 11, 12, 13, 14, 15, 16, 17, 18};
            int slotIndex = 0;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    char c = shape[row].charAt(col);
                    if (c != ' ') {
                        ItemStack ingredient = ingredientMap.get(c);
                        if (ingredient != null) {
                            inventory.setItem(slots[slotIndex], ingredient);
                        }
                    }
                    slotIndex++;
                }
            }

            inventory.setItem(4, engine.getItem());
        } else {
            inventory.setItem(13, new ItemStack(Material.BARRIER));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (!(holder instanceof EnginesMenuHolder) && !(holder instanceof RecipeMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(holder instanceof EnginesMenuHolder)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        String key = Engine.getEngineKey(plugin, clicked);
        if (key == null) {
            return;
        }

        Optional<Engine> engine = Engine.byKey(key);
        engine.ifPresent(value -> openRecipeMenu(player, value));
    }

    private static final class EnginesMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 27);
        }
    }

    private record RecipeMenuHolder(String engineKey) implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 27);
        }
    }
}
