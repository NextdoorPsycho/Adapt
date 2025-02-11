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

package com.volmit.adapt.content.adaptation.rift;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.M;
import lombok.NoArgsConstructor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;


public class RiftBlink extends SimpleAdaptation<RiftBlink.Config> {
    private final Map<Player, Long> lastJump = new HashMap<>();

    public RiftBlink() {
        super("rift-blink");
        registerConfiguration(Config.class);
        setDescription(Adapt.dLocalize("Rift","RiftBlink", "Description"));
        setDisplayName(Adapt.dLocalize("Rift","RiftBlink", "Name"));
        setIcon(Material.FEATHER);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(9288);
    }

    private double getBlinkDistance(int level) {
        return getConfig().baseDistance + (getLevelPercent(level) * getConfig().distanceFactor);
    }

    private int getCooldownDuration(int level) {
        return 2000;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + "+ " + (getBlinkDistance(level)) + C.GRAY + Adapt.dLocalize("Rift","RiftBlink", "Lore1"));
        v.addLore(C.ITALIC + Adapt.dLocalize("Rift","RiftBlink", "Lore2") + C.DARK_PURPLE + Adapt.dLocalize("Rift","RiftBlink", "Lore3"));
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        lastJump.remove(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (hasAdaptation(e.getPlayer()) && p.getGameMode().equals(GameMode.SURVIVAL)) {
            e.setCancelled(true);
            p.setAllowFlight(false);

            if (lastJump.get(p) != null && M.ms() - lastJump.get(p) <= getCooldownDuration(getLevel(p))) {
                return;
            }
            if (p.isSprinting()) {

                Location loc = p.getLocation().clone();
                Location locOG = p.getLocation().clone();
                Vector dir = loc.getDirection();
                double dist = getBlinkDistance(getLevel(p));
                dir.multiply(dist);
                loc.add(dir);
                double cd = dist * 2;
                loc.subtract(0, dist, 0);

                while (!isSafe(loc) && cd-- > 0) {
                    loc.add(0, 1, 0);
                }

                if (!isSafe(loc)) {
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1.24f);
                    lastJump.put(p, M.ms());
                    return;
                }

                if (getPlayer(p).getData().getSkillLines().get("rift").getAdaptations().get("rift-resist") != null &&
                        getPlayer(p).getData().getSkillLines().get("rift").getAdaptations().get("rift-resist").getLevel() > 0) {
                    RiftResist.riftResistStackAdd(p, 10, 5);
                }


                vfxParticleLine(locOG, loc, Particle.REVERSE_PORTAL, 50, 8, 0.1D, 1D, 0.1D, 0D, null, false, l -> l.getBlock().isPassable());
                J.s(() -> {
                    Vector v = p.getVelocity().clone();
                    p.teleport(loc.add(0, 1, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    p.setVelocity(v.multiply(3));
                });

                lastJump.put(p, M.ms());

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.50f, 1.0f);
                vfxLevelUp(p);

            }
        }
    }

    public boolean isSafe(Location l) {
        return l.getBlock().getType().isSolid() && !l.getBlock().getRelative(BlockFace.UP).getType().isSolid() && !l.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().isSolid();
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (hasAdaptation(e.getPlayer()) && p.getGameMode().equals(GameMode.SURVIVAL)) {


            if (lastJump.get(p) != null && M.ms() - lastJump.get(p) <= getCooldownDuration(getLevel(p))) {
                p.setAllowFlight(false);
                return;
            }

            Location loc = p.getLocation().clone();
            Vector dir = loc.getDirection();
            double dist = getBlinkDistance(getLevel(p));
            dir.multiply(dist);
            loc.add(dir);
            double cd = dist * 2;
            loc.subtract(0, dist, 0);

            while (!isSafe(loc) && cd-- > 0) {
                loc.add(0, 1, 0);
            }
            if (!isSafe(loc)) {
                return;
            } else if (isSafe(loc)) {
                p.setAllowFlight(e.getPlayer().getFallDistance() < 4.5 && e.getPlayer().isSprinting());
            }

        }
    }


    @Override
    public void onTick() {

    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
        int baseCost = 15;
        double costFactor = 1;
        int maxLevel = 5;
        int initialCost = 5;
        double baseDistance = 6;
        double distanceFactor = 5;
    }
}