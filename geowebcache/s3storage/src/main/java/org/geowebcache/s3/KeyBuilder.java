package org.geowebcache.s3;

import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;

/**
 * Created by sadam on 6/3/16.
 */
public interface KeyBuilder {
    String layerId(String layerName);
    String forTile(TileObject obj);
    String forLayer(final String layerName);
    String forGridset(final String layerName, final String gridsetId);
    String layerMetadata(final String layerName);
    /**
     * @return the key prefix up to the coordinates (i.e.
     *         {@code "<prefix>/<layer>/<gridset>/<format>/<parametersId>"})
     */
    String coordinatesPrefix(TileRange obj);
    String pendingDeletes();
}
