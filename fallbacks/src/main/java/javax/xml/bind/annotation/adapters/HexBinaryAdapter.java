package javax.xml.bind.annotation.adapters;

import javax.xml.bind.DatatypeConverter;

/**
 * Partial Implementation For Java9 support if
 * {@code javax.xml.bind.annotation.adapters.HexBinaryAdapter} is missing
 */
@Deprecated
public class HexBinaryAdapter extends XmlAdapter<byte[], String> {
    @Override
    @Deprecated
    public String unmarshal(byte[] v) throws Exception {
        return DatatypeConverter.printHexBinary(v);
    }

    @Override
    @Deprecated
    public byte[] marshal(String v) throws Exception {
        return DatatypeConverter.parseHexBinary(v);
    }
}