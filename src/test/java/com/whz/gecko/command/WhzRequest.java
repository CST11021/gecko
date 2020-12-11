package com.whz.gecko.command;

import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.util.OpaqueGenerator;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:41 PM
 */
public class WhzRequest implements RequestCommand, Serializable {

    private static final long serialVersionUID = 5749226672361719157L;

    private String message;

    private int opaque;

    public WhzRequest(String message) {
        this.message = message;
        this.opaque = OpaqueGenerator.getNextOpaque();
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }



    /**
     * 实现CommandHeader接口，返回一个命令ID
     *
     * @return
     */
    @Override
    public Integer getOpaque() {
        return opaque;
    }

    /**
     * 实现RequestCommand接口，返回命令头
     *
     * @return
     */
    @Override
    public CommandHeader getRequestHeader() {
        return this;
    }

}
