package io.github.singlerr.task;

import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class VotingTask extends TimerTask {
    private TheMurder murder;
    private int currentVotingTime;
    private int prev = 0;
    private boolean running;
    private int k = 0;
    public BossBar bossBar;
    private boolean a;
    public BossBar bossBar2;
    public VotingTask(TheMurder murder){
        this.murder = murder;
        currentVotingTime = 0;
        running = false;
        a = false;
    }
    public void createBossBar(){
         bossBar = Bukkit.createBossBar(Utils.translateAlternateColorCodes("&a&l투표 시작까지 &c&l"+(murder.getSettings().votingTime - currentVotingTime - 60)+"&a&l초"), BarColor.GREEN, BarStyle.SOLID);
        for(GamePlayer gamePlayer : murder.getPlayers())
            bossBar.addPlayer(gamePlayer.getPlayer());
        bossBar.setVisible(true);
        k = (murder.getSettings().votingTime - currentVotingTime - 60);
    }
    @Override
    public void run() {
        running = true;
                if((murder.getSettings().votingTime - currentVotingTime - 60) == 0)
                    bossBar.removeAll();
                if((murder.getSettings().votingTime - currentVotingTime) == 60){
                    Utils.sendMessage(murder.getPlayers(),"&a&l투표 시간이 시작되었습니다.",true);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                        @Override
                        public void run() {
                            murder.getPlayers().stream().filter(p -> ! p.isDead()).forEach(p -> Utils.openVotePanel(p.getPlayer(),murder.getPlayers()));
                        }
                    },40);
                    bossBar2 = Bukkit.createBossBar(Utils.translateAlternateColorCodes("&a&l투표 종료까지 &c&l"+(murder.getSettings().votingTime - currentVotingTime)+"&a&l초"), BarColor.GREEN, BarStyle.SOLID);
                    for(GamePlayer gamePlayer : murder.getPlayers())
                        bossBar2.addPlayer(gamePlayer.getPlayer());
                    bossBar2.setVisible(true);
                }
                if ((murder.getSettings().votingTime - currentVotingTime) <= 5) {
                   if((murder.getSettings().votingTime - currentVotingTime) == 0 || a){
                       int max = 0;
                       GamePlayer player = null;
                       int duplicated = 0;
                       for(GamePlayer gamePlayer : murder.getPlayers()){
                           if(! gamePlayer.isDead()){
                               if(gamePlayer.getVote() > max) {
                                   max = gamePlayer.getVote();
                                   player = gamePlayer;
                               }
                           }
                       }
                        for(GamePlayer gamePlayer : murder.getPlayers()){
                            if(gamePlayer.getVote() == max)
                                duplicated++;
                        }
                       if(duplicated > 1 || player == null){
                           //SKIPPED
                           Utils.sendMessage(murder.getPlayers(),"&a&l그 누구도 투표로 처형되지 않았습니다.",true);
                           murder.getPlayers().forEach(p ->{
                               p.resetVote();
                               p.getPlayer().teleport(murder.currentMap.getSpawnLocation());
                               murder.progressTask.setPaused(false);
                           });
                           bossBar2.removeAll();
                           murder.setAttackCancelled(false);
                           cancel();
                       }else{

                           if(prev == 0) {
                               prev = currentVotingTime;
                                a = true;
                               Utils.sendMessage(murder.getPlayers(),"&c&l"+player.getPlayer().getName()+"&a&l님이 최다 투표로 처형됩니다.",true);
                               bossBar2.removeAll();
                           }
                           if(currentVotingTime == (prev+2) || currentVotingTime == (prev+4) || currentVotingTime == (prev+6)){
                               if(player.isMurder()){
                                   if(currentVotingTime == (prev+2))
                                        Utils.sendMessage(murder.getPlayers(),"&c&l"+player.getPlayer().getName()+"&a&l님은 &c&l살인자&al&l였습니다!",true);
                                   if(currentVotingTime == (prev+4)) {
                                       player.setDead(true);
                                       List<GamePlayer> murders = murder.getPlayers().stream().filter(p -> p.isMurder()).filter(p -> !p.isDead()).collect(Collectors.toList());
                                       if (murders.size() > 0) {
                                           Utils.sendMessage(murder.getPlayers(), "&a&l이제 &c&l" + murders.size() + "&a&l명의 살인자가 남았습니다.", true);

                                           Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                               @Override
                                               public void run() {
                                                   murder.getPlayers().forEach(p ->{
                                                       p.resetVote();
                                                       p.getPlayer().teleport(murder.currentMap.getSpawnLocation());
                                                       murder.progressTask.setPaused(false);
                                                   });
                                                   murder.setAttackCancelled(false);
                                               }
                                           },40L);
                                           running = false;
                                           cancel();
                                       } else {
                                           StringBuilder builder = new StringBuilder();
                                           for (GamePlayer m : murder.murders)
                                               builder.append(m.getPlayer().getName()).append(",");
                                           builder.setLength(builder.length() - 1);
                                           murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f));
                                           Utils.sendMessage(murder.getPlayers(), "&b&l시민들의 승리! \n 모든 살인자를 밝혀냈습니다.", true);
                                           Utils.sendMessage(murder.getPlayers(), "&a살인자: &c" + builder.toString(), true);
                                           Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                               @Override
                                               public void run() {
                                                   murder.reset();
                                               }
                                           },20L);
                                       }
                                   }
                               }else{
                                   if(currentVotingTime == (prev+2))
                                   Utils.sendMessage(murder.getPlayers(),"&c&l"+player.getPlayer().getName()+"&a&l님은 &b&l시민&a&l이었습니다!",true);

                                   if(currentVotingTime == (prev+4)) {
                                       player.setDead(true);
                                       List<GamePlayer> murders = murder.getPlayers().stream().filter(p -> p.isMurder()).filter(p -> !p.isDead()).collect(Collectors.toList());
                                       int survived = murder.getPlayers().stream().filter(p -> !p.isDead()).collect(Collectors.toList()).size();
                                       if (survived == 0) {
                                           StringBuilder builder = new StringBuilder();
                                           for (GamePlayer m : murder.murders)
                                               builder.append(m.getPlayer().getName()).append(",");
                                           builder.setLength(builder.length() - 1);
                                           murder.getPlayers().forEach(p -> p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f));
                                           Utils.sendMessage(murder.getPlayers(), "&b&l살인자의 승리! \n 모든 시민들을 죽였습니다.", true);
                                           Utils.sendMessage(murder.getPlayers(), "&a살인자: &c" + builder.toString(), true);
                                           Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                               @Override
                                               public void run() {
                                                   murder.reset();
                                               }
                                           },40L);
                                       } else if (murders.size() > 0) {
                                           Utils.sendMessage(murder.getPlayers(), "&c&l" + murders.size() + "&a&l명의 살인자가 남아있습니다.", true);

                                           Bukkit.getScheduler().scheduleSyncDelayedTask(murder, new Runnable() {
                                               @Override
                                               public void run() {
                                                   murder.getPlayers().forEach(p ->{
                                                       p.resetVote();
                                                       p.getPlayer().teleport(murder.currentMap.getSpawnLocation());
                                                       murder.progressTask.setPaused(false);
                                                   });
                                                   murder.setAttackCancelled(false);
                                               }
                                           },40L);
                                           running = false;
                                           cancel();
                                       }
                                   }

                               }
                           }

                       }

                   }
                 //  Utils.sendMessage(murder.getPlayers(),"&c&l"+(murder.getSettings().votingTime - currentVotingTime),true);
                }
                currentVotingTime++;
                if(murder.getSettings().votingTime - currentVotingTime - 60 >= 0)
                bossBar.setProgress((double)(murder.getSettings().votingTime - currentVotingTime - 60)/k);
                bossBar.setTitle(Utils.translateAlternateColorCodes("&a&l투표 시작까지 &c&l"+(murder.getSettings().votingTime - currentVotingTime-60)+"&a&l초"));
                if(bossBar2 != null){
                    if((double)(murder.getSettings().votingTime - currentVotingTime)/murder.getSettings().votingTime > 0)
                    bossBar2.setProgress((double)(murder.getSettings().votingTime - currentVotingTime)/murder.getSettings().votingTime);
                    bossBar2.setTitle(Utils.translateAlternateColorCodes("&a&l투표 종료까지 &c&l"+(murder.getSettings().votingTime - currentVotingTime)+"&a&l초"));
                }

    }

    public boolean isRunning() {
        return running;
    }
}
