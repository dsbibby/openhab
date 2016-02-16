package org.openhab.binding.heatmiser.internal.thermostat;

import java.util.Calendar;

import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

public class HeatmiserThermostatV3 extends HeatmiserThermostat {
    private int dcbStart;

    public HeatmiserThermostatV3() {
        super();
    }

    public Versions getProtocolVersion() {
        return protocolVersion;
    }

    protected int getInt(int pos) {
        int val;
        val = (getData(pos) & 0xFF) + ((getData(pos + 1) & 0xFF) * 256);
        return val;
    }

    protected double getTemp(int pos) {
        double val;
        val = (double) ((getData(pos + 1) & 0xFF) + ((getData(pos) & 0xFF) * 256)) / 10;
        return val;
    }

    /*
     * Sets the new packet data for the thermostat This function processes the
     * basic data and checks the validity of the incoming data
     *
     * @param in Input data byte array
     */

    @Override
    public boolean setData(byte in[]) {
        if (in.length < 9) {
            return false;
        }

        setData(in);
        frameLength = getInt(1);
        if (in.length != frameLength) {
            return false;
        }

        int crc = getInt(frameLength - 2);
        if (crc != checkCRC(getData())) {
            return false;
        }

        setAddress(getData(3));
        function = getData(4);
        if (function == 1) {
            return false;
        }

        // Check that the whole DCB is returned
        // While this isn't 100% necessary, it's what the binding does to make
        // things easier!
        dcbStart = getInt(5);
        if (dcbStart != 0) {
            return false;
        }

        switch (getData(13)) {
            case 0:
                dcbModel = Models.DT;
                break;
            case 1:
                dcbModel = Models.DTE;
                break;
            case 2:
                dcbModel = Models.PRT;
                break;
            case 3:
                dcbModel = Models.PRTE;
                break;
            case 4:
                dcbModel = Models.PRTHW;
                break;
        }

        return true;
    }

    private int checkCRC(byte[] packet) {
        int crc = 0xFFFF; // initial value
        int polynomial = 0x1021; // 0001 0000 0010 0001 (0, 5, 12)

        for (int cnt = 0; cnt < packet.length - 2; cnt++) {
            byte b = packet[cnt];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }

        crc &= 0xffff;

        return crc;
    }

    @Override
    protected byte[] makePacket(boolean write, int start, int length, byte[] data) {
        byte[] outPacket;

        if (write == false) {
            outPacket = new byte[10];
        } else {
            outPacket = new byte[10 + length];
        }

        outPacket[0] = (byte) getAddress();
        if (write) {
            outPacket[1] = (byte) (length + 10);
            outPacket[3] = 1;
        } else {
            outPacket[1] = 10;
            outPacket[3] = 0;
        }
        outPacket[2] = (byte) 0x81;
        outPacket[4] = (byte) (start & 0xff);
        outPacket[5] = (byte) ((start >> 8) & 0xff);
        outPacket[6] = (byte) (length & 0xff);
        outPacket[7] = (byte) ((length >> 8) & 0xff);

        if (write == true) {
            for (byte cnt = 0; cnt < length; cnt++) {
                outPacket[8 + cnt] = data[cnt];
            }
        } else {
            length = 0;
        }

        int crc = checkCRC(outPacket);
        outPacket[length + 8] = (byte) (crc & 0xff);
        outPacket[length + 9] = (byte) ((crc >> 8) & 0xff);

        return outPacket;
    }

