package io.github.singlerr.utils;


import io.github.singlerr.misc.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Utils {
    public static void sendMessage(List<GamePlayer> gamePlayers, String msg, boolean title){
        gamePlayers.forEach(gamePlayer -> gamePlayer.getPlayer().sendMessage(translateAlternateColorCodes(msg)));
        if(title)
            gamePlayers.forEach(gamePlayer -> gamePlayer.getPlayer().sendTitle("",translateAlternateColorCodes(msg),10,15,10));
    }
    public static void openVotePanel(Player player,List<GamePlayer> players){

        Inventory inventory = Bukkit.createInventory(null,54, translateAlternateColorCodes("&c&l투표"));
        //11부터
        for(int i = 0;i<players.size();i++){
            if(players.get(i).isDead()){
                ItemStack itemStack = new ItemStack(Material.OBSIDIAN);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(translateAlternateColorCodes("&c&l"+players.get(i).getPlayer().getName()));
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(i*2+11,itemStack);
            }else {
                ItemStack itemStack = new ItemStack(Material.DIAMOND_BLOCK);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(translateAlternateColorCodes("&a&l" + players.get(i).getPlayer().getName()));
                itemMeta.setLore(Arrays.asList(translateAlternateColorCodes("&c- 클릭시 투표")));
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(i * 2 + 11, itemStack);
            }
        }
        player.openInventory(inventory);
    }
    public static void sendMessage(List<GamePlayer> gamePlayers,String first,String second){
        gamePlayers.forEach(gamePlayer -> gamePlayer.getPlayer().sendTitle(translateAlternateColorCodes(first),translateAlternateColorCodes(second)));
    }
    public static String translateAlternateColorCodes(String text){
        return text.replaceAll("&","§");
    }
    public static void sendMessage(Player player,String msg,boolean title){
        player.sendMessage(translateAlternateColorCodes(msg));
        if(title)
            player.sendTitle(translateAlternateColorCodes(msg),"",10,15,10);
    }
    public static void sendMessage(Player player,String first,String second){
            player.sendTitle(translateAlternateColorCodes(first),translateAlternateColorCodes(second),10,15,10);
    }

}
