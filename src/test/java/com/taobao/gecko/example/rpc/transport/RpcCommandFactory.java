package com.taobao.gecko.example.rpc.transport;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.example.rpc.transport.heart.RpcHeartRequest;
import com.taobao.gecko.example.rpc.transport.heart.RpcHeartResponse;

/**
 * @Author: wanghz
 * @Date: 2020/12/1 2:09 PM
 */
public class RpcCommandFactory implements CommandFactory {

    @Override
    public HeartBeatRequestCommand createHeartBeatCommand() {
        return new RpcHeartRequest();
    }

    @Override
    public BooleanAckCommand createBooleanAckCommand(final CommandHeader request, final ResponseStatus responseStatus, final String errorMsg) {
        return new RpcHeartResponse(request.getOpaque(), responseStatus, errorMsg);
    }

}
