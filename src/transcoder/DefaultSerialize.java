package transcoder;

import com.alibaba.fastjson.JSON;
import redis.clients.util.SafeEncoder;

/**
 * User: yangxuehua
 * Time: 下午2:25
 * 对Object类型的序列化与反序列化工具类。
 */
public class DefaultSerialize implements ISerialize {
    @Override
    public byte[] encode(Object o) {
        if (o == null) return null;
        if (o instanceof String) {
            return SafeEncoder.encode((String) o);
        } else {
            String str = JSON.toJSONString(o);
            return SafeEncoder.encode(str);
        }
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> type) {
        if (bytes == null) {
            return null;
        }
        String str = SafeEncoder.encode(bytes);
        if (type == String.class) {
            return (T) str;
        } else {
            return JSON.parseObject(str, type);
        }
    }

}
