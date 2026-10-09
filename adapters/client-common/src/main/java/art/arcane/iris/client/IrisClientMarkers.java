package art.arcane.iris.client;

import art.arcane.iris.spi.protocol.IrisMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Marker overlays keyed by tile, straight off the wire. Access-ordered and capped: the server decides how many
 * distinct tiles it sends markers for, so an unbounded map here is a remote memory dial.
 */
public final class IrisClientMarkers {
    static final int MAX_TILES = 64;

    private final LinkedHashMap<IrisTileKey, List<IrisMessage.VisionMarkers.Marker>> byTile;

    public IrisClientMarkers() {
        this.byTile = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IrisTileKey, List<IrisMessage.VisionMarkers.Marker>> eldest) {
                return size() > MAX_TILES;
            }
        };
    }

    public synchronized void onMarkers(IrisMessage.VisionMarkers markers) {
        byTile.put(new IrisTileKey(markers.tileX(), markers.tileZ(), markers.zoomLevel()), List.copyOf(markers.markers()));
    }

    public synchronized List<IrisMessage.VisionMarkers.Marker> forTile(IrisTileKey key) {
        return byTile.get(key);
    }

    public synchronized int trackedTiles() {
        return byTile.size();
    }

    public synchronized void clear() {
        byTile.clear();
    }
}
