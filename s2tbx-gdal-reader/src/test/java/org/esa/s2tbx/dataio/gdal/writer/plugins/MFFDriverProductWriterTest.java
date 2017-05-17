package org.esa.s2tbx.dataio.gdal.writer.plugins;

import org.esa.s2tbx.dataio.gdal.reader.plugins.MFFDriverProductReaderPlugIn;

/**
 * @author Jean Coravu
 */
public class MFFDriverProductWriterTest extends AbstractTestDriverProductWriter {

    public MFFDriverProductWriterTest() {
        super("MFF", ".hdr", "Byte UInt16 Float32 CInt16 CFloat32", new MFFDriverProductReaderPlugIn(), new MFFDriverProductWriterPlugIn());
    }
}