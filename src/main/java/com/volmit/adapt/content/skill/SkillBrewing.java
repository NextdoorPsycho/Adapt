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

import art.arcane.spatial.matter.SpatialMatter;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.data.WorldData;
import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.adaptation.brewing.BrewingLingering;
import com.volmit.adapt.content.adaptation.brewing.BrewingSuperHeated;
import com.volmit.adapt.content.matter.BrewingStandOwner;
import com.volmit.adapt.content.matter.BrewingStandOwnerMatter;
import com.volmit.adapt.util.C;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.meta.PotionMeta;

public class SkillBrewing extends SimpleSkill<SkillBrewing.Config> {
    public SkillBrewing() {
        super("brewing", Adapt.dLocalize("Skill", "Brewing", "Icon"));
        registerConfiguration(Config.class);
        setColor(C.LIGHT_PURPLE);
        setDescription(Adapt.dLocalize("Skill", "Brewing", "Description"));
        setDisplayName(Adapt.dLocalize("Skill", "Brewing", "Name"));
        setInterval(5251);
        setIcon(Material.LINGERING_POTION);
        registerAdaptation(new BrewingLingering());
        registerAdaptation(new BrewingSuperHeated());
        SpatialMatter.registerSliceType(new BrewingStandOwnerMatter());
    }

    @EventHandler
    public void on(PlayerItemConsumeEvent e) {
        if (e.getItem().getItemMeta() instanceof PotionMeta o) {
            xp(e.getPlayer(), e.getPlayer().getLocation(),
                    getConfig().splashXP
                            + (getConfig().splashMultiplier * o.getCustomEffects().stream().mapToDouble(i -> (i.getAmplifier() + 1) * (i.getDuration() / 20D)).sum())
                            + (getConfig().splashMultiplier * (o.getBasePotionData().isUpgraded() ? 50 : 25)));
        }
    }

    @EventHandler
    public void on(PotionSplashEvent e) {
        if (e.getPotion().getShooter() instanceof Player p) {
            AdaptPlayer a = getPlayer(p);
            getPlayer(p).getData().addStat("brewing.splashes", 1);
            xp(a.getPlayer(), e.getEntity().getLocation(), getConfig().splashXP + (getConfig().splashMultiplier * e.getPotion().getEffects().stream().mapToDouble(i -> (i.getAmplifier() + 1) * (i.getDuration() / 20D)).sum()));
        }
    }

    @EventHandler
    public void on(BlockPlaceEvent e) {
        if (e.getBlock().getType().equals(Material.BREWING_STAND)) {
            if (!e.isCancelled()) {
                WorldData.of(e.getBlock().getWorld()).getMantle().set(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), new BrewingStandOwner(e.getPlayer().getUniqueId()));
            }
        }
    }

    @EventHandler
    public void on(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            if (e.getBlock().getType().equals(Material.BREWING_STAND)) {
                WorldData.of(e.getBlock().getWorld()).getMantle().remove(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), BrewingStandOwner.class);
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
        double splashXP = 115;
        double splashMultiplier = 0.25;
    }
}
