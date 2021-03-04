package io.github.singlerr.misc;

import io.github.singlerr.TheMurder;
import org.bukkit.entity.Player;

import java.util.Set;

public class GamePlayer{
    private boolean isMurder;
    private Player player;
    public Set<Mission> allocatedMissions;
    private TheMurder murder;
    private boolean dead;
    private boolean locked;
    public boolean voted;
    private int vote;
    public GamePlayer(Player player, Set<Mission> allocatedMissions, TheMurder murder){
        this.player = player;
        this.allocatedMissions = allocatedMissions;
        this.murder = murder;
        isMurder = false;
        vote = 0;
        voted = false;
        dead = false;
        locked = false;
    }

    public boolean isMurder() {
        return isMurder;
    }
    public void setMurder(boolean flag){
        isMurder = flag;
    }
    public void setMissionCompleted(Mission mission, boolean flag){
        if(flag){
            if(allocatedMissions.contains(mission)){
                allocatedMissions.remove(mission);
            }
        }else{
            if(! allocatedMissions.contains(mission))
                allocatedMissions.add(mission);
        }
    }

    public Set<Mission> getAllocatedMissions() {
        return allocatedMissions;
    }
    public boolean isAllocatedMission(Mission mission){
        return allocatedMissions.contains(mission);
    }
    public Player getPlayer(){
        return player;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void setMurder(TheMurder murder) {
        this.murder = murder;
    }
    public void addVote(){
        vote++;
    }

    public int getVote() {
        return vote;
    }
    public void resetVote(){
        vote = 0;
        voted = false;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
