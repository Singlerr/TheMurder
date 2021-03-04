package io.github.singlerr.commands;

import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.GamePlayer;
import io.github.singlerr.misc.Mission;
import io.github.singlerr.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class JoinCommand implements CommandExecutor {

    private TheMurder murder;

    public JoinCommand(TheMurder murder){
        this.murder = murder;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            if(murder.murders.size() > 0){
                  p.sendMessage(Utils.translateAlternateColorCodes("&c이미 게임이 진행 중입니다."));
                  return false;
            }
            if(murder.getPlayers().stream().filter(gp -> gp.getPlayer().getName().equalsIgnoreCase(p.getName())).collect(Collectors.toList()).size() > 0){
                p.sendMessage(Utils.translateAlternateColorCodes("&c이미 게임에 참여한 상태입니다."));
                return false;
            }
            if(murder.players.size() >= murder.getSettings().maxPlayers){
                p.sendMessage(Utils.translateAlternateColorCodes("&c참여한 플레이어 수가 최대치에 도달했습니다.("+murder.getPlayers().size()+"/"+murder.getSettings().maxPlayers+")"));
                return false;
            }
            GamePlayer gamePlayer = new GamePlayer(p,new HashSet<>(),murder);
            murder.players.add(gamePlayer);
            Utils.sendMessage(p,"&a게임에 참여했습니다.("+murder.getPlayers().size()+"/"+murder.getSettings().maxPlayers+")",true);
        }else{
            commandSender.sendMessage(Utils.translateAlternateColorCodes("&c이 명령어는 콘솔에서 사용할 수 없습니다."));
        }
        return false;
    }
}
