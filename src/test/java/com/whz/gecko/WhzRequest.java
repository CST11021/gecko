package com.whz.gecko;

import com.taobao.gecko.core.util.OpaqueGenerator;
import com.taobao.gecko.service.notify.OpCode;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:41 PM
 */
public class WhzRequest extends NotifyRequestCommand implements Serializable {

    private static final long serialVersionUID = 5749226672361719157L;

    private String message;

    public WhzRequest(String message) {
        this.message = message;
        this.opaque = OpaqueGenerator.getNextOpaque();
        this.opCode = OpCode.SEND_MESSAGE;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void encodeContent() {

    }

    @Override
    public void decodeContent() {

    }
}
