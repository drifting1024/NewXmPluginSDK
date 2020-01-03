package com.ryeex.groot.lib.ble.beacon;

import android.text.TextUtils;

/**
 * Created by chenhao on 2017/12/20.
 */

public class MiotPacket {

    public FrameControl frameControl;
    public int productId;
    public int frameCounter;
    public String mac;
    public Capability capability;
    public Event event;
    public String comboKey;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("productId = 0x%2x", productId)).append("\n");
        sb.append(String.format("frameCounter = %d", frameCounter)).append("\n");
        sb.append(String.format("mac = %s", mac)).append("\n");

        if (frameControl != null) {
            sb.append("\n").append("FrameControl: ").append("\n");
            sb.append(frameControl.toString()).append("\n");
        }

        if (capability != null) {
            sb.append("\n").append("Capability: ").append("\n");
            sb.append(capability.toString()).append("\n");
        }

        if (event != null) {
            sb.append("\n").append("Event: ").append("\n");
            sb.append(event.toString()).append("\n");
        }

        if (!TextUtils.isEmpty(comboKey)) {
            sb.append("\n").append(String.format("comboKey: %s", comboKey)).append("\n");
        }

        return sb.toString();
    }

    public boolean isComboPacket() {
        return capability != null && capability.bindable == 3
                && frameControl != null && frameControl.version >= 3;
    }

    public boolean isBinding() {
        return frameControl != null && frameControl.binding;
    }

    public static class FrameControl {

        /*
         * 1 未绑定，还在出厂设置, 0 已经跟绑定过用户或不需要绑定
         */
        public boolean factoryNew;
        /*
         * 1 当前已连接, 0 当前未连接
         */
        public boolean connected;
        /*
         * 1 当前是Central，0，当前是Peripheral,如果bit1为1，则此位无效
         */
        public boolean central;
        /*
         * 1 该包已加密，0，该包未加密
         */
        public boolean encrypted;
        /*
         * 1 Frame control后包含6个byte的MAC地址，0 不包含6个BYTE的MAC地址
         */
        public boolean withMac;
        /*
         * 1 包含capability, 0 不包含capability
         */
        public boolean withCapability;
        /*
         * 1 包含事件，0,不包含事件
         */
        public boolean withEvent;
        /*
         * 1 包含厂商自定义数据，0,不包含自定义数据
         */
        public boolean withCustomData;
        /*
         * 1 包含厂商自定义米家副标题展示数据，0,不包含副标题数据
         */
        public boolean withSubtitle;
        /*
         * 1 是一个绑定确认包，0，不是绑定的确认包
         */
        public boolean binding;
        /*
         * 协议版本号
         */
        public int version;

        FrameControl() {

        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("");
            sb.append("factoryNew = " + factoryNew).append("\n");
            sb.append("connected = " + connected).append("\n");
            sb.append("central = " + central).append("\n");
            sb.append("encrypted = " + encrypted).append("\n");
            sb.append("withMac = " + withMac).append("\n");
            sb.append("withCapability = " + withCapability).append("\n");
            sb.append("withEvent = " + withEvent).append("\n");
            sb.append("withCustomData = " + withCustomData).append("\n");
            sb.append("withSubtitle = " + withSubtitle).append("\n");
            sb.append("binding = " + binding).append("\n");
            sb.append("version = " + version);
            return sb.toString();
        }

    }

    public static class Capability {
        /*
         * 1 有建立连接能力, 0 不能建立连接
         */
        public boolean connectable;
        /*
         * 1 能做蓝牙的Central, 0 不能做Central
         */
        public boolean centralable;
        /*
         * 1 有加密的能力，0，没有加密的能力
         */
        public boolean encryptable;
        /*
         * 0， 无确认能力，1，前绑定，2，后绑定
         */
        public int bindable;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("connectable = " + connectable).append("\n");
            sb.append("centralable = " + centralable).append("\n");
            sb.append("encryptable = " + encryptable).append("\n");
            sb.append("bindable = " + bindable);
            return sb.toString();
        }
    }

    public static class Event {
        public int eventId;
        public int eventData;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("");
            sb.append(String.format("eventId = 0x%x", eventId)).append("\n");
            sb.append("eventData = " + eventData);
            return sb.toString();
        }
    }
}
