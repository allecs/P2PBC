package protocol;

import java.io.Serializable;

/**
 * User: aolx
 * Date: 5/9/13
 * Time: 10:55 PM
 * Description:
 */
public class ResponseHeader implements Serializable {

    public enum retCode {
        SYN_ACK,
        RST,
        RECURSION
    }

    public retCode ret;
    public long seq;
    public String addr1;
    public String addr2;
    public int port1;
    public int port2;

    public ResponseHeader(retCode ret, long seq, String addr1, String addr2, int port1, int port2) {
        this.ret = ret;
        this.seq = seq;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.port1 = port1;
        this.port2 = port2;
    }
}
