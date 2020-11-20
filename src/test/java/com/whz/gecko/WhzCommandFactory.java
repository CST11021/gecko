package com.whz.gecko;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.whz.gecko.client.WhzRequest;
import com.whz.gecko.server.WhzResponse;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 11:06 AM
 */
public class WhzCommandFactory implements CommandFactory {

    /**
     * 心跳检测的的请求对象
     *
     * @return
     */
    public HeartBeatRequestCommand createHeartBeatCommand() {
        // return new WhzHeartBeatRequestCommand();

        return new WhzRequest("心跳测试。。。");
    }

    /**
     * 正常ACK请求的对象
     *
     * @param request        请求头
     * @param responseStatus 响应状态
     * @param errorMsg       错误信息
     * @return
     */
    public BooleanAckCommand createBooleanAckCommand(final CommandHeader request, final ResponseStatus responseStatus, final String errorMsg) {
        // final WhzBooleanAckCommand ack = new WhzBooleanAckCommand();
        // ack.setResponseStatus(responseStatus);
        // ack.setErrorMsg(errorMsg);
        // return ack;

        BooleanAckCommand ack = new WhzResponse(null, request.getOpaque(), responseStatus);
        ack.setErrorMsg(errorMsg);
        return ack;
    }

}
