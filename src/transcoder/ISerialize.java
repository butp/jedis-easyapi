package transcoder;

/**
 * User: tom
 * Time: 下午3:48
 */
public interface ISerialize {
    public byte[] encode(Object o);

    public <T> T decode(byte[] bytes, Class<T> type);
}
