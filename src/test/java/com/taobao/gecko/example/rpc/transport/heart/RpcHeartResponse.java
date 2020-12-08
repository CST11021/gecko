package com.taobao.gecko.example.rpc.transport.heart;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.example.rpc.command.RpcResponse;

/**
 * @Author: wanghz
 * @Date: 2020/12/8 4:27 PM
 */
public class RpcHeartResponse extends RpcResponse implements BooleanAckCommand {

    public RpcHeartResponse(final Integer opaque, final ResponseStatus responseStatus, final String errorMsg) {
        super(opaque, responseStatus, null);
        this.setErrorMsg(errorMsg);
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

}
