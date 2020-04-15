package net.earthcomputer.multiconnect.protocols.v1_12_2;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.Dynamic;
import io.netty.buffer.Unpooled;
import net.earthcomputer.multiconnect.impl.*;
import net.earthcomputer.multiconnect.protocols.ProtocolRegistry;
import net.earthcomputer.multiconnect.protocols.v1_12_2.mixin.*;
import net.earthcomputer.multiconnect.protocols.v1_13.Protocol_1_13;
import net.earthcomputer.multiconnect.protocols.v1_13_2.Protocol_1_13_2;
import net.earthcomputer.multiconnect.protocols.v1_13_2.mixin.ZombieEntityAccessor;
import net.earthcomputer.multiconnect.transformer.*;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.PaintingType;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.particles.*;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.StatType;
import net.minecraft.tags.*;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import net.minecraft.util.datafix.fixes.EntityRenaming1510;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.storage.MapDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Protocol_1_12_2 extends Protocol_1_13 {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, PaintingType> OLD_MOTIVE_NAMES = new HashMap<>();
    static {
        OLD_MOTIVE_NAMES.put("Kebab", PaintingType.KEBAB);
        OLD_MOTIVE_NAMES.put("Aztec", PaintingType.AZTEC);
        OLD_MOTIVE_NAMES.put("Alban", PaintingType.ALBAN);
        OLD_MOTIVE_NAMES.put("Aztec2", PaintingType.AZTEC2);
        OLD_MOTIVE_NAMES.put("Bomb", PaintingType.BOMB);
        OLD_MOTIVE_NAMES.put("Plant", PaintingType.PLANT);
        OLD_MOTIVE_NAMES.put("Wasteland", PaintingType.WASTELAND);
        OLD_MOTIVE_NAMES.put("Pool", PaintingType.POOL);
        OLD_MOTIVE_NAMES.put("Courbet", PaintingType.COURBET);
        OLD_MOTIVE_NAMES.put("Sea", PaintingType.SEA);
        OLD_MOTIVE_NAMES.put("Sunset", PaintingType.SUNSET);
        OLD_MOTIVE_NAMES.put("Creebet", PaintingType.CREEBET);
        OLD_MOTIVE_NAMES.put("Wanderer", PaintingType.WANDERER);
        OLD_MOTIVE_NAMES.put("Graham", PaintingType.GRAHAM);
        OLD_MOTIVE_NAMES.put("Match", PaintingType.MATCH);
        OLD_MOTIVE_NAMES.put("Bust", PaintingType.BUST);
        OLD_MOTIVE_NAMES.put("Stage", PaintingType.STAGE);
        OLD_MOTIVE_NAMES.put("Void", PaintingType.VOID);
        OLD_MOTIVE_NAMES.put("SkullAndRoses", PaintingType.SKULL_AND_ROSES);
        OLD_MOTIVE_NAMES.put("Wither", PaintingType.WITHER);
        OLD_MOTIVE_NAMES.put("Fighters", PaintingType.FIGHTERS);
        OLD_MOTIVE_NAMES.put("Pointer", PaintingType.POINTER);
        OLD_MOTIVE_NAMES.put("Pigscene", PaintingType.PIGSCENE);
        OLD_MOTIVE_NAMES.put("BurningSkull", PaintingType.BURNING_SKULL);
        OLD_MOTIVE_NAMES.put("Skeleton", PaintingType.SKELETON);
        OLD_MOTIVE_NAMES.put("DonkeyKong", PaintingType.DONKEY_KONG);
    }

    private static final Pattern NON_IDENTIFIER_CHARS = Pattern.compile("[^a-z0-9/._\\-]");

    private static final DataParameter<Integer> OLD_AREA_EFFECT_CLOUD_PARTICLE_ID = DataTrackerManager.createOldDataParameter(DataSerializers.VARINT);
    private static final DataParameter<Integer> OLD_AREA_EFFECT_CLOUD_PARTICLE_PARAM1 = DataTrackerManager.createOldDataParameter(DataSerializers.VARINT);
    private static final DataParameter<Integer> OLD_AREA_EFFECT_CLOUD_PARTICLE_PARAM2 = DataTrackerManager.createOldDataParameter(DataSerializers.VARINT);
    private static final DataParameter<String> OLD_CUSTOM_NAME = DataTrackerManager.createOldDataParameter(DataSerializers.STRING);
    private static final DataParameter<Integer> OLD_MINECART_DISPLAY_TILE = DataTrackerManager.createOldDataParameter(DataSerializers.VARINT);
    private static final DataParameter<Integer> OLD_WOLF_COLLAR_COLOR = DataTrackerManager.createOldDataParameter(DataSerializers.VARINT);

    public static void registerTranslators() {
        ProtocolRegistry.registerInboundTranslator(ChunkData.class, buf -> {
            int verticalStripBitmask = CurrentChunkDataPacket.get().getAvailableSections();
            buf.enablePassthroughMode();
            for (int sectionY = 0; sectionY < 16; sectionY++) {
                if ((verticalStripBitmask & (1 << sectionY)) != 0) {
                    int paletteSize = buf.readByte();
                    if (paletteSize <= 8) {
                        // array and bimap palette data look the same enough to use the same code here
                        int size = buf.readVarInt();
                        for (int i = 0; i < size; i++)
                            buf.readVarInt(); // state id
                    } else {
                        buf.disablePassthroughMode();
                        buf.readVarInt(); // dummy 0
                        buf.enablePassthroughMode();
                    }
                    buf.readLongArray(new long[paletteSize * 64]); // chunk data
                    buf.readBytes(new byte[16 * 16 * 16 / 2]); // block light
                    assert Minecraft.getInstance().world != null;
                    if (Minecraft.getInstance().world.dimension.hasSkyLight())
                        buf.readBytes(new byte[16 * 16 * 16 / 2]); // sky light
                }
            }
            buf.disablePassthroughMode();
            if (CurrentChunkDataPacket.get().isFullChunk()) {
                for (int i = 0; i < 256; i++) {
                    buf.pendingRead(Integer.class, buf.readByte() & 0xff);
                }
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SCustomPayloadPlayPacket.class, buf -> {
            String channel = buf.readString();
            ResourceLocation newChannel;
            if ("MC|Brand".equals(channel)) {
                newChannel = SCustomPayloadPlayPacket.BRAND;
            } else if ("MC|TrList".equals(channel)) {
                newChannel = Protocol_1_13_2.CUSTOM_PAYLOAD_TRADE_LIST;
            } else if ("MC|BOpen".equals(channel)) {
                newChannel = Protocol_1_13_2.CUSTOM_PAYLOAD_OPEN_BOOK;
            } else {
                newChannel = new ResourceLocation(NON_IDENTIFIER_CHARS.matcher(channel.toLowerCase(Locale.ENGLISH)).replaceAll("_"));
            }
            buf.pendingRead(ResourceLocation.class, newChannel);
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SPlaySoundPacket.class, buf -> {
            buf.pendingRead(ResourceLocation.class, new ResourceLocation(buf.readString(256)));
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SMapDataPacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readVarInt(); // map id
            buf.readByte(); // map scale
            buf.readBoolean(); // show icons
            int iconCount = buf.readVarInt();
            buf.disablePassthroughMode();
            for (int i = 0; i < iconCount; i++) {
                int metadata = buf.readByte();
                buf.pendingRead(MapDecoration.Type.class, MapDecoration.Type.byIcon((byte) ((metadata >> 4) & 15)));
                buf.enablePassthroughMode();
                buf.readByte(); // icon x
                buf.readByte(); // icon y
                buf.disablePassthroughMode();
                buf.pendingRead(Byte.class, (byte) (metadata & 15)); // rotation
                buf.pendingRead(Boolean.class, false); // has text
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SSpawnParticlePacket.class, buf -> {
            buf.enablePassthroughMode();
            ParticleType<?> particleType = Registry.PARTICLE_TYPE.getByValue(buf.readInt());
            if (particleType != ParticleTypes.ITEM && particleType != ParticleTypes.DUST) {
                buf.disablePassthroughMode();
                buf.applyPendingReads();
                return;
            }
            buf.readBoolean(); // long distance
            buf.readFloat(); // x
            buf.readFloat(); // y
            buf.readFloat(); // z
            float red = 0, green = 0, blue = 0;
            if (particleType == ParticleTypes.DUST) {
                buf.disablePassthroughMode();
                red = buf.readFloat();
                green = buf.readFloat();
                blue = buf.readFloat();
                buf.pendingRead(Float.class, 0f); // offset x
                buf.pendingRead(Float.class, 0f); // offset y
                buf.pendingRead(Float.class, 0f); // offset z
                buf.enablePassthroughMode();
            } else {
                buf.readFloat(); // offset x
                buf.readFloat(); // offset y
                buf.readFloat(); // offset z
            }
            buf.readFloat(); // speed
            buf.readInt(); // count
            buf.disablePassthroughMode();
            if (particleType == ParticleTypes.ITEM) {
                Item item = Registry.ITEM.getByValue(buf.readVarInt());
                int meta = buf.readVarInt();
                ItemStack stack = Items_1_12_2.oldItemStackToNew(new ItemStack(item), meta);
                buf.pendingRead(ItemStack.class, stack);
            } else {
                buf.pendingRead(Float.class, red);
                buf.pendingRead(Float.class, green);
                buf.pendingRead(Float.class, blue);
                buf.pendingRead(Float.class, 1f); // scale
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SPlaceGhostRecipePacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readByte(); // sync id
            buf.disablePassthroughMode();
            buf.pendingRead(ResourceLocation.class, new ResourceLocation(String.valueOf(buf.readVarInt())));
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SRecipeBookPacket.class, buf -> {
            buf.enablePassthroughMode();
            SRecipeBookPacket.State action = buf.readEnumValue(SRecipeBookPacket.State.class);
            buf.readBoolean(); // gui open
            buf.readBoolean(); // filtering craftable
            buf.pendingRead(Boolean.class, false); // furnace gui open
            buf.pendingRead(Boolean.class, false); // furnace filtering craftable
            int idChangeCount = buf.readVarInt();
            buf.disablePassthroughMode();
            for (int i = 0; i < idChangeCount; i++) {
                buf.pendingRead(ResourceLocation.class, new ResourceLocation(String.valueOf(buf.readVarInt())));
            }
            if (action == SRecipeBookPacket.State.INIT) {
                buf.enablePassthroughMode();
                int idInitCount = buf.readVarInt();
                buf.disablePassthroughMode();
                for (int i = 0; i < idInitCount; i++) {
                    buf.pendingRead(ResourceLocation.class, new ResourceLocation(String.valueOf(buf.readVarInt())));
                }
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SScoreboardObjectivePacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readString(16); // name
            int mode = buf.readByte();
            buf.disablePassthroughMode();
            if (mode == 0 || mode == 2) {
                buf.pendingRead(ITextComponent.class, new StringTextComponent(buf.readString(32))); // display name
                String renderTypeName = buf.readString(16);
                ScoreCriteria.RenderType renderType = "hearts".equals(renderTypeName) ? ScoreCriteria.RenderType.HEARTS : ScoreCriteria.RenderType.INTEGER;
                buf.pendingRead(ScoreCriteria.RenderType.class, renderType);
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SSpawnPaintingPacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readVarInt(); // id
            buf.readUniqueId(); // uuid
            buf.disablePassthroughMode();
            PaintingType motive = OLD_MOTIVE_NAMES.getOrDefault(buf.readString(13), PaintingType.KEBAB);
            buf.pendingRead(VarInt.class, new VarInt(Registry.MOTIVE.getId(motive)));
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SStatisticsPacket.class, buf -> {
            int count = buf.readVarInt();
            List<Pair<StatType<?>, Integer>> stats = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ResourceLocation stat = new ResourceLocation(buf.readString(32767));
                int value = buf.readVarInt();
                if (Registry.STATS.containsKey(stat))
                    stats.add(Pair.of(Registry.STATS.getOrDefault(stat), value));
            }
            buf.pendingRead(VarInt.class, new VarInt(stats.size()));
            for (Pair<StatType<?>, Integer> stat : stats) {
                buf.pendingRead(VarInt.class, new VarInt(Registry.STATS.getId(stat.getLeft())));
                buf.pendingRead(VarInt.class, new VarInt(stat.getRight()));
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(STabCompletePacket.class, buf -> {
            TabCompletionManager.Entry entry = TabCompletionManager.nextEntry();
            if (entry == null) {
                LOGGER.error("Received unrequested tab completion packet");
                int count = buf.readVarInt();
                for (int i = 0; i < count; i++)
                    buf.readString(32767);
                buf.pendingRead(VarInt.class, new VarInt(0)); // completion id
                buf.pendingRead(VarInt.class, new VarInt(0)); // range start
                buf.pendingRead(VarInt.class, new VarInt(0)); // range length
                buf.pendingRead(VarInt.class, new VarInt(0)); // suggestion count
                buf.applyPendingReads();
                return;
            }

            buf.pendingRead(VarInt.class, new VarInt(entry.getId())); // completion id
            String message = entry.getMessage();
            int start = message.lastIndexOf(' ') + 1;
            if (start == 0 && message.startsWith("/"))
                start = 1;
            buf.pendingRead(VarInt.class, new VarInt(start)); // range start
            buf.pendingRead(VarInt.class, new VarInt(message.length() - start)); // range length
            buf.enablePassthroughMode();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                buf.readString(32767); // suggestion
                buf.pendingRead(Boolean.class, false); // has tooltip
            }
            buf.disablePassthroughMode();
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(STeamsPacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readString(16); // team name
            int mode = buf.readByte();
            buf.disablePassthroughMode();
            if (mode == 0 || mode == 2) {
                buf.pendingRead(ITextComponent.class, new StringTextComponent(buf.readString(32))); // display name
                StringTextComponent prefix = new StringTextComponent(buf.readString(16));
                StringTextComponent suffix = new StringTextComponent(buf.readString(16));
                buf.enablePassthroughMode();
                buf.readByte(); // flags
                buf.readString(32); // name tag visibility rule
                buf.readString(32); // collision rule
                buf.disablePassthroughMode();
                TextFormatting color = TextFormatting.fromColorIndex(buf.readByte());
                buf.pendingRead(TextFormatting.class, color);
                buf.pendingRead(ITextComponent.class, prefix);
                buf.pendingRead(ITextComponent.class, suffix);
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SUpdateBossInfoPacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readUniqueId(); // uuid
            SUpdateBossInfoPacket.Operation type = buf.readEnumValue(SUpdateBossInfoPacket.Operation.class);
            buf.disablePassthroughMode();
            if (type == SUpdateBossInfoPacket.Operation.UPDATE_PROPERTIES) {
                int flags = buf.readUnsignedByte();
                buf.pendingRead(UnsignedByte.class, new UnsignedByte((short) (flags | ((flags & 2) << 1)))); // copy bit 2 to 4
            }
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(SSpawnObjectPacket.class, buf -> {
            buf.enablePassthroughMode();
            buf.readVarInt(); // entity id
            buf.readUniqueId(); // uuid
            int type = buf.readByte();
            if (type != 70) { // falling block
                buf.disablePassthroughMode();
                buf.applyPendingReads();
                return;
            }
            buf.readDouble(); // x
            buf.readDouble(); // y
            buf.readDouble(); // z
            buf.readByte(); // pitch
            buf.readByte(); // yaw
            buf.disablePassthroughMode();
            buf.pendingRead(Integer.class, Blocks_1_12_2.convertToStateRegistryId(buf.readInt()));
            buf.applyPendingReads();
        });

        ProtocolRegistry.registerInboundTranslator(ItemStack.class, new InboundTranslator<ItemStack>() {
            @Override
            public void onRead(TransformerByteBuf buf) {
                short itemId = buf.readShort();
                if (itemId == -1) {
                    buf.pendingRead(Short.class, itemId);
                    buf.applyPendingReads();
                    return;
                }
                byte count = buf.readByte();
                short meta = buf.readShort();
                CompoundNBT tag = buf.readCompoundTag();
                if (tag == null)
                    tag = new CompoundNBT();
                tag.putShort("Damage", meta);
                buf.pendingRead(Short.class, itemId);
                buf.pendingRead(Byte.class, count);
                buf.pendingRead(CompoundNBT.class, tag);
                buf.applyPendingReads();
            }

            @Override
            public ItemStack translate(ItemStack from) {
                if (from.isEmpty())
                    return from;
                from = from.copy();
                int meta = from.getDamage();
                assert from.getTag() != null;
                from.getTag().remove("Damage");
                if (from.getTag().isEmpty())
                    from.setTag(null);
                return Items_1_12_2.oldItemStackToNew(from, meta);
            }
        });

        ProtocolRegistry.registerOutboundTranslator(CPlaceRecipePacket.class, buf -> {
            buf.passthroughWrite(Byte.class); // sync id
            Supplier<ResourceLocation> recipeId = buf.skipWrite(ResourceLocation.class);
            buf.pendingWrite(VarInt.class, () -> {
                try {
                    return new VarInt(Integer.parseInt(recipeId.get().getPath()));
                } catch (NumberFormatException e) {
                    return new VarInt(0);
                }
            }, val -> buf.writeVarInt(val.get()));
        });

        ProtocolRegistry.registerOutboundTranslator(CRecipeInfoPacket.class, buf -> {
            Supplier<CRecipeInfoPacket.Purpose> mode = buf.passthroughWrite(CRecipeInfoPacket.Purpose.class);
            buf.whenWrite(() -> {
                if (mode.get() == CRecipeInfoPacket.Purpose.SHOWN) {
                    Supplier<ResourceLocation> recipeId = buf.skipWrite(ResourceLocation.class);
                    buf.pendingWrite(Integer.class, () -> {
                        try {
                            return Integer.parseInt(recipeId.get().getPath());
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }, buf::writeInt);
                } else if (mode.get() == CRecipeInfoPacket.Purpose.SETTINGS) {
                    buf.passthroughWrite(Boolean.class); // is gui open
                    buf.passthroughWrite(Boolean.class); // filtering craftable
                    buf.skipWrite(Boolean.class); // furnace gui open
                    buf.skipWrite(Boolean.class); // furnace filtering craftable
                }
            });
        });

        ProtocolRegistry.registerOutboundTranslator(CTabCompletePacket.class, buf -> {
            Supplier<VarInt> completionId = buf.skipWrite(VarInt.class);
            Supplier<String> command = buf.skipWrite(String.class);

            buf.whenWrite(() -> TabCompletionManager.addTabCompletionRequest(completionId.get().get(), command.get()));
            buf.pendingWrite(String.class, command, val -> buf.writeString(val, 32767));
            RayTraceResult hitResult = Minecraft.getInstance().objectMouseOver;
            boolean hasTarget = hitResult != null && hitResult.getType() == RayTraceResult.Type.BLOCK;
            buf.pendingWrite(Boolean.class, () -> false, buf::writeBoolean);
            buf.pendingWrite(Boolean.class, () -> hasTarget, buf::writeBoolean);
            if (hasTarget)
                buf.pendingWrite(BlockPos.class, ((BlockRayTraceResult) hitResult)::getPos, buf::writeBlockPos);
        });

        ProtocolRegistry.registerOutboundTranslator(ItemStack.class, new OutboundTranslator<ItemStack>() {
            @Override
            public void onWrite(TransformerByteBuf buf) {
                Supplier<Short> itemId = buf.skipWrite(Short.class);
                buf.whenWrite(() -> {
                    if (itemId.get() == -1) {
                        buf.pendingWrite(Short.class, itemId, (Consumer<Short>) buf::writeShort);
                    } else {
                        Supplier<Byte> count = buf.skipWrite(Byte.class);
                        Supplier<CompoundNBT> tag = buf.skipWrite(CompoundNBT.class);
                        buf.whenWrite(() -> {
                            CompoundNBT oldTag = tag.get();
                            int meta = oldTag.getInt("Damage");
                            oldTag.remove("Damage");
                            buf.pendingWrite(Short.class, itemId, (Consumer<Short>) buf::writeShort);
                            buf.pendingWrite(Byte.class, count, (Consumer<Byte>) buf::writeByte);
                            buf.pendingWrite(Short.class, () -> (short) meta, (Consumer<Short>) buf::writeShort);
                            buf.pendingWrite(CompoundNBT.class, () -> oldTag.isEmpty() ? null : oldTag, buf::writeCompoundTag);
                        });
                    }
                });
            }

            @Override
            public ItemStack translate(ItemStack from) {
                Pair<ItemStack, Integer> itemAndMeta = Items_1_12_2.newItemStackToOld(from);
                ItemStack to = itemAndMeta.getLeft();
                to.setDamage(itemAndMeta.getRight());
                return to;
            }
        });
    }

    @Override
    public void setup(boolean resourceReload) {
        TabCompletionManager.reset();
        super.setup(resourceReload);
    }

    @Override
    public List<PacketInfo<?>> getClientboundPackets() {
        List<PacketInfo<?>> packets = super.getClientboundPackets();
        remove(packets, STabCompletePacket.class);
        insertAfter(packets, SServerDifficultyPacket.class, PacketInfo.of(STabCompletePacket.class, STabCompletePacket::new));
        remove(packets, SCommandListPacket.class);
        remove(packets, SQueryNBTResponsePacket.class);
        remove(packets, SPlayerLookPacket.class);
        remove(packets, SStopSoundPacket.class);
        remove(packets, SUpdateRecipesPacket.class);
        remove(packets, STagsListPacket.class);
        return packets;
    }

    @Override
    public List<PacketInfo<?>> getServerboundPackets() {
        List<PacketInfo<?>> packets = super.getServerboundPackets();
        remove(packets, CTabCompletePacket.class);
        insertAfter(packets, CConfirmTeleportPacket.class, PacketInfo.of(CTabCompletePacket.class, CTabCompletePacket::new));
        remove(packets, CQueryTileEntityNBTPacket.class);
        remove(packets, CEditBookPacket.class);
        remove(packets, CQueryEntityNBTPacket.class);
        remove(packets, CPickItemPacket.class);
        remove(packets, CRenameItemPacket.class);
        remove(packets, CSelectTradePacket.class);
        remove(packets, CUpdateBeaconPacket.class);
        remove(packets, CUpdateCommandBlockPacket.class);
        remove(packets, CUpdateMinecartCommandBlockPacket.class);
        remove(packets, CUpdateStructureBlockPacket.class);
        remove(packets, CCustomPayloadPacket.class);
        insertAfter(packets, CCloseWindowPacket.class, PacketInfo.of(CustomPayloadC2SPacket_1_12_2.class, CustomPayloadC2SPacket_1_12_2::new));
        return packets;
    }

    @Override
    public boolean onSendPacket(IPacket<?> packet) {
        if (!super.onSendPacket(packet))
            return false;
        if (packet.getClass() == CQueryTileEntityNBTPacket.class || packet.getClass() == CQueryEntityNBTPacket.class) {
            return false;
        }
        ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
        if (packet.getClass() == CCustomPayloadPacket.class) {
            assert connection != null;
            CustomPayloadC2SAccessor customPayload = (CustomPayloadC2SAccessor) packet;
            String channel;
            if (customPayload.multiconnect_getChannel().equals(CCustomPayloadPacket.BRAND))
                channel = "MC|Brand";
            else
                channel = customPayload.multiconnect_getChannel().toString();
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2(channel, customPayload.multiconnect_getData()));
            return false;
        }
        if (packet.getClass() == CEditBookPacket.class) {
            assert connection != null;
            CEditBookPacket bookUpdate = (CEditBookPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeItemStack(bookUpdate.getStack());
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2(bookUpdate.shouldUpdateAll() ? "MC|BSign" : "MC|BEdit", buf));
            return false;
        }
        if (packet.getClass() == CRenameItemPacket.class) {
            assert connection != null;
            CRenameItemPacket renameItem = (CRenameItemPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeString(renameItem.getName(), 32767);
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|ItemName", buf));
            return false;
        }
        if (packet.getClass() == CSelectTradePacket.class) {
            assert connection != null;
            CSelectTradePacket selectTrade = (CSelectTradePacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeInt(selectTrade.func_210353_a());
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|TrSel", buf));
            return false;
        }
        if (packet.getClass() == CUpdateBeaconPacket.class) {
            assert connection != null;
            CUpdateBeaconPacket updateBeacon = (CUpdateBeaconPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeInt(updateBeacon.getPrimaryEffect());
            buf.writeInt(updateBeacon.getSecondaryEffect());
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|Beacon", buf));
            return false;
        }
        if (packet.getClass() == CUpdateCommandBlockPacket.class) {
            assert connection != null;
            CUpdateCommandBlockPacket updateCmdBlock = (CUpdateCommandBlockPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeInt(updateCmdBlock.getPos().getX());
            buf.writeInt(updateCmdBlock.getPos().getY());
            buf.writeInt(updateCmdBlock.getPos().getZ());
            buf.writeString(updateCmdBlock.getCommand());
            buf.writeBoolean(updateCmdBlock.shouldTrackOutput());
            switch (updateCmdBlock.getMode()) {
                case AUTO:
                    buf.writeString("AUTO");
                    break;
                case REDSTONE:
                    buf.writeString("REDSTONE");
                    break;
                case SEQUENCE:
                    buf.writeString("SEQUENCE");
                    break;
                default:
                    LOGGER.error("Unknown command block type: " + updateCmdBlock.getMode());
                    return false;
            }
            buf.writeBoolean(updateCmdBlock.isConditional());
            buf.writeBoolean(updateCmdBlock.isAuto());
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|AutoCmd", buf));
            return false;
        }
        if (packet.getClass() == CUpdateMinecartCommandBlockPacket.class) {
            assert connection != null;
            CUpdateMinecartCommandBlockPacket updateCmdMinecart = (CUpdateMinecartCommandBlockPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeByte(1); // command block type (minecart)
            buf.writeInt(((CommandBlockMinecartC2SAccessor) updateCmdMinecart).getEntityId());
            buf.writeString(updateCmdMinecart.getCommand());
            buf.writeBoolean(updateCmdMinecart.shouldTrackOutput());
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|AdvCmd", buf));
            return false;
        }
        if (packet.getClass() == CUpdateStructureBlockPacket.class) {
            assert connection != null;
            CUpdateStructureBlockPacket updateStructBlock = (CUpdateStructureBlockPacket) packet;
            TransformerByteBuf buf = new TransformerByteBuf(Unpooled.buffer(), null);
            buf.writeTopLevelType(CustomPayload.class);
            buf.writeInt(updateStructBlock.getPos().getX());
            buf.writeInt(updateStructBlock.getPos().getY());
            buf.writeInt(updateStructBlock.getPos().getZ());
            switch (updateStructBlock.func_210384_b()) {
                case UPDATE_DATA:
                    buf.writeByte(1);
                    break;
                case SAVE_AREA:
                    buf.writeByte(2);
                    break;
                case LOAD_AREA:
                    buf.writeByte(3);
                    break;
                case SCAN_AREA:
                    buf.writeByte(4);
                    break;
                default:
                    LOGGER.error("Unknown structure block action: " + updateStructBlock.getMode());
                    return false;
            }
            switch (updateStructBlock.getMode()) {
                case SAVE:
                    buf.writeString("SAVE");
                    break;
                case LOAD:
                     buf.writeString("LOAD");
                     break;
                case CORNER:
                    buf.writeString("CORNER");
                    break;
                case DATA:
                    buf.writeString("DATA");
                    break;
                default:
                    LOGGER.error("Unknown structure block mode: " + updateStructBlock.getMode());
                    return false;
            }
            buf.writeString(updateStructBlock.getName());
            buf.writeInt(updateStructBlock.getPosition().getX());
            buf.writeInt(updateStructBlock.getPosition().getY());
            buf.writeInt(updateStructBlock.getPosition().getZ());
            buf.writeInt(updateStructBlock.getSize().getX());
            buf.writeInt(updateStructBlock.getSize().getY());
            buf.writeInt(updateStructBlock.getSize().getZ());
            switch (updateStructBlock.getMirror()) {
                case NONE:
                    buf.writeString("NONE");
                    break;
                case LEFT_RIGHT:
                    buf.writeString("LEFT_RIGHT");
                    break;
                case FRONT_BACK:
                    buf.writeString("FRONT_BACK");
                    break;
                default:
                    LOGGER.error("Unknown mirror: " + updateStructBlock.getMirror());
                    return false;
            }
            switch (updateStructBlock.getRotation()) {
                case NONE:
                    buf.writeString("NONE");
                    break;
                case CLOCKWISE_90:
                    buf.writeString("CLOCKWISE_90");
                    break;
                case CLOCKWISE_180:
                    buf.writeString("CLOCKWISE_180");
                    break;
                case COUNTERCLOCKWISE_90:
                    buf.writeString("COUNTERCLOCKWISE_90");
                    break;
                default:
                    LOGGER.error("Unknown rotation: " + updateStructBlock.getRotation());
                    return false;
            }
            buf.writeString(updateStructBlock.getMetadata());
            buf.writeBoolean(updateStructBlock.shouldIgnoreEntities());
            buf.writeBoolean(updateStructBlock.shouldShowAir());
            buf.writeBoolean(updateStructBlock.shouldShowBoundingBox());
            buf.writeFloat(updateStructBlock.getIntegrity());
            buf.writeVarLong(updateStructBlock.getSeed());
            // have fun with all that, server!
            connection.sendPacket(new CustomPayloadC2SPacket_1_12_2("MC|Struct", buf));
            return false;
        }

        return true;
    }

    @Override
    protected void removeIDataSerializers() {
        super.removeIDataSerializers();
        removeIDataSerializer(DataSerializers.OPTIONAL_TEXT_COMPONENT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readDataParameter(IDataSerializer<T> handler, PacketBuffer buf) {
        if (handler == DataSerializers.OPTIONAL_BLOCK_STATE) {
            int stateId = buf.readVarInt();
            if (stateId == 0)
                return (T) Optional.empty();
            return (T) Optional.ofNullable(Block.BLOCK_STATE_IDS.getByValue(Blocks_1_12_2.convertToStateRegistryId(stateId)));
        }
        return super.readDataParameter(handler, buf);
    }

    @Override
    public boolean acceptEntityData(Class<? extends Entity> clazz, DataParameter<?> data) {
        if (!super.acceptEntityData(clazz, data))
            return false;

        if (clazz == AreaEffectCloudEntity.class && data == AreaEffectCloudEntityAccessor.getParticleId()) {
            DataTrackerManager.registerOldDataParameter(AreaEffectCloudEntity.class,
                    OLD_AREA_EFFECT_CLOUD_PARTICLE_ID,
                    Registry.PARTICLE_TYPE.getId(ParticleTypes.ENTITY_EFFECT),
                    (entity, val) -> {
                ParticleType<?> type = Registry.PARTICLE_TYPE.getByValue(val);
                if (type == null)
                    type = ParticleTypes.ENTITY_EFFECT;
                setParticleType(entity, type);
            });
            DataTrackerManager.registerOldDataParameter(AreaEffectCloudEntity.class,
                    OLD_AREA_EFFECT_CLOUD_PARTICLE_PARAM1,
                    0,
                    (entity, val) -> {
                ((IAreaEffectCloudEntity) entity).multiconnect_setParam1(val);
                setParticleType(entity, entity.getParticleData().getType());
            });
            DataTrackerManager.registerOldDataParameter(AreaEffectCloudEntity.class,
                    OLD_AREA_EFFECT_CLOUD_PARTICLE_PARAM2,
                    0,
                    (entity, val) -> {
                ((IAreaEffectCloudEntity) entity).multiconnect_setParam2(val);
                setParticleType(entity, entity.getParticleData().getType());
            });
            return false;
        }

        if (clazz == Entity.class && data == EntityAccessor.getCustomName()) {
            DataTrackerManager.registerOldDataParameter(Entity.class, OLD_CUSTOM_NAME, "",
                    (entity, val) -> entity.setCustomName(val.isEmpty() ? null : new StringTextComponent(val)));
            return false;
        }

        if (clazz == BoatEntity.class && data == BoatEntityAccessor.getBubbleWobbleTicks()) {
            return false;
        }

        if (clazz == ZombieEntity.class && data == ZombieEntityAccessor.getConvertingInWater()) {
            return false;
        }

        if (clazz == AbstractMinecartEntity.class) {
            DataParameter<Integer> displayTile = AbstractMinecartEntityAccessor.getCustomBlockId();
            if (data == displayTile) {
                DataTrackerManager.registerOldDataParameter(AbstractMinecartEntity.class, OLD_MINECART_DISPLAY_TILE, 0,
                        (entity, val) -> entity.getDataManager().set(displayTile, Blocks_1_12_2.convertToStateRegistryId(val)));
                return false;
            }
        }

        if (clazz == WolfEntity.class) {
            DataParameter<Integer> collarColor = WolfEntityAccessor.getCollarColor();
            if (data == collarColor) {
                DataTrackerManager.registerOldDataParameter(WolfEntity.class, OLD_WOLF_COLLAR_COLOR, 1,
                        (entity, val) -> entity.getDataManager().set(collarColor, 15 - val));
                return false;
            }
        }

        return true;
    }

    private static void setParticleType(AreaEffectCloudEntity entity, ParticleType<?> type) {
        IAreaEffectCloudEntity iaece = (IAreaEffectCloudEntity) entity;
        if (type.getDeserializer() == ItemParticleData.DESERIALIZER) {
            Item item = Registry.ITEM.getByValue(iaece.multiconnect_getParam1());
            int meta = iaece.multiconnect_getParam2();
            ItemStack stack = Items_1_12_2.oldItemStackToNew(new ItemStack(item), meta);
            entity.setParticleData(createParticle(type, buf -> buf.writeItemStack(stack)));
        } else if (type.getDeserializer() == BlockParticleData.DESERIALIZER) {
            entity.setParticleData(createParticle(type, buf -> buf.writeVarInt(iaece.multiconnect_getParam1())));
        } else if (type.getDeserializer() == RedstoneParticleData.DESERIALIZER) {
            entity.setParticleData(createParticle(type, buf -> {
                buf.writeFloat(1);
                buf.writeFloat(0);
                buf.writeFloat(0);
                buf.writeFloat(1);
            }));
        } else {
            entity.setParticleData(createParticle(type, buf -> {}));
        }
    }

    private static <T extends IParticleData> T createParticle(ParticleType<T> type, Consumer<PacketBuffer> function) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        function.accept(buf);
        return type.getDeserializer().read(type, buf);
    }

    @Override
    protected void recomputeBlockStates() {
        final int leavesId = Registry.BLOCK.getId(Blocks.OAK_LEAVES);
        final int leaves2Id = Registry.BLOCK.getId(Blocks.ACACIA_LEAVES);
        final int torchId = Registry.BLOCK.getId(Blocks.TORCH);
        final int redstoneTorchId = Registry.BLOCK.getId(Blocks.REDSTONE_TORCH);
        final int unlitRedstoneTorchId = Registry.BLOCK.getId(Blocks_1_12_2.UNLIT_REDSTONE_TORCH);
        final int skullId = Registry.BLOCK.getId(Blocks.SKELETON_SKULL);
        final int tallGrassId = Registry.BLOCK.getId(Blocks.GRASS);
        final int chestId = Registry.BLOCK.getId(Blocks.CHEST);
        final int enderChestId = Registry.BLOCK.getId(Blocks.ENDER_CHEST);
        final int trappedChestId = Registry.BLOCK.getId(Blocks.TRAPPED_CHEST);
        final int wallBannerId = Registry.BLOCK.getId(Blocks.WHITE_WALL_BANNER);

        ((IIdList) Block.BLOCK_STATE_IDS).multiconnect_clear();
        for (int blockId = 0; blockId < 256; blockId++) {
            if (blockId == leavesId) {
                registerLeavesStates(blockId, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES);
            } else if (blockId == leaves2Id) {
                registerLeavesStates(blockId, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.ACACIA_LEAVES, Blocks.ACACIA_LEAVES);
            } else if (blockId == torchId) {
                registerTorchStates(blockId, Blocks.TORCH.getDefaultState(), Blocks.WALL_TORCH.getDefaultState());
            } else if (blockId == redstoneTorchId) {
                registerTorchStates(blockId, Blocks.REDSTONE_TORCH.getDefaultState(), Blocks.REDSTONE_WALL_TORCH.getDefaultState());
            } else if (blockId == unlitRedstoneTorchId) {
                registerTorchStates(blockId, Blocks.REDSTONE_TORCH.getDefaultState().with(RedstoneTorchBlock.LIT, false), Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(RedstoneWallTorchBlock.REDSTONE_TORCH_LIT, false));
            } else if (blockId == skullId) {
                final Direction[] dirs = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP};
                for (int meta = 0; meta < 16; meta++) {
                    Direction dir = dirs[meta & 7];
                    BlockState state;
                    if (dir == Direction.DOWN || dir == Direction.UP) {
                        state = Blocks.SKELETON_SKULL.getDefaultState();
                    } else {
                        state = Blocks.SKELETON_WALL_SKULL.getDefaultState().with(WallSkullBlock.FACING, dir);
                    }
                    Block.BLOCK_STATE_IDS.put(state, blockId << 4 | meta);
                }
            } else if (blockId == tallGrassId) {
                Block.BLOCK_STATE_IDS.put(Blocks.DEAD_BUSH.getDefaultState(), blockId << 4);
                Block.BLOCK_STATE_IDS.put(Blocks.GRASS.getDefaultState(), blockId << 4 | 1);
                Block.BLOCK_STATE_IDS.put(Blocks.FERN.getDefaultState(), blockId << 4 | 2);
                for (int meta = 3; meta < 16; meta++)
                    Block.BLOCK_STATE_IDS.put(Blocks.DEAD_BUSH.getDefaultState(), blockId << 4 | meta);
            } else if (blockId == chestId) {
                registerHorizontalFacingStates(blockId, Blocks.CHEST);
            } else if (blockId == enderChestId) {
                registerHorizontalFacingStates(blockId, Blocks.ENDER_CHEST);
            } else if (blockId == trappedChestId) {
                registerHorizontalFacingStates(blockId, Blocks.TRAPPED_CHEST);
            } else if (blockId == wallBannerId) {
                registerHorizontalFacingStates(blockId, Blocks.WHITE_WALL_BANNER);
            } else {
                for (int meta = 0; meta < 16; meta++) {
                    Dynamic<?> dynamicState = BlockStateFlatteningMap.getFixedNBTForID(blockId << 4 | meta);
                    String fixedName = dynamicState.get("Name").asString("");
                    if (meta == 0 || fixedName.equals(BlockStateFlatteningMap.updateId(blockId << 4)))
                        fixedName = BlockStateReverseFlattening.reverseLookupStateBlock(blockId << 4);
                    fixedName = EntityRenaming1510.BLOCK_RENAME_MAP.getOrDefault(fixedName, fixedName);
                    Block block = Registry.BLOCK.getOrDefault(new ResourceLocation(fixedName));
                    if (block == Blocks.AIR && blockId != 0) {
                        dynamicState = BlockStateReverseFlattening.reverseLookupState(blockId << 4 | meta);
                        fixedName = dynamicState.get("Name").asString("");
                        block = Registry.BLOCK.getOrDefault(new ResourceLocation(fixedName));
                    }
                    if (block != Blocks.AIR || blockId == 0) {
                        StateContainer<Block, BlockState> stateManager = block instanceof DummyBlock ? ((DummyBlock) block).original.getBlock().getStateContainer() : block.getStateContainer();
                        BlockState _default = block instanceof DummyBlock ? ((DummyBlock) block).original : block.getDefaultState();
                        BlockState state = _default;
                        for (Map.Entry<String, String> entry : dynamicState.get("Properties").asMap(k -> k.asString(""), v -> v.asString("")).entrySet()) {
                            state = addProperty(stateManager, state, entry.getKey(), entry.getValue());
                        }
                        if (!acceptBlockState(state))
                            state = _default;
                        Block.BLOCK_STATE_IDS.put(state, blockId << 4 | meta);
                    }
                }
            }
        }
        Set<BlockState> addedStates = new HashSet<>();
        Block.BLOCK_STATE_IDS.iterator().forEachRemaining(addedStates::add);

        for (Block block : Registry.BLOCK) {
            for (BlockState state : block.getStateContainer().getValidStates()) {
                if (!addedStates.contains(state) && acceptBlockState(state)) {
                    Block.BLOCK_STATE_IDS.add(state);
                }
            }
        }
    }

    private void registerLeavesStates(int blockId, Block... leavesBlocks) {
        for (int type = 0; type < 4; type++) {
            Block.BLOCK_STATE_IDS.put(leavesBlocks[type].getDefaultState(), blockId << 4 | type);
            Block.BLOCK_STATE_IDS.put(leavesBlocks[type].getDefaultState().with(LeavesBlock.PERSISTENT, true), blockId << 4 | 4 | type);
            Block.BLOCK_STATE_IDS.put(leavesBlocks[type].getDefaultState().with(LeavesBlock.DISTANCE, 6), blockId << 4 | 8 | type);
            Block.BLOCK_STATE_IDS.put(leavesBlocks[type].getDefaultState().with(LeavesBlock.PERSISTENT, true).with(LeavesBlock.DISTANCE, 6), blockId << 4 | 12 | type);
        }
    }

    private void registerTorchStates(int blockId, BlockState torch, BlockState wallTorch) {
        Block.BLOCK_STATE_IDS.put(torch, blockId << 4);
        Block.BLOCK_STATE_IDS.put(wallTorch.with(WallTorchBlock.HORIZONTAL_FACING, Direction.EAST), blockId << 4 | 1);
        Block.BLOCK_STATE_IDS.put(wallTorch.with(WallTorchBlock.HORIZONTAL_FACING, Direction.WEST), blockId << 4 | 2);
        Block.BLOCK_STATE_IDS.put(wallTorch.with(WallTorchBlock.HORIZONTAL_FACING, Direction.SOUTH), blockId << 4 | 3);
        Block.BLOCK_STATE_IDS.put(wallTorch.with(WallTorchBlock.HORIZONTAL_FACING, Direction.NORTH), blockId << 4 | 4);
        for (int meta = 5; meta < 16; meta++)
            Block.BLOCK_STATE_IDS.put(torch, blockId << 4 | meta);
    }

    private void registerHorizontalFacingStates(int blockId, Block block) {
        registerHorizontalFacingStates(blockId, block, block);
    }

    private void registerHorizontalFacingStates(int blockId, Block standingBlock, Block wallBlock) {
        Block.BLOCK_STATE_IDS.put(standingBlock.getDefaultState(), blockId << 4);
        Block.BLOCK_STATE_IDS.put(standingBlock.getDefaultState(), blockId << 4 | 1);
        Block.BLOCK_STATE_IDS.put(wallBlock.getDefaultState().with(HorizontalFaceBlock.HORIZONTAL_FACING, Direction.NORTH), blockId << 4 | 2);
        Block.BLOCK_STATE_IDS.put(wallBlock.getDefaultState().with(HorizontalFaceBlock.HORIZONTAL_FACING, Direction.SOUTH), blockId << 4 | 3);
        Block.BLOCK_STATE_IDS.put(wallBlock.getDefaultState().with(HorizontalFaceBlock.HORIZONTAL_FACING, Direction.WEST), blockId << 4 | 4);
        Block.BLOCK_STATE_IDS.put(wallBlock.getDefaultState().with(HorizontalFaceBlock.HORIZONTAL_FACING, Direction.EAST), blockId << 4 | 5);
        for (int meta = 6; meta < 16; meta++)
            Block.BLOCK_STATE_IDS.put(standingBlock.getDefaultState(), blockId << 4 | meta);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState addProperty(StateContainer<Block, BlockState> stateManager, BlockState state, String propName, String valName) {
        IProperty<T> prop = (IProperty<T>) stateManager.getProperty(propName);
        return prop == null ? state : state.with(prop, prop.parseValue(valName).orElseGet(() -> state.get(prop)));
    }

    @Override
    public boolean acceptBlockState(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.TNT)
            return super.acceptBlockState(state.with(TNTBlock.UNSTABLE, false)); // re-add unstable because it was absent from 1.13.0 :thonkjang:

        if (!super.acceptBlockState(state))
            return false;

        if (block instanceof LeavesBlock && state.get(LeavesBlock.DISTANCE) < 6)
            return false;
        if (block == Blocks.PISTON_HEAD && state.get(PistonHeadBlock.SHORT))
            return false;
        if (state.getProperties().contains(BlockStateProperties.WATERLOGGED) && state.get(BlockStateProperties.WATERLOGGED))
            return false;
        if (block == Blocks.LEVER) {
            AttachFace face = state.get(LeverBlock.FACE);
            Direction facing = state.get(LeverBlock.HORIZONTAL_FACING);
            if ((face == AttachFace.FLOOR || face == AttachFace.CEILING) && (facing == Direction.SOUTH || facing == Direction.EAST))
                return false;
        }
        if (block instanceof TrapDoorBlock && state.get(TrapDoorBlock.POWERED))
            return false;
        if ((block == Blocks.OAK_WOOD || block == Blocks.SPRUCE_WOOD
                || block == Blocks.BIRCH_WOOD || block == Blocks.JUNGLE_WOOD
                || block == Blocks.ACACIA_WOOD || block == Blocks.DARK_OAK_WOOD)
                && state.get(RotatedPillarBlock.AXIS) != Direction.Axis.Y)
            return false;
        return true;
    }

    public Multimap<Tag<Block>, Block> getBlockTags() {
        Multimap<Tag<Block>, Block> tags = HashMultimap.create();
        tags.putAll(BlockTags.WOOL, Arrays.asList(
            Blocks.WHITE_WOOL,
            Blocks.ORANGE_WOOL,
            Blocks.MAGENTA_WOOL,
            Blocks.LIGHT_BLUE_WOOL,
            Blocks.YELLOW_WOOL,
            Blocks.LIME_WOOL,
            Blocks.PINK_WOOL,
            Blocks.GRAY_WOOL,
            Blocks.LIGHT_GRAY_WOOL,
            Blocks.CYAN_WOOL,
            Blocks.PURPLE_WOOL,
            Blocks.BLUE_WOOL,
            Blocks.BROWN_WOOL,
            Blocks.GREEN_WOOL,
            Blocks.RED_WOOL,
            Blocks.BLACK_WOOL));
        tags.putAll(BlockTags.PLANKS, Arrays.asList(
            Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS,
            Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS,
            Blocks.ACACIA_PLANKS,
            Blocks.DARK_OAK_PLANKS));
        tags.putAll(BlockTags.STONE_BRICKS, Arrays.asList(
            Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.CHISELED_STONE_BRICKS));
        tags.put(BlockTags.WOODEN_BUTTONS, Blocks.OAK_BUTTON);
        tags.putAll(BlockTags.BUTTONS, tags.get(BlockTags.WOODEN_BUTTONS));
        tags.put(BlockTags.BUTTONS, Blocks.STONE_BUTTON);
        tags.putAll(BlockTags.CARPETS, Arrays.asList(
            Blocks.WHITE_CARPET,
            Blocks.ORANGE_CARPET,
            Blocks.MAGENTA_CARPET,
            Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET,
            Blocks.LIME_CARPET,
            Blocks.PINK_CARPET,
            Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET,
            Blocks.CYAN_CARPET,
            Blocks.PURPLE_CARPET,
            Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET,
            Blocks.GREEN_CARPET,
            Blocks.RED_CARPET,
            Blocks.BLACK_CARPET));
        tags.putAll(BlockTags.WOODEN_DOORS, Arrays.asList(
            Blocks.OAK_DOOR,
            Blocks.SPRUCE_DOOR,
            Blocks.BIRCH_DOOR,
            Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR,
            Blocks.DARK_OAK_DOOR));
        tags.putAll(BlockTags.WOODEN_STAIRS, Arrays.asList(
            Blocks.OAK_STAIRS,
            Blocks.SPRUCE_STAIRS,
            Blocks.BIRCH_STAIRS,
            Blocks.JUNGLE_STAIRS,
            Blocks.ACACIA_STAIRS,
            Blocks.DARK_OAK_STAIRS));
        tags.putAll(BlockTags.WOODEN_SLABS, Arrays.asList(
            Blocks.OAK_SLAB,
            Blocks.SPRUCE_SLAB,
            Blocks.BIRCH_SLAB,
            Blocks.JUNGLE_SLAB,
            Blocks.ACACIA_SLAB,
            Blocks.DARK_OAK_SLAB));
        tags.putAll(BlockTags.WOODEN_FENCES, Arrays.asList(
            Blocks.OAK_FENCE,
            Blocks.ACACIA_FENCE,
            Blocks.DARK_OAK_FENCE,
            Blocks.SPRUCE_FENCE,
            Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE));
        tags.putAll(BlockTags.DOORS, tags.get(BlockTags.WOODEN_DOORS));
        tags.put(BlockTags.DOORS, Blocks.IRON_DOOR);
        tags.putAll(BlockTags.SAPLINGS, Arrays.asList(
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.DARK_OAK_SAPLING));
        tags.putAll(BlockTags.DARK_OAK_LOGS, Arrays.asList(
            Blocks.DARK_OAK_LOG,
            Blocks.DARK_OAK_WOOD));
        tags.putAll(BlockTags.OAK_LOGS, Arrays.asList(
            Blocks.OAK_LOG,
            Blocks.OAK_WOOD));
        tags.putAll(BlockTags.ACACIA_LOGS, Arrays.asList(
            Blocks.ACACIA_LOG,
            Blocks.ACACIA_WOOD));
        tags.putAll(BlockTags.BIRCH_LOGS, Arrays.asList(
            Blocks.BIRCH_LOG,
            Blocks.BIRCH_WOOD));
        tags.putAll(BlockTags.JUNGLE_LOGS, Arrays.asList(
            Blocks.JUNGLE_LOG,
            Blocks.JUNGLE_WOOD));
        tags.putAll(BlockTags.SPRUCE_LOGS, Arrays.asList(
            Blocks.SPRUCE_LOG,
            Blocks.SPRUCE_WOOD));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.DARK_OAK_LOGS));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.OAK_LOGS));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.ACACIA_LOGS));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.BIRCH_LOGS));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.JUNGLE_LOGS));
        tags.putAll(BlockTags.LOGS, tags.get(BlockTags.SPRUCE_LOGS));
        tags.putAll(BlockTags.ANVIL, Arrays.asList(
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL));
        tags.putAll(BlockTags.SMALL_FLOWERS, Arrays.asList(
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY));
        tags.putAll(BlockTags.ENDERMAN_HOLDABLE, tags.get(BlockTags.SMALL_FLOWERS));
        tags.putAll(BlockTags.ENDERMAN_HOLDABLE, Arrays.asList(
            Blocks.GRASS_BLOCK,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.PODZOL,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.GRAVEL,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.TNT,
            Blocks.CACTUS,
            Blocks.CLAY,
            Blocks.CARVED_PUMPKIN,
            Blocks.MELON,
            Blocks.MYCELIUM,
            Blocks.NETHERRACK));
        tags.putAll(BlockTags.FLOWER_POTS, Arrays.asList(
            Blocks.FLOWER_POT,
            Blocks.POTTED_POPPY,
            Blocks.POTTED_BLUE_ORCHID,
            Blocks.POTTED_ALLIUM,
            Blocks.POTTED_AZURE_BLUET,
            Blocks.POTTED_RED_TULIP,
            Blocks.POTTED_ORANGE_TULIP,
            Blocks.POTTED_WHITE_TULIP,
            Blocks.POTTED_PINK_TULIP,
            Blocks.POTTED_OXEYE_DAISY,
            Blocks.POTTED_DANDELION,
            Blocks.POTTED_OAK_SAPLING,
            Blocks.POTTED_SPRUCE_SAPLING,
            Blocks.POTTED_BIRCH_SAPLING,
            Blocks.POTTED_JUNGLE_SAPLING,
            Blocks.POTTED_ACACIA_SAPLING,
            Blocks.POTTED_DARK_OAK_SAPLING,
            Blocks.POTTED_RED_MUSHROOM,
            Blocks.POTTED_BROWN_MUSHROOM,
            Blocks.POTTED_DEAD_BUSH,
            Blocks.POTTED_FERN,
            Blocks.POTTED_CACTUS));
        tags.putAll(BlockTags.BANNERS, Arrays.asList(
            Blocks.WHITE_BANNER,
            Blocks.ORANGE_BANNER,
            Blocks.MAGENTA_BANNER,
            Blocks.LIGHT_BLUE_BANNER,
            Blocks.YELLOW_BANNER,
            Blocks.LIME_BANNER,
            Blocks.PINK_BANNER,
            Blocks.GRAY_BANNER,
            Blocks.LIGHT_GRAY_BANNER,
            Blocks.CYAN_BANNER,
            Blocks.PURPLE_BANNER,
            Blocks.BLUE_BANNER,
            Blocks.BROWN_BANNER,
            Blocks.GREEN_BANNER,
            Blocks.RED_BANNER,
            Blocks.BLACK_BANNER,
            Blocks.WHITE_WALL_BANNER,
            Blocks.ORANGE_WALL_BANNER,
            Blocks.MAGENTA_WALL_BANNER,
            Blocks.LIGHT_BLUE_WALL_BANNER,
            Blocks.YELLOW_WALL_BANNER,
            Blocks.LIME_WALL_BANNER,
            Blocks.PINK_WALL_BANNER,
            Blocks.GRAY_WALL_BANNER,
            Blocks.LIGHT_GRAY_WALL_BANNER,
            Blocks.CYAN_WALL_BANNER,
            Blocks.PURPLE_WALL_BANNER,
            Blocks.BLUE_WALL_BANNER,
            Blocks.BROWN_WALL_BANNER,
            Blocks.GREEN_WALL_BANNER,
            Blocks.RED_WALL_BANNER,
            Blocks.BLACK_WALL_BANNER));
        tags.put(BlockTags.WOODEN_PRESSURE_PLATES, Blocks.OAK_PRESSURE_PLATE);
        tags.putAll(BlockTags.STAIRS, Arrays.asList(
            Blocks.OAK_STAIRS,
            Blocks.COBBLESTONE_STAIRS,
            Blocks.SPRUCE_STAIRS,
            Blocks.SANDSTONE_STAIRS,
            Blocks.ACACIA_STAIRS,
            Blocks.JUNGLE_STAIRS,
            Blocks.BIRCH_STAIRS,
            Blocks.DARK_OAK_STAIRS,
            Blocks.NETHER_BRICK_STAIRS,
            Blocks.STONE_BRICK_STAIRS,
            Blocks.BRICK_STAIRS,
            Blocks.PURPUR_STAIRS,
            Blocks.QUARTZ_STAIRS,
            Blocks.RED_SANDSTONE_STAIRS));
        tags.putAll(BlockTags.SLABS, Arrays.asList(
            Blocks.SMOOTH_STONE_SLAB,
            Blocks.STONE_BRICK_SLAB,
            Blocks.SANDSTONE_SLAB,
            Blocks.ACACIA_SLAB,
            Blocks.BIRCH_SLAB,
            Blocks.DARK_OAK_SLAB,
            Blocks.JUNGLE_SLAB,
            Blocks.OAK_SLAB,
            Blocks.SPRUCE_SLAB,
            Blocks.PURPUR_SLAB,
            Blocks.QUARTZ_SLAB,
            Blocks.RED_SANDSTONE_SLAB,
            Blocks.BRICK_SLAB,
            Blocks.COBBLESTONE_SLAB,
            Blocks.NETHER_BRICK_SLAB,
            Blocks.PETRIFIED_OAK_SLAB));
        tags.putAll(BlockTags.WALLS, Arrays.asList(
            Blocks.COBBLESTONE_WALL,
            Blocks.MOSSY_COBBLESTONE_WALL));
        tags.putAll(BlockTags.SAND, Arrays.asList(
            Blocks.SAND,
            Blocks.RED_SAND));
        tags.putAll(BlockTags.RAILS, Arrays.asList(
            Blocks.RAIL,
            Blocks.POWERED_RAIL,
            Blocks.DETECTOR_RAIL,
            Blocks.ACTIVATOR_RAIL));
        tags.putAll(BlockTags.ICE, Arrays.asList(
            Blocks.ICE,
            Blocks.PACKED_ICE,
            Blocks.BLUE_ICE,
            Blocks.FROSTED_ICE));
        tags.putAll(BlockTags.VALID_SPAWN, Arrays.asList(
            Blocks.GRASS_BLOCK,
            Blocks.PODZOL));
        tags.putAll(BlockTags.LEAVES, Arrays.asList(
            Blocks.JUNGLE_LEAVES,
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.BIRCH_LEAVES));
        tags.putAll(BlockTags.IMPERMEABLE, Arrays.asList(
            Blocks.GLASS,
            Blocks.WHITE_STAINED_GLASS,
            Blocks.ORANGE_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS,
            Blocks.LIME_STAINED_GLASS,
            Blocks.PINK_STAINED_GLASS,
            Blocks.GRAY_STAINED_GLASS,
            Blocks.LIGHT_GRAY_STAINED_GLASS,
            Blocks.CYAN_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.BLUE_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS,
            Blocks.GREEN_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS,
            Blocks.BLACK_STAINED_GLASS));
        tags.put(BlockTags.WOODEN_TRAPDOORS, Blocks.OAK_TRAPDOOR);
        tags.putAll(BlockTags.TRAPDOORS, tags.get(BlockTags.WOODEN_TRAPDOORS));
        tags.put(BlockTags.TRAPDOORS, Blocks.IRON_TRAPDOOR);
        tags.putAll(BlockTags.STANDING_SIGNS, Arrays.asList(
            Blocks.OAK_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.ACACIA_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.DARK_OAK_SIGN));
        tags.putAll(BlockTags.WALL_SIGNS, Arrays.asList(
            Blocks.OAK_WALL_SIGN,
            Blocks.SPRUCE_WALL_SIGN,
            Blocks.BIRCH_WALL_SIGN,
            Blocks.ACACIA_WALL_SIGN,
            Blocks.JUNGLE_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN));
        tags.putAll(BlockTags.SIGNS, tags.get(BlockTags.STANDING_SIGNS));
        tags.putAll(BlockTags.SIGNS, tags.get(BlockTags.WALL_SIGNS));
        tags.putAll(BlockTags.BEDS, Arrays.asList(
            Blocks.RED_BED,
            Blocks.BLACK_BED,
            Blocks.BLUE_BED,
            Blocks.BROWN_BED,
            Blocks.CYAN_BED,
            Blocks.GRAY_BED,
            Blocks.GREEN_BED,
            Blocks.LIGHT_BLUE_BED,
            Blocks.LIGHT_GRAY_BED,
            Blocks.LIME_BED,
            Blocks.MAGENTA_BED,
            Blocks.ORANGE_BED,
            Blocks.PINK_BED,
            Blocks.PURPLE_BED,
            Blocks.WHITE_BED,
            Blocks.YELLOW_BED));
        tags.putAll(BlockTags.FENCES, tags.get(BlockTags.WOODEN_FENCES));
        tags.put(BlockTags.FENCES, Blocks.NETHER_BRICK_FENCE);
        tags.putAll(BlockTags.DRAGON_IMMUNE, Arrays.asList(
            Blocks.BARRIER,
            Blocks.BEDROCK,
            Blocks.END_PORTAL,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_GATEWAY,
            Blocks.COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK,
            Blocks.JIGSAW,
            Blocks.MOVING_PISTON,
            Blocks.OBSIDIAN,
            Blocks.END_STONE,
            Blocks.IRON_BARS));
        tags.putAll(BlockTags.WITHER_IMMUNE, Arrays.asList(
            Blocks.BARRIER,
            Blocks.BEDROCK,
            Blocks.END_PORTAL,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_GATEWAY,
            Blocks.COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK,
            Blocks.JIGSAW,
            Blocks.MOVING_PISTON));
        tags.putAll(BlockTags.TALL_FLOWERS, Arrays.asList(
            Blocks.SUNFLOWER,
            Blocks.LILAC,
            Blocks.PEONY,
            Blocks.ROSE_BUSH));
        tags.putAll(BlockTags.FLOWERS, tags.get(BlockTags.SMALL_FLOWERS));
        tags.putAll(BlockTags.FLOWERS, tags.get(BlockTags.TALL_FLOWERS));
        tags.putAll(BlockTags.CROPS, Arrays.asList(
            Blocks.BEETROOTS,
            Blocks.CARROTS,
            Blocks.POTATOES,
            Blocks.WHEAT,
            Blocks.MELON_STEM,
            Blocks.PUMPKIN_STEM));
        tags.putAll(BlockTags.SHULKER_BOXES, Arrays.asList(
            Blocks.SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX));
        tags.putAll(BlockTags.PORTALS, Arrays.asList(
            Blocks.NETHER_PORTAL,
            Blocks.END_PORTAL,
            Blocks.END_GATEWAY));
        return tags;
    }

    public Multimap<Tag<Item>, Item> getItemTags() {
        Multimap<Tag<Block>, Block> blockTags = getBlockTags();
        Multimap<Tag<Item>, Item> tags = HashMultimap.create();
        copyBlockItemTags(tags, blockTags, ItemTags.WOOL, BlockTags.WOOL);
        copyBlockItemTags(tags, blockTags, ItemTags.PLANKS, BlockTags.PLANKS);
        copyBlockItemTags(tags, blockTags, ItemTags.STONE_BRICKS, BlockTags.STONE_BRICKS);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_BUTTONS, BlockTags.WOODEN_BUTTONS);
        copyBlockItemTags(tags, blockTags, ItemTags.BUTTONS, BlockTags.BUTTONS);
        copyBlockItemTags(tags, blockTags, ItemTags.CARPETS, BlockTags.CARPETS);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_DOORS, BlockTags.WOODEN_DOORS);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_STAIRS, BlockTags.WOODEN_STAIRS);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_SLABS, BlockTags.WOODEN_SLABS);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_FENCES, BlockTags.WOODEN_FENCES);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_PRESSURE_PLATES, BlockTags.WOODEN_PRESSURE_PLATES);
        copyBlockItemTags(tags, blockTags, ItemTags.DOORS, BlockTags.DOORS);
        copyBlockItemTags(tags, blockTags, ItemTags.SAPLINGS, BlockTags.SAPLINGS);
        copyBlockItemTags(tags, blockTags, ItemTags.OAK_LOGS, BlockTags.OAK_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.DARK_OAK_LOGS, BlockTags.DARK_OAK_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.BIRCH_LOGS, BlockTags.BIRCH_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.ACACIA_LOGS, BlockTags.ACACIA_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.SPRUCE_LOGS, BlockTags.SPRUCE_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.JUNGLE_LOGS, BlockTags.JUNGLE_LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.LOGS, BlockTags.LOGS);
        copyBlockItemTags(tags, blockTags, ItemTags.SAND, BlockTags.SAND);
        copyBlockItemTags(tags, blockTags, ItemTags.SLABS, BlockTags.SLABS);
        copyBlockItemTags(tags, blockTags, ItemTags.WALLS, BlockTags.WALLS);
        copyBlockItemTags(tags, blockTags, ItemTags.STAIRS, BlockTags.STAIRS);
        copyBlockItemTags(tags, blockTags, ItemTags.ANVIL, BlockTags.ANVIL);
        copyBlockItemTags(tags, blockTags, ItemTags.RAILS, BlockTags.RAILS);
        copyBlockItemTags(tags, blockTags, ItemTags.LEAVES, BlockTags.LEAVES);
        copyBlockItemTags(tags, blockTags, ItemTags.WOODEN_TRAPDOORS, BlockTags.WOODEN_TRAPDOORS);
        copyBlockItemTags(tags, blockTags, ItemTags.TRAPDOORS, BlockTags.TRAPDOORS);
        copyBlockItemTags(tags, blockTags, ItemTags.SMALL_FLOWERS, BlockTags.SMALL_FLOWERS);
        copyBlockItemTags(tags, blockTags, ItemTags.BEDS, BlockTags.BEDS);
        copyBlockItemTags(tags, blockTags, ItemTags.FENCES, BlockTags.FENCES);
        copyBlockItemTags(tags, blockTags, ItemTags.TALL_FLOWERS, BlockTags.TALL_FLOWERS);
        copyBlockItemTags(tags, blockTags, ItemTags.FLOWERS, BlockTags.FLOWERS);
        tags.putAll(ItemTags.BANNERS, Arrays.asList(
            Items.WHITE_BANNER,
            Items.ORANGE_BANNER,
            Items.MAGENTA_BANNER,
            Items.LIGHT_BLUE_BANNER,
            Items.YELLOW_BANNER,
            Items.LIME_BANNER,
            Items.PINK_BANNER,
            Items.GRAY_BANNER,
            Items.LIGHT_GRAY_BANNER,
            Items.CYAN_BANNER,
            Items.PURPLE_BANNER,
            Items.BLUE_BANNER,
            Items.BROWN_BANNER,
            Items.GREEN_BANNER,
            Items.RED_BANNER,
            Items.BLACK_BANNER));
        tags.putAll(ItemTags.BOATS, Arrays.asList(
            Items.OAK_BOAT,
            Items.SPRUCE_BOAT,
            Items.BIRCH_BOAT,
            Items.JUNGLE_BOAT,
            Items.ACACIA_BOAT,
            Items.DARK_OAK_BOAT));
        tags.putAll(ItemTags.FISHES, Arrays.asList(
            Items.COD,
            Items.COOKED_COD,
            Items.SALMON,
            Items.COOKED_SALMON,
            Items.PUFFERFISH,
            Items.TROPICAL_FISH));
        copyBlockItemTags(tags, blockTags, ItemTags.SIGNS, BlockTags.STANDING_SIGNS);
        tags.putAll(ItemTags.MUSIC_DISCS, Arrays.asList(
            Items.MUSIC_DISC_13,
            Items.MUSIC_DISC_CAT,
            Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR,
            Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WARD,
            Items.MUSIC_DISC_11,
            Items.MUSIC_DISC_WAIT));
        tags.putAll(ItemTags.COALS, Arrays.asList(
            Items.COAL,
            Items.CHARCOAL));
        tags.putAll(ItemTags.ARROWS, Arrays.asList(
            Items.ARROW,
            Items.TIPPED_ARROW,
            Items.SPECTRAL_ARROW));
        tags.putAll(ItemTags.LECTERN_BOOKS, Arrays.asList(
            Items.WRITTEN_BOOK,
            Items.WRITABLE_BOOK));
        return tags;
    }

    protected void copyBlockItemTags(Multimap<Tag<Item>, Item> itemTags, Multimap<Tag<Block>, Block> blockTags, Tag<Item> itemTag, Tag<Block> blockTag) {
        itemTags.putAll(itemTag, Collections2.transform(blockTags.get(blockTag), Item.BLOCK_TO_ITEM::get));
    }

    public Multimap<Tag<Fluid>, Fluid> getFluidTags() {
        Multimap<Tag<Fluid>, Fluid> tags = HashMultimap.create();
        tags.putAll(FluidTags.WATER, Arrays.asList(
            Fluids.WATER,
            Fluids.FLOWING_WATER));
        tags.putAll(FluidTags.LAVA, Arrays.asList(
            Fluids.LAVA,
            Fluids.FLOWING_LAVA));
        return tags;
    }

    public Multimap<Tag<EntityType<?>>, EntityType<?>> getEntityTypeTags() {
        Multimap<Tag<EntityType<?>>, EntityType<?>> tags = HashMultimap.create();
        tags.putAll(EntityTypeTags.SKELETONS, Arrays.asList(
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON));
        tags.putAll(EntityTypeTags.ARROWS, Arrays.asList(
            EntityType.ARROW,
            EntityType.SPECTRAL_ARROW));
        return tags;
    }

    public List<RecipeInfo<?>> getCraftingRecipes() {
        return Recipes_1_12_2.getRecipes();
    }

    @Override
    public boolean shouldBlockChangeReplaceBlockEntity(Block oldBlock, Block newBlock) {
        if (!super.shouldBlockChangeReplaceBlockEntity(oldBlock, newBlock))
            return false;

        if (oldBlock instanceof AbstractSkullBlock && newBlock instanceof AbstractSkullBlock)
            return false;
        if (oldBlock instanceof AbstractBannerBlock && newBlock instanceof AbstractBannerBlock)
            return false;
        if (oldBlock instanceof FlowerPotBlock && newBlock instanceof FlowerPotBlock)
            return false;

        return true;
    }

    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "unchecked"})
    @Override
    public void modifyRegistry(ISimpleRegistry<?> registry) {
        super.modifyRegistry(registry);

        // just fucking nuke them all, it's the flattening after all
        if (registry == Registry.BLOCK) {
            Blocks_1_12_2.registerBlocks((ISimpleRegistry<Block>) registry);
        } else if (registry == Registry.ITEM) {
            Items_1_12_2.registerItems((ISimpleRegistry<Item>) registry);
        } else if (registry == Registry.ENTITY_TYPE) {
            Entities_1_12_2.registerEntities((ISimpleRegistry<EntityType<?>>) registry);
        } else if (registry == Registry.ENCHANTMENT) {
            Enchantments_1_12_2.registerEnchantments((ISimpleRegistry<Enchantment>) registry);
        } else if (registry == Registry.POTION) {
            modifyPotionRegistry((ISimpleRegistry<Potion>) registry);
        } else if (registry == Registry.BIOME) {
            modifyBiomeRegistry((ISimpleRegistry<Biome>) registry);
        } else if (registry == Registry.PARTICLE_TYPE) {
            Particles_1_12_2.registerParticles((ISimpleRegistry<ParticleType<?>>) registry);
        } else if (registry == Registry.BLOCK_ENTITY_TYPE) {
            BlockEntities_1_12_2.registerBlockEntities((ISimpleRegistry<TileEntityType<?>>) registry);
        } else if (registry == Registry.EFFECTS) {
            modifyStatusEffectRegistry((ISimpleRegistry<Effect>) registry);
        } else if (registry == Registry.SOUND_EVENT) {
            modifySoundRegistry((ISimpleRegistry<SoundEvent>) registry);
        }
    }

    private static void modifyPotionRegistry(ISimpleRegistry<Potion> registry) {
        registry.unregister(Potions.STRONG_SLOWNESS);
        registry.unregister(Potions.TURTLE_MASTER);
        registry.unregister(Potions.LONG_TURTLE_MASTER);
        registry.unregister(Potions.STRONG_TURTLE_MASTER);
        registry.unregister(Potions.SLOW_FALLING);
        registry.unregister(Potions.LONG_SLOW_FALLING);
    }

    private static void modifyBiomeRegistry(ISimpleRegistry<Biome> registry) {
        rename(registry, Biomes.MOUNTAINS, "extreme_hills");
        rename(registry, Biomes.SWAMP, "swampland");
        rename(registry, Biomes.NETHER, "hell");
        rename(registry, Biomes.THE_END, "sky");
        rename(registry, Biomes.SNOWY_TUNDRA, "ice_flats");
        rename(registry, Biomes.SNOWY_MOUNTAINS, "ice_mountains");
        rename(registry, Biomes.MUSHROOM_FIELDS, "mushroom_island");
        rename(registry, Biomes.MUSHROOM_FIELD_SHORE, "mushroom_island_shore");
        rename(registry, Biomes.BEACH, "beaches");
        rename(registry, Biomes.WOODED_HILLS, "forest_hills");
        rename(registry, Biomes.MOUNTAIN_EDGE, "smaller_extreme_hills");
        rename(registry, Biomes.STONE_SHORE, "stone_beach");
        rename(registry, Biomes.SNOWY_BEACH, "cold_beach");
        rename(registry, Biomes.DARK_FOREST, "roofed_forest");
        rename(registry, Biomes.SNOWY_TAIGA, "taiga_cold");
        rename(registry, Biomes.SNOWY_TAIGA_HILLS, "taiga_cold_hills");
        rename(registry, Biomes.GIANT_TREE_TAIGA, "redwood_taiga");
        rename(registry, Biomes.GIANT_TREE_TAIGA_HILLS, "redwood_taiga_hills");
        rename(registry, Biomes.WOODED_MOUNTAINS, "extreme_hills_with_trees");
        rename(registry, Biomes.SAVANNA_PLATEAU, "savanna_rock");
        rename(registry, Biomes.BADLANDS, "mesa");
        rename(registry, Biomes.WOODED_BADLANDS_PLATEAU, "mesa_rock");
        rename(registry, Biomes.BADLANDS_PLATEAU, "mesa_clear_rock");
        registry.purge(Biomes.SMALL_END_ISLANDS);
        registry.purge(Biomes.END_MIDLANDS);
        registry.purge(Biomes.END_HIGHLANDS);
        registry.purge(Biomes.END_BARRENS);
        registry.purge(Biomes.WARM_OCEAN);
        registry.purge(Biomes.LUKEWARM_OCEAN);
        registry.purge(Biomes.COLD_OCEAN);
        registry.purge(Biomes.DEEP_WARM_OCEAN);
        registry.purge(Biomes.DEEP_LUKEWARM_OCEAN);
        registry.purge(Biomes.DEEP_WARM_OCEAN);
        registry.purge(Biomes.DEEP_LUKEWARM_OCEAN);
        registry.purge(Biomes.DEEP_COLD_OCEAN);
        registry.purge(Biomes.DEEP_FROZEN_OCEAN);
        rename(registry, Biomes.THE_VOID, "void");
        rename(registry, Biomes.SUNFLOWER_PLAINS, "mutated_plains");
        rename(registry, Biomes.DESERT_LAKES, "mutated_desert");
        rename(registry, Biomes.GRAVELLY_MOUNTAINS, "mutated_extreme_hills");
        rename(registry, Biomes.FLOWER_FOREST, "mutated_forest");
        rename(registry, Biomes.TAIGA_MOUNTAINS, "mutated_taiga");
        rename(registry, Biomes.SWAMP_HILLS, "mutated_swampland");
        rename(registry, Biomes.ICE_SPIKES, "mutated_ice_flats");
        rename(registry, Biomes.MODIFIED_JUNGLE, "mutated_jungle");
        rename(registry, Biomes.MODIFIED_JUNGLE_EDGE, "mutated_jungle_edge");
        rename(registry, Biomes.TALL_BIRCH_FOREST, "mutated_birch_forest");
        rename(registry, Biomes.TALL_BIRCH_HILLS, "mutated_birch_forest_hills");
        rename(registry, Biomes.DARK_FOREST_HILLS, "mutated_roofed_forest_hills");
        rename(registry, Biomes.SNOWY_TAIGA_MOUNTAINS, "mutated_taiga_cold");
        rename(registry, Biomes.GIANT_SPRUCE_TAIGA, "mutated_redwood_taiga");
        rename(registry, Biomes.GIANT_SPRUCE_TAIGA_HILLS, "mutated_redwood_taiga_hills");
        rename(registry, Biomes.MODIFIED_GRAVELLY_MOUNTAINS, "mutated_extreme_hills_with_trees");
        rename(registry, Biomes.SHATTERED_SAVANNA, "mutated_savanna");
        rename(registry, Biomes.SHATTERED_SAVANNA_PLATEAU, "mutated_savanna_rock");
        rename(registry, Biomes.ERODED_BADLANDS, "mutated_mesa");
        rename(registry, Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, "mutated_mesa_rock");
        rename(registry, Biomes.MODIFIED_BADLANDS_PLATEAU, "mutated_mesa_clear_rock");
    }

    private static void modifyStatusEffectRegistry(ISimpleRegistry<Effect> registry) {
        registry.unregister(Effects.SLOW_FALLING);
        registry.unregister(Effects.CONDUIT_POWER);
        registry.unregister(Effects.DOLPHINS_GRACE);
    }

    private static void modifySoundRegistry(ISimpleRegistry<SoundEvent> registry) {
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_ENTER);
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_EXIT);
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_LOOP);
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS);
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE);
        registry.unregister(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE);
        registry.unregister(SoundEvents.BLOCK_BEACON_ACTIVATE);
        registry.unregister(SoundEvents.BLOCK_BEACON_AMBIENT);
        registry.unregister(SoundEvents.BLOCK_BEACON_DEACTIVATE);
        registry.unregister(SoundEvents.BLOCK_BEACON_POWER_SELECT);
        registry.unregister(SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP);
        registry.unregister(SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT);
        registry.unregister(SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE);
        registry.unregister(SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT);
        registry.unregister(SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE);
        registry.unregister(SoundEvents.BLOCK_CONDUIT_ACTIVATE);
        registry.unregister(SoundEvents.BLOCK_CONDUIT_AMBIENT);
        registry.unregister(SoundEvents.BLOCK_CONDUIT_AMBIENT_SHORT);
        registry.unregister(SoundEvents.BLOCK_CONDUIT_ATTACK_TARGET);
        registry.unregister(SoundEvents.BLOCK_CONDUIT_DEACTIVATE);
        registry.unregister(SoundEvents.BLOCK_WET_GRASS_BREAK);
        registry.unregister(SoundEvents.BLOCK_WET_GRASS_FALL);
        registry.unregister(SoundEvents.BLOCK_WET_GRASS_HIT);
        registry.unregister(SoundEvents.BLOCK_WET_GRASS_PLACE);
        registry.unregister(SoundEvents.BLOCK_WET_GRASS_STEP);
        registry.unregister(SoundEvents.BLOCK_CORAL_BLOCK_BREAK);
        registry.unregister(SoundEvents.BLOCK_CORAL_BLOCK_FALL);
        registry.unregister(SoundEvents.BLOCK_CORAL_BLOCK_HIT);
        registry.unregister(SoundEvents.BLOCK_CORAL_BLOCK_PLACE);
        registry.unregister(SoundEvents.BLOCK_CORAL_BLOCK_STEP);
        registry.unregister(SoundEvents.BLOCK_PUMPKIN_CARVE);
        registry.unregister(SoundEvents.ENTITY_COD_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_COD_DEATH);
        registry.unregister(SoundEvents.ENTITY_COD_FLOP);
        registry.unregister(SoundEvents.ENTITY_COD_HURT);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_AMBIENT_WATER);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_ATTACK);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_DEATH);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_EAT);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_HURT);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_JUMP);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_PLAY);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_SPLASH);
        registry.unregister(SoundEvents.ENTITY_DOLPHIN_SWIM);
        registry.unregister(SoundEvents.ENTITY_DROWNED_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_DROWNED_AMBIENT_WATER);
        registry.unregister(SoundEvents.ENTITY_DROWNED_DEATH);
        registry.unregister(SoundEvents.ENTITY_DROWNED_DEATH_WATER);
        registry.unregister(SoundEvents.ENTITY_DROWNED_HURT);
        registry.unregister(SoundEvents.ENTITY_DROWNED_HURT_WATER);
        registry.unregister(SoundEvents.ENTITY_DROWNED_SHOOT);
        registry.unregister(SoundEvents.ENTITY_DROWNED_STEP);
        registry.unregister(SoundEvents.ENTITY_DROWNED_SWIM);
        registry.unregister(SoundEvents.ENTITY_FISH_SWIM);
        registry.unregister(SoundEvents.ENTITY_HUSK_CONVERTED_TO_ZOMBIE);
        registry.unregister(SoundEvents.ENTITY_PARROT_IMITATE_DROWNED);
        registry.unregister(SoundEvents.ENTITY_PARROT_IMITATE_PHANTOM);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_BITE);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_DEATH);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_FLAP);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_HURT);
        registry.unregister(SoundEvents.ENTITY_PHANTOM_SWOOP);
        registry.unregister(SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_DEATH);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_FLOP);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_HURT);
        registry.unregister(SoundEvents.ENTITY_PUFFER_FISH_STING);
        registry.unregister(SoundEvents.ENTITY_SALMON_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_SALMON_DEATH);
        registry.unregister(SoundEvents.ENTITY_SALMON_FLOP);
        registry.unregister(SoundEvents.ENTITY_SALMON_HURT);
        registry.unregister(SoundEvents.ENTITY_SKELETON_HORSE_SWIM);
        registry.unregister(SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT_WATER);
        registry.unregister(SoundEvents.ENTITY_SKELETON_HORSE_GALLOP_WATER);
        registry.unregister(SoundEvents.ENTITY_SKELETON_HORSE_JUMP_WATER);
        registry.unregister(SoundEvents.ENTITY_SKELETON_HORSE_STEP_WATER);
        registry.unregister(SoundEvents.ENTITY_SQUID_SQUIRT);
        registry.unregister(SoundEvents.ENTITY_TROPICAL_FISH_AMBIENT);
        registry.unregister(SoundEvents.ENTITY_TROPICAL_FISH_DEATH);
        registry.unregister(SoundEvents.ENTITY_TROPICAL_FISH_FLOP);
        registry.unregister(SoundEvents.ENTITY_TROPICAL_FISH_HURT);
        registry.unregister(SoundEvents.ENTITY_TURTLE_AMBIENT_LAND);
        registry.unregister(SoundEvents.ENTITY_TURTLE_DEATH);
        registry.unregister(SoundEvents.ENTITY_TURTLE_DEATH_BABY);
        registry.unregister(SoundEvents.ENTITY_TURTLE_EGG_BREAK);
        registry.unregister(SoundEvents.ENTITY_TURTLE_EGG_CRACK);
        registry.unregister(SoundEvents.ENTITY_TURTLE_EGG_HATCH);
        registry.unregister(SoundEvents.ENTITY_TURTLE_HURT);
        registry.unregister(SoundEvents.ENTITY_TURTLE_HURT_BABY);
        registry.unregister(SoundEvents.ENTITY_TURTLE_LAY_EGG);
        registry.unregister(SoundEvents.ENTITY_TURTLE_SHAMBLE);
        registry.unregister(SoundEvents.ENTITY_TURTLE_SHAMBLE_BABY);
        registry.unregister(SoundEvents.ENTITY_TURTLE_SWIM);
        registry.unregister(SoundEvents.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED);
        registry.unregister(SoundEvents.ENTITY_ZOMBIE_DESTROY_EGG);
        registry.unregister(SoundEvents.ITEM_ARMOR_EQUIP_TURTLE);
        registry.unregister(SoundEvents.ITEM_AXE_STRIP);
        registry.unregister(SoundEvents.ITEM_BUCKET_EMPTY_FISH);
        registry.unregister(SoundEvents.ITEM_BUCKET_FILL_FISH);
        registry.unregister(SoundEvents.ITEM_TRIDENT_HIT);
        registry.unregister(SoundEvents.ITEM_TRIDENT_HIT_GROUND);
        registry.unregister(SoundEvents.ITEM_TRIDENT_RETURN);
        registry.unregister(SoundEvents.ITEM_TRIDENT_RIPTIDE_1);
        registry.unregister(SoundEvents.ITEM_TRIDENT_RIPTIDE_2);
        registry.unregister(SoundEvents.ITEM_TRIDENT_RIPTIDE_3);
        registry.unregister(SoundEvents.ITEM_TRIDENT_THROW);
        registry.unregister(SoundEvents.ITEM_TRIDENT_THUNDER);
        registry.unregister(SoundEvents.MUSIC_UNDER_WATER);
    }
}
