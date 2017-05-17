package org.esa.s2tbx.dataio.gdal.writer.plugins;

import org.esa.s2tbx.dataio.gdal.reader.plugins.SAGADriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class SAGADriverProductWriterTest extends AbstractTestDriverProductWriter {

    public SAGADriverProductWriterTest() {
        super("SAGA", ".sdat", "Byte Int16 UInt16 Int32 UInt32 Float32 Float64", new SAGADriverProductReaderPlugIn(), new SAGADriverProductWriterPlugIn());
    }
}