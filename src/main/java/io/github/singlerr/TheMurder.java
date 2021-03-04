package io.github.singlerr;

import io.github.singlerr.commands.AdminCommand;
import io.github.singlerr.commands.JoinCommand;
import io.github.singlerr.commands.LeaveCommand;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.misc.Map;
import io.github.singlerr.misc.Mission;
import io.github.singlerr.misc.Settings;
import io.github.singlerr.task.GameTask;
import io.github.singlerr.task.ReadyTask;
import io.github.singlerr.task.VotingTask;
import io.github.singlerr.utils.FileManager;
import io.github.singlerr.utils.Utils;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.Lever;
import org.bukkit.plugin.java.JavaPlugin;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class TheMurder extends JavaPlugin {

    private boolean attackCancelled;

    public Map currentMap;
    public List<GamePlayer> murders;
    private Settings settings;
    private FileManager manager;
    private FileManager.Config maps;
    private FileManager.Config missions;
    private FileManager.Config config;
    public List<GamePlayer> players;
    private HashMap<String,Location> mapLocations;
    private HashMap<Mission.MissionType,Mission> loadedMissions;
    public List<Mission.MissionType> enabledMissions;
    private List<Map> loadedMaps;
    public GameTask progressTask;
    public ReadyTask startTask;
    public VotingTask votingTask;
    public HashMap<String,List<Location>> modes;
    public Timer timer;
    private static TheMurder instance;
    {
        instance = this;
    }
     public void onEnable(){
        murders = new ArrayList<>();
        players = new ArrayList<>();
        modes = new HashMap<>();
        reset();
        getCommand("join").setExecutor(new JoinCommand(instance));
        getCommand("leave").setExecutor(new LeaveCommand(instance));
        getCommand("dm").setExecutor(new AdminCommand(instance));
        getServer().getPluginManager().registerEvents(new UniversalListener(instance),instance);
        getLogger().log(Level.INFO,"오류 없이 플러그인이 활성화되었습니다.");
        timer = new Timer();
     }

    public FileManager.Config getMapConfig() {
        return maps;
    }

    public FileManager.Config getMissionConfig() {
        return missions;
    }

    public void registerAllMaps(){
         loadedMaps = new ArrayList<>();
         if( ! maps.get().isConfigurationSection("maps"))
             maps.get().createSection("maps");
         maps.save();
         maps.reload();
        ConfigurationSection mapSection = maps.get().getConfigurationSection("maps");
        if(mapSection.getKeys(false).size() > 0){
            mapSection.getKeys(false).forEach(key -> {
                Map map = new Map(instance,key);
                String world = mapSection.getString(key+".world");
                double x = mapSection.getDouble(key+".x");
                double y = mapSection.getDouble(key+".y");
                double z = mapSection.getDouble(key+".z");
                map.setSpawnLocation(new Location(Bukkit.getWorld(world),x,y,z));
                loadedMaps.add(map);
            });

        }else{
            getLogger().log(Level.SEVERE,"등록된 맵이 하나도 존재하지 않습니다.");
            return;
        }
    }
    public void setSpawnLocation(Location loc){
        config.get().set("spawn.world",loc.getWorld().getName());
        config.get().set("spawn.x",loc.getX());
        config.get().set("spawn.y",loc.getY());
        config.get().set("spawn.z",loc.getZ());
        config.save();
        settings.spawnLocation = loc;
    }
    public void setVotingLocation(String mapName,Location loc){
        ConfigurationSection section = maps.get().getConfigurationSection("hub");
        section.set(mapName+".world",loc.getWorld().getName());
        section.set(mapName+".x",loc.getX());
        section.set(mapName+".y",loc.getY());
        section.set(mapName+".z",loc.getZ());
        maps.save();
        maps.reload();
        settings.votingLocations.put(mapName,loc);
    }
    public void registerAllMissions(){
        loadedMissions = new HashMap<>();
         for(Mission.MissionType type : Mission.MissionType.values()){
             if(! missions.get().contains(type.name())){
                 getLogger().log(Level.SEVERE,type.name()+" 임무 항목이 missionSettings.yml 파일에 존재하지 않습니다.");
                return;
             }
         }
         loadedMissions = new HashMap<>();
        //이건 맨 마지막에
        ConfigurationSection bt = missions.get().getConfigurationSection(Mission.MissionType.BUILDING_TREE.name());
        if(bt.getKeys(false).size() >= 1) {
            Mission<StructureGrowEvent> buildingTrees = new Mission<>(Mission.MissionType.BUILDING_TREE, new Mission.EventCallback<StructureGrowEvent>() {
                @Override
                public void on(StructureGrowEvent event, Mission mission,Map map) {
                    if (event.isFromBonemeal()) {
                        GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                        if (gamePlayer != null) {

                            if(gamePlayer.isAllocatedMission(mission)) {
                                Player p = event.getPlayer();
                                if (mission.isInMissionArea(p.getLocation(),map.getName())) {
                                    if(! mission.isEnabled()){
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                        return;
                                    }
                                    gamePlayer.setMissionCompleted(mission, true);
                                    gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                    //나무 복구
                                }
                            }
                        }
                    }
                }
            },instance);
            set(bt,buildingTrees);

            getServer().getPluginManager().registerEvent(StructureGrowEvent.class,buildingTrees, EventPriority.NORMAL,buildingTrees,instance);
            loadedMissions.put(Mission.MissionType.BUILDING_TREE,buildingTrees);
        }else{
            getLogger().log(Level.WARNING,"'나무 기르기' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
       //이건 맨 마지막에
        ConfigurationSection conf = missions.get().getConfigurationSection(Mission.MissionType.CUTTING_TREE.name());
        if(conf.getKeys(false).size() >= 1) {
            Mission<BlockBreakEvent> cuttingTrees = new Mission<>(Mission.MissionType.CUTTING_TREE, new Mission.EventCallback<BlockBreakEvent>() {
                @Override
                public void on(BlockBreakEvent event, Mission mission,Map map) {
                    if (event.getBlock().getType().equals(Material.WOOD)) {
                        GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                        if (gamePlayer != null) {

                            if(gamePlayer.isAllocatedMission(mission)) {
                                Player p = event.getPlayer();
                                if (mission.isInMissionArea(p.getLocation(),map.getName())) {
                                    if(! mission.isEnabled()){
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                        return;
                                    }
                                    //나무 재생 및 벌목은 ChopTree로 해결
                                    gamePlayer.setMissionCompleted(mission, true);
                                    gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                }
                            }
                            }

                    }
                }
            },instance);
            set(conf,cuttingTrees);
            getServer().getPluginManager().registerEvent(BlockBreakEvent.class,cuttingTrees,EventPriority.NORMAL,cuttingTrees,instance);
            loadedMissions.put(Mission.MissionType.CUTTING_TREE,cuttingTrees);
        }else{
            getLogger().log(Level.WARNING,"'나무 자르기' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        //회로 연결-> 여기는 회로 위치 정하는 커맨드 만들고 나서
       ConfigurationSection circuitConf = missions.get().getConfigurationSection(Mission.MissionType.CONNECTING_CIRCUIT.name());
        if(notNull(circuitConf)){
            if(circuitConf.getKeys(false).size() > 0) {
                Mission<BlockPlaceEvent> connectingCircuit = new Mission<>(Mission.MissionType.CONNECTING_CIRCUIT, new Mission.EventCallback<BlockPlaceEvent>() {
                    @Override
                    public void on(BlockPlaceEvent event, Mission mission,Map map) {
                        if (event.getBlockPlaced().getType().equals(Material.REDSTONE)) {
                            GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                            if(mission.ccLocations.containsKey(map.getName())){
                                Set<Location> locs = (Set<Location>) mission.ccLocations.get(map.getName());
                            if (locs.contains(event.getBlock().getLocation())) {
                                if (gamePlayer != null) {

                                    if (gamePlayer.isAllocatedMission(mission)) {
                                        if(! mission.isEnabled()){
                                            Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                            return;
                                        }
                                        locs.remove(event.getBlock().getLocation());
                                        if (locs.size() <= 0) {
                                            gamePlayer.setMissionCompleted(mission, true);
                                            gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                            Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                        }
                                        mission.ccLocations.put(map.getName(),locs);
                                    }
                                }
                            }
                            }
                        }
                    }
                },instance);
                for(String key : circuitConf.getKeys(false)){
                    String mapName = key;
                    ConfigurationSection sec = circuitConf.getConfigurationSection(mapName);
                    Set<Location> tmp = new HashSet<>();
                    for(String key2 : sec.getKeys(false)){
                        double x = sec.getDouble(key2+".x");
                        double y = sec.getDouble(key2+".y");
                        double z = sec.getDouble(key2+".z");
                        String world = sec.getString(key2+".world");
                        tmp.add(new Location(Bukkit.getWorld(world),x,y,z));
                    }
                    connectingCircuit.availableMaps.add(findMap(mapName));
                    connectingCircuit.ccLocations.put(mapName,tmp);
                }
                getServer().getPluginManager().registerEvent(BlockPlaceEvent.class,connectingCircuit,EventPriority.NORMAL,connectingCircuit,instance);
                loadedMissions.put(Mission.MissionType.CONNECTING_CIRCUIT,connectingCircuit);
            }else{
                getLogger().log(Level.WARNING,"'회로 연결' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
                getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
            }
        }else{
            getLogger().log(Level.WARNING,"'회로 연결' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        /*
            버킷 내 이벤트 호출이 되지 않으므로 보류
         */
        /*
        ConfigurationSection portal = missions.get().getConfigurationSection(Mission.MissionType.GENERATING_PORTAL.name());
        if(portal.getKeys(false).size() >= 1) {
            Mission<EntityCreatePortalEvent> gp = new Mission<>(Mission.MissionType.GENERATING_PORTAL, new Mission.EventCallback<EntityCreatePortalEvent>() {
                @Override
                public void on(EntityCreatePortalEvent event, Mission mission,Map map) {
                    if(event.getPortalType().equals(PortalType.NETHER)){
                        if(event.getEntityType().equals(EntityType.PLAYER)){
                            GamePlayer gamePlayer = findPlayer(((Player)event.getEntity()).getName());
                            if (gamePlayer != null) {

                                if(gamePlayer.isAllocatedMission(mission)) {
                                    if (mission.isInMissionArea(((Player) event.getEntity()).getLocation(),map.getName())) {
                                        if(! mission.isEnabled()){
                                            Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                            return;
                                        }
                                        event.setCancelled(true);
                                        gamePlayer.setMissionCompleted(mission, true);
                                        gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                    }
                                }
                            }

                        }
                    }
                    Bukkit.broadcastMessage("ddd");
                }
            },instance);
            set(portal,gp);
            getServer().getPluginManager().registerEvent(EntityCreatePortalEvent.class,gp,EventPriority.NORMAL,gp,instance);
            loadedMissions.put(Mission.MissionType.GENERATING_PORTAL,gp);
        }else{
            getLogger().log(Level.WARNING,"'포탈 생성' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
         */
        ConfigurationSection breedingConf = missions.get().getConfigurationSection(Mission.MissionType.BREEDING.name());
        if(breedingConf.getKeys(false).size() >= 1){
            Mission<EntityBreedEvent> breeding = new Mission<>(Mission.MissionType.BREEDING, new Mission.EventCallback<EntityBreedEvent>() {
                @Override
                public void on(EntityBreedEvent event, Mission mission,Map map) {
                    if(event.getEntityType().equals(EntityType.COW) && event.getBreeder().getType().equals(EntityType.PLAYER)){
                        GamePlayer gamePlayer = findPlayer(((Player)event.getBreeder()).getName());
                        if(gamePlayer != null){
                            if(gamePlayer.isAllocatedMission(mission)){
                                if(mission.isInMissionArea(gamePlayer.getPlayer().getLocation(),map.getName())){
                                    if(! mission.isEnabled()){
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                        return;
                                    }
                                    event.setCancelled(true);
                                    gamePlayer.setMissionCompleted(mission,true);
                                    gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                }
                            }
                        }
                    }
                }
            },instance);
            set(breedingConf,breeding);
            getServer().getPluginManager().registerEvent(EntityBreedEvent.class,breeding,EventPriority.NORMAL,breeding,instance);
            loadedMissions.put(Mission.MissionType.BREEDING,breeding);
        }else{
            getLogger().log(Level.WARNING,"'동물 교배' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        ConfigurationSection miningConf = missions.get().getConfigurationSection(Mission.MissionType.MINING.name());
        if(miningConf.getKeys(false).size() >= 1){
            Mission<BlockBreakEvent> mining = new Mission<>(Mission.MissionType.MINING, new Mission.EventCallback<BlockBreakEvent>() {
                @Override
                public void on(BlockBreakEvent event, Mission mission,Map map) {
                    if(event.getBlock().getType().equals(Material.STONE)){
                        if(mission.isInMissionArea(event.getPlayer().getLocation(),map.getName())){
                            GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                            if(gamePlayer != null){

                                if(gamePlayer.isAllocatedMission(mission)){
                                    if(! mission.isEnabled()){
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                        return;
                                    }
                                    event.setCancelled(true);
                                    gamePlayer.setMissionCompleted(mission,true);
                                    gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                }

                            }
                        }

                    }
                }
            },instance);
        set(miningConf,mining);
            getServer().getPluginManager().registerEvent(BlockBreakEvent.class,mining,EventPriority.NORMAL,mining,instance);
        loadedMissions.put(Mission.MissionType.MINING,mining);
        }else{
            getLogger().log(Level.WARNING,"'채굴' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        ConfigurationSection lampsConf = missions.get().getConfigurationSection(Mission.MissionType.TURNING_ON_LAMPS.name());
        if(miningConf.getKeys(false).size() >= 1){
            Mission<PlayerInteractEvent> lamps = new Mission<>(Mission.MissionType.TURNING_ON_LAMPS, new Mission.EventCallback<PlayerInteractEvent>() {
                @Override
                public void on(PlayerInteractEvent event, Mission mission,Map map) {
                    if(event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.LEVER)){
                        if(mission.isInMissionArea(event.getClickedBlock().getLocation(),map.getName())){
                            GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                            if(gamePlayer != null){

                                if(gamePlayer.isAllocatedMission(mission)){
                                    if(! mission.isEnabled()){
                                        Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                        return;
                                    }
                                    gamePlayer.setMissionCompleted(mission,true);
                                    event.setCancelled(true);
                                    gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                                }
                            }
                        }
                    }
                }
            },instance);
            set(lampsConf,lamps);
            getServer().getPluginManager().registerEvent(PlayerInteractEvent.class,lamps,EventPriority.NORMAL,lamps,instance);
            loadedMissions.put(Mission.MissionType.TURNING_ON_LAMPS,lamps);
        }else{
            getLogger().log(Level.WARNING,"'전등' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        ConfigurationSection enchantConf = missions.get().getConfigurationSection(Mission.MissionType.ENCHANTING.name());
        if(enchantConf.getKeys(false).size() >= 1){
            Mission<EnchantItemEvent> enchant = new Mission<>(Mission.MissionType.ENCHANTING, new Mission.EventCallback<EnchantItemEvent>() {
                @Override
                public void on(EnchantItemEvent event, Mission mission,Map map) {
                    if(mission.isInMissionArea(event.getEnchanter().getLocation(),map.getName())){
                        GamePlayer gamePlayer = findPlayer(event.getEnchanter().getName());
                        if(gamePlayer != null){

                            if(gamePlayer.isAllocatedMission(mission)){
                                if(! mission.isEnabled()){
                                    Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                    return;
                                }
                                gamePlayer.setMissionCompleted(mission,true);
                                event.getEnchanter().closeInventory();
                                event.setCancelled(true);
                                gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                            }
                        }
                    }
                }
            },instance);
            set(enchantConf,enchant);
            getServer().getPluginManager().registerEvent(EnchantItemEvent.class,enchant,EventPriority.NORMAL,enchant,instance);
            loadedMissions.put(Mission.MissionType.ENCHANTING,enchant);
        }else{
            getLogger().log(Level.WARNING,"'인챈트' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        ConfigurationSection drainConf = missions.get().getConfigurationSection(Mission.MissionType.DRAINING.name());
        if(drainConf.getKeys(false).size() >= 1){
            Mission<PlayerBucketFillEvent> drain = new Mission<>(Mission.MissionType.DRAINING, new Mission.EventCallback<PlayerBucketFillEvent>() {
                @Override
                public void on(PlayerBucketFillEvent event, Mission mission,Map map) {
                    if(mission.isInMissionArea(event.getPlayer().getLocation(),map.getName())){
                        GamePlayer gamePlayer = findPlayer(event.getPlayer().getName());
                        if(gamePlayer != null){
                            if(! mission.isEnabled()){
                                Utils.sendMessage(gamePlayer.getPlayer(),"&c&l이 임무는 활성화된 상태가 아닙니다.",true);
                                return;
                            }
                            if(gamePlayer.isAllocatedMission(mission)){
                                gamePlayer.setMissionCompleted(mission,true);
                                event.setCancelled(true);
                                gamePlayer.getPlayer().playSound(gamePlayer.getPlayer().getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,10,10);
                                Utils.sendMessage(gamePlayer.getPlayer(),"&a&l이 임무를 완료했습니다.",true);
                            }
                        }
                    }
                }
            },instance);
            set(drainConf,drain);
            getServer().getPluginManager().registerEvent(PlayerBucketFillEvent.class,drain,EventPriority.NORMAL,drain,instance);
            loadedMissions.put(Mission.MissionType.DRAINING,drain);
        }else{
            getLogger().log(Level.WARNING,"'물 푸기' 임무에 필요한 좌표가 없습니다. 좌표를 지정하고 다시 시도하세요.");
            getLogger().log(Level.WARNING,"이 임무는 제외됩니다.");
        }
        if(loadedMissions.keySet().size() <= 0){
            getLogger().log(Level.SEVERE,"불러온 임무가 존재하지 않습니다.");
        }
    }

    public GamePlayer findPlayer(String name){
         if(players == null || players.size() <= 0)
             return null;

         GamePlayer player = null;
         player = players.stream().filter(gamePlayer -> gamePlayer.getPlayer().getName().equalsIgnoreCase(name)).findAny().get();
         return player;
    }

    public boolean notNull(ConfigurationSection section){
         boolean Null = true;
         try{
             section.getList(Mission.MissionType.CONNECTING_CIRCUIT.name());
         }catch (NullPointerException ex){
             Null = false;
         }
         return Null;
    }

    private void set(ConfigurationSection section,Mission mission){
         for(String key : section.getKeys(false)){
             Mission.MissionLocation loc = new Mission.MissionLocation();
             loc.fromX = section.getDouble(key+".fromX");
             loc.fromY = section.getDouble(key+".fromY");
             loc.fromZ = section.getDouble(key+".fromZ");
             loc.toX = section.getDouble(key+".toX");
             loc.toY = section.getDouble(key+".toY");
             loc.toZ = section.getDouble(key+".toZ");
             mission.locations.put(key,loc);
             mission.availableMaps.add(findMap(key));
         }

    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public Settings getSettings(){
         return settings;
    }

    public List<Map> getLoadedMaps() {
        return loadedMaps;
    }


    public boolean isAttackCancelled() {
        return attackCancelled;
    }

    public void setAttackCancelled(boolean attackCancelled) {
        instance.attackCancelled = attackCancelled;
    }

    public Map getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(Map currentMap) {
        instance.currentMap = currentMap;
    }
    private void createFile(){
        File file = new File(getDataFolder(),"missionSettings.yml");
        if(! file.exists()){
            try{
                file.createNewFile();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }
    public void reset(){
        if(! getDataFolder().exists())
            getDataFolder().mkdir();

        manager = new FileManager(this);
        createFile();
        maps = manager.getConfig("maps.yml").copyDefaults(true).save();
        missions = manager.getConfig("missionSettings.yml").save();
        config = manager.getConfig("config.yml").copyDefaults(true).save();
         int playTime = config.get().getInt("playTime");
         int votingTime = config.get().getInt("votingTime");
         int nmurders = config.get().getInt("number of murders");
         int nmaxPlayers = config.get().getInt("maxPlayers");
         int nmissions = config.get().getInt("number of missions");
         int mct = config.get().getInt("murderCooltime");
         String world = config.get().getString("spawn.world");
         double x = config.get().getDouble("spawn.x");
         double y = config.get().getDouble("spawn.y");
         double z = config.get().getDouble("spawn.z");
         settings = new Settings();
         settings.playTime = playTime;
         settings.missions = nmissions;
         settings.votingTime = votingTime;
         settings.murder = nmurders;
         settings.maxPlayers = nmaxPlayers;
         settings.murderCooltime = mct;
         settings.spawnLocation = new Location(Bukkit.getWorld(world),x,y,z);
         registerVotingLocations();
         if(progressTask != null && progressTask.isRunning())
             progressTask.cancel();
         if(startTask != null && startTask.isRunning())
             startTask.cancel();
         if(votingTask != null && votingTask.isRunning())
             votingTask.cancel();

         if(progressTask != null && progressTask.bossBar != null)
             progressTask.bossBar.removeAll();

         if(votingTask != null && votingTask.bossBar2  != null)
             votingTask.bossBar2.removeAll();
         if(votingTask != null && votingTask.bossBar != null)
             votingTask.bossBar.removeAll();
         progressTask = new GameTask(instance);
         startTask = new ReadyTask(instance);
         votingTask = new VotingTask(instance);
         murders = new ArrayList<>();
         enabledMissions = new ArrayList<>();
         if(players.size() > 0){
             players.forEach(p -> {
                 p.setDead(false);
                 p.allocatedMissions.clear();
                 p.setMurder(false);
                 p.getPlayer().getInventory().remove(Material.PAPER);
                 if(p.getPlayer().getInventory().contains(Material.GOLD_SWORD)){
                     p.getPlayer().getInventory().remove(Material.GOLD_SWORD);
                 }
                 if(p.getPlayer().getGameMode().equals(GameMode.SPECTATOR))
                     p.getPlayer().setGameMode(GameMode.SURVIVAL);
                 p.getPlayer().teleport(settings.spawnLocation);
             });
             try {
                 CorpseAPI.removeAllCorpses();
             }catch (Exception ex){

             }
             }
         loadedMissions = new HashMap<>();
         loadedMaps = new ArrayList<>();
        registerAllMaps();
         registerAllMissions();

    }

    public HashMap<Mission.MissionType, Mission> getLoadedMissions() {
        return loadedMissions;
    }

    public void createMap(String mapName,Location loc){
        maps.get().set("maps."+mapName+".world",loc.getWorld().getName());
        maps.get().set("maps."+mapName+".x",loc.getX());
        maps.get().set("maps."+mapName+".y",loc.getY());
        maps.get().set("maps."+mapName+".z",loc.getZ());
        maps.save();
        maps.reload();
        Map map = new Map(instance,mapName);
        map.setSpawnLocation(loc);
        loadedMaps.add(map);
    }
    public Map findMap(String mapName){
        return loadedMaps.stream().filter(map -> map.getName().equals(mapName)).findAny().get();
    }

    public void createMissionArea(Location loc1, Location loc2, String mapName, Mission.MissionType missionType){
        String same = missionType.name()+"."+mapName;
        missions.get().set(same+".fromX",loc1.getX());
        missions.get().set(same+".fromY",loc1.getY());
        missions.get().set(same+".fromZ",loc1.getZ());
        missions.get().set(same+".toX",loc2.getX());
        missions.get().set(same+".toY",loc2.getY());
        missions.get().set(same+".toZ",loc2.getZ());
        missions.save();
        missions.reload();
        registerAllMissions();
    }
    public void registerVotingLocations(){
        if(! maps.get().isConfigurationSection("hub"))
            maps.get().createSection("hub");
        maps.save();
        maps.reload();
        ConfigurationSection section = maps.get().getConfigurationSection("hub");
        settings.votingLocations = new HashMap<>();

        for(String key : section.getKeys(false)){
            String world = section.getString(key+".world");
            double x = section.getDouble(key+".x");
            double y = section.getDouble(key+".y");
            double z = section.getDouble(key+".z");
            settings.votingLocations.put(key,new Location(Bukkit.getWorld(world),x,y,z));
        }
    }
    public void addLocToCCMission(List<Location> locs,String mapName){
        if(missions.get().contains(Mission.MissionType.CONNECTING_CIRCUIT.name()+"."+mapName)) {
            ConfigurationSection section = missions.get().getConfigurationSection(Mission.MissionType.CONNECTING_CIRCUIT.name() + "." + mapName);
            int size = section.getKeys(false).size();
            for (int i = 0; i < locs.size(); i++) {
                int k = size + i + 1;
                section.set(k + ".world", locs.get(i).getWorld().getName());
                section.set(k + ".x", locs.get(i).getBlockX());
                section.set(k + ".y", locs.get(i).getBlockY());
                section.set(k + ".z", locs.get(i).getBlockZ());
            }
            missions.save();
            missions.reload();
        }else{
            missions.get().createSection(Mission.MissionType.CONNECTING_CIRCUIT.name());
            missions.get().createSection(Mission.MissionType.CONNECTING_CIRCUIT.name() + "." + mapName);
            missions.save();
            missions.reload();
            ConfigurationSection section = missions.get().getConfigurationSection(Mission.MissionType.CONNECTING_CIRCUIT.name() + "." + mapName);

            try {
                int size = section.getKeys(false).size();
                for (int i = 0; i < locs.size(); i++) {
                    int k = size + i + 1;
                    section.set(k + ".world", locs.get(i).getWorld().getName());
                    section.set(k + ".x", locs.get(i).getBlockX());
                    section.set(k + ".y", locs.get(i).getBlockY());
                    section.set(k + ".z", locs.get(i).getBlockZ());
                }
            }catch (NullPointerException ex){
                for (int i = 0; i < locs.size(); i++) {
                    int k =  i+1;
                    section.set(k + ".world", locs.get(i).getWorld().getName());
                    section.set(k + ".x", locs.get(i).getBlockX());
                    section.set(k + ".y", locs.get(i).getBlockY());
                    section.set(k + ".z", locs.get(i).getBlockZ());
                }
            }
            missions.save();
            missions.reload();
        }
    }
    public void removeMap(String mapName){
        List<String> mapList = maps.get().getStringList("maps");
        mapList.remove(mapName);
        maps.get().set("maps",mapList);
        missions.get().getKeys(false).forEach(key -> missions.get().set(key+"."+mapName,null));
        maps.save();
        missions.save();
        maps.reload();
        missions.reload();
    }
}
