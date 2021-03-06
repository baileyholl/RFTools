package mcjty.rftools.blocks.storagemonitor;

import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.WorldTools;
import mcjty.rftools.ClientCommandHandler;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StorageScannerTools {

    public static void scannerSearch(EntityPlayer player, Integer dim, BlockPos pos, String text) {
        World world = DimensionManager.getWorld(dim);
        if (WorldTools.chunkLoaded(world, pos)) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof StorageScannerTileEntity) {
                StorageScannerTileEntity scannerTileEntity = (StorageScannerTileEntity) te;
                Set<BlockPos> inventories = scannerTileEntity.performSearch(text);
                RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_SCANNER_SEARCH,
                        TypedMap.builder()
                                .put(ClientCommandHandler.PARAM_INVENTORIES, new ArrayList<>(inventories)));
            }
        }
    }

    public static void requestContents(EntityPlayer player, Integer dim, BlockPos pos, BlockPos invpos) {
        World world = DimensionManager.getWorld(dim);
        if (WorldTools.chunkLoaded(world, pos)) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof StorageScannerTileEntity) {
                StorageScannerTileEntity scannerTileEntity = (StorageScannerTileEntity) te;
                List<ItemStack> inv = scannerTileEntity.getInventoryForBlock(invpos);
                RFToolsMessages.sendToClient(player, ClientCommandHandler.CMD_RETURN_SCANNER_CONTENTS,
                        TypedMap.builder()
                                .put(ClientCommandHandler.PARAM_STACKS, inv)
                );
            }
        }
    }
}
