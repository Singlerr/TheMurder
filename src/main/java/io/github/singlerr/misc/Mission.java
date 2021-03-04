package io.github.singlerr.misc;

import io.github.singlerr.TheMurder;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Mission<T> implements Listener, EventExecutor {

    //Cutting Tree, Building Tree
    public HashMap<String,MissionLocation> locations;
    public HashMap<String,Set<Location>> ccLocations;
    public List<Map> availableMaps;
    private TheMurder murder;
    private MissionType missionType;
    private EventCallback<T> callback;
    private boolean enabled;
    public enum MissionType{
        BUILDING_TREE("나무 기르기"),
        CUTTING_TREE("벌목"),
        CONNECTING_CIRCUIT("회로 연결"),
        GENERATING_PORTAL("포탈 생성"),
        BREEDING("동물 교배"),
        MINING("채굴"),
        TURNING_ON_LAMPS("점등"),
        ENCHANTING("인챈트"),
        DRAINING("물 푸기");

        private String kor;
        MissionType(String kor){
            this.kor = kor;
        }
        public String getName(){
            return kor;
        }
    }
    public Mission(MissionType type, EventCallback<T> callback, TheMurder murder){
        Validate.notNull(type);
        Validate.notNull(callback);
        this.missionType = type;
        this.callback = callback;
        locations = new HashMap<>();
        this.murder = murder;
        ccLocations = new HashMap<>();
        availableMaps = new ArrayList<>();
        enabled = false;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        callback.on((T)event,this,murder.currentMap);
    }


    public interface EventCallback<T>{
        public void on(T event,Mission mission,Map map);
    }

    public boolean isInMissionArea(Location location,String mapName){
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        MissionLocation loc = locations.get(mapName);
        boolean a = false;
        if(loc.toX > loc.fromX)
            a = (x > loc.fromX && x < loc.toX);
        else
            a = (x > loc.toX && x < loc.fromX);
        boolean b = false;
        if(loc.toY > loc.fromY)
            b = (y > loc.fromY && y < loc.toY);
        else
            b = (y > loc.toY && y < loc.fromY);
        boolean c = false;
        if(loc.toZ > loc.fromZ)
            c = (z > loc.fromZ && z < loc.toZ);
        else
            c = (z > loc.toZ && z < loc.fromZ);
        return a && b && c;
    }
    public static class MissionLocation{
        public double fromX,fromY,fromZ;
        public double toX,toY,toZ;
        public MissionLocation(){

        }
    }
    public Mission<T> clone(){
        Mission<T> instance = new Mission<>(missionType,callback,murder);
        return instance;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public boolean isEnabled() {
        return murder.enabledMissions.contains(getMissionType());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        murder.enabledMissions.add(getMissionType());
    }
}
