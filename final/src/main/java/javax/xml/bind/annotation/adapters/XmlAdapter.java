package javax.xml.bind.annotation.adapters;

/**
 * Partial Implementation For Java9 support if
 * {@code javax.xml.bind.annotation.adapters.XmlAdapter} is missing
 */
@Deprecated
@SuppressWarnings({"DeprecatedIsStillUsed"})
public abstract class XmlAdapter<To,From> {
    @Deprecated
    protected XmlAdapter() {}

    @Deprecated
    public abstract To marshal(From v) throws Exception;

    @Deprecated
    public abstract From unmarshal(To v) throws Exception;
}