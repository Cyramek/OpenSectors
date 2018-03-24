package pl.socketbyte.opensectors.system.packet;

import java.util.Arrays;

public class PacketCustomPayload extends Packet {

    private String channel;
    private Object[] data;

    public PacketCustomPayload() {

    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PacketCustomPayload{" +
                "channel='" + channel + '\'' +
                ", synchronizable=" + Arrays.toString(data) +
                '}';
    }
}
