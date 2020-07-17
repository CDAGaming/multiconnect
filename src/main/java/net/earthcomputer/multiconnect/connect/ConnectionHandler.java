package net.earthcomputer.multiconnect.connect;

import net.earthcomputer.multiconnect.impl.ConnectionInfo;
import net.earthcomputer.multiconnect.mixin.connect.HandshakePacketAccessor;
import net.earthcomputer.multiconnect.protocols.ProtocolRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.ServerAddress;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class ConnectionHandler {

    private static final Logger LOGGER = LogManager.getLogger("multiconnect");

    public static boolean preConnect() throws UnknownHostException {
        String address;
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        if (serverInfo != null) {
            address = serverInfo.address;
        } else if (ConnectionInfo.port == 25565) {
            address = ConnectionInfo.ip;
        } else {
            address = ConnectionInfo.ip + ":" + ConnectionInfo.port;
        }

        // Hypixel has their own closed-source connection proxy and closed-source anti-cheat.
        // Users were getting banned for odd reasons. Their maps are designed to have fair play between clients on any
        // version, so we force the current protocol version here to disable any kind of bridge, in the hope that users
        // don't get banned because they are using multiconnect.
        String ip = normalizeAddress(address).split(":")[0].toLowerCase(Locale.ROOT);
        if (ip.endsWith(".")) {
            ip = ip.substring(0, ip.length() - 1);
        }
        if (ip.equals("hypixel.net") || ip.endsWith(".hypixel.net")) {
            if (SharedConstants.getGameVersion().isStable()) {
                ConnectionInfo.protocolVersion = SharedConstants.getGameVersion().getProtocolVersion();
            } else {
                ConnectionInfo.protocolVersion = ConnectionMode.protocolValues()[1].getValue();
            }
            LOGGER.info("Hypixel detected, protocol version forced to " + ConnectionInfo.protocolVersion + " (" + ConnectionMode.byValue(ConnectionInfo.protocolVersion).getName() + ")");
            return true;
        }


        int forcedVersion = ServersExt.getInstance().getForcedProtocol(address);
        forcedVersion = forcedVersion == ConnectionMode.AUTO.getValue() ? ConnectionInfo.globalForcedProtocolVersion.getValue() : forcedVersion;

        if (forcedVersion != ConnectionMode.AUTO.getValue()) {
            ConnectionInfo.protocolVersion = forcedVersion;
            LOGGER.info("Protocol version forced to " + ConnectionInfo.protocolVersion + " (" + ConnectionMode.byValue(forcedVersion).getName() + ")");
            return true;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof ConnectScreen))
            return true;
        IConnectScreen connectScreen = (IConnectScreen) screen;

        ClientConnection connection = ClientConnection.connect(InetAddress.getByName(ConnectionInfo.ip), ConnectionInfo.port, false);
        connectScreen.multiconnect_setVersionRequestConnection(connection);
        GetProtocolPacketListener listener = new GetProtocolPacketListener(connection);
        connection.setPacketListener(listener);

        HandshakeC2SPacket handshake  = new HandshakeC2SPacket(ConnectionInfo.ip, ConnectionInfo.port, NetworkState.STATUS);
        //noinspection ConstantConditions
        ((HandshakePacketAccessor) handshake).setProtocolVersion(-1);
        connection.send(handshake);
        connection.send(new QueryRequestC2SPacket());

        while (!listener.hasCompleted()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (connectScreen.isConnectingCancelled()) {
                connection.disconnect(new TranslatableText("multiplayer.disconnected"));
                return false;
            }
        }

        if (listener.hasFailed()) {
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().openScreen(new DisconnectedScreen(connectScreen.getParent(), "connect.failed", new LiteralText("Failed to request server protocol version"))));
        }

        connectScreen.multiconnect_setVersionRequestConnection(null);

        if (listener.hasFailed()) {
            return false;
        }

        int protocol = listener.getProtocol();
        if (ConnectionMode.isSupportedProtocol(protocol)) {
            LOGGER.info("Discovered server protocol: " + protocol + " (" + ConnectionMode.byValue(protocol).getName() + ")");
            ConnectionInfo.protocolVersion = protocol;
        } else {
            LOGGER.info("Discovered server protocol: " + protocol + " (unsupported), " +
                    "falling back to " + SharedConstants.getGameVersion().getProtocolVersion() + " (" + SharedConstants.getGameVersion().getName() + ")");
            ConnectionInfo.protocolVersion = SharedConstants.getGameVersion().getProtocolVersion();
        }

        return true;
    }

    public static void onSendHandshake(ClientConnection connect, Packet<?> handshakePacket) {
        if (ConnectionMode.isSupportedProtocol(ConnectionInfo.protocolVersion)) {
            ((HandshakePacketAccessor) handshakePacket).setProtocolVersion(ConnectionInfo.protocolVersion);
            ConnectionInfo.protocol = ProtocolRegistry.get(ConnectionInfo.protocolVersion);
            ConnectionInfo.protocol.setup(false);
        }
    }

    public static String normalizeAddress(String address) {
        ServerAddress addr = ServerAddress.parse(address);
        return addr.getAddress() + ":" + addr.getPort();
    }
}
