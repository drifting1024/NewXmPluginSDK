package com.ryeex.groot.lib.ble.beacon;

import com.ryeex.groot.lib.common.util.ListUtil;

import java.util.List;

/**
 * Created by chenhao on 2017/12/20.
 */

public class MiotPacketParser {
    public static MiotPacket parse(List<BleAdvertise> bleAdvertiseList) {
        if (ListUtil.isEmpty(bleAdvertiseList)) {
            return null;
        }

        MiotPacket packet = null;

        try {
            for (BleAdvertise item : bleAdvertiseList) {
                packet = MiotPacketParser.parse(item);
                if (packet != null) {
                    break;
                }
            }
        } catch (Exception e) {
        }

        return packet;
    }

    private static MiotPacket parse(BleAdvertise bleAdvertise) {
        MiotPacket packet = null;

        if (bleAdvertise.type != BtConstants.MIOT_ADV_TYPE) {
            return packet;
        }

        PacketReader reader = new PacketReader(bleAdvertise);

        int service = reader.getShort();

        if (service != BtConstants.MIIO_UUID) {
            return packet;
        }

        try {
            packet = tryParse(reader);
        } catch (Exception e) {

        }

        return packet;
    }

    private static MiotPacket tryParse(PacketReader reader) {
        MiotPacket packet = new MiotPacket();

        packet.frameControl = new MiotPacket.FrameControl();

        int firstByte = reader.getByte();
        packet.frameControl.factoryNew = reader.getBit(firstByte, 0);
        packet.frameControl.connected = reader.getBit(firstByte, 1);
        packet.frameControl.central = reader.getBit(firstByte, 2);
        packet.frameControl.encrypted = reader.getBit(firstByte, 3);
        packet.frameControl.withMac = reader.getBit(firstByte, 4);
        packet.frameControl.withCapability = reader.getBit(firstByte, 5);
        packet.frameControl.withEvent = reader.getBit(firstByte, 6);
        packet.frameControl.withCustomData = reader.getBit(firstByte, 7);

        int secondByte = reader.getByte();
        packet.frameControl.withSubtitle = reader.getBit(secondByte, 0);
        packet.frameControl.binding = reader.getBit(secondByte, 1);
        packet.frameControl.version = reader.getInt(secondByte, 4, 7);

        if (!isBeaconProtocolSupported(packet.frameControl.version)) {
            throw new IllegalArgumentException(String.format("beacon version not supported: %d", packet.frameControl.version));
        }

        packet.productId = reader.getShort();

        packet.frameCounter = reader.getByte();

        if (packet.frameControl.withMac) {
            packet.mac = reader.getMac();
        }

        if (packet.frameControl.withCapability) {
            int capabilityByte = reader.getByte();
            packet.capability = new MiotPacket.Capability();
            packet.capability.connectable = reader.getBit(capabilityByte, 0);
            packet.capability.centralable = reader.getBit(capabilityByte, 1);
            packet.capability.encryptable = reader.getBit(capabilityByte, 2);
            packet.capability.bindable = reader.getInt(capabilityByte, 3, 4);
        }

        if (packet.frameControl.withEvent) {
            packet.event = new MiotPacket.Event();
            packet.event.eventId = reader.getShort();
            packet.event.eventData = reader.getByte();
        }

        if (packet.isComboPacket()) {
            packet.comboKey = reader.getShortString();
        }

        return packet;
    }

    public static boolean isBeaconProtocolSupported(int version) {
        return BtConstants.SUPPORTED_PROTOCOL_VERSION >= version;
    }
}
