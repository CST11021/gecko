package com.whz.gecko;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.service.config.WireFormatType;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 11:04 AM
 */
public class WhzWireFormatType extends WireFormatType {

    public static String PARAM_TYPE = "param_type";

    @Override
    public String getScheme() {
        return "test";
    }

    @Override
    public String name() {
        return "whz";
    }

    @Override
    public CodecFactory newCodecFactory() {
        return new WhzCodecFactory();
    }

    @Override
    public CommandFactory newCommandFactory() {
        return new WhzCommandFactory();
    }

}
