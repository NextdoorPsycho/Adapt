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

package com.volmit.adapt.content.adaptation.unarmed;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Form;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class UnarmedSuckerPunch extends SimpleAdaptation<UnarmedSuckerPunch.Config> {
    public UnarmedSuckerPunch() {
        super("unarmed-sucker-punch");
        registerConfiguration(Config.class);
        setDescription(Adapt.dLocalize("Unarmed","SuckerPunch", "Description"));
        setDisplayName(Adapt.dLocalize("Unarmed","SuckerPunch", "Name"));
        setIcon(Material.OBSIDIAN);
        setBaseCost(getConfig().baseCost);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
        int baseCost = 2;
        int initialCost = 4;
        double costFactor = 0.225;
        double baseDamage = 0.2;
        double damageFactor = 0.55;
    }

    @Override
    public void addStats(int level, Element v) {
        double f = getLevelPercent(level);
        double d = getDamage(f);
        v.addLore(C.GREEN + "+ " + Form.pc(d, 0) + C.GRAY + " " +Adapt.dLocalize("Unarmed","SuckerPunch", "Lore1"));
        v.addLore(C.GRAY + Adapt.dLocalize("Unarmed","SuckerPunch", "Lore2"));
    }

    private double getDamage(double f) {
        return getConfig().baseDamage + (f * getConfig().damageFactor);
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            if (!hasAdaptation(p)) {
                return;
            }
            if (p.getInventory().getItemInMainHand().getType() != Material.AIR || p.getInventory().getItemInOffHand().getType() != Material.AIR) {
                return;
            }
            double factor = getLevelPercent(p);

            if (!p.isSprinting()) {
                return;
            }

            if (factor <= 0) {
                return;
            }

            if (isTool(p.getInventory().getItemInMainHand())) {
                return;
            }

            e.setDamage(e.getDamage() * getDamage(factor));
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1.8f);
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_BASALT_BREAK, 1f, 0.6f);
            getSkill().xp(p, 6.221 * e.getDamage());
            if (e.getDamage() > 5) {
                getSkill().xp(p, 0.42 * e.getDamage());
                e.getEntity().getWorld().spawnParticle(Particle.FLASH, e.getEntity().getLocation(), 1);
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
}
