package io.github.singlerr.task;


import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.misc.Map;
import io.github.singlerr.misc.Mission;
import io.github.singlerr.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class ReadyTask extends TimerTask{
    private TheMurder murder;
    private int readyCount;
    private boolean running;
    public ReadyTask(TheMurder murder){
        this.murder = murder;
        this.readyCount = 0;
        this.running = false;
    }
    @Override
    public void run() {
            running = true;
           switch (readyCount){
               case 2:
                   sendMessage(murder.getPlayers(),"&a게임이 시작됩니다.",true);
                   murder.getPlayers().forEach(p -> p.resetVote());
                   break;
               case 4:
                   sendMessage(murder.getPlayers(),"&a게임 기본 설정을 준비합니다..",true);
                   break;
               case 6:
                   sendMessage(murder.getPlayers(),"&a맵을 선택합니다...",true);
                   break;
               case 8:
                   if(getAvailableMaps().size() == 0){
                       sendMessage(murder.getPlayers(),"&a원활한 게임 플레이가 가능한 맵이 없습니다.",true);
                       murder.reset();
                   }
                   int k = new Random().nextInt(getAvailableMaps().size());
                   murder.currentMap = getAvailableMaps().get(k);
                   break;
               case 10:
                   sendMessage(murder.getPlayers(),"&a임무 선택 중...",true);
                   break;
               case 12:
                   Set<Mission> missions = new HashSet<>();
                   List<Mission> temp_ = getMissions(murder.currentMap);
                   StringBuilder builder = new StringBuilder();
                   for(int i = 0;i<murder.getSettings().missions;i++){
                       int r = new Random().nextInt(temp_.size());
                       Mission mission = temp_.get(r);
                       if(mission.getMissionType().equals(Mission.MissionType.CONNECTING_CIRCUIT))
                            mission = temp_.get(r).clone();
                       temp_.remove(r);
                       mission.setEnabled(true);
                       missions.add(mission);
                       builder.append(mission.getMissionType().getName()).append(",");
                   }
                   for(GamePlayer player : murder.getPlayers())
                       player.allocatedMissions = new HashSet<>(missions);
                   builder.setLength(builder.length()-1);
                   Utils.sendMessage(murder.getPlayers(),"&a임무: ","&c"+builder.toString());
                   Utils.sendMessage(murder.getPlayers(),"&a임무: \n&c"+builder.toString(),false);
                   break;
               case 14:
                   sendMessage(murder.getPlayers(),"&a살인자 추첨 중...",true);
                   break;
               case 16:
                   List<GamePlayer> temp = new ArrayList<>(murder.getPlayers());
                   for(int i = 0;i<murder.getSettings().murder;i++){
                       int r = new Random().nextInt(temp.size());
                       GamePlayer gamePlayer = temp.get(r);
                       gamePlayer.setMurder(true);
                       murder.murders.add(gamePlayer);
                       temp.remove(r);
                   }
                   murder.getPlayers().stream().filter(p -> ! p.isMurder()).forEach(p -> Utils.sendMessage(p.getPlayer(),"&a당신은 시민입니다.",true));
                   murder.murders.forEach(p -> Utils.sendMessage(p.getPlayer(),"&c당신은 살인자입니다.",true));
                   break;
               case 18:
                   sendMessage(murder.getPlayers(),"&c총 &a"+murder.getSettings().murder+"&c명의 살인자가 있습니다.",true);
                   break;
               case 20:
                   sendMessage(murder.getPlayers(),"&c5&a초",true);
                   break;
               case 21:
                   sendMessage(murder.getPlayers(),"&c4&a초",true);
                   break;
               case 22:
                   sendMessage(murder.getPlayers(),"&c3&a초",true);
                   break;
               case 23:
                   sendMessage(murder.getPlayers(),"&c2&a초",true);
                   break;
               case 24:
                   sendMessage(murder.getPlayers(),"&c1&a초",true);
                   break;
               case 25:
                   murder.getPlayers().forEach(p -> p.getPlayer().teleport(murder.currentMap.getSpawnLocation()));
                   ItemStack itemStack = new ItemStack(Material.PAPER);
                   ItemMeta itemMeta = itemStack.getItemMeta();
                   itemMeta.setDisplayName(Utils.translateAlternateColorCodes("&c&l신고"));
                   itemMeta.setLore(Arrays.asList(Utils.translateAlternateColorCodes("&a - 시체에 이 아이템을 들고 우클릭합니다.")));
                   itemStack.setItemMeta(itemMeta);
                   ItemStack knife = new ItemStack(Material.PAPER);
                   ItemMeta km = knife.getItemMeta();
                   km.setDisplayName(Utils.translateAlternateColorCodes("&c&l위장된 칼"));
                   km.setLore(Arrays.asList(Utils.translateAlternateColorCodes("&c - 생존자를 이 아이템으르 클릭합니다.")));
                   knife.setItemMeta(km);
                   murder.murders.forEach(p -> p.getPlayer().getInventory().addItem(knife));
                   murder.getPlayers().forEach(p -> p.getPlayer().getInventory().addItem(itemStack));
                   sendMessage(murder.getPlayers(),"&a게임이 시작되었습니다.",true);
                   murder.setAttackCancelled(false);
                   murder.progressTask.createBossBar();
                   murder.timer.scheduleAtFixedRate(murder.progressTask,0,1000);
                   running = false;
                   cancel();
                   break;

           }
           readyCount++;
    }

    public boolean isRunning() {
        return running;
    }

    public void sendMessage(List<GamePlayer> gamePlayers, String msg, boolean title){
        Utils.sendMessage(gamePlayers,msg,title);
    }
    public List<Map> getAvailableMaps(){
        List<Map> maps = new ArrayList<>();
        for(Map map : murder.getLoadedMaps()) {
            for (Mission mission : murder.getLoadedMissions().values()) {
                if (mission.availableMaps.contains(map) && murder.getSettings().votingLocations.containsKey(map.getName()) && ! maps.contains(map)){
                   if(getMissions(map).size() >= murder.getSettings().missions){
                       maps.add(map);
                   }
                }
            }
        }
        return maps;
    }
    public List<Mission> getMissions(Map map){
        return murder.getLoadedMissions().values().stream().filter(mission -> mission.availableMaps.contains(map)).collect(Collectors.toList());
    }
}