    /**
     * Command to set the room temperature
     *
     * @param command
     * @return byte array with the command data
     */
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
        return makePacket(true, 18, 1, cmdByte);
    }

    /**
     * Sets the frost temperature
     *
     * @param command
     * @return byte array with the command packet
     */
    @Override
    public byte[] setFrostTemperature(Command command) {
        byte[] cmdByte = new byte[1];

        byte temperature = ((DecimalType) command).byteValue();
        if (temperature < 5) {
            temperature = 5;
        }
        if (temperature > 18) {
            temperature = 18;
        }

        cmdByte[0] = temperature;
        return makePacket(true, 17, 1, cmdByte);
    }

    /**
     * Sets the holiday time
     *
     * @param command time to set holiday mode - specified in days
     * @return command string to send to thermostat
     */
    @Override
    public byte[] setHolidayTime(Command command) {
        byte[] cmdBytes = new byte[2];

        // Convert the time into an integer - 99 days is the maximum
        int time = Integer.parseInt(command.toString());
        if (time < 0) {
            time = 0;
        }
        if (time > 99) {
            time = 0;
        }

        // Convert time to hours
        time = time * 24;

        // Subtract the hours gone today
        Calendar now = Calendar.getInstance();
        time -= now.get(Calendar.HOUR_OF_DAY);

        // Sanity check
        if (time < 0) {
            time = 0;
        }
        if (time > (99 * 24)) {
            time = 0;
        }
        logger.debug("Setting holiday time {} days = {} hours.", command.toString(), time);

        cmdBytes[0] = (byte) (time & 0xff);
        cmdBytes[1] = (byte) ((time >> 8) & 0xff);

        return makePacket(true, 24, 2, cmdBytes);
    }

    /**
     * Sets the current time for the thermostat
     *
     * @param command
     * @return command string to send to thermostat
     */
    @Override
    public byte[] setTime(Command command) {
        byte[] cmdBytes = new byte[4];

        // Now.wday = Now.wday - 1
        // if(Now.wday == 0) then
        // Now.wday = Now.wday + 7
        // end
        // cmdFrame =
        // Dec2Hex(Now.wday)..Dec2Hex(Now.hour)..Dec2Hex(Now.min)..Dec2Hex(Now.sec)
        return makePacket(true, 43, 4, cmdBytes);
    }

    /**
     * Enables or disables the thermostat
     *
     * @param command
     * @return
     */
    @Override
    public byte[] setOnOff(Command command) {
        byte[] cmdByte = new byte[1];

        if (command.toString().contentEquals("ON")) {
            cmdByte[0] = 1;
        } else {
            cmdByte[0] = 0;
        }
        return makePacket(true, 21, 1, cmdByte);
    }

    @Override
    public byte[] setRunMode(Command command) {
        byte[] cmdByte = new byte[1];

        if (command.toString().contentEquals("ON")) {
            cmdByte[0] = 1;
        } else {
            cmdByte[0] = 0;
        }
        return makePacket(true, 23, 1, cmdByte);
    }

    /**
     * Returns the current room temperature
     *
     * @param itemType
     * @return
     */
    /*
     * @Override
     * public State getTemperature(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return StringType.valueOf(Double.toString(dcbRoomTemperature));
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Double.toString(dcbRoomTemperature));
     * }
     */

    /**
     * Returns the current frost temperature
     *
     * @param itemType
     * @return
     */
    /*
     * @Override
     * public State getFrostTemperature(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return StringType.valueOf(Double.toString(dcbFrostTemperature));
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Double.toString(dcbFrostTemperature));
     * }
     */

    /**
     * Returns the current floor temperature
     *
     * @param itemType
     * @return
     */
    /*
     * @Override
     * public State getFloorTemperature(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return StringType.valueOf(Double.toString(dcbFloorTemperature));
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Double.toString(dcbFloorTemperature));
     * }
     */

    /**
     * Returns the current state of the thermostat
     * This is a consolidated status that brings together a number of
     * status registers within the thermostat.
     *
     * @param itemType
     * @return
     */
    @Override
    public State getState(Class<? extends Item> itemType) {
        // If this is a switch, then just treat this like the getOnOffState() function
        if (itemType == SwitchItem.class) {
            return dcbState == 1 ? OnOffType.ON : OnOffType.OFF;
        }

        // Default to a string
        if (dcbState == 0) {
            return StringType.valueOf(States.OFF.toString());
        }
        if (dcbHolidayTime != 0) {
            return StringType.valueOf(States.HOLIDAY.toString());
        }
        if (dcbHoldTime != 0) {
            return StringType.valueOf(States.HOLD.toString());
        }

        return StringType.valueOf(States.ON.toString());
    }

    /**
     * Returns the current heating state
     *
     * @param itemType
     * @return
     */
    /*
     * @Override
     * public State getOnOffState(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return dcbState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
     * }
     * if (itemType == SwitchItem.class) {
     * return dcbState == 1 ? OnOffType.ON : OnOffType.OFF;
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Integer.toString(dcbState));
     * }
     */

    /*
     * @Override
     * public State getWaterState(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return dcbWaterState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
     * }
     * if (itemType == SwitchItem.class) {
     * return dcbWaterState == 1 ? OnOffType.ON : OnOffType.OFF;
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Integer.toString(dcbWaterState));
     * }
     */

    // @Override
    // public State getSetTemperature(Class<? extends Item> itemType) {
    // if (itemType == StringItem.class) {
    // return StringType.valueOf(Double.toString(dcbSetTemperature));
    // }
    //
    // // Default to DecimalType
    // return DecimalType.valueOf(Double.toString(dcbSetTemperature));
    // }

    /*
     * @Override
     * public State getHeatState(Class<? extends Item> itemType) {
     * if (itemType == StringItem.class) {
     * return dcbHeatState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
     * }
     * if (itemType == SwitchItem.class) {
     * return dcbHeatState == 1 ? OnOffType.ON : OnOffType.OFF;
     * }
     *
     * // Default to DecimalType
     * return DecimalType.valueOf(Integer.toString(dcbHeatState));
     * }
     */

    @Override
    public State getHolidayMode(Class<? extends Item> itemType) {
        return dcbHolidayTime > 0 ? OnOffType.ON : OnOffType.OFF;
    }

    @Override
    public State getHolidayTime(Class<? extends Item> itemType) {
        if (itemType == SwitchItem.class) {
            return dcbHolidayTime > 0 ? OnOffType.ON : OnOffType.OFF;
        }

        // Return a date with the end time
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.add(Calendar.HOUR, dcbHolidayTime);
        return new DateTimeType(now);
    }

    @Override
    public State getHolidaySet(Class<? extends Item> itemType) {
        if (itemType == SwitchItem.class) {
            return dcbHolidayTime > 0 ? OnOffType.ON : OnOffType.OFF;
        }

        // Return the number of days (midnights) remaining
        // ie midnight tonight == 1
        int days = 0;
        if (dcbHolidayTime > 0) {
            days = dcbHolidayTime / 24 + 1;
        }

        return DecimalType.valueOf(Integer.toString(days));
    }

    @Override
    public State getHoldMode(Class<? extends Item> itemType) {
        return dcbHoldTime > 0 ? OnOffType.ON : OnOffType.OFF;
    }

    @Override
    public State getHoldTime(Class<? extends Item> itemType) {
        if (itemType == SwitchItem.class) {
            return dcbHoldTime > 0 ? OnOffType.ON : OnOffType.OFF;
        }

        // Return a date with the end time
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, dcbHoldTime);
        return new DateTimeType(now);
    }
}