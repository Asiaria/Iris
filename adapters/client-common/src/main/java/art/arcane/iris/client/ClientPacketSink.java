package art.arcane.iris.client;

@FunctionalInterface
public interface ClientPacketSink {
    void send(byte[] frame);
}
