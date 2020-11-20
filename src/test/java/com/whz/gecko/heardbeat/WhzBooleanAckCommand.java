package com.whz.gecko.heardbeat;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.whz.gecko.WhzCommand;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 7:40 PM
 */
public class WhzBooleanAckCommand implements BooleanAckCommand, WhzCommand, Serializable {

    private static final long serialVersionUID = 4198714392288509518L;

    @Override
    public String getErrorMsg() {
        return null;
    }

    @Override
    public void setErrorMsg(String errorMsg) {

    }

    @Override
    public ResponseStatus getResponseStatus() {
        return null;
    }

    @Override
    public void setResponseStatus(ResponseStatus responseStatus) {

    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public InetSocketAddress getResponseHost() {
        return null;
    }

    @Override
    public void setResponseHost(InetSocketAddress address) {

    }

    @Override
    public long getResponseTime() {
        return 0;
    }

    @Override
    public void setResponseTime(long time) {

    }

    @Override
    public void setOpaque(Integer opaque) {

    }

    @Override
    public Integer getOpaque() {
        return null;
    }

    @Override
    public Object decode(IoBuffer buffer) {
        return heartBeat_response;
    }

    @Override
    public IoBuffer encode() {
        return heartBeat_response;
    }
}
