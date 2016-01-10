package mcjty.rftools.blocks.screens;

import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.network.PacketServerCommand;
import mcjty.rftools.blocks.screens.modules.ScreenModule;
import mcjty.rftools.blocks.screens.modulesclient.ClientScreenModule;
import mcjty.rftools.network.RFToolsMessages;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class ScreenTileEntity extends GenericTileEntity implements ITickable, DefaultSidedInventory {

    public static final String CMD_CLICK = "click";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, ScreenContainer.factory, ScreenContainer.SCREEN_MODULES);

    // This is a map that contains a map from the coordinate of the screen to a map of screen data from the server indexed by slot number,
    // @todo dimension in the map!!!
    public static Map<BlockPos, Map<Integer, Object[]>> screenData = new HashMap<BlockPos, Map<Integer, Object[]>>();

    // Cached client screen modules
    private List<ClientScreenModule> clientScreenModules = null;

    // A list of tags linked to computer modules.
//    private final Map<String,List<ComputerScreenModule>> computerModules = new HashMap<String, List<ComputerScreenModule>>();

    private boolean needsServerData = false;
    private boolean powerOn = false;        // True if screen is powered.
    private boolean connected = false;      // True if screen is connected to a controller.
    private int size = 0;                   // Size of screen (0 is normal, 1 is large, 2 is huge)
    private boolean transparent = false;    // Transparent screen.
    private int color = 0;                  // Color of the screen.

    public static final int SIZE_NORMAL = 0;
    public static final int SIZE_LARGE = 1;
    public static final int SIZE_HUGE = 2;

    // Cached server screen modules
    private List<ScreenModule> screenModules = null;
    private List<ActivatedModule> clickedModules = new ArrayList<ActivatedModule>();

    private static class ActivatedModule {
        int module;
        int ticks;
        int x;
        int y;

        public ActivatedModule(int module, int ticks, int x, int y) {
            this.module = module;
            this.ticks = ticks;
            this.x = x;
            this.y = y;
        }
    }

    private int totalRfPerTick = 0;     // The total rf per tick for all modules.

    public long lastTime = 0;

    public ScreenTileEntity() {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        int xCoord = getPos().getX();
        int yCoord = getPos().getY();
        int zCoord = getPos().getZ();
        return AxisAlignedBB.fromBounds(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + size + 1, yCoord + size + 1, zCoord + size + 1);
    }

    @Override
    public void update() {
        if (worldObj.isRemote) {
            checkStateClient();
        } else {
            checkStateServer();
        }
    }

    @Override
    protected void checkStateClient() {
        if (clickedModules.isEmpty()) {
            return;
        }
        List<ActivatedModule> newClickedModules = new ArrayList<ActivatedModule>();
        for (ActivatedModule cm : clickedModules) {
            cm.ticks--;
            if (cm.ticks > 0) {
                newClickedModules.add(cm);
            } else {
                List<ClientScreenModule> modules = getClientScreenModules();
                if (cm.module < modules.size()) {
                    modules.get(cm.module).mouseClick(worldObj, cm.x, cm.y, false);
                }
            }
        }
        clickedModules = newClickedModules;
    }

    protected void checkStateServer() {
        if (clickedModules.isEmpty()) {
            return;
        }
        List<ActivatedModule> newClickedModules = new ArrayList<ActivatedModule>();
        for (ActivatedModule cm : clickedModules) {
            cm.ticks--;
            if (cm.ticks > 0) {
                newClickedModules.add(cm);
            } else {
                List<ScreenModule> modules = getScreenModules();
                if (cm.module < modules.size()) {
                    modules.get(cm.module).mouseClick(worldObj, cm.x, cm.y, false);
                }
            }
        }
        clickedModules = newClickedModules;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return ScreenContainer.factory.getAccessibleSlots();
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return ScreenContainer.factory.isOutputSlot(index);
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return ScreenContainer.factory.isInputSlot(index);
    }

    @Override
    public int getSizeInventory() {
        return inventoryHelper.getCount();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventoryHelper.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        resetModules();
        return inventoryHelper.decrStackSize(index, amount);
    }

    private void resetModules() {
        clientScreenModules = null;
        screenModules = null;
        clickedModules.clear();
//        computerModules.clear();
    }

    public void hitScreenClient(double hitX, double hitY, double hitZ, EnumFacing side) {
        float factor = size+1.0f;
        float dx = 0;
        float dy = (float) ((-hitY + 1.0) / factor);
        switch (side) {
            case NORTH:
                dx = (float) ((1.0-hitX) / factor);
                break;
            case SOUTH:
                dx = (float) (hitX / factor);
                break;
            case WEST:
                dx = (float) (hitZ / factor);
                break;
            case EAST:
                dx = (float) ((1.0 - hitZ) / factor);
                break;
        }
        int x = (int) (dx * 128);
        int y = (int) (dy * 128);
        int currenty = 7;

        int moduleIndex = 0;
        List<ClientScreenModule> clientScreenModules = getClientScreenModules();
        for (ClientScreenModule module : clientScreenModules) {
            if (module != null) {
                int height = module.getHeight();
                // Check if this module has enough room
                if (currenty + height <= 124) {
                    if (currenty <= y && y < (currenty + height)) {
                        break;
                    }
                    currenty += height;
                }
            }
            moduleIndex++;
        }
        if (moduleIndex >= clientScreenModules.size()) {
            return;
        }

        clientScreenModules.get(moduleIndex).mouseClick(worldObj, x, y - currenty, true);
        clickedModules.add(new ActivatedModule(moduleIndex, 5, x, y));

        RFToolsMessages.INSTANCE.sendToServer(new PacketServerCommand(getPos(), CMD_CLICK,
                new Argument("x", x),
                new Argument("y", y - currenty),
                new Argument("module", moduleIndex)));
    }

    private void hitScreenServer(int x, int y, int module) {
        List<ScreenModule> screenModules = getScreenModules();
        ScreenModule screenModule = screenModules.get(module);
        if (screenModule != null) {
            screenModule.mouseClick(worldObj, x, y, true);
            clickedModules.add(new ActivatedModule(module, 5, x, y));
        }
    }

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventoryHelper.setInventorySlotContents(getInventoryStackLimit(), index, stack);
        resetModules();
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        powerOn = tagCompound.getBoolean("powerOn");
        connected = tagCompound.getBoolean("connected");
        totalRfPerTick = tagCompound.getInteger("rfPerTick");
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
        resetModules();
        if (tagCompound.hasKey("large")) {
            size = tagCompound.getBoolean("large") ? 1 : 0;
        } else {
            size = tagCompound.getInteger("size");
        }
        transparent = tagCompound.getBoolean("transparent");
        color = tagCompound.getInteger("color");
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("powerOn", powerOn);
        tagCompound.setBoolean("connected", connected);
        tagCompound.setInteger("rfPerTick", totalRfPerTick);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
        tagCompound.setInteger("size", size);
        tagCompound.setBoolean("transparent", transparent);
        tagCompound.setInteger("color", color);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        markDirtyClient();
    }

    public void setSize(int size) {
        this.size = size;
        markDirtyClient();
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
        markDirtyClient();
    }

    public int getSize() {
        return size;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setPower(boolean power) {
        if (powerOn == power) {
            return;
        }
        powerOn = power;
        markDirtyClient();
    }

    public boolean isPowerOn() {
        return powerOn;
    }

    public void setConnected(boolean c) {
        if (connected == c) {
            return;
        }
        connected = c;
        markDirtyClient();
    }

    public boolean isConnected() {
        return connected;
    }

    public void updateModuleData(int slot, NBTTagCompound tagCompound) {
        ItemStack stack = inventoryHelper.getStackInSlot(slot);
        stack.setTagCompound(tagCompound);
        screenModules = null;
        clientScreenModules = null;
//        computerModules.clear();
        markDirty();
    }

    // This is called client side.
    public List<ClientScreenModule> getClientScreenModules() {
        if (clientScreenModules == null) {
            needsServerData = false;
            clientScreenModules = new ArrayList<ClientScreenModule>();
            for (int i = 0 ; i < inventoryHelper.getCount() ; i++) {
                ItemStack itemStack = inventoryHelper.getStackInSlot(i);
                if (itemStack != null && itemStack.getItem() instanceof ModuleProvider) {
                    ModuleProvider moduleProvider = (ModuleProvider) itemStack.getItem();
                    ClientScreenModule clientScreenModule;
                    try {
                        clientScreenModule = moduleProvider.getClientScreenModule().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                        continue;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    clientScreenModule.setupFromNBT(itemStack.getTagCompound(), worldObj.provider.getDimensionId(), getPos().getX(), getPos().getY(), getPos().getZ());
                    clientScreenModules.add(clientScreenModule);
                    if (clientScreenModule.needsServerData()) {
                        needsServerData = true;
                    }
                } else {
                    clientScreenModules.add(null);        // To keep the indexing correct so that the modules correspond with there slot number.
                }
            }

        }
        return clientScreenModules;
    }

    public boolean isNeedsServerData() {
        return needsServerData;
    }

    public int getTotalRfPerTick() {
        if (screenModules == null) {
            getScreenModules();
        }
        return totalRfPerTick;
    }

    // This is called server side.
    public List<ScreenModule> getScreenModules() {
        if (screenModules == null) {
            totalRfPerTick = 0;
            screenModules = new ArrayList<ScreenModule>();
            for (int i = 0 ; i < inventoryHelper.getCount() ; i++) {
                ItemStack itemStack = inventoryHelper.getStackInSlot(i);
                if (itemStack != null && itemStack.getItem() instanceof ModuleProvider) {
                    ModuleProvider moduleProvider = (ModuleProvider) itemStack.getItem();
                    ScreenModule screenModule;
                    try {
                        screenModule = moduleProvider.getServerScreenModule().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                        continue;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    screenModule.setupFromNBT(itemStack.getTagCompound(), worldObj.provider.getDimensionId(), getPos().getX(), getPos().getY(), getPos().getZ());
                    screenModules.add(screenModule);
                    totalRfPerTick += screenModule.getRfPerTick();

//                    if (screenModule instanceof ComputerScreenModule) {
//                        ComputerScreenModule computerScreenModule = (ComputerScreenModule) screenModule;
//                        String tag = computerScreenModule.getTag();
//                        if (!computerModules.containsKey(tag)) {
//                            computerModules.put(tag, new ArrayList<ComputerScreenModule>());
//                        }
//                        computerModules.get(tag).add(computerScreenModule);
//                    }
                } else {
                    screenModules.add(null);        // To keep the indexing correct so that the modules correspond with there slot number.
                }
            }

        }
        return screenModules;
    }

//    public List<ComputerScreenModule> getComputerModules(String tag) {
//        return computerModules.get(tag);
//    }
//
//    public Set<String> getTags() {
//        return computerModules.keySet();
//    }

    // This is called server side.
    public Map<Integer, Object[]> getScreenData(long millis) {
        Map<Integer, Object[]> map = new HashMap<Integer, Object[]>();
        List<ScreenModule> screenModules = getScreenModules();
        int moduleIndex = 0;
        for (ScreenModule module : screenModules) {
            if (module != null) {
                Object[] data = module.getData(worldObj, millis);
                if (data != null) {
                    map.put(moduleIndex, data);
                }
            }
            moduleIndex++;
        }
        return map;
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_CLICK.equals(command)) {
            int x = args.get("x").getInteger();
            int y = args.get("y").getInteger();
            int module = args.get("module").getInteger();
            hitScreenServer(x, y, module);
            return true;
        }
        return false;
    }
}