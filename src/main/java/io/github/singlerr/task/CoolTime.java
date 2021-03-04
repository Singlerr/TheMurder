package io.github.singlerr.task;

import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

public class CoolTime extends BukkitRunnable {
    private TheMurder murder;
    private int currentCoolTime;
    private GamePlayer player;
    public CoolTime(TheMurder murder,GamePlayer player){
        this.murder = murder;
        currentCoolTime = 0;
        this.player = player;
    }

    @Override
    public void run() {
           int leftTime = murder.getSettings().murderCooltime - currentCoolTime;
           if(leftTime > 0){
               currentCoolTime++;
               player.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Utils.translateAlternateColorCodes("&a&l살인 대기 시간: &c&l"+leftTime+"&a&l초")));
           }
           if(leftTime == 0){
               player.setLocked(false);
               player.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Utils.translateAlternateColorCodes("&a&l이제 &c&l살인&a&l을 하실 수 있습니다.")));
               Utils.sendMessage(player.getPlayer(),"&a&l이제 &c&l살인&a&l을 하실 수 있습니다.",false);
               cancel();
           }
    }
}
