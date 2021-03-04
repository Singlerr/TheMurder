package io.github.singlerr.task;

import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.misc.Map;
import io.github.singlerr.misc.Mission;
import io.github.singlerr.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class GameTask extends TimerTask {
    private boolean ignore;
    private TheMurder murder;
    private int currentPlayTime;
    public BossBar bossBar;
    private boolean running;
    public GameTask(TheMurder murder){

        this.murder = murder;
        this.ignore = false;
        currentPlayTime = 0;
        running = false;
    }
    public void createBossBar(){
        bossBar = Bukkit.createBossBar("", BarColor.BLUE,BarStyle.SOLID);
        for(GamePlayer p : murder.getPlayers())
            bossBar.addPlayer(p.getPlayer());

        bossBar.setVisible(true);
    }
    @Override
    public void run() {

                running = true;
                if(currentPlayTime >= murder.getSettings().playTime){
                    //Game End.
                    endGame();
                }
                long comp = murder.getPlayers().stream().filter(p -> !p.isMurder()).filter(p -> p.getAllocatedMissions().size() == 0).count();
                long non = murder.getPlayers().size() - murder.getSettings().murder;
                if(comp == non){
                    endGame_();
                }
                long murders = murder.murders.stream().filter(p -> ! p.isDead()).count();
                long survived = murder.getPlayers().stream().filter(p -> ! p.isDead()).count();
                if(survived == 0){
                    StringBuilder builder = new StringBuilder();
                    for(GamePlayer m : murder.murders)
                        builder.append(m.getPlayer().getName()).append(",");
                    builder.setLength(builder.length()-1);
                    murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f));
                    Utils.sendMessage(murder.getPlayers(),"&b&l살인자의 승리! \n 모든 시민들을 죽였습니다.",true);
                    Utils.sendMessage(murder.getPlayers(),"&a살인자: &c"+builder.toString(),true);
                    running = false;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                        @Override
                        public void run() {
                            murder.reset();
                        }
                    },20);

                }else  if(murders == 0){
                    StringBuilder builder = new StringBuilder();
                    for(GamePlayer m : murder.murders)
                        builder.append(m.getPlayer().getName()).append(",");
                    builder.setLength(builder.length()-1);
                    murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f));
                    Utils.sendMessage(murder.getPlayers(),"&b&l시민들의 승리! \n 모든 살인자를 밝혀냈습니다.",true);
                    Utils.sendMessage(murder.getPlayers(),"&a살인자: &c"+builder.toString(),true);
                    running = false;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                        @Override
                        public void run() {
                            murder.reset();
                        }
                    },20);
                }
                if(! ignore) {
                    currentPlayTime++;
                }
                bossBar.setTitle(Utils.translateAlternateColorCodes("&a 종료까지 &c&l"+convertToHours((murder.getSettings().playTime-currentPlayTime))+"&a시간 &c&l"+convertToMinutes((murder.getSettings().playTime-currentPlayTime))+"&a분 &c&l"
                        + convertToSeconds((murder.getSettings().playTime-currentPlayTime))+"&a초"));
                if(murder.getSettings().playTime-currentPlayTime >= 0)
                    bossBar.setProgress((double)(murder.getSettings().playTime-currentPlayTime)/murder.getSettings().playTime);

    }
    private int convertToHours(int time){
        return time/(60*60);
    }
    private int convertToMinutes(int time){
        return time/(60);
    }
    private int convertToSeconds(int time){
        return time - convertToHours(time)*60*60 - convertToMinutes(time)*60;
    }
    private void endGame(){
        List<GamePlayer> murders = murder.getPlayers().stream().filter(p -> p.isMurder()).collect(Collectors.toList());
        List<GamePlayer> survived = murder.getPlayers().stream().filter(p -> ! p.isDead()).collect(Collectors.toList());
        List<GamePlayer> murdersSurvived = survived.stream().filter(p -> p.isMurder()).collect(Collectors.toList());

        if(survived.size() > murdersSurvived.size()){
            StringBuilder builder = new StringBuilder();
            for(GamePlayer m : murders)
                builder.append(m.getPlayer().getName()).append(",");
            builder.setLength(builder.length()-1);
            murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f));
            Utils.sendMessage(murder.getPlayers(),"&b&l시간 초과! \n 시민들이 승리했습니다.",true);
            Utils.sendMessage(murder.getPlayers(),"&a살인자: &c"+builder.toString(),true);
            Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                @Override
                public void run() {
                    murder.reset();
                }
            },50);
            running = false;
            cancel();
        }
    }
    private void endGame_(){
            StringBuilder builder = new StringBuilder();
            for(GamePlayer m : murder.murders)
                builder.append(m.getPlayer().getName()).append(",");
            builder.setLength(builder.length()-1);
            murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f));
            Utils.sendMessage(murder.getPlayers(),"&b&l시민들의 승리! \n 모든 임무를 완료했습니다.",true);
            Utils.sendMessage(murder.getPlayers(),"&a살인자: &c"+builder.toString(),true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
            @Override
            public void run() {
                murder.reset();
            }
        },50);
        running = false;
        cancel();
    }
    public int getCurrentPlayTime() {
        return currentPlayTime;
    }

    public boolean isPaused() {
        return ignore;
    }

    public void setPaused(boolean ignore) {
        this.ignore = ignore;
    }

    public boolean isRunning() {
        return running;
    }
}
