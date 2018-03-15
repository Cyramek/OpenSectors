package pl.socketbyte.opensectors.linker;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.socketbyte.opensectors.linker.api.Callback;
import pl.socketbyte.opensectors.linker.api.CallbackManager;
import pl.socketbyte.opensectors.linker.api.ChannelManager;
import pl.socketbyte.opensectors.linker.api.PacketExtender;
import pl.socketbyte.opensectors.linker.packet.*;
import pl.socketbyte.opensectors.linker.packet.types.Weather;
import pl.socketbyte.opensectors.linker.util.PlayerInfoHolder;

import java.util.UUID;

public class ClientAdapter extends Listener {

    @Override
    public void received(Connection connection, Object object) {
        for (PacketExtender packetExtender : PacketExtender.getPacketExtenders())
            if (object.getClass().isAssignableFrom(packetExtender.getPacket())) {
                if (packetExtender.getPacketAdapter() == null)
                    continue;

                packetExtender.getPacketAdapter().received(connection, object);
            }

        if (object instanceof PacketConfigurationInfo) {
            PacketConfigurationInfo packet = (PacketConfigurationInfo)object;

            OpenSectorLinker.setConfiguration(packet.getJsonConfig());
            OpenSectorLinker.getInstance().getLogger().info("Successfully received configuration data from the proxy server.");
            OpenSectorLinker.ready();
        }
        else if (object instanceof PacketQueryExecute) {
            PacketQueryExecute packet = (PacketQueryExecute)object;

            Callback callback = CallbackManager.getCallbackMap().get(packet.getUniqueId());
            callback.received(packet);
        }
        else if (object instanceof PacketCustomPayload) {
            ChannelManager.execute(connection, (PacketCustomPayload) object);
        }
        else if (object instanceof PacketTimeInfo) {
            PacketTimeInfo packet = (PacketTimeInfo)object;

            Bukkit.getWorlds().get(0).setTime(packet.getBukkitTime());
        }
        else if (object instanceof PacketPlayerInfo) {
            PacketPlayerInfo packet = (PacketPlayerInfo)object;

            PlayerInfoHolder.getPlayerInfos().put(UUID.fromString(packet.getPlayerUniqueId()), packet);
        }
        else if (object instanceof PacketWeatherInfo) {
            PacketWeatherInfo weatherInfo = (PacketWeatherInfo) object;
            Weather weather = weatherInfo.getWeather();

            World world = Bukkit.getWorlds().get(0);

            switch (weather) {
                case RAIN:
                    world.setStorm(true);
                    world.setThundering(false);
                    break;
                case CLEAR:
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
                case STORM:
                    world.setStorm(true);
                    world.setThundering(true);
                    break;
            }
        }
        else if (object instanceof PacketItemTransfer) {
            PacketItemTransfer packet = (PacketItemTransfer)object;

            Player player = Bukkit.getPlayer(UUID.fromString(packet.getPlayerUniqueId()));
            ItemStack itemStack = packet.getItemStack().deserialize();

            player.getInventory().addItem(itemStack);
        }

        super.received(connection, object);
    }
}
