package io.github.singlerr;

import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.task.CoolTime;
import io.github.singlerr.task.VotingTask;
import io.github.singlerr.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import org.golde.bukkit.corpsereborn.Main;
import org.golde.bukkit.corpsereborn.nms.Corpses;

import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;


public class UniversalListener implements Listener {
    private TheMurder murder;
    public UniversalListener(TheMurder murder){
        this.murder = murder;
    }
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e){
        if(e.getDamager().getType().equals(EntityType.PLAYER) && e.getEntity().getType().equals(EntityType.PLAYER)) {
            Player damager = (Player) e.getDamager();
            Player entity = (Player) e.getEntity();
            if (damager.getInventory().getItemInMainHand().getType().equals(Material.PAPER) || damager.getInventory().getItemInOffHand().getType().equals(Material.PAPER)){
                if (murder.murders.size() > 0) {
                    if ((damager.getInventory().getItemInMainHand().getType().equals(Material.PAPER) && damager.getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals("§c§l위장된 칼"))
                            || (damager.getInventory().getItemInOffHand().getType().equals(Material.PAPER) && damager.getInventory().getItemInOffHand().getItemMeta().getDisplayName().equals("§c§l위장된 칼"))){
                        GamePlayer attacker = murder.findPlayer(((Player) e.getDamager()).getName());
                        GamePlayer victim = murder.findPlayer(((Player) e.getEntity()).getName());
                    if (attacker != null && victim != null) {
                        if (attacker.isMurder() && (!victim.isMurder())) {
                            if (!murder.isAttackCancelled()) {
                                if(! attacker.isLocked()) {
                                    victim.setDead(true);
                                    CorpseAPI.spawnCorpse(entity,entity.getLocation());
                                    victim.getPlayer().setGameMode(GameMode.SPECTATOR);
                                    //시체 남기기

                                    if (murder.getPlayers().stream().filter(p -> !p.isDead()).filter(p -> ! p.isMurder()).collect(Collectors.toList()).size() == 0) {
                                        Utils.sendMessage(murder.getPlayers(), "&a모든 시민이 사망하였습니다.\n시민들의 패배!", true);
                                        Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                            @Override
                                            public void run() {
                                                murder.reset();
                                            }
                                        },50);

                                    }else {
                                        attacker.setLocked(true);
                                        Utils.sendMessage(attacker.getPlayer(), "&c&l" + entity.getName() + "님을 죽였습니다!", "&c&l이제 &a&l" + murder.getSettings().murderCooltime + "&c&l초 동안 살인을 하실 수 없습니다.");
                                        new CoolTime(murder,attacker).runTaskTimerAsynchronously(murder,0,20L);
                                    }
                                }else{
                                    Utils.sendMessage(attacker.getPlayer(),"&a&l아직 살인을 하실 수 없습니다.",false);
                                }
                            }
                        }
                        e.setCancelled(true);
                    }
                }
                }
            }
        }
    }

    @EventHandler
    public void onInteractAtCorpse(PlayerInteractEvent e){
        if(murder.murders.size() > 0) {
            if (e.getItem() != null && e.getItem().getType().equals(Material.PAPER) && e.getItem().getItemMeta().getDisplayName().equals("§c§l신고")) {
                GamePlayer gamePlayer = murder.findPlayer(e.getPlayer().getName());
                if(gamePlayer != null){
                    if(! gamePlayer.isDead()){
                        List<Corpses.CorpseData> corpses = CorpseAPI.getCorpseInRadius(e.getPlayer().getLocation(),2);
                        if(corpses.size() > 0 && ! murder.isAttackCancelled()){
                            Corpses.CorpseData corpse = corpses.get(0);
                            Utils.sendMessage(murder.getPlayers(),"&a"+corpse.getCorpseName()+"&c님의 시체가 발견되었습니다.\n발견지: &a"+e.getPlayer().getName(),true);
                            murder.setAttackCancelled(true);
                            corpse.destroyCorpseFromEveryone();
                            murder.votingTask = new VotingTask(murder);
                            Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                @Override
                                public void run() {
                                    murder.getPlayers().stream().forEach(p -> p.getPlayer().teleport(murder.getSettings().votingLocations.get(murder.currentMap.getName())));
                                    murder.votingTask.createBossBar();
                                    murder.timer.scheduleAtFixedRate(murder.votingTask,1000,1000);
                                    murder.progressTask.setPaused(true);
                                }
                            },50);
                        }
                    }
                }
            }
        }

    }
    @EventHandler
    public void onBreak(BlockBreakEvent e){
        if(e.getBlock() != null &&  ! e.getBlock().getType().equals(Material.AIR)) {
            if (murder.modes.containsKey(e.getPlayer().getName())) {
                List<Location> loc = murder.modes.get(e.getPlayer().getName());
                loc.add(e.getPlayer().getLocation());
                murder.modes.put(e.getPlayer().getName(),loc);
                e.setCancelled(true);
                Utils.sendMessage(e.getPlayer(),"&a지점을 설정했습니다.",false);
            }
        }
    }
    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getCurrentItem() == null || e.getCurrentItem().getType().equals(Material.AIR))
            return;

        if(e.getClickedInventory().getTitle().equalsIgnoreCase("§C§l투표")){
            if(murder.votingTask.isRunning()) {
                if(e.getWhoClicked().getType().equals(EntityType.PLAYER)){
                    GamePlayer gamePlayer = murder.findPlayer(((Player)e.getWhoClicked()).getName());
                    if(gamePlayer != null){
                        if(! gamePlayer.isDead()){
                            if(! e.getCurrentItem().getType().equals(Material.OBSIDIAN)) {
                                String name = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                                GamePlayer target = murder.findPlayer(name);
                                target.addVote();
                                gamePlayer.voted = true;
                                gamePlayer.getPlayer().closeInventory();
                                Utils.sendMessage(gamePlayer.getPlayer(),"&a&l투표하셨습니다!",true);
                                Utils.sendMessage(murder.getPlayers(),"&c&l"+gamePlayer.getPlayer().getName()+"&a&l님이 투표하셨습니다!",false);

                            }
                        }
                    }
                }
            }
            e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClose(InventoryCloseEvent e){
        if(e.getInventory().getTitle().equalsIgnoreCase("§C§l투표")){
            if(murder.votingTask.isRunning()){
                if(e.getPlayer().getType().equals(EntityType.PLAYER)){
                    GamePlayer gamePlayer = murder.findPlayer(e.getPlayer().getName());
                    if(gamePlayer != null){
                        if(! gamePlayer.isDead() && ! gamePlayer.voted){
                                Utils.sendMessage(gamePlayer.getPlayer(),"&c&l투표에 기권하셨습니다.",true);
                                Utils.sendMessage(murder.getPlayers(),"&c&l"+e.getPlayer().getName()+"&a&l님이 기권하셨습니다.",false);
                        }
                    }
                }
            }
        }
    }
}
