package com.whz.gecko;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.heartbeat.BooleanAckCommand;
import com.taobao.gecko.core.command.heartbeat.HeartBeatRequestCommand;
import com.whz.gecko.command.HeartResponse;
import com.whz.gecko.command.HeartRequest;

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
        return new HeartRequest();
    }

    /**
     * 心跳response对象
     *
     * @param request        请求头
     * @param responseStatus 响应状态
     * @param errorMsg       错误信息
     * @return
     */
    public BooleanAckCommand createBooleanAckCommand(final CommandHeader request, final ResponseStatus responseStatus, final String errorMsg) {
        return new HeartResponse(request.getOpaque(), responseStatus, errorMsg);
    }

}
