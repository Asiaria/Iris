package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;

public final class IrisClientDimension {
    private volatile IrisMessage.DimensionStatus status;

    public boolean onDimensionStatus(IrisMessage.DimensionStatus incoming) {
        IrisMessage.DimensionStatus previous = status;
        status = incoming;
        if (previous == null) {
            return true;
        }
        return !previous.equals(incoming);
    }

    public IrisMessage.DimensionStatus status() {
        return status;
    }

    public boolean irisWorld() {
        IrisMessage.DimensionStatus current = status;
        return current != null && current.irisWorld();
    }

    public void clear() {
        status = null;
    }
}
