package WayofTime.alchemicalWizardry.common.summoning.meteor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import WayofTime.alchemicalWizardry.AlchemicalWizardry;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.common.blocks.TileEntityOres;

public class MeteorParadigm {

    public List<MeteorParadigmComponent> oreList = new ArrayList<>();
    public List<MeteorParadigmComponent> fillerList = new ArrayList<>();
    public ItemStack focusStack;
    public int radius;
    public int cost;
    public int fillerChance; //Out of 100

    public static Random rand = new Random();

    public MeteorParadigm(ItemStack focusStack, int radius, int cost) {
        new MeteorParadigm(focusStack, radius, cost ,0);
    }

    public MeteorParadigm(ItemStack focusStack, int radius, int cost, int fillerWeight) {
        this.focusStack = focusStack;
        this.radius = radius;
        this.cost = cost;
        this.fillerChance = fillerWeight;
    }

    // modId:itemName:meta:weight
    private static final Pattern itemNamePattern = Pattern.compile("(.*):(.*):(\\d+):(\\d+)");
    // OREDICT:oreDictName:weight
    private static final Pattern oredictPattern = Pattern.compile("OREDICT:(.*):(\\d+)");

    public void parseStringArray(String[] oreArray) {
        parseStringArray(oreArray, false);
    }

    public void parseStringArray(String[] oreArray, boolean filler) {
        List<MeteorParadigmComponent> addList = filler ? fillerList : oreList;
        for (int i = 0; i < oreArray.length; ++i) {
            String oreName = oreArray[i];
            boolean success = false;

            Matcher matcher = itemNamePattern.matcher(oreName);
            if (matcher.matches()) {
                String modID = matcher.group(1);
                String itemName = matcher.group(2);
                int meta = Integer.parseInt(matcher.group(3));
                int weight = Integer.parseInt(matcher.group(4));

                ItemStack stack = GameRegistry.findItemStack(modID, itemName, 1);
                if (stack != null && stack.getItem() instanceof ItemBlock) {
                    stack.setItemDamage(meta);
                    addList.add(new MeteorParadigmComponent(stack, weight));
                    success = true;
                }

            } else if ((matcher = oredictPattern.matcher(oreName)).matches()) {
                String oreDict = matcher.group(1);
                int weight = Integer.parseInt(matcher.group(2));

                List<ItemStack> list = OreDictionary.getOres(oreDict);
                for (ItemStack stack : list) {
                    if (stack != null && stack.getItem() instanceof ItemBlock) {
                        addList.add(new MeteorParadigmComponent(stack, weight));
                        success = true;
                        break;
                    }
                }

            } else {
                // Legacy config
                String oreDict = oreName;
                int weight = Integer.parseInt(oreArray[++i]);

                List<ItemStack> list = OreDictionary.getOres(oreDict);
                for (ItemStack stack : list) {
                    if (stack != null && stack.getItem() instanceof ItemBlock) {
                        addList.add(new MeteorParadigmComponent(stack, weight));
                        success = true;
                        break;
                    }
                }
            }

            if (!success) {
                AlchemicalWizardry.logger.warn("Unable to add Meteor Paradigm \"" + oreName + "\"");
            }
        }
    }

    public int getTotalOreWeight() {
        int totalWeight = 0;
        for (MeteorParadigmComponent mpc : oreList) {
            totalWeight += mpc.getChance();
        }
        return totalWeight;
    }

    public int getTotalFillerWeight() {
        int totalWeight = 0;
        for (MeteorParadigmComponent mpc : fillerList) {
            totalWeight += mpc.getChance();
        }
        return totalWeight;
    }

