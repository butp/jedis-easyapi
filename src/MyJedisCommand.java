import com.qlc.common.util.log.LogUtil;
import com.qlc.common.util.transcoder.ISerialize;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;
import transcoder.ISerialize;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

public abstract class MyJedisCommand<T, C extends JedisCommands & BinaryJedisCommands & Closeable> {

    private Pool<C> jedisPool;
    private ISerialize serialize;

    public MyJedisCommand(Pool<C> jedisPool, ISerialize serialize) {
        this.jedisPool = jedisPool;
        this.serialize = serialize;
    }

    public abstract T execute(C connection);

    public T run() {
        C connection = null;
        try {
            connection = jedisPool.getResource();
            return execute(connection);
        } catch (JedisException je) {
            releaseConnection(connection, true);
            connection = null;
            throw je;
        } finally {
            releaseConnection(connection, false);
        }

    }

    private void releaseConnection(C connection, boolean broken) {
        if (connection != null) {
//            if (broken) {
//                jedisPool.returnBrokenResource(connection);
//            } else {
//                jedisPool.returnResource(connection);
//            }
            try {
                connection.close();
            } catch (IOException e) {
                LogUtil.error(e);
            }
        }
    }

    protected void notNullAssert(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("not support the Object-type of Null");
        }
    }

    protected <T extends Serializable> byte[][] getBArrArrFromObjectArr(T... items) {
        byte[][] bArrArr = new byte[items.length][];
        for (int i = items.length - 1; i >= 0; i--) {
            bArrArr[i] = serialize.encode(items[i]);
        }
        return bArrArr;
    }

    protected byte[][] getBArrArrFromStrArr(String... thisKeys) {
        byte[][] bkeys = new byte[thisKeys.length][];
        for (int i = thisKeys.length - 1; i >= 0; i--) {
            bkeys[i] = SafeEncoder.encode(thisKeys[i]);
        }
        return bkeys;
    }

}