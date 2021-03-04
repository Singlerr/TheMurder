package io.github.singlerr.misc;

import io.github.singlerr.TheMurder;
import org.bukkit.Location;

public class Map {
    private TheMurder plugin;
    private String mapName;
    private Location spawn;

    public Map(TheMurder plugin,String mapName){
        this.plugin = plugin;
        this.mapName = mapName;
    }
    public void setSpawnLocation(Location loc){
        this.spawn = loc;
    }
    public Location getSpawnLocation(){
        return spawn;
    }

    public String getName() {
        return mapName;
    }
    //임무 위치 저장되어있음
}
