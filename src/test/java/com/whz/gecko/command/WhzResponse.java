package com.whz.gecko.command;

import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:55 PM
 */
public class WhzResponse implements ResponseCommand, BooleanAckCommand, Serializable {

    private static final long serialVersionUID = -4601915156778195626L;

    private String result;

    private Integer opaque;
    private InetSocketAddress responseHost;
    private ResponseStatus responseStatus = ResponseStatus.NO_ERROR;
    private long responseTime;
    private String errorMsg;


    public WhzResponse(String result, Integer opaque) {
        this.result = result;
        this.opaque = opaque;
        this.responseTime = System.nanoTime();
    }

    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result;
    }


    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    @Override
    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * 是否为BooleanAckCommand
     *
     * @return
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public InetSocketAddress getResponseHost() {
        return this.responseHost;
    }

    @Override
    public void setResponseHost(InetSocketAddress address) {
        this.responseHost = address;
    }

    @Override
    public long getResponseTime() {
        return this.responseTime;
    }
    @Override
    public void setResponseTime(long time) {
        this.responseTime = time;
    }

    @Override
    public void setOpaque(Integer opaque) {
        this.opaque = opaque;
    }
    @Override
    public Integer getOpaque() {
        return opaque;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }
    @Override
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}
