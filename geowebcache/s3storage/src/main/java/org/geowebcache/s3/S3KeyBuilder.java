package org.geowebcache.s3;

/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Stuart Adam, Ordnance Survey, Copyright 2014
 *  
 */

import com.google.common.base.Throwables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.blobstore.file.FilePathGenerator;

import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.geowebcache.storage.blobstore.file.FilePathUtils.appendGridsetZoomLevelDir;

public class S3KeyBuilder implements KeyBuilder {

    private static final Log LOG = LogFactory.getLog(S3KeyBuilder.class);
    public static final String LAYER_METADATA_OBJECT_NAME = "metadata.properties";
    private static final String LAYER_METADATA_FORMAT = "%s/%s/" + LAYER_METADATA_OBJECT_NAME;

    private String prefix="";
    private TileLayerDispatcher layers;

    public S3KeyBuilder(final String prefix, TileLayerDispatcher layers) {
        this.prefix = prefix;
        this.layers = layers;
    }

    public String forTile(TileObject obj) {
        checkNotNull(obj.getLayerName());
        checkNotNull(obj.getGridSetId());
        checkNotNull(obj.getBlobFormat());
        checkNotNull(obj.getXYZ());
        String format = obj.getBlobFormat();
        try {
            MimeType mimeType = MimeType.createFromFormat(format);
            return tileKey(obj, mimeType);
        } catch (MimeException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Builds the storage path for a tile and returns it as a File reference
     * <p>
     * </p>
     * 
     * @param layerName
     *            name of the layer the tile belongs to
     * @param tileIndex
     *            the [x,y,z] index for the tile
     * @param gridSetId
     *            the name of the gridset for the tile inside layer
     * @param mimeType
     *            the storage mime type
     * @param parameters_id
     *            the parameters identifier
     * @return File pointer to the tile image
     */
    public String tileKey(TileObject tile, MimeType mimeType) {
        final long[] tileIndex = tile.getXYZ();
        long x = tileIndex[0];
        long y = tileIndex[1];
        long z = tileIndex[2];
        String gridSetId = tile.getGridSetId();
        setTileParametersId(tile);
        String parametersId = tile.getParametersId();

        return tileKey(x, y, z, gridSetId, parametersId, mimeType);
    }

    public String tileKey(long x, long y, long z, String gridSetId, String parametersId, MimeType mimeType) {
        StringBuilder key = new StringBuilder(256);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("tileKey for %s %s %s %s %s", x, y, z, gridSetId, parametersId,
                    mimeType.getMimeType()));
        }

        long shift = z / 2;
        long half = 2 << shift;
        int digits = 1;
        if (half > 10) {
            digits = (int) (Math.log10(half)) + 1;
        }
        long halfx = x / half;
        long halfy = y / half;

        String fileExtension = mimeType.getFileExtension();

        appendDimension(key, 2 * digits, x, y);

        key.append(File.separatorChar);
        appendDimension(key, digits, halfx, halfy);

        key.append(File.separatorChar);
        appendGridsetZoomLevelDir(gridSetId, z, key);
        appendParameters(parametersId, key);

        key.append('.');
        key.append(fileExtension);

        String keyString = key.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug(keyString);
        }
        return keyString;
    }

    public String layerMetadata(final String layerName) {
        String layerId = layerId(layerName);
        return String.format(LAYER_METADATA_FORMAT, prefix, layerId);
    }

    public String layerId(String layerName) {
        TileLayer layer;
        try {
            layer = layers.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            throw Throwables.propagate(e);
        }
        return layer.getId();
    }

    private void appendParameters(String parametersId, StringBuilder key) {
        if (parametersId != null) {
            key.append('#');
            key.append(parametersId);
        }
    }

    private void setTileParametersId(TileObject tile) {
        String parametersId = tile.getParametersId();
        Map<String, String> parameters = tile.getParameters();
        if (parametersId == null && parameters != null && !parameters.isEmpty()) {
            parametersId = FilePathGenerator.getParametersId(parameters);
            tile.setParametersId(parametersId);
        }
    }

    private void appendDimension(StringBuilder key, int digits, long x, long y) {
        // zeroPadder(x, 2 * digits, key);
        key.append(x);
        key.append('_');
        key.append(y);
        // zeroPadder(y, 2 * digits, key);
    }

    public long[] getCoords(String key) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("get coords:" + key);
        }
        long[] fullCoords = new long[3];
        String[] path = key.split(File.separator);
        String dimension = path[0];
        String[] coords = dimension.split("_");
        fullCoords[0] = Long.valueOf(coords[0]);
        fullCoords[1] = Long.valueOf(coords[1]);

        String gridZoomParamFormat = path[path.length - 1];
        String[] stackCoords = gridZoomParamFormat.split("_");
        String zoomFormat = stackCoords[stackCoords.length - 1];
        String[] zoom = zoomFormat.split("\\.");
        zoom = zoom[0].split("#");
        fullCoords[2] = Long.valueOf(zoom[0]);
        return fullCoords;
    }

    public String pendingDeletes() {
        return String.format("%s/%s", prefix, "_pending_deletes.properties");
    }

    public String forGridset(final String layerName, final String gridsetId) {
        throw new UnsupportedOperationException("This key style does not support gridset prefixes");
    }

    public String forLayer(final String layerName) {
        throw new UnsupportedOperationException("This key style does not support layer prefixes");
    }

    public String coordinatesPrefix(TileRange obj) {
        throw new UnsupportedOperationException("This key style does not support coordinate prefixes");
    }
}
