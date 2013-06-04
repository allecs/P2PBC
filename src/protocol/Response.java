package protocol;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * User: aolx
 * Date: 5/9/13
 * Time: 10:55 PM
 * Description:
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 42L;

    public enum retCode {
        SYN_ACK,
        RST,
        RECURSION
    }

    public retCode ret;
    public long seq;
    public InetSocketAddress[] children;

    public Response(retCode ret, long seq, InetSocketAddress[] children) {
        this.ret = ret;
        this.seq = seq;
        this.children = children;
    }
}
