
package org.esa.s2tbx.dataio.s2.l1c.plugins;

import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProduct60MReaderPlugIn;

/**
 * Reader plugin for S2 MSI L1C over WGS84 / UTM Zone 50 N
 */
public class Sentinel2L1CProduct_60M_UTM50N_ReaderPlugIn extends Sentinel2L1CProduct60MReaderPlugIn {

    @Override
    public String getEPSG()
    {
        return "EPSG:32650";
    }

}