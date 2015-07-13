package org.esa.s2tbx.dataio.s2;


import https.psd_13_sentinel2_eo_esa_int.dico._1_0.pdgs.dimap.A_PRODUCT_INFO;
import https.psd_13_sentinel2_eo_esa_int.dico._1_0.pdgs.dimap.A_PRODUCT_ORGANIZATION;
import https.psd_13_sentinel2_eo_esa_int.psd.s2_pdi_level_1c_tile_metadata.Level1C_Tile;
import https.psd_13_sentinel2_eo_esa_int.psd.user_product_level_1c.Level1C_User_Product;
import junit.framework.Assert;
import org.esa.s2tbx.dataio.s2.l1c.filepaterns.S2L1CGranuleDirFilename;
import org.esa.s2tbx.dataio.s2.l1c.L1cMetadata;
import org.esa.s2tbx.dataio.s2.l1c.L1cMetadataProc;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by opicas-p on 24/06/2014.
 */
public class MetadataTest {


    /**
     * Test that if we have the (old) psd 12 root xml file, we can still unmarshall it after update
     */
    @Test
    public void testUpdatePSD12RootXML() {
        String psd12RootXmlFileName =
                "l1c/metadata/S2A_OPER_MTD_SAFL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025.xml";
        try {
            InputStream inputStream = getClass().getResourceAsStream(psd12RootXmlFileName);
            InputStream updatedInputStream = S2Metadata.changePSDIfRequired(inputStream, "13");

            JAXBContext jaxbContext = L1cMetadataProc.getJaxbContext();
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            Object unmarshalled =  unmarshaller.unmarshal(updatedInputStream);
            Object castedUnmarshalled = ((JAXBElement) unmarshalled).getValue();
            assertTrue(Level1C_User_Product.class.isInstance(castedUnmarshalled));

        } catch (FileNotFoundException e) {
            org.junit.Assert.fail("The file was not found: " + psd12RootXmlFileName);
            e.printStackTrace();
        } catch (IOException e) {
            org.junit.Assert.fail(e.getMessage());
            e.printStackTrace();
        } catch (JAXBException e) {
            org.junit.Assert.fail("Could not unmarshall PSD12 Root XML: " + e.getMessage());
        }
    }

    public Level1C_User_Product getUserProduct() throws Exception
    {
        Level1C_User_Product o = null;

        JAXBContext jaxbContext = JAXBContext
                .newInstance(S2MetadataType.L1C);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Marshaller marshaller = jaxbContext.createMarshaller();


        InputStream stream = getClass().getResourceAsStream("l1c/metadata/S2A_OPER_MTD_SAFL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025.xml");

        Object ob =  unmarshaller.unmarshal(stream);

        o = (Level1C_User_Product) ((JAXBElement)ob).getValue();

        return o;
    }

    public Level1C_Tile getTileProduct() throws Exception
    {
        Level1C_Tile o = null;

        JAXBContext jaxbContext = JAXBContext
                .newInstance(S2MetadataType.L1C);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Marshaller marshaller = jaxbContext.createMarshaller();


        InputStream stream = getClass().getResourceAsStream("l1c/metadata/S2A_OPER_MTD_L1C_TL_CGS1_20130621T120000_A000065_T14SLF.xml");

        Object ob =  unmarshaller.unmarshal(stream);

        o = (Level1C_Tile) ((JAXBElement)ob).getValue();

        return o;
    }



    @Test
    public void test1() throws Exception
    {
        Level1C_User_Product o = getUserProduct();

        Assert.assertNotNull(o);

        L1cMetadata.ProductCharacteristics pchar = L1cMetadataProc.parseCharacteristics(o);
        assertEquals("Sentinel-2A", pchar.getSpacecraft());
        assertEquals("2013-06-21T12:00:00Z", pchar.getDatasetProductionDate());
        assertEquals("LEVEL___1_C", pchar.getProcessingLevel());
        assertEquals(0.0000082, pchar.getBandInformations()[0].getSpectralResponseValues()[1], 1e-15);
    }

    @Test
    public void test2() throws Exception
    {
        Level1C_User_Product product = getUserProduct();

        Assert.assertNotNull(product);

        A_PRODUCT_INFO.Product_Organisation info = product.getGeneral_Info().getProduct_Info().getProduct_Organisation();

        A_PRODUCT_ORGANIZATION.Granules granulesList = info.getGranule_List().get(0).getGranules();
        String granuleId = granulesList.getGranuleIdentifier();

        assertEquals("S2A_OPER_MSI_L1C_TL_CGS1_20130621T120000_A000065_T14SLD_N01.01", granuleId);

        S2L1CGranuleDirFilename gdir = S2L1CGranuleDirFilename.create(granuleId);

        Assert.assertEquals("S2A_OPER_MTD_L1C_TL_CGS1_20130621T120000_A000065_T14SLD.xml", gdir.getMetadataFilename().name);
    }

    @Test
    public void test3() throws Exception
    {
        Level1C_User_Product product = getUserProduct();

        Assert.assertNotNull(product);

        Collection<String> tiles = L1cMetadataProc.getTiles(product);

        for (String granuleName: tiles)
        {
            System.err.println(granuleName);
        }

    }


    @Test
    public void testTileProductsMetadataExistence() throws Exception
    {
        Level1C_User_Product product = getUserProduct();

        Assert.assertNotNull(product);

        Collection<String> tiles = L1cMetadataProc.getTiles(product);

        URL aUrl = getClass().getResource(
                "l1c/data/S2A_OPER_PRD_MSIL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025.SAFE");

        if(aUrl != null)
        {
            File baseDir = new File(aUrl.toURI());
            if(baseDir.exists())
            {
                assertTrue(baseDir.exists());
                assertTrue(baseDir.isDirectory());

                for (String granuleName: tiles)
                {
                    File nestedMetadata = new File(baseDir, "GRANULE\\" + granuleName);
                    System.err.println(nestedMetadata.getAbsolutePath());
                    assertTrue(nestedMetadata.exists());
                    assertTrue(nestedMetadata.isDirectory());

                    S2L1CGranuleDirFilename aGranuleDir = S2L1CGranuleDirFilename.create(granuleName);
                    String theName = aGranuleDir.getMetadataFilename().name;

                    File nestedGranuleMetadata = new File(baseDir, "GRANULE\\" + granuleName + "\\" + theName);
                    assertTrue(nestedGranuleMetadata.exists());
                    assertTrue(nestedGranuleMetadata.isFile());
                }
            }
        }
    }


    @Test
    public void testPopulateTileInfo() throws Exception
    {
        Level1C_Tile product = getTileProduct();

        Assert.assertNotNull(product);

        L1cMetadataProc.getTileGeometries(product);
    }


    @Test
    public void testPopulateOtherTileInfo() throws Exception
    {
        Level1C_Tile product = getTileProduct();

        Assert.assertNotNull(product);

        L1cMetadataProc.getSunGrid(product);
        L1cMetadataProc.getAnglesGrid(product);
    }

}