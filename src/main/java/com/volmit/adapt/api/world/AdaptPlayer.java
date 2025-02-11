/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.api.world;

import com.google.gson.Gson;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.notification.AdvancementNotification;
import com.volmit.adapt.api.notification.Notifier;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.tick.TickedObject;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.ChronoLatch;
import com.volmit.adapt.util.Form;
import com.volmit.adapt.util.IO;
import com.volmit.adapt.util.JSONObject;
import com.volmit.adapt.util.M;
import com.volmit.adapt.util.RollingSequence;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = false)
@Data
public class AdaptPlayer extends TickedObject {
    private final Player player;
    private final PlayerData data;
    private ChronoLatch savelatch;
    private ChronoLatch updatelatch;
    private Notifier not;
    private Notifier actionBarNotifier;
    private AdvancementHandler advancementHandler;
    private RollingSequence speed;
    private long lastloc;
    private Vector velocity;
    private Location lastpos;

    public AdaptPlayer(Player p) {
        super("players", p.getUniqueId().toString(), 50);
        this.player = p;
        data = loadPlayerData();
        updatelatch = new ChronoLatch(1000);
        savelatch = new ChronoLatch(60000);
        not = new Notifier(this);
        actionBarNotifier = new Notifier(this);
        advancementHandler = new AdvancementHandler(this);
        speed = new RollingSequence(7);
        lastloc = M.ms();
        getAdvancementHandler().activate();
        velocity = new Vector();
    }

    public boolean canConsumeFood(double cost, int minFood)
    {
        return (player.getFoodLevel() + player.getSaturation()) - minFood > cost;
    }

    public boolean consumeFood(double cost, int minFood)
    {
        if(canConsumeFood(cost, minFood))
        {
            int food = player.getFoodLevel();
            double sat = player.getSaturation();

            if(sat >= cost)
            {
                sat = (player.getSaturation() - cost);
                cost = 0;
            }

            else if(player.getSaturation() > 0)
            {
                cost -= sat;
                sat = 0;
            }

            if(cost >= 1)
            {
                food -= (int) Math.floor(cost);
                cost = Math.floor(cost);
            }

            if(cost > 0)
            {
                if(sat >= cost)
                {
                    sat -= cost;
                    cost = 0;
                }

                else
                {
                    sat++;
                    food--;
                }
            }

            if(sat >= cost && cost > 0)
            {
                sat -= cost;
                cost = 0;
            }

            player.setFoodLevel(food);
            player.setSaturation((float) sat);

            return true;
        }

        return false;
    }

    public boolean isBusy() {
        return not.isBusy();
    }

    public PlayerSkillLine getSkillLine(String l) {
        return getData().getSkillLine(l);
    }

    @SneakyThrows
    private void save() {
        IO.writeAll(new File(Bukkit.getServer().getPluginManager().getPlugin(Adapt.instance.getName()).getDataFolder() + File.separator + "data" + File.separator + "players" + File.separator + player.getUniqueId() + ".json"), new JSONObject(new Gson().toJson(data)).toString(4));
    }

    @Override
    public void unregister() {
        super.unregister();
        getAdvancementHandler().deactivate();
        save();
    }

    private PlayerData loadPlayerData() {
        File f = new File(Bukkit.getServer().getPluginManager().getPlugin(Adapt.instance.getName()).getDataFolder() + File.separator + "data" + File.separator + "players" + File.separator + player.getUniqueId() + ".json");

        if(f.exists()) {
            try {
                return new Gson().fromJson(IO.readAll(f), PlayerData.class);
            } catch(Throwable ignored) {

            }
        }

        return new PlayerData();
    }

    @Override
    public void onTick() {
        if(updatelatch.flip()) {
            getData().update(this);
        }

        if(savelatch.flip()) {
            save();
        }

        getServer().takeSpatial(this);

        Location at = player.getLocation();

        if(lastpos != null) {
            if(lastpos.getWorld().equals(at.getWorld())) {
                if(lastpos.distanceSquared(at) <= 7 * 7) {
                    speed.put(lastpos.distance(at) / ((double) (M.ms() - lastloc) / 50D));
                    velocity = velocity.clone().add(at.clone().subtract(lastpos).toVector()).multiply(0.5);
                    velocity.setX(Math.abs(velocity.getX()) < 0.01 ? 0 : velocity.getX());
                    velocity.setY(Math.abs(velocity.getY()) < 0.01 ? 0 : velocity.getY());
                    velocity.setZ(Math.abs(velocity.getZ()) < 0.01 ? 0 : velocity.getZ());
                }
            }
        }

        lastpos = at.clone();
        lastloc = M.ms();
    }

    public double getSpeed() {
        return speed.getAverage();
    }

    public void giveXPToRecents(AdaptPlayer p, double xpGained, int ms) {
        for(PlayerSkillLine i : p.getData().getSkillLines().v()) {
            if(M.ms() - i.getLast() < ms) {
                i.giveXP(not, xpGained);
            }
        }
    }

    public void giveXPToRandom(AdaptPlayer p, double xpGained) {
        p.getData().getSkillLines().v().getRandom().giveXP(p.getNot(), xpGained);
    }

    public void boostXPToRandom(AdaptPlayer p, double boost, int ms) {
        p.getData().getSkillLines().v().getRandom().boost(boost, ms);
    }

    public void boostXPToRecents(AdaptPlayer p, double boost, int ms) {
        for(PlayerSkillLine i : p.getData().getSkillLines().v()) {
            if(M.ms() - i.getLast() < ms) {
                i.boost(boost, ms);
            }
        }
    }

    public void loggedIn() {
        long timeGone = M.ms() - getData().getLastLogin();
        boolean first = getData().getLastLogin() == 0;
        getData().setLastLogin(M.ms());
        long boostTime = (long) Math.min(timeGone / 12D, TimeUnit.HOURS.toMillis(1));

        if(boostTime < TimeUnit.MINUTES.toMillis(5)) {
            return;
        }

        double boostAmount = M.lerp(0.1, 0.25, (double) boostTime / (double) TimeUnit.HOURS.toMillis(1));
        getData().globalXPMultiplier(boostAmount, (int) boostTime);
        getNot().queue(AdvancementNotification.builder()
            .title(first ? "Welcome!" : "Welcome Back!")
            .description("+" + C.GREEN + Form.pc(boostAmount, 0) + C.GRAY + " XP for " + C.AQUA + Form.duration(boostTime, 0))
            .build());
    }

    public boolean hasSkill(Skill s) {
        return getData().getSkillLines().containsKey(s.getName()) && getData().getSkillLines().get(s.getId()).getXp() > 1;
    }
}
