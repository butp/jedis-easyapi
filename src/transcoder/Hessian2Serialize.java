package transcoder;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.qlc.common.util.log.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Hessian2Serialize implements ISerialize {
    @Override
    public byte[] encode(Object o) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Hessian2Output h2o = new Hessian2Output(os);
        try {
            h2o.writeObject(o);
            h2o.flush();
        } catch (IOException e) {
            LogUtil.error(e);
        }
        byte[] buffer = os.toByteArray();
        return buffer;
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> type) {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Hessian2Input h2i = new Hessian2Input(is);
        T o = null;
        try {
            o = type == null ? (T) h2i.readObject() : (T) h2i.readObject(type);
        } catch (IOException e) {
            LogUtil.error(e);
        }
        return o;
    }
}
