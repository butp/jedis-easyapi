import com.qlc.common.util.log.MyLoggerFactory;
import com.qlc.common.util.transcoder.DefaultSerialize;
import com.qlc.common.util.transcoder.ISerialize;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;
import transcoder.DefaultSerialize;
import transcoder.ISerialize;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生成Redis操作的客户端实例（线程安全，且使用了连接池）。
 * 其有两种方式
 * 方式一： 通过RedisDBFactory的静态方法获取，此方式适应于任何Java应用。
 * e.g: ICacheDB redisDB = RedisDBFactory.getRedisDB(String ipPort, String passwd); redis客户端分片方式连接redis server
 * e.g: ICacheDB redisDB =  RedisDBFactory.getRedisClusterDB(String sentinelIpPorts, String masterName, String
 * passwd); redis服务端集群的客户端
 * <p>
 * 方式二： 通过Spring xml生成bean， 此方式仅适用于Spring框架. 此bean返回结果是一个ICacheDB对象
 * <bean class="RedisDBFactory">
 * <property name="redisIpPorts" value="${redisIpPorts}"/>
 * <property name="passwd" value="${passwd}"/>
 * </bean>
 * <p>
 * Created by yangxuehua on 2014/7/30.
 */
public class RedisDBFactory implements FactoryBean<ICacheDB>, InitializingBean {
    private static Logger logger = MyLoggerFactory.getLogger(RedisDBFactory.class);

    private static Map<String, JedisPoolDB> name2redisCache = new ConcurrentHashMap<String, JedisPoolDB>();

    private String redisIpPorts;//spring-iframe ioc注入的连接参数--redis地址
    private String passwd;//spring-iframe ioc注入的连接参数--redis密码
    private ISerialize serialize;//spring-iframe ioc注入的连接参数--自定义序列化（可不注入）
    private ICacheDB cacheDB;//spring-iframe bean返回对象

    /**
     * redis客户端分片方式连接redis server
     *
     * @param ipPorts ip:port,ip:port,ip:port
     * @param passwd
     * @return
     */
    public static JedisPoolDB getRedisDB(String ipPorts, String passwd) {
        if (ipPorts.trim().split("[^0-9a-zA-Z_\\-\\.:]+").length > 1) {
            return getRedisShardedDB(ipPorts, passwd, new DefaultSerialize());
        } else {
            return getRedisAloneDB(ipPorts, passwd, 0, new DefaultSerialize());
        }
    }

    /**
     * redise服务端集群
     *
     * @param sentinelIpPorts
     * @param masterName
     * @param passwd
     * @return
     */
    public static JedisPoolDB getRedisClusterDB(String sentinelIpPorts, String masterName, String passwd) {
        return getRedisClusterDB(sentinelIpPorts, masterName, passwd, new DefaultSerialize());
    }

    private static JedisPoolDB getRedisAloneDB(String ipPort, String passwd, int database, ISerialize serialize) {
        if (StringUtils.isEmpty(ipPort)) {
            return null;
        }
        if (serialize == null) {
            serialize = new DefaultSerialize();
        }
        String name = ipPort + passwd + database + serialize.getClass().getName();
        JedisPoolDB jedislDB = name2redisCache.get(name);
        if (jedislDB == null) {
            synchronized (RedisDBFactory.class) {
                jedislDB = name2redisCache.get(name);
                if (jedislDB == null) {
                    if (passwd != null && passwd.length() == 0) {
                        passwd = null;
                    }
                    String ip = ipPort.split(":")[0];
                    int port = Integer.parseInt(ipPort.split(":")[1]);
                    JedisPool jedisPool = new JedisPool(getPoolConf(), ip, port, 2000, passwd, database);//设置超时时间为2秒
                    jedislDB = new JedisPoolDB(jedisPool, serialize);
                    name2redisCache.put(name, jedislDB);
                }
            }
        }
        return jedislDB;
    }

