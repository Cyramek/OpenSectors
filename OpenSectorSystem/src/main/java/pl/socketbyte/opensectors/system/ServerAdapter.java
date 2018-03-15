package pl.socketbyte.opensectors.system;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.socketbyte.opensectors.linker.packet.PacketSendMessage;
import pl.socketbyte.opensectors.system.api.ChannelManager;
import pl.socketbyte.opensectors.system.api.LinkerConnection;
import pl.socketbyte.opensectors.system.api.LinkerStorage;
import pl.socketbyte.opensectors.system.api.PacketExtender;
import pl.socketbyte.opensectors.system.logging.ByteInformator;
import pl.socketbyte.opensectors.system.logging.ByteUtil;
import pl.socketbyte.opensectors.system.packet.*;
import pl.socketbyte.opensectors.system.packet.types.Receiver;
import pl.socketbyte.opensectors.system.util.NetworkManager;
import pl.socketbyte.opensectors.system.util.ServerManager;
import pl.socketbyte.opensectors.system.util.Util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ServerAdapter extends Listener {

    @Override
    public void disconnected(Connection connection) {
        OpenSectorSystem.log().info("Linker (ID #" + connection.getID() + ") connection lost. ("
                + connection.getRemoteAddressTCP() + ")");

        super.disconnected(connection);
    }

    @Override
    public void received(Connection connection, Object object) {
        ByteInformator.incrementBytesReceived(ByteUtil.sizeof(object));

        if (!SecurityModule.isAuthorized(connection, object))
            return;

        for (PacketExtender packetExtender : PacketExtender.getPacketExtenders())
            if (object.getClass().isAssignableFrom(packetExtender.getPacket())) {
                if (packetExtender.getPacketAdapter() == null)
                    continue;

                packetExtender.getPacketAdapter().received(connection, object);
            }

        if (object instanceof PacketLinkerAuthRequest) {
            PacketLinkerAuthRequest packet = (PacketLinkerAuthRequest)object;
            String password = packet.getPassword();
            if (!password.equals(OpenSectorSystem.getPassword()))
                connection.close();
            else {
                AuthorizedConnectionData.getAuthorizedConnectionMap()
                        .put(connection.getID(), connection);

                LinkerConnection linkerConnection = new LinkerConnection(packet.getServerId(), connection.getID());
                linkerConnection.setConnection(connection);
                LinkerStorage.getLinkerConnectionMap().put(linkerConnection.getServerId(), linkerConnection);

                PacketConfigurationInfo packetConfigurationInfo = new PacketConfigurationInfo();
                packetConfigurationInfo.setJsonConfig(OpenSectorSystem.getConfig());

                NetworkManager.sendTCP(connection, packetConfigurationInfo);

                OpenSectorSystem.log().info("New linker connection estabilished with " + connection.getRemoteAddressTCP() +
                        " (ID #" + connection.getID() + ", server id: " + linkerConnection.getServerId() + ")");
            }
        }
        else if (object instanceof PacketCustomPayload) {
            ChannelManager.execute(connection, (PacketCustomPayload) object);
        }
        else if (object instanceof PacketUpdatePlayerSession) {
            PacketUpdatePlayerSession packet = (PacketUpdatePlayerSession)object;

            Database.setPlayerSession(UUID.fromString(packet.getPlayerUniqueId()), packet.getServerId());
        }
        else if (object instanceof PacketPlayerTransferRequest) {
            PacketPlayerTransferRequest packet = (PacketPlayerTransferRequest)object;
            UUID uniqueId = UUID.fromString(packet.getPlayerUniqueId());
            int id = packet.getServerId();

            PacketPlayerInfo packetPlayerInfo = packet.getPlayerInfo();
            LinkerConnection linkerConnection = LinkerStorage.getLinker(id);

            if (linkerConnection == null) {
                OpenSectorSystem.log().warning("Linker with id " + id + " is not connected or it is not responding!");
                return;
            }

            NetworkManager.sendTCP(linkerConnection.getConnection(), packetPlayerInfo);
            ServerManager.transfer(uniqueId, id);
        }
        else if (object instanceof PacketQueryExecute) {
            PacketQueryExecute packet = (PacketQueryExecute) object;

            ProxyServer.getInstance().getScheduler().runAsync(OpenSectorSystem.getInstance(),
                    () -> NetworkManager.sendTCP(connection, Database.executeQuery(packet)));
        }
        else if (object instanceof PacketQuery) {
            PacketQuery packet = (PacketQuery) object;

            ProxyServer.getInstance().getScheduler().runAsync(OpenSectorSystem.getInstance(), () -> {
                PreparedStatement statement = Database.executeUpdate(packet);
                try {
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        statement.close();
                        statement.getConnection().close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else if (object instanceof PacketSendMessage) {
            PacketSendMessage packet = (PacketSendMessage)object;
            switch (packet.getReceiver()) {
                case ALL:
                    for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers())
                        proxiedPlayer.sendMessage(ChatMessageType.valueOf(packet.getMessageType().toString().toUpperCase()),
                                new TextComponent(Util.fixColors(packet.getMessage())));
                    break;
                case PLAYER:
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(UUID.fromString(packet.getUniqueId()));
                    player.sendMessage(ChatMessageType.valueOf(packet.getMessageType().toString().toUpperCase()),
                            new TextComponent(Util.fixColors(packet.getMessage())));
                    break;
            }
        }
        else if (object instanceof PacketItemTransfer) {
            PacketItemTransfer packet = (PacketItemTransfer) object;
            switch (packet.getReceiver()) {
                case ALL:
                    for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
                        LinkerConnection linkerConnection = LinkerStorage.getLinkerByPlayer(proxiedPlayer);
                        if (linkerConnection == null)
                            return;
                        packet.setReceiver(Receiver.PLAYER, proxiedPlayer.getUniqueId().toString());
                        linkerConnection.sendTCP(packet);
                    }
                    break;
                case PLAYER:
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(UUID.fromString(packet.getPlayerUniqueId()));
                    LinkerConnection linkerConnection = LinkerStorage.getLinkerByPlayer(player);
                    if (linkerConnection == null)
                        return;
                    linkerConnection.sendTCP(packet);
                    break;
            }

        }

        super.received(connection, object);
    }
}
