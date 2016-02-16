package org.openhab.binding.heatmiser.internal.thermostat;

import java.util.Calendar;

import org.apache.commons.lang.ArrayUtils;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.Command;

public class HeatmiserThermostatV2 extends HeatmiserThermostat {
    public HeatmiserThermostatV2() {
        super();
    }

    @Override
    public boolean setData(byte in[]) {
        if (in.length < 4) {
            return false;
        }

        frameLength = in[2];
        super.setData(ArrayUtils.remove(in, 2));

        if (getData().length != frameLength) {
            return false;
        }

        int crc = getData(frameLength - 1) & 0xFF;
        if (crc != checkCRC(getData())) {
            return false;
        }

        setAddress(getData(0));
        function = getData(1);

        switch (getData(2)) {
            case 85:
                dcbModel = Models.DT;
                break;
            case 81:
                dcbModel = Models.PRT;
                break;
            case 83:
                dcbModel = Models.FCV;
                break;
            case 82:
                dcbModel = Models.PRTHW;
                break;
        }
        return true;
    }

    private int checkCRC(byte[] packet) {
        int crc = 0;
        for (int cnt = 0; cnt < packet.length - 1; cnt++) {
            byte b = packet[cnt];
            crc += b & 0xFF;
        }
        crc &= 0xff;
        return crc;
    }

    /* Set byte 0 of data to command to send */
    /* if data is null then a "Read Parameter" query is sent */
    /* length should be the length of the payload data, not including the command byte */
    @Override
    protected byte[] makePacket(boolean write, int cmd, int length, byte[] data) {
        byte[] outPacket;

        if (write == false) {
            outPacket = new byte[4];
        } else {
            outPacket = new byte[4 + length - 1];
        }
        outPacket[0] = (byte) getAddress();
        if (write == false) {
            if (dcbModel == Models.PRTHW) {
                outPacket[1] = 0x29;
            } else {
                outPacket[1] = 0x26;
            }
            outPacket[2] = 0x00;
            length = 0;
        } else {
            outPacket[1] = (byte) (cmd & 0xff);
            outPacket[1] += 128;
            if (data != null) {
                for (byte cnt = 0; cnt < length; cnt++) {
                    outPacket[2 + cnt] = data[cnt];
                }
            }
            length -= 1;
        }

        int crc = checkCRC(outPacket);
        outPacket[length + 3] = (byte) (crc & 0xff);

        return outPacket;
    }

    protected byte getBit(byte data, int position) {
        return (byte) ((data >> position) & 1);
    }

    protected boolean getBitState(byte data, int position) {
        return getBit(data, position) == 1;
    }

    @Override
    public byte[] setFrostTemperature(Command command) {
        byte[] cmdByte = new byte[1];

        byte temperature = ((DecimalType) command).byteValue();
        if (temperature < 7) {
            temperature = 7;
        }
        if (temperature > 17) {
            temperature = 17;
        }

        cmdByte[0] = temperature;
        return makePacket(true, 0x07, 1, cmdByte);
    }

    @Override
    public byte[] setRoomTemperature(Command command) {
        byte[] cmdByte = new byte[1];

        if (!(command instanceof DecimalType)) {
            return null;
        }

        byte temperature = ((DecimalType) command).byteValue();

        if (temperature < 5) {
            return null;
        }
        if (temperature > 35) {
            return null;
        }

        cmdByte[0] = temperature;
        return makePacket(true, 0x04, 1, cmdByte);
    }

    @Override
    public byte[] setTime(Command command) {
        byte[] cmdBytes = getCurrentWriteStatusPacket();

        if (!(command instanceof DateTimeType)) {
            return null;
        }

        Calendar c = ((DateTimeType) command).getCalendar();
        cmdBytes[1] = (byte) ((c.get(Calendar.DAY_OF_WEEK) - 1) & 0x0F);
        cmdBytes[2] = (byte) (c.get(Calendar.HOUR_OF_DAY) & 0xFF);
        cmdBytes[3] = (byte) (c.get(Calendar.MINUTE) & 0xFF);
        if (cmdBytes[1] < 1) {
            cmdBytes[1] += 7;
        }
        return makePacket(true, 0x26, 13, cmdBytes);
    }

    protected byte[] getCurrentWriteStatusPacket() {
        byte[] status = new byte[13];
        byte[] oldStatus = ArrayUtils.subarray(getData(), 2, 15);

        status[0] = oldStatus[0]; // Model Code
        status[1] = (byte) (oldStatus[1] & 0x0F); // Day of week
        status[2] = oldStatus[2]; // Hour
        status[3] = oldStatus[3]; // Mins
        status[4] = 0; // Temperature Calibration
        status[5] = (byte) (oldStatus[5] & 0x0F); // Part No.
        status[6] = (byte) (oldStatus[5] & 0xF0); // Switching Differential
        status[7] = oldStatus[6]; // Status Data
        status[8] = oldStatus[7]; // Set Temperature
        status[9] = oldStatus[8]; // Frost Temperature
        status[10] = oldStatus[9]; // Output delay
        status[11] = (byte) ((oldStatus[1] & 0x70) >> 3); // Preheat
        status[12] = oldStatus[10]; // Floor Temperature
        return status;
    }

    @Override
    public byte[] setOnOff(Command command) {
        byte[] cmdByte = new byte[1];

        if (command.toString().contentEquals("ON")) {
            cmdByte[0] = (byte) 0xff;
        } else {
            cmdByte[0] = (byte) 0x00;
        }
        return makePacket(true, 0x02, 1, cmdByte);
    }

    @Override
    public byte[] setLock(Command command) {
        byte[] cmdByte = getCurrentWriteStatusPacket();
        int status = cmdByte[7];

        if (command.toString().contentEquals("ON")) {
            cmdByte[7] = (byte) ((status & 0xFF) | 0x40);
        } else {
            cmdByte[7] = (byte) ((status & 0xFF) & ~0x40);
        }
        return makePacket(true, 0x26, 13, cmdByte);
    }
}