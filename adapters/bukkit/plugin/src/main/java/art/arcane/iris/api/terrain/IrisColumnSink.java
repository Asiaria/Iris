package art.arcane.iris.api.terrain;

@FunctionalInterface
public interface IrisColumnSink {
    void accept(IrisColumnSample sample);
}