    private static JedisPoolDB getRedisShardedDB(String ipPorts, String passwd, ISerialize serialize) {
        if (StringUtils.isEmpty(ipPorts)) {
            throw new IllegalArgumentException("ipPorts is illegal, please set value like 'ip:port,ip:port'");
        }
        if (serialize == null) {
            serialize = new DefaultSerialize();
        }
        String name = ipPorts + passwd + serialize.getClass().getName();
        JedisPoolDB jedislDB = name2redisCache.get(name);
        if (jedislDB == null) {
            synchronized (RedisDBFactory.class) {
                jedislDB = name2redisCache.get(name);
                if (jedislDB == null) {
                    if (passwd != null && passwd.length() == 0) {
                        passwd = null;
                    }
                    List<JedisShardInfo> shards = new ArrayList<>();
                    for (String ipPort : ipPorts.trim().split("[^0-9a-zA-Z_\\-\\.:]+")) {
                        JedisShardInfo shardInfo = new JedisShardInfo(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]), 2000);
                        shardInfo.setPassword(passwd);
                        shards.add(shardInfo);
                    }
                    ShardedJedisPool shardedJedisPool = new ShardedJedisPool(getPoolConf(), shards);
                    jedislDB = new JedisPoolDB(shardedJedisPool, serialize);
                    name2redisCache.put(name, jedislDB);
                }
            }
        }
        return jedislDB;
    }

    private static JedisPoolDB getRedisClusterDB(String sentinelIpPorts, String masterName, String passwd, ISerialize serialize) {
        if (StringUtils.isEmpty(sentinelIpPorts) || StringUtils.isEmpty(masterName)) {
            throw new IllegalArgumentException("sentinelIpPorts is illegal(please set value like 'ip:port,ip:port'), or masterName is empty");
        }
        String name = sentinelIpPorts + masterName + passwd + serialize.getClass().getName();
        JedisPoolDB jedislDB = name2redisCache.get(name);
        if (jedislDB == null) {
            synchronized (RedisDBFactory.class) {
                jedislDB = name2redisCache.get(name);
                if (jedislDB == null) {
                    Set<String> sentinels = new HashSet<>();
                    for (String sentinel : sentinelIpPorts.trim().split("[^0-9a-zA-Z_\\-\\.:]+")) {
                        if (sentinel.length() > 0) {
                            sentinels.add(sentinel);
                        }
                    }
                    JedisSentinelPool jedisSentinelPool = new JedisSentinelPool(masterName, sentinels, getPoolConf(), passwd);
                    jedislDB = new JedisPoolDB(jedisSentinelPool, serialize);
                    name2redisCache.put(name, jedislDB);
                }
            }
        }
        return jedislDB;
    }

    private static GenericObjectPoolConfig getPoolConf() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(10);
        config.setMinIdle(1);
        config.setMaxTotal(500);
        config.setMaxWaitMillis(2000);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRunsMillis(60000);
        return config;
    }

    public void setRedisIpPorts(String redisIpPorts) {
        this.redisIpPorts = redisIpPorts;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public void setSerialize(ISerialize serialize) {
        this.serialize = serialize;
    }

    @Override
    public ICacheDB getObject() throws Exception {
        return cacheDB;
    }

    @Override
    public Class<ICacheDB> getObjectType() {
        return ICacheDB.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (redisIpPorts == null || redisIpPorts.trim().length() <= 10) {
            throw new IllegalArgumentException("redisIpPorts 设置不正确：" + redisIpPorts);
        }
        if (redisIpPorts.trim().split("[^0-9a-zA-Z_\\-\\.:]+").length > 1) {
            if (serialize == null) {
                cacheDB = getRedisShardedDB(redisIpPorts, passwd, new DefaultSerialize());
            } else {
                cacheDB = getRedisShardedDB(redisIpPorts, passwd, serialize);
            }
        } else {
            if (serialize == null) {
                cacheDB = getRedisAloneDB(redisIpPorts, passwd, 0, new DefaultSerialize());
            } else {
                cacheDB = getRedisAloneDB(redisIpPorts, passwd, 0, serialize);
            }
        }
    }
}
