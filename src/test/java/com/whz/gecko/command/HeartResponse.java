package com.whz.gecko.command;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 7:40 PM
 */
public class HeartResponse implements BooleanAckCommand, Serializable {

    private static final long serialVersionUID = 4198714392288509518L;

    private WhzResponse response;

    public HeartResponse(Integer opaque, ResponseStatus status, String errorMsg) {
        response = new WhzResponse("心跳测试", opaque);
        response.setResponseStatus(status);
        response.setErrorMsg(errorMsg);
    }

    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public Integer getOpaque() {
        return response.getOpaque();
    }
    @Override
    public void setOpaque(Integer opaque) {
        response.setOpaque(opaque);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return response.getResponseStatus();
    }
    @Override
    public void setResponseStatus(ResponseStatus responseStatus) {
        response.setResponseStatus(responseStatus);
    }

    @Override
    public InetSocketAddress getResponseHost() {
        return response.getResponseHost();
    }
    @Override
    public void setResponseHost(InetSocketAddress address) {
        response.setResponseHost(address);
    }

    @Override
    public long getResponseTime() {
        return response.getResponseTime();
    }
    @Override
    public void setResponseTime(long time) {
        response.setResponseTime(time);
    }

    @Override
    public String getErrorMsg() {
        return response.getErrorMsg();
    }
    @Override
    public void setErrorMsg(String errorMsg) {
        response.setErrorMsg(errorMsg);
    }
}
