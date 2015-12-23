package mcjty.rftools.blocks.storage;

import mcjty.lib.api.Infusable;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.GenericRFToolsBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class RemoteStorageBlock extends GenericRFToolsBlock implements Infusable {

    public RemoteStorageBlock() {
        super(Material.iron, RemoteStorageTileEntity.class, RemoteStorageContainer.class, GuiRemoteStorage.class, "remote_storage", true);
    }

    @Override
    public int getGuiID() {
        return RFTools.GUI_REMOTE_STORAGE;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            list.add(EnumChatFormatting.WHITE + "Make storage modules remotely available.");
            list.add(EnumChatFormatting.WHITE + "Requires energy to do this.");
            list.add(EnumChatFormatting.YELLOW + "Infusing bonus: reduced power consumption");
        } else {
            list.add(EnumChatFormatting.WHITE + RFTools.SHIFT_MESSAGE);
        }
    }
}