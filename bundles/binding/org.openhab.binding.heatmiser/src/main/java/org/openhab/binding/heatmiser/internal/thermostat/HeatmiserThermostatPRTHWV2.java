package org.openhab.binding.heatmiser.internal.thermostat;

import org.apache.commons.lang.ArrayUtils;
import org.openhab.core.types.Command;

/**
 * Thermostat class for the PRTHW thermostat (Programmable Room Thermostat - Hot Water)
 * Most functions are handled by the base class
 *
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public class HeatmiserThermostatPRTHWV2 extends HeatmiserThermostatPRTV2 {
    @Override
    public boolean setData(byte in[]) {
        if (super.setData(in) == false) {
            return false;
        }
        /*
         * dcbState = getData(30);
         * dcbHeatState = getData(44);
         * dcbFrostTemperature = getData(26);
         * dcbRoomTemperature = getTemp(41);
         * dcbSetTemperature = getData(27);
         *
         * dcbHolidayTime = (getData(34) & 0xFF) + ((getData(33) & 0xFF) * 256);
         * dcbHoldTime = (getData(36) & 0xFF) + ((getData(35) & 0xFF) * 256);
         */
        dcbWaterState = getBit(getData(8), 3);

        return true;
    }

    private byte[] setWaterState(Command command) {
        byte[] cmdByte = getCurrentWriteStatusPacket();

        if (command.toString().contentEquals("ON")) {
            cmdByte[0] = 1;
        } else {
            cmdByte[0] = 0;
        }
        return makePacket(true, 42, 1, cmdByte);
    }

    @Override
    public byte[] formatCommand(Functions function, Command command) {
        switch (function) {
            case WATERSTATE:
                return setWaterState(command);
            default:
                // Default to calling the parent class.
                return super.formatCommand(function, command);
        }
    }

    @Override
    public byte[] setLock(Command command) {
        byte[] cmdByte = getCurrentWriteStatusPacket();

        if (command.toString().contentEquals("ON")) {
            cmdByte[6] = (byte) (cmdByte[7] & 0x40);
        } else {
            cmdByte[6] = (byte) (cmdByte[7] & ~0x40);
        }
        return makePacket(true, 0x1A, 1, cmdByte);
    }

    @Override
    protected byte[] getCurrentWriteStatusPacket() {
        byte[] status = new byte[13];
        byte[] oldStatus = ArrayUtils.subarray(getData(), 2, 15);

        status[0] = oldStatus[0]; // Model Code
        status[1] = (byte) (oldStatus[1] & 0x0F); // Day of week
        status[2] = oldStatus[2]; // Hour
        status[3] = oldStatus[3]; // Mins
        status[4] = 0; // Temperature Calibration

        status[5] = oldStatus[5]; // Set Temperature
        status[6] = oldStatus[6]; // Status Data
        status[7] = oldStatus[7]; // Switching Differential
        status[8] = oldStatus[8]; // Frost Temperature
        status[9] = oldStatus[9]; // Output delay
        status[10] = oldStatus[10]; // Preheat
        status[11] = 0; // Reserved
        status[12] = 0; // Reserved
        return status;
    }
}