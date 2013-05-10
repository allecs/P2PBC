package protocol;

import java.io.Serializable;

/**
 * User: aolx
 * Date: 5/10/13
 * Time: 2:07 PM
 * Description:
 */
public class RequestHeader implements Serializable {
    public enum reqCode{
        SYN,
        RST
    }

    public reqCode req;
    public int seq;

    public RequestHeader(reqCode req, int seq) {
        this.req = req;
        this.seq = seq;
    }
}
