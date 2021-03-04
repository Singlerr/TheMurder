package io.github.singlerr.commands;

import io.github.singlerr.TheMurder;
import io.github.singlerr.misc.Map;
import io.github.singlerr.misc.Mission;
import io.github.singlerr.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.rmi.CORBA.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor {
    private HashMap<String, Location> loc1 = new HashMap<>();
    private HashMap<String,Location> loc2 = new HashMap<>();
    private TheMurder murder;

    public AdminCommand(TheMurder murder){
        this.murder = murder;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(commandSender instanceof Player) {
            Player p = (Player) commandSender;
            if (p.isOp()){
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("create")) {
                        if (args.length > 1) {
                            //CREATE MAP
                            String mapName = args[1];
                            if (murder.getLoadedMaps().stream().filter(m -> m.getName().equals(mapName)).collect(Collectors.toList()).size() == 0) {
                                murder.createMap(mapName, p.getLocation());
                                Utils.sendMessage(p, "&a" + mapName + "맵을 생성하고 현재 위치를 맵의 시작 위치로 설정했습니다.", false);
                            } else {
                                Utils.sendMessage(p, "&a해당 맵은 이미 존재합니다.", false);
                            }
                        } else {
                            //ARGS-CREATE-MAP
                        }
                    } else if (args[0].equalsIgnoreCase("setpos1")) {
                        loc1.put(p.getName(), p.getLocation());
                        Utils.sendMessage(p, "&a설정 완료.", false);
                    } else if (args[0].equalsIgnoreCase("setpos2")) {
                        loc2.put(p.getName(), p.getLocation());
                        Utils.sendMessage(p, "&a설정 완료.", false);
                    } else if (args[0].equalsIgnoreCase("cma")) {
                        if (args.length > 2) {
                            if (loc1.containsKey(p.getName()) && loc2.containsKey(p.getName())) {
                                String mapName = args[1];
                                String type = args[2];
                                if (contains(type)) {
                                    murder.createMissionArea(loc1.get(p.getName()), loc2.get(p.getName()), mapName, Mission.MissionType.valueOf(type));
                                    Utils.sendMessage(p, "&a성공적으로 작업을 완료했습니다.", false);
                                } else {
                                    Utils.sendMessage(p, "&c해당 임무를 찾을 수 없습니다.", false);
                                    StringBuilder builder = new StringBuilder();
                                    for (Mission.MissionType mt : Mission.MissionType.values())
                                        builder.append(mt.name()).append(", ");
                                    builder.setLength(builder.length() - 1);

                                    Utils.sendMessage(p, "&a임무: &b" + builder.toString(), false);
                                }
                            } else {
                                Utils.sendMessage(p, "&c먼저 양쪽 지점을 설정해주십시오.", false);
                            }
                        } else {
                            Utils.sendMessage(p, "&c/dm cma [맵이름] [임무]", false);
                        }
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        murder.reset();
                        Utils.sendMessage(p, "&a리로드 완료.", false);
                    } else if (args[0].equalsIgnoreCase("start")) {
                        new Timer().scheduleAtFixedRate(murder.startTask,1000,1000);
                    } else if (args[0].equalsIgnoreCase("on")) {
                        if (!murder.modes.containsKey(p.getName())) {
                            murder.modes.put(p.getName(), new ArrayList<>());
                            Utils.sendMessage(p, "&a설정 모드 활성화.", false);
                        } else {
                            murder.modes.remove(p.getName());
                            Utils.sendMessage(p, "&c설정 모드 비활성화.", false);
                        }
                    } else if (args[0].equalsIgnoreCase("savecc")) {
                        if (args.length > 1) {
                            if (murder.modes.containsKey(p.getName())) {
                                List<Location> loc = murder.modes.get(p.getName());
                                if (loc.size() > 0) {
                                    murder.addLocToCCMission(loc, args[1]);
                                    Utils.sendMessage(p, "&a작업을 완료했습니다.", false);
                                } else {
                                    Utils.sendMessage(p, "&c추가한 지점이 없습니다.", false);
                                }
                            } else {
                                Utils.sendMessage(p, "&c추가한 지점이 없습니다.", false);
                            }
                        } else {
                            Utils.sendMessage(p, "&c맵 이름이 필요합니다.", false);
                        }
                    } else if (args[0].equalsIgnoreCase("mlist")) {
                        StringBuilder builder = new StringBuilder();
                        for (Map map : murder.getLoadedMaps())
                            builder.append(map.getName()).append(", ");
                        if (builder.length() > 0)
                            builder.setLength(builder.length() - 2);
                        Utils.sendMessage(p, "&a불러온 맵:", false);
                        Utils.sendMessage(p, "&b&l" + builder.toString(), false);
                    } else if (args[0].equalsIgnoreCase("missions")) {
                        if (args.length > 1) {
                            String mapName = args[1];
                            StringBuilder builder = new StringBuilder();
                            Map map = murder.findMap(mapName);
                            for(Mission m : murder.getLoadedMissions().values()){
                                if(m.availableMaps.contains(map))
                                    builder.append(m.getMissionType().name()).append(", ");
                            }
                            if (builder.length() > 0)
                                builder.setLength(builder.length() - 2);
                            Utils.sendMessage(p, "&a맵(" + mapName + ")에 적용된 임무들:", false);
                            Utils.sendMessage(p, "&b&l" + builder.toString(), false);
                        } else {
                            Utils.sendMessage(p, "&c맵 이름이 필요합니다.", false);
                        }
                    } else if (args[0].equalsIgnoreCase("remove")) {
                        if (args.length > 1) {
                            String mapName = args[1];
                            if (murder.getLoadedMaps().stream().filter(m -> m.getName().equals(mapName)).collect(Collectors.toList()).size() > 0) {
                                murder.removeMap(mapName);
                                Utils.sendMessage(p, "&c성공적으로 해당 맵을 제거했습니다.", false);
                            } else {
                                Utils.sendMessage(p, "&c해당 맵은 존재하지 않습니다.", false);
                            }
                        } else {
                            Utils.sendMessage(p, "&c맵 이름이 필요합니다.", false);
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        if (murder.murders.size() > 0) {
                            murder.reset();
                            Utils.sendMessage(murder.getPlayers(), "&c게임이 중지되었습니다.", false);
                            Utils.sendMessage(p, "&a게임을 중지했습니다.", false);
                        } else {
                            Utils.sendMessage(p, "&c게임이 진행 중이지 않습니다.", false);
                        }
                    }else if(args[0].equalsIgnoreCase("setv")){
                        if (args.length > 1) {
                            String mapName = args[1];
                            if (murder.getLoadedMaps().stream().filter(m -> m.getName().equals(mapName)).collect(Collectors.toList()).size() > 0) {
                                murder.setVotingLocation(mapName,p.getLocation());
                                Utils.sendMessage(p,"&a현재 위치를 해당 맵의 투표 장소로 설정하였습니다.",false);
                            } else {
                                Utils.sendMessage(p, "&c해당 맵은 존재하지 않습니다.", false);
                            }
                        } else {
                            Utils.sendMessage(p, "&c맵 이름이 필요합니다.", false);
                        }
                    }else if(args[0].equalsIgnoreCase("setspawn")){
                        murder.setSpawnLocation(p.getLocation());
                        Utils.sendMessage(p,"&a현재 위치를 스폰 장소로 설정하였습니다.",false);
                    }else if(args[0].equalsIgnoreCase("loaded")){
                        murder.getLoadedMaps().forEach(m -> p.sendMessage(m.getName()));
                    }else if(args[0].equalsIgnoreCase("loadedm")){
                    }else if(args[0].equalsIgnoreCase("plays")) {
                        p.playSound(p.getLocation(), Sound.valueOf(args[1].toUpperCase()), 10, 10);
                    }
                } else {
                    //ARGS
                    Utils.sendMessage(p, "&a/dm stop - 현재 진행 중인 게임을 중지합니다.", false);
                    Utils.sendMessage(p, "&a/dm start - 게임을 시작합니다.", false);
                    Utils.sendMessage(p, "&a/dm create [맵이름] - 맵을 생성하고 스폰 위치를 현재 자신의 위치로 설정합니다.", false);
                    Utils.sendMessage(p, "&a/dm setpos1/setpos2 - 임무 위치를 정하기 위한 지점을 설정합니다.", false);
                    Utils.sendMessage(p, "&a/dm cma [맵이름] [임무] - 해당 맵에 임무를 추가합니다. /dm cma [맵이름] 입력으로 임무 목록 확인 가능.", false);
                    Utils.sendMessage(p, "&a/dm mlist - 저장된 모든 맵을 확인합니다.", false);
                    Utils.sendMessage(p, "&a/dm missions [맵이름] - 해당 맵에 할당된 임무를 확인합니다.", false);
                    Utils.sendMessage(p, "&a/dm savecc [맵이름] - [회로 연결] 임무에 필요한 회로 위치를 해당 맵에 설정합니다. /dm on으로 위치를 설정하세요.", false);
                    Utils.sendMessage(p, "&a/dm on - 회로 위치 설정 모드를 변경합니다.", false);
                    Utils.sendMessage(p, "&a/dm remove [맵이름] - 맵과 맵에 할당된 모든 임무를 삭제합니다.", false);
                    Utils.sendMessage(p,"&a/dm setv [맵이름] - 해당 맵의 투표 장소를 현재 위치로 설정합니다.",false);
                    Utils.sendMessage(p,"&a/dm setspawn - 현재 위치를 스폰 장소로 설정합니다.",false);
                    Utils.sendMessage(p, "&a/dm reload - 모든 설정을 다시 불러옵니다.(임무,맵,기본 설정 포함)", false);
                }
            }else{
                Utils.sendMessage(p,"&c이 명령어에 대한 권한이 없습니다.",false);
            }
        }else{
           commandSender.sendMessage(Utils.translateAlternateColorCodes("&c이 명령어는 콘솔에서 사용할 수 없습니다."));
        }
        return false;
    }
    private boolean contains(String type){
        try{
            Mission.MissionType.valueOf(type);
            return true;
        }catch (Exception ex){
            return false;
        }
    }
}
