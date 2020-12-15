package com.whz.gecko.command;

import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.heartbeat.HeartBeatRequestCommand;
import com.taobao.gecko.core.util.OpaqueGenerator;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 7:33 PM
 */
public class HeartRequest implements HeartBeatRequestCommand, Serializable {

    private static final long serialVersionUID = -7695674856130929464L;

    private WhzRequest request = new WhzRequest("心跳测试");

    @Override
    public Integer getOpaque() {
        return OpaqueGenerator.getNextOpaque();
    }

    @Override
    public CommandHeader getRequestHeader() {
        return request.getRequestHeader();
    }
}
