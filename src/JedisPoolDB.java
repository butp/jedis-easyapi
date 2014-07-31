import com.qlc.common.util.transcoder.DefaultSerialize;
import com.qlc.common.util.transcoder.ISerialize;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.JedisCommands;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;
import transcoder.DefaultSerialize;
import transcoder.ISerialize;

import java.io.Closeable;
import java.io.Serializable;
import java.util.*;

/**
 * 基于Jedis线程池实现的ICacheDB
 * Created by yangxuehua on 2014/7/30.
 */
public class JedisPoolDB<C extends JedisCommands & BinaryJedisCommands & Closeable> implements ICacheDB {
    private Pool<C> jedisPool;
    private ISerialize serialize;


    public JedisPoolDB(Pool<C> jedisPool, ISerialize serialize) {
        if (jedisPool == null) {
            throw new IllegalArgumentException("jedisPool can't be NULL");
        }
        this.jedisPool = jedisPool;
        if (serialize == null) {
            this.serialize = new DefaultSerialize();
        } else {
            this.serialize = serialize;
        }
    }

    @Override
    public <T extends Serializable> T getObject(final String key, final Class<T> type) {
        return new MyJedisCommand<T, C>(jedisPool, serialize) {
            @Override
            public T execute(C connection) {
                notNullAssert(key);
                byte[] value = connection.get(SafeEncoder.encode(key));
                if (value != null) {
                    return serialize.decode(value, type);
                }
                return null;
            }
        }.run();
    }

    @Override
    public String getString(final String key) {
        return new MyJedisCommand<String, C>(jedisPool, serialize) {
            @Override
            public String execute(C connection) {
                notNullAssert(key);
                return connection.get(key);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> boolean setObject(final String key, final int expirePeriodInSecond, final T value) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(value);
                String ret;
                if (expirePeriodInSecond <= 0) {
                    ret = connection.set(SafeEncoder.encode(key), serialize.encode(value));
                } else {
                    ret = connection.setex(SafeEncoder.encode(key), expirePeriodInSecond, serialize.encode(value));
                }
                return "OK".equals(ret);
            }
        }.run();
    }

