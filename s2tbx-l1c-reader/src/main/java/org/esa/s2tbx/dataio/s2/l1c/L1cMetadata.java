/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.s2.l1c;

import https.psd_13_sentinel2_eo_esa_int.psd.s2_pdi_level_1c_tile_metadata.Level1C_Tile;
import https.psd_13_sentinel2_eo_esa_int.psd.user_product_level_1c.Level1C_User_Product;
import org.esa.s2tbx.dataio.jp2.TileLayout;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.s2tbx.dataio.Utils;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2Metadata;
import org.esa.s2tbx.dataio.s2.S2SpatialResolution;
import org.esa.s2tbx.dataio.s2.S2SpectralInformation;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.esa.s2tbx.dataio.s2.l1c.filepaterns.S2L1CGranuleDirFilename;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.util.SystemUtils;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Represents the Sentinel-2 MSI L1C XML metadata header file.
 * <p>
 * Note: No data interpretation is done in this class, it is intended to serve the pure metadata content only.
 *
 * @author Norman Fomferra
 */
public class L1cMetadata extends S2Metadata {

    private static final String PSD_STRING = "13";

    private MetadataElement metadataElement;
    protected Logger logger = SystemUtils.LOG;



    static class QuicklookDescriptor {
        int imageNCols;
        int imageNRows;
        Histogram[] histogramList;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class Histogram {
        public int bandId;
        int[] values;
        int step;
        double min;
        double max;
        double mean;
        double stdDev;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    private List<Tile> tileList;
    private Map<String, List<Tile>> allTileLists; // Key is UTM zone, values are the tiles associated to a UTM zone

    private ProductCharacteristics productCharacteristics;

    public static L1cMetadata parseHeader(File file, String granuleName, S2Config config, String epsg) throws JDOMException, IOException, JAXBException {
        return new L1cMetadata(new FileInputStream(file), file, file.getParent(), granuleName, config, epsg);
    }

    public List<Tile> getTileList() {
        return tileList;
    }

    public Set<String> getUTMZonesList() {
        return allTileLists.keySet();
    }

    public List<Tile> getTileList(String utmZone) {
        return allTileLists.get(utmZone);
    }

    public ProductCharacteristics getProductCharacteristics() {
        return productCharacteristics;
    }


    public MetadataElement getMetadataElement() {
        return metadataElement;
    }

    private L1cMetadata(InputStream stream, File file, String parent, String granuleName, S2Config config, String epsg) throws JDOMException, JAXBException, FileNotFoundException {
        super(config, L1cMetadataProc.getJaxbContext(), PSD_STRING);

        try {
            Object userProduct = updateAndUnmarshal(stream);

            if(userProduct instanceof Level1C_User_Product)
            {
                initProduct(stream, file, parent, granuleName, userProduct, epsg);
            }
            else
            {
                initTile(stream, file, parent, userProduct);
            }
        } catch (UnmarshalException|JDOMException e) {
            logger.severe(String.format("Product is not conform to PSD: %s", e.getMessage()));
            throw e;
        } catch (JAXBException | IOException e) {
            logger.severe(Utils.getStackTrace(e));
        }
    }

    private void initProduct(InputStream stream, File file, String parent, String granuleName, Object casted, String epsg) throws IOException, JAXBException, JDOMException {
        Level1C_User_Product product = (Level1C_User_Product) casted;
        productCharacteristics = L1cMetadataProc.getProductOrganization(product);

        Collection<String> tileNames;

        if(granuleName == null ) {
            tileNames = L1cMetadataProc.getTiles(product);
        } else {
            tileNames = Collections.singletonList(granuleName);
        }
        List<File> fullTileNamesList = new ArrayList<>();


        // todo populate allTileLists
        allTileLists = new HashMap<>();

        tileList = new ArrayList<>();

        for (String tileName : tileNames) {
            FileInputStream fi = (FileInputStream) stream;
            File nestedMetadata = new File(parent, "GRANULE" + File.separator + tileName);

            S2L1CGranuleDirFilename aGranuleDir = S2L1CGranuleDirFilename.create(tileName);

            if(aGranuleDir != null) {
                String theName = aGranuleDir.getMetadataFilename().name;

                File nestedGranuleMetadata = new File(parent, "GRANULE" + File.separator + tileName + File.separator + theName);
                if (nestedGranuleMetadata.exists()) {
                    fullTileNamesList.add(nestedGranuleMetadata);
                } else {
                    String errorMessage = "Corrupted product: the file for the granule " + tileName + " is missing";
                    logger.log(Level.WARNING, errorMessage);
                }
            }
        }

        Map<String, Counter> counters = new HashMap<>();

        for (File aGranuleMetadataFile : fullTileNamesList) {
            FileInputStream granuleStream = new FileInputStream(aGranuleMetadataFile);
            Level1C_Tile aTile = (Level1C_Tile) updateAndUnmarshal(granuleStream);
            Map<Integer, TileGeometry> geoms = L1cMetadataProc.getTileGeometries(aTile);

            Tile tile = new Tile(aTile.getGeneral_Info().getTILE_ID().getValue());
            tile.setHorizontalCsCode(aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_CODE());
            tile.setHorizontalCsName(aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_NAME());

            if (! tile.getHorizontalCsCode().equals(epsg)) {
                // skip tiles that are not in the desired UTM zone
                logger.info(String.format("Skipping tile %s because it has crs %s instead of requested %s", aGranuleMetadataFile.getName(), tile.getHorizontalCsCode(), epsg));
                continue;
            }

            // counting tiles by UTM zones
            String key = tile.getHorizontalCsCode();
            if (counters.containsKey(key)) {
                counters.get(key).increment();
            } else {
                counters.put(key, new Counter(key));
                counters.get(key).increment();
            }

            tile.setTileGeometry10M(geoms.get(S2SpatialResolution.R10M.resolution));
            tile.setTileGeometry20M(geoms.get(S2SpatialResolution.R20M.resolution));
            tile.setTileGeometry60M(geoms.get(S2SpatialResolution.R60M.resolution));

            tile.setSunAnglesGrid(L1cMetadataProc.getSunGrid(aTile));
            tile.setViewingIncidenceAnglesGrids(L1cMetadataProc.getAnglesGrid(aTile));

            tile.setMaskFilenames(L1cMetadataProc.getMasks(aTile, aGranuleMetadataFile));

            tileList.add(tile);
        }


        for(String key: counters.keySet())
        {
            List<L1cMetadata.Tile> aUTMZone = tileList.stream().filter(i -> i.getHorizontalCsCode().equals(key)).collect(Collectors.toList());
            allTileLists.put(key, aUTMZone);
        }

        // if it's a multi-UTM product, we create the product using only the main UTM zone (the one with more tiles)
        // todo make sure that tileList and allTileLists.get(maximus) are the same before removing this code

        //if (counters.values().size() > 1) {
        //    Counter maximus = Collections.max(counters.values());
        //    logger.info(String.format("There are %d UTM zones in this product, the main zone is [%s]", counters.size(), maximus.getName()));
        //    tileList = tileList.stream().filter(i -> i.horizontalCsCode.equals(maximus.getName())).collect(Collectors.toList());
        //}

        S2DatastripFilename stripName = L1cMetadataProc.getDatastrip(product);
        S2DatastripDirFilename dirStripName = L1cMetadataProc.getDatastripDir(product);

        File dataStripMetadata = new File(parent, "DATASTRIP" + File.separator + dirStripName.name + File.separator + stripName.name);

        metadataElement = new MetadataElement("root");
        MetadataElement userProduct = parseAll(new SAXBuilder().build(file).getRootElement());
        MetadataElement dataStrip = parseAll(new SAXBuilder().build(dataStripMetadata).getRootElement());
        metadataElement.addElement(userProduct);
        metadataElement.addElement(dataStrip);
        MetadataElement granulesMetaData = new MetadataElement("Granules");

        for (File aGranuleMetadataFile : fullTileNamesList) {
            MetadataElement aGranule = parseAll(new SAXBuilder().build(aGranuleMetadataFile).getRootElement());
            granulesMetaData.addElement(aGranule);
        }

        metadataElement.addElement(granulesMetaData);
    }

    private void initTile(InputStream stream, File file, String parent, Object casted) throws IOException, JAXBException, JDOMException {
        Level1C_Tile product = (Level1C_Tile) casted;
        productCharacteristics = new L1cMetadata.ProductCharacteristics();

        tileList = new ArrayList<>();
        allTileLists = new HashMap<>();

        // todo move this code to a common function
        {
            Map<Integer, TileGeometry> geoms = L1cMetadataProc.getTileGeometries(product);

            Tile tile = new Tile(product.getGeneral_Info().getTILE_ID().getValue());
            tile.setHorizontalCsCode(product.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_CODE());
            tile.setHorizontalCsName(product.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_NAME());

            tile.setTileGeometry10M(geoms.get(10));
            tile.setTileGeometry20M(geoms.get(20));
            tile.setTileGeometry60M(geoms.get(60));

            tile.setSunAnglesGrid(L1cMetadataProc.getSunGrid(product));
            tile.setViewingIncidenceAnglesGrids(L1cMetadataProc.getAnglesGrid(product));

            L1cMetadataProc.getMasks(product, file);
            tile.setMaskFilenames(L1cMetadataProc.getMasks(product, file));

            tileList.add(tile);

            allTileLists.put(tile.getHorizontalCsCode(), tileList);
        }
    }
}