package com.whz.gecko;

import com.taobao.gecko.service.notify.response.NotifyResponseCommand;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:55 PM
 */
public class WhzResponse extends NotifyResponseCommand {

    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public void encodeContent() {

    }

    @Override
    public void decodeContent() {

    }
}
