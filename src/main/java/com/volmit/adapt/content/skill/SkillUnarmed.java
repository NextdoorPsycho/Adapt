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

package com.volmit.adapt.content.skill;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.adaptation.unarmed.UnarmedGlassCannon;
import com.volmit.adapt.content.adaptation.unarmed.UnarmedPower;
import com.volmit.adapt.content.adaptation.unarmed.UnarmedSuckerPunch;
import com.volmit.adapt.util.C;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class SkillUnarmed extends SimpleSkill<SkillUnarmed.Config> {
    public SkillUnarmed() {
        super("unarmed", Adapt.dLocalize("Skill", "Unarmed", "Icon"));
        registerConfiguration(Config.class);
        setColor(C.YELLOW);
        setDescription(Adapt.dLocalize("Skill", "Unarmed", "Description"));
        setDisplayName(Adapt.dLocalize("Skill", "Unarmed", "Name"));
        setInterval(2570);
        registerAdaptation(new UnarmedSuckerPunch());
        registerAdaptation(new UnarmedPower());
        registerAdaptation(new UnarmedGlassCannon());
        setIcon(Material.FIRE_CHARGE);
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent e) {
        if (!e.isCancelled()) {
            if (e.getDamager() instanceof Player) {
                AdaptPlayer a = getPlayer((Player) e.getDamager());
                ItemStack hand = a.getPlayer().getInventory().getItemInMainHand();
                if (!isMelee(hand)) {
                    xp(a.getPlayer(), e.getEntity().getLocation(), getConfig().damageXPMultiplier * e.getDamage());
                }
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
        double damageXPMultiplier = 11.44;
    }
}
