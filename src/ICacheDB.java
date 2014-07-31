import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存操作接口
 * Created by yangxuehua on 2014/7/30
 */
public interface ICacheDB {
    public <T extends Serializable> T getObject(final String key, Class<T> type);

    public String getString(final String key);

    /**
     * @param key
     * @param expirePeriodInSecond 0和负数表示不过期
     * @param value
     * @return
     */
    public <T extends Serializable> boolean setObject(final String key, final int expirePeriodInSecond, final T value);

    public boolean setString(final String key, final int expirePeriodInSecond, final String value);

    public boolean setStringIfNotExist(final String key, final int expirePeriodInSecond, final String value);

    /**
     *
     * @param key
     * @param expirePeriodInSecond
     * @param value
     * @return default 0
     */
    public boolean incrDecrInit(final String key, final int expirePeriodInSecond, final long value);

    public long incrDecrGet(final String key);

    public long incrBy(final String key, final int step);

    public long decrBy(final String key, final int step);

    /**
     * @param key
     * @return
     */
    public boolean delete(final String key);

    /**
     * @param key
     * @return -1 key存在但未设置过期, -2 key不存在, 正数 所示秒后过期
     */
    public int ttl(final String key);

    public boolean expire(final String key, final int expirePeriodInSecond);

    /**
     * 一次向链接表的左端新增多个对象。效果等同于从左右到右依次拿list的对象进行lpush
     *
     * @param key
     * @param items
     * @return
     */
    public <T extends Serializable> int lpushObject(String key, T... items);

    /**
     * 一次向链接表的左端新增多个数据块。效果等同于从左右到右依次拿list的数据块进行lpush
     *
     * @param key
     * @param items
     * @return
     */
    public int lpushString(String key, String... items);

    /**
     * 一次向链接表的右端新增多个对象。效果等同于从左右到右依次拿list的对象进行rpush
     *
     * @param key
     * @param items
     * @return 操作完成后的list长度
     */
    public <T extends Serializable> int rpushObject(String key, T... items);

    /**
     * 一次向链接表的右端新增多个数据块。效果等同于从左右到右依次拿list的数据块进行rpush
     *
     * @param key
     * @param items
     * @return 操作完成后的list长度
     */
    public int rpushString(String key, String... items);

    /**
     * 在list的最左端以Object的方式，取出并删除一个item
     *
     * @param key
     * @return
     */
    public <T extends Serializable> T lpopObject(String key, Class<T> type);

    /**
     * 在list的最左端以String的方式，取出并删除一个item
     *
     * @param key
     * @return
     */
    public String lpopString(String key);

    /**
     * 在list的最右端以Object的方式，取出并删除一个item
     *
     * @param key
     * @return
     */
    public <T extends Serializable> T rpopObject(String key, Class<T> type);

    /**
     * 在list的最右端以String的方式，取出并删除一个item
     *
     * @param key
     * @return
     */
    public String rpopString(String key);

    /**
     * 以Object的方式取出list的某位置区间上的items
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public <T extends Serializable> List<T> lrangeObject(String key, int start, int end, Class<T> type);

    /**
     * 以String的方式取出list的某位置区间上的items
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public List<String> lrangeString(String key, int start, int end);

    /**
     * 对list在服务端进行截取，范围之外部分将被服务端永久丢弃
     *
     * @param key
     * @param start
     * @param end
     * @return 截取成功返回true；如果指定范围超出list的实际范围，返回false
     */
    public boolean ltrim(String key, int start, int end);

    /**
     * 获取某list的长度
     *
     * @param key
     * @return
     */
    public int llen(String key);

    /**
     * 增加或更新hashmap的某一数据项（Object）。<br/>不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     *
     * @param key
     * @param field
     * @param value
     * @return
     */
    public <T extends Serializable> boolean hSetObject(String key, String field, T value);

    /**
     * 增加或更新hashmap的某一数据项。<br/>不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     *
     * @param key
     * @param field
     * @param value
     * @return 0 成功更新数据项，1成功新数据项，-1操作失败
     */
    public boolean hSetString(String key, String field, String value);

    /**
     * 同时更新某hashmap下的若干个数据项（Object）。<br/>不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     *
     * @param key
     * @param fieldValues
     * @return
     */
    public <T extends Serializable> boolean hMultiSetObject(String key, Map<String, T> fieldValues);

    /**
     * 同时更新某hasmap下的若干个数据项（byte[]数据块）。<br/>不可针对某数据项单独设置过期时间，可对整个hashmap使用expire(key,time)设置过期时间
     *
     * @param key
     * @param fieldValues
     * @return
     */
    public boolean hMultiSetString(String key, Map<String, String> fieldValues);

    /**
     * 以Object方式取出某hashmap下的某数据项
     *
     * @param key
     * @param field
     * @return
     */
    public <T extends Serializable> T hGetObject(String key, String field, Class<T> type);

    /**
     * 以数据块的方式 取出某hashmap下的某数据项
     *
     * @param key
     * @param field
     * @return
     */
    public String hGetString(String key, String field);

    /**
     * 以Object的方式 取出某hashmap下的所有数据项
     *
     * @param key
     * @return
     */
    public <T extends Serializable> Map<String, T> hGetAllObject(String key, Class<T> type);

    /**
     * 以String的方式 取出某hashmap下的所有数据项
     *
     * @param key
     * @return
     */
    public Map<String, String> hGetAllString(String key);

    /**
     * 以Object的方式 取出某hashmap下的多个数据项
     *
     * @param key
     * @param fields
     * @return
     */
    public <T extends Serializable> Map<String, T> hMultiGetObject(String key, Class<T> type, String... fields);

    /**
     * 以String的方式 取出某hashmap下的多个数据项
     *
     * @param key
     * @param fields
     * @return
     */
    public Map<String, String> hMultiGetString(String key, String... fields);

    /**
     * 删除某hashmap的某项
     *
     * @param key
     * @param field
     * @return 成功删除的field数
     */
    public int hDelete(String key, String... field);

    /**
     * 获取某hashmap中的items数
     *
     * @param key
     * @return
     */
    public int hLen(String key);

    /**
     * 获取某hashmap下的所有key
     *
     * @param key
     * @return
     */
    public Set<String> hKeys(String key);

    /**
     * 判断某hashmap下是否包含某field
     *
     * @param key
     * @param field
     * @return
     */
    public boolean hExists(String key, String field);

}
