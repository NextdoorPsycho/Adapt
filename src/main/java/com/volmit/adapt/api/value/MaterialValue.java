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

package com.volmit.adapt.api.value;

import com.google.gson.Gson;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.util.Form;
import com.volmit.adapt.util.IO;
import com.volmit.adapt.util.JSONObject;
import com.volmit.adapt.util.PrecisionStopwatch;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
public class MaterialValue {
    private static MaterialValue valueCache = null;
    private final Map<Material, Double> value = new HashMap<>();
    private static final Map<Material, Double> valueMultipliers = new HashMap<>();


    public static void save() {
        if(valueCache == null) {
            return;
        }

        File l = Adapt.instance.getDataFile("data", "value-cache.json");
        try {
            IO.writeAll(l, new JSONObject(new Gson().toJson(valueCache)).toString(4));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static MaterialValue get() {
        if(valueCache == null) {
            MaterialValue dummy = new MaterialValue();
            File l = Adapt.instance.getDataFile("data", "value-cache.json");

            if(!l.exists()) {
                try {
                    IO.writeAll(l, new JSONObject(new Gson().toJson(dummy)).toString(4));
                } catch(IOException e) {
                    e.printStackTrace();
                    valueCache = dummy;
                    return dummy;
                }
            }

            try {
                valueCache = new Gson().fromJson(IO.readAll(l), MaterialValue.class);
            } catch(IOException e) {
                e.printStackTrace();
                valueCache = new MaterialValue();
            }
        }

        return valueCache;
    }

    public static void debugValue(Material m) {
        debugValue(m, 0, 1, new HashSet<>());
    }

    private static void debugValue(Material m, int ind, int x, Set<MaterialRecipe> ignore) {
        PrecisionStopwatch p = PrecisionStopwatch.start();
        Adapt.info(Form.repeat("  ", ind) + m.name() + ": " + getValue(m) + (x == 1 ? "" : " (x" + x + ")"));

        int r = 0;
        for(MaterialRecipe i : getRecipes(m)) {
            if(ignore.contains(i)) {
                continue;
            }

            ignore.add(i);

            int o = i.getOutput().getAmount();
            Adapt.info(Form.repeat("  ", ind) + "# Recipe [" + ind + "x" + r + (o == 1 ? "]" : "] (x" + o + ")"));

            for(MaterialCount j : i.getInput()) {
                debugValue(j.getMaterial(), ind + 1, j.getAmount(), ignore);
            }

            r++;
        }
        Adapt.info(Form.repeat("  ", ind) + " took " + Form.duration(p.getMilliseconds(), 0));
    }

    private static double getMultiplier(Material m) {
        Double d = AdaptConfig.get().getValue().getValueMutlipliers().get(m);

        return d == null ? 1 : d;
    }

    public static double getValue(Material m) {
        return getValue(m, new HashSet<>());
    }

    private static double getValue(Material m, Set<MaterialRecipe> ignore) {
        if(get().value.containsKey(m)) {
            return get().value.get(m);
        }

        double v = AdaptConfig.get().getValue().getBaseValue();

        List<MaterialRecipe> recipes = getRecipes(m);

        if(recipes.isEmpty()) {
            get().value.put(m, v * getMultiplier(m));
        } else {
            List<Double> d = new ArrayList<>();
            for(MaterialRecipe i : recipes) {
                if(ignore.contains(i)) {
                    continue;
                }

                ignore.add(i);

                double vx = v;

                for(MaterialCount j : i.getInput()) {
                    vx += getValue(j.getMaterial(), ignore);
                }

                d.add(vx / i.getOutput().getAmount());
            }

            if(d.size() > 0) {
                v += d.stream().mapToDouble(i -> i).average().getAsDouble();
            }

            get().value.put(m, v);
        }

        return get().value.get(m);
    }

    private static List<MaterialRecipe> getRecipes(Material mat) {
        List<MaterialRecipe> r = new ArrayList<>();

        try {
            ItemStack is = new ItemStack(mat);

            try {
                is.setDurability((short) -1);
            } catch(Throwable e) {

            }

            Bukkit.getRecipesFor(is).forEach(i -> {
                MaterialRecipe rx = toMaterial(i);

                if(rx != null) {
                    r.add(rx);
                }
            });
        } catch(Throwable e) {

        }

        return r;
    }

    private static MaterialRecipe toMaterial(Recipe r) {
        try {
            if(r instanceof ShapelessRecipe recipe) {
                return MaterialRecipe.builder()
                    .input(new ArrayList<>(recipe.getIngredientList().stream().map(i -> new MaterialCount(i.getType(), 1)).toList()))
                    .output(new MaterialCount(recipe.getResult().getType(), recipe.getResult().getAmount()))
                    .build();
            } else if(r instanceof ShapedRecipe recipe) {
                MaterialRecipe re = MaterialRecipe.builder()
                    .input(new ArrayList<>())
                    .output(new MaterialCount(recipe.getResult().getType(), recipe.getResult().getAmount()))
                    .build();
                Map<Material, Integer> f = new HashMap<>();
                for(ItemStack i : recipe.getIngredientMap().values()) {
                    if(i == null || i.getType() == null || i.getType().isAir()) {
                        continue;
                    }

                    f.compute(i.getType(), (k, v) -> v == null ? 1 : v + 1);
                }

                f.forEach((k, v) -> re.getInput().add(new MaterialCount(k, v)));

                return re;
            } else if(r instanceof CookingRecipe recipe) {
                List<MaterialCount> a = new ArrayList<>();
                a.add(new MaterialCount(recipe.getInput().getType(), 1));

                return MaterialRecipe.builder()
                    .input(a)
                    .output(new MaterialCount(recipe.getResult().getType(), recipe.getResult().getAmount()))
                    .build();
            } else if(r instanceof MerchantRecipe recipe) {
                return MaterialRecipe.builder()
                    .input(new ArrayList<>(recipe.getIngredients().stream().map(i -> new MaterialCount(i.getType(), 1)).toList()))
                    .output(new MaterialCount(recipe.getResult().getType(), recipe.getResult().getAmount()))
                    .build();
            } else if(r instanceof StonecuttingRecipe recipe) {
                List<MaterialCount> a = new ArrayList<>();
                a.add(new MaterialCount(recipe.getInput().getType(), 1));

                return MaterialRecipe.builder()
                    .input(a)
                    .output(new MaterialCount(recipe.getResult().getType(), recipe.getResult().getAmount()))
                    .build();
            }
        } catch(Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    static {
        AdaptConfig.get().getValue().getValueMutlipliers().forEach((k, v) -> {
            try {
                Material m = Material.valueOf(k.toUpperCase());

                if(m != null) {
                    valueMultipliers.put(m, v);
                }
            } catch(Throwable e) {

            }
        });
    }
}
