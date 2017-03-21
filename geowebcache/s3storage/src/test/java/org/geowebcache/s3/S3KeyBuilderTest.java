package org.geowebcache.s3;

import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.mime.MimeException;
import org.geowebcache.storage.TileObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class S3KeyBuilderTest {

    private static final String PARAMS_ID = "2f2656d9405e530e73952d40019bdd452c05b190";
    private static final String PNG_MIME = "image/png";
    private static final String EPSG_27700 = "EPSG:27700";
    private static final String STANDARD_KEY = "90_27/11_3/EPSG_27700_05.png";
    private static final String PARAMETERISED_KEY = "90_27/11_3/EPSG_27700_05#"
            + PARAMS_ID + ".png";
    private static S3KeyBuilder gennie;

    @BeforeClass
    public static void setupGenerator() {
        GridSetBroker gsb = new GridSetBroker(true, true);
        TileLayerDispatcher tileLayerDispatcher = new TileLayerDispatcher(gsb);
        gennie = new S3KeyBuilder("", tileLayerDispatcher);
    }

    @Test
    public void testGetKey() throws MimeException {
        String tileKey = gennie.tileKey(90L, 27L, 5L, EPSG_27700,
                (String) null, ImageMime.createFromFormat(PNG_MIME));
        assertEquals(STANDARD_KEY, tileKey);
    }

    @Test
    public void testGetKeyForTile() throws MimeException {
        long[] coords = { 90L, 27L, 5L };
        TileObject tile = TileObject.createQueryTileObject("LayerName", coords,
                EPSG_27700, PNG_MIME, Collections.EMPTY_MAP);
        String tileKey = gennie.tileKey(tile,
                ImageMime.createFromFormat(PNG_MIME));
        assertEquals(STANDARD_KEY, tileKey);
    }

    @Test
    public void testGetCoords() {
        long[] coords = gennie.getCoords(STANDARD_KEY);
        assertEquals(90L, coords[0]);
        assertEquals(27L, coords[1]);
        assertEquals(5L, coords[2]);
    }

    @Test
    public void testGetKeyParameterised() throws MimeException {
        String tileKey = gennie.tileKey(90L, 27L, 5L, EPSG_27700, PARAMS_ID,
                ImageMime.createFromFormat(PNG_MIME));
        assertEquals(PARAMETERISED_KEY, tileKey);
    }

    @Test
    public void testGetKeyForParameterisedTile() throws MimeException {
        long[] coords = { 90L, 27L, 5L };
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("Date", "2012");
        TileObject tile = TileObject.createQueryTileObject("LayerName", coords,
                EPSG_27700, PNG_MIME, paramMap);
        String tileKey = gennie.tileKey(tile,
                ImageMime.createFromFormat(PNG_MIME));
        assertEquals(PARAMETERISED_KEY, tileKey);
    }

    @Test
    public void testGetCoordsFromParameterisedKey() {
        long[] coords = gennie.getCoords(PARAMETERISED_KEY);
        assertEquals(90L, coords[0]);
        assertEquals(27L, coords[1]);
        assertEquals(5L, coords[2]);
    }

}
