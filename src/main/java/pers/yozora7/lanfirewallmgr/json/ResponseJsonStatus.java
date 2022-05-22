package pers.yozora7.lanfirewallmgr.json;

public enum ResponseJsonStatus {

    // 操作成功
    SUCCESS(200, "操作成功"),
    // 操作失败
    FAILURE(50000, "操作失败"),
    // 请求错误
    REQUEST_ERROR(400, "请求错误"),
    // 服务端程序错误
    INTERNAL_SERVER_ERROR(500, "服务端程序错误");

    // 状态码
    private int status;
    // 信息描述
    private String message;

    ResponseJsonStatus () {}
    ResponseJsonStatus (int status, String message) {
        this.status = status;
        this.message = message;
    }
    public int getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
}