    public void createMeteorImpact(World world, int x, int y, int z, boolean[] flags) {
        boolean hasTerrae = false;
        boolean hasOrbisTerrae = false;
        boolean hasCrystallos = false;
        boolean hasIncendium = false;
        boolean hasTennebrae = false;

        if (flags != null && flags.length >= 5) {
            hasTerrae = flags[0];
            hasOrbisTerrae = flags[1];
            hasCrystallos = flags[2];
            hasIncendium = flags[3];
            hasTennebrae = flags[4];
        }

        int newRadius = radius;

        if (hasOrbisTerrae) {
            newRadius += 2;
        } else if (hasTerrae) {
            newRadius += 1;
        }

        world.createExplosion(null, x, y, z, newRadius * 4, AlchemicalWizardry.doMeteorsDestroyBlocks);

        float iceChance = hasCrystallos ? 1 : 0;
        float soulChance = hasIncendium ? 1 : 0;
        float obsidChance = hasTennebrae ? 1 : 0;

        float totalChance = iceChance + soulChance + obsidChance;

        int totalOreWeight = getTotalOreWeight();
        int totalFillerWeight = getTotalFillerWeight();

        for (int i = -newRadius; i <= newRadius; i++) {
            for (int j = -newRadius; j <= newRadius; j++) {
                for (int k = -newRadius; k <= newRadius; k++) {
                    if (i * i + j * j + k * k >= (newRadius + 0.50f) * (newRadius + 0.50f)) {
                        continue;
                    }

                    if (!world.isAirBlock(x + i, y + j, z + k)) {
                        continue;
                    }

                    if (world.rand.nextInt(100) >= fillerChance) {
                        setMeteorBlock(x + i, y + j, z + k, world, oreList, totalOreWeight);
                    } else {
                        float randChance = rand.nextFloat() * totalChance;

                        if (randChance < iceChance) {
                            world.setBlock(x + i, y + j, z + k, Blocks.ice, 0, 3);
                        } else {
                            randChance -= iceChance;

                            if (randChance < soulChance) {
                                switch (rand.nextInt(3)) {
                                    case 0:
                                        world.setBlock(x + i, y + j, z + k, Blocks.soul_sand, 0, 3);
                                        break;
                                    case 1:
                                        world.setBlock(x + i, y + j, z + k, Blocks.glowstone, 0, 3);
                                        break;
                                    case 2:
                                        world.setBlock(x + i, y + j, z + k, Blocks.netherrack, 0, 3);
                                        break;
                                }
                            } else {
                                randChance -= soulChance;

                                if (randChance < obsidChance) {
                                    world.setBlock(x + i, y + j, z + k, Blocks.obsidian, 0, 3);
                                } else {
                                    if (!this.fillerList.isEmpty()) {
                                        setMeteorBlock(x + i, y + j, z + k, world, fillerList, totalFillerWeight);
                                    } else {
                                        world.setBlock(x + i, y + j, z + k, Blocks.stone, 0, 3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setMeteorBlock(int x, int y, int z, World world, List<MeteorParadigmComponent> blockList, int totalListWeight) {
        int randNum = world.rand.nextInt(totalListWeight);
        for (MeteorParadigmComponent mpc : blockList) {
            randNum -= mpc.getChance();

            if (randNum < 0) {
                ItemStack blockStack = mpc.getValidBlockParadigm();
                if (blockStack != null && blockStack.getItem() instanceof ItemBlock) {
                    ((ItemBlock) blockStack.getItem()).placeBlockAt(
                            blockStack,
                            null,
                            world,
                            x,
                            y,
                            z,
                            0,
                            0,
                            0,
                            0,
                            blockStack.getItemDamage());
                    if (AlchemicalWizardry.isGregTechLoaded)
                        setGTOresNaturalIfNeeded(world, x, y, z);
                    world.markBlockForUpdate(x, y, z);
                    break;
                }
            }
        }
    }

    @Optional.Method(modid = "gregtech")
    private static void setGTOresNaturalIfNeeded(World world, int x, int y, int z) {
        final TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityOres) {
            ((TileEntityOres) tileEntity).mNatural = true;
        }
    }
}
