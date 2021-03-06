package pl.socketbyte.opensectors.system.adapters.player;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.socketbyte.opensectors.system.OpenSectorSystem;
import pl.socketbyte.opensectors.system.api.LinkerConnection;
import pl.socketbyte.opensectors.system.api.LinkerStorage;
import pl.socketbyte.opensectors.system.packet.PacketPlayerInfo;
import pl.socketbyte.opensectors.system.packet.PacketPlayerTransfer;
import pl.socketbyte.opensectors.system.util.NetworkManager;
import pl.socketbyte.opensectors.system.util.ServerManager;
import pl.socketbyte.opensectors.system.util.Util;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public class PlayerTransferListener extends Listener {

    @Override
    public void received(Connection connection, Object object) {
        super.received(connection, object);

        if (!(object instanceof PacketPlayerTransfer))
            return;

        PacketPlayerTransfer packet = (PacketPlayerTransfer)object;
        UUID uniqueId = UUID.fromString(packet.getPlayerUniqueId());
        int id = packet.getServerId();

        PacketPlayerInfo packetPlayerInfo = packet.getPlayerInfo();
        LinkerConnection linkerConnection = LinkerStorage.getLinker(id);

        if (linkerConnection == null) {
            OpenSectorSystem.log().warning("Linker with id " + id + " is not connected or it is not responding!");
            return;
        }
        ProxiedPlayer player = Util.getPlayer(UUID.fromString(packet.getPlayerUniqueId()));
        if (player == null || !player.isConnected())
            return;
        ServerManager.transfer(uniqueId, id);

        try {
            // To ensure that player is actually being transfered
            // That actually helps a little with the packet response time
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        NetworkManager.sendTCP(linkerConnection.getConnection(), packetPlayerInfo);
    }

}
