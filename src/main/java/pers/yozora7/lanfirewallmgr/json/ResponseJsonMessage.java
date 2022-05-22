package pers.yozora7.lanfirewallmgr.json;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResponseJsonMessage {
//    private static final long serialVersionUID = -7127875856370230011L;
    // 状态码
    private int status = 20000;
    // 消息描述
    private String message;
    // 数据
    private Object data;

    public ResponseJsonMessage () {}
    public ResponseJsonMessage (int status, String message) {
        this.status = status;
        this.message = message;
    }
    public ResponseJsonMessage (int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