    @Override
    public boolean setString(final String key, final int expirePeriodInSecond, final String value) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(value);
                String ret;
                if (expirePeriodInSecond <= 0) {
                    ret = connection.set(key, value);
                } else {
                    ret = connection.setex(key, expirePeriodInSecond, value);
                }
                return "OK".equals(ret);
            }
        }.run();
    }

    @Override
    public boolean setStringIfNotExist(String key, int expirePeriodInSecond, String value) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(value);
                Long ret;
                ret = connection.setnx(key, value);
                if (expirePeriodInSecond > 0) {
                    connection.expire(key, expirePeriodInSecond);
                }
                return new Long(1).equals(ret);
            }
        }.run();
    }

    @Override
    public boolean incrDecrInit(String key, int expirePeriodInSecond, long value) {
        return setString(key, expirePeriodInSecond, String.valueOf(value));
    }

    @Override
    public long incrDecrGet(String key) {
        String str = getString(key);
        if(str!=null) {
            try {
                return Long.parseLong(str);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    @Override
    public long incrBy(String key, int step) {
        return new MyJedisCommand<Long, C>(jedisPool, serialize) {
            @Override
            public Long execute(C connection) {
                notNullAssert(key);
                Long ret;
                ret = connection.incrBy(key, step);
                return ret;
            }
        }.run();
    }

    @Override
    public long decrBy(String key, int step) {
        return new MyJedisCommand<Long, C>(jedisPool, serialize) {
            @Override
            public Long execute(C connection) {
                notNullAssert(key);
                Long ret;
                ret = connection.decrBy(key, step);
                return ret;
            }
        }.run();
    }

    @Override
    public boolean delete(final String key) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                Long ret = connection.del(key);
                return ret != null && ret > 0;
            }
        }.run();
    }

    @Override
    public int ttl(final String key) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {

            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.ttl(key);
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public boolean expire(final String key, final int expirePeriodInSecond) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {

            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                Long ret = connection.expire(key, expirePeriodInSecond);
                return ret != null && ret > 0;
            }
        }.run();
    }

    @Override
    public <T extends Serializable> int lpushObject(final String key, final T... items) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.lpush(SafeEncoder.encode(key), getBArrArrFromObjectArr(items));
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public int lpushString(final String key, final String... items) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.lpush(key, items);
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public <T extends Serializable> int rpushObject(final String key, final T... items) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.rpush(SafeEncoder.encode(key), getBArrArrFromObjectArr(items));
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public int rpushString(final String key, final String... items) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.lpush(key, items);
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public <T extends Serializable> T lpopObject(final String key, final Class<T> type) {
        return new MyJedisCommand<T, C>(jedisPool, serialize) {
            @Override
            public T execute(C connection) {
                notNullAssert(key);
                byte[] b = connection.lpop(SafeEncoder.encode(key));
                if (b != null) {
                    return serialize.decode(b, type);
                } else {
                    return null;
                }
            }
        }.run();
    }

    @Override
    public String lpopString(final String key) {
        return new MyJedisCommand<String, C>(jedisPool, serialize) {
            @Override
            public String execute(C connection) {
                notNullAssert(key);
                return connection.lpop(key);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> T rpopObject(final String key, final Class<T> type) {
        return new MyJedisCommand<T, C>(jedisPool, serialize) {
            @Override
            public T execute(C connection) {
                notNullAssert(key);
                byte[] b = connection.rpop(SafeEncoder.encode(key));
                if (b != null) {
                    return serialize.decode(b, type);
                } else {
                    return null;
                }
            }
        }.run();
    }

    @Override
    public String rpopString(final String key) {
        return new MyJedisCommand<String, C>(jedisPool, serialize) {
            @Override
            public String execute(C connection) {
                notNullAssert(key);
                return connection.rpop(key);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> List<T> lrangeObject(final String key, final int start, final int end, final Class<T> type) {
        return new MyJedisCommand<List<T>, C>(jedisPool, serialize) {
            @Override
            public List<T> execute(C connection) {
                notNullAssert(key);
                List<byte[]> temp = connection.lrange(SafeEncoder.encode(key), start, end);
                List<T> ret = new ArrayList<T>(temp.size());
                for (byte[] b : temp) {
                    ret.add(serialize.<T>decode(b, type));
                }
                return ret;
            }
        }.run();
    }

    @Override
    public List<String> lrangeString(final String key, final int start, final int end) {
        return new MyJedisCommand<List<String>, C>(jedisPool, serialize) {
            @Override
            public List<String> execute(C connection) {
                notNullAssert(key);
                return connection.lrange(key, start, end);
            }
        }.run();
    }

    @Override
    public boolean ltrim(final String key, final int start, final int end) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                String ret = connection.ltrim(key, start, end);
                return "OK".equals(ret);
            }
        }.run();
    }

    @Override
    public int llen(final String key) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                Long ret = connection.llen(key);
                return ret.intValue();
            }
        }.run();
    }

    @Override
    public <T extends Serializable> boolean hSetObject(final String key, final String field, final T value) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(field);
                notNullAssert(value);
                Long ret = connection.hset(SafeEncoder.encode(key), SafeEncoder.encode(field), serialize.encode(value));
                return ret != null && ret >= 0;
            }
        }.run();
    }

    @Override
    public boolean hSetString(final String key, final String field, final String value) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(field);
                notNullAssert(value);
                Long ret = connection.hset(key, field, value);
                return ret != null && ret >= 0;
            }
        }.run();
    }

    @Override
    public <T extends Serializable> boolean hMultiSetObject(final String key, final Map<String, T> fieldValues) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(fieldValues);
                Map<byte[], byte[]> fieldValuesB = new HashMap<byte[], byte[]>(fieldValues.size());
                for (String filed : fieldValues.keySet()) {
                    fieldValuesB.put(SafeEncoder.encode(filed), serialize.encode(fieldValues.get(filed)));
                }
                String ret = connection.hmset(SafeEncoder.encode(key), fieldValuesB);
                return "OK".equals(ret);
            }
        }.run();
    }

    @Override
    public boolean hMultiSetString(final String key, final Map<String, String> fieldValues) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(fieldValues);
                String ret = connection.hmset(key, fieldValues);
                return "OK".equals(ret);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> T hGetObject(final String key, final String field, final Class<T> type) {
        return new MyJedisCommand<T, C>(jedisPool, serialize) {
            @Override
            public T execute(C connection) {
                notNullAssert(key);
                notNullAssert(field);
                byte[] b = connection.hget(SafeEncoder.encode(key), SafeEncoder.encode(field));
                if (b != null) {
                    return serialize.decode(b, type);
                } else {
                    return null;
                }
            }
        }.run();
    }

    @Override
    public String hGetString(final String key, final String field) {
        return new MyJedisCommand<String, C>(jedisPool, serialize) {
            @Override
            public String execute(C connection) {
                notNullAssert(key);
                notNullAssert(field);
                return connection.hget(key, field);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> Map<String, T> hGetAllObject(final String key, final Class<T> type) {
        return new MyJedisCommand<Map<String, T>, C>(jedisPool, serialize) {
            @Override
            public Map<String, T> execute(C connection) {
                notNullAssert(key);
                Map<byte[], byte[]> temp = connection.hgetAll(SafeEncoder.encode(key));
                Map<String, T> ret = new HashMap<String, T>(temp.size());
                if (temp != null) {
                    for (byte[] field : temp.keySet()) {
                        byte[] value = temp.get(field);
                        if (value != null) {
                            ret.put(SafeEncoder.encode(field), (T) serialize.decode(value, type));
                        }
                    }
                }
                return ret;
            }
        }.run();
    }

    @Override
    public Map<String, String> hGetAllString(final String key) {
        return new MyJedisCommand<Map<String, String>, C>(jedisPool, serialize) {
            @Override
            public Map<String, String> execute(C connection) {
                notNullAssert(key);
                return connection.hgetAll(key);
            }
        }.run();
    }

    @Override
    public <T extends Serializable> Map<String, T> hMultiGetObject(final String key, final Class<T> type, final String... fields) {
        return new MyJedisCommand<Map<String, T>, C>(jedisPool, serialize) {
            @Override
            public Map<String, T> execute(C connection) {
                notNullAssert(key);
                notNullAssert(fields);
                List<byte[]> values = connection.hmget(SafeEncoder.encode(key), getBArrArrFromStrArr(fields));
                Map<String, T> ret = new HashMap<String, T>(values.size());
                for (int i = fields.length - 1; i >= 0; i--) {
                    if (values.size() > i && values.get(i) != null) {
                        ret.put(fields[i], (T) serialize.decode(values.get(i), type));
                    }
                }
                return ret;
            }
        }.run();
    }

    @Override
    public Map<String, String> hMultiGetString(final String key, final String... fields) {
        return new MyJedisCommand<Map<String, String>, C>(jedisPool, serialize) {
            @Override
            public Map<String, String> execute(C connection) {
                notNullAssert(key);
                notNullAssert(fields);
                List<String> values = connection.hmget(key, fields);
                Map<String, String> ret = new HashMap<String, String>(values.size());
                for (int i = fields.length - 1; i >= 0; i--) {
                    if (values.size() > i) {
                        ret.put(fields[i], values.get(i));
                    }
                }
                return ret;
            }
        }.run();
    }

    @Override
    public int hDelete(final String key, final String... fields) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                notNullAssert(fields);
                return connection.hdel(key, fields).intValue();
            }
        }.run();
    }

    @Override
    public int hLen(final String key) {
        return new MyJedisCommand<Integer, C>(jedisPool, serialize) {
            @Override
            public Integer execute(C connection) {
                notNullAssert(key);
                return connection.hlen(key).intValue();
            }
        }.run();
    }

    @Override
    public Set<String> hKeys(final String key) {
        return new MyJedisCommand<Set<String>, C>(jedisPool, serialize) {
            @Override
            public Set<String> execute(C connection) {
                notNullAssert(key);
                return connection.hkeys(key);
            }
        }.run();
    }

    @Override
    public boolean hExists(final String key, final String field) {
        return new MyJedisCommand<Boolean, C>(jedisPool, serialize) {
            @Override
            public Boolean execute(C connection) {
                notNullAssert(key);
                notNullAssert(field);
                return connection.hexists(key, field);
            }
        }.run();
    }
}
