/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heatmiser.internal.thermostat;

import java.util.Calendar;

import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the Heatmiser thermostats.
 * This provides the core functionality - other thermostat classes
 * extend this to provide or update the specific functionality of that thermostat.
 *
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public class HeatmiserThermostat {
    protected static Logger logger = LoggerFactory.getLogger(HeatmiserThermostat.class);

    private byte data[];
    private byte address;
    protected int frameLength;
    protected byte function;
    protected Models dcbModel;
    protected byte dcbLock;
    protected byte dcbState;
    protected Calendar dcbTime;
    protected byte dcbHeatState;
    protected byte dcbWaterState;
    protected double dcbRoomTemperature;
    protected double dcbFrostTemperature;
    protected double dcbFloorTemperature;
    protected double dcbSetTemperature;
    protected int dcbHolidayTime;
    protected int dcbHoldTime;
    protected Versions protocolVersion;

    protected HeatmiserThermostat() {
    }

    public static HeatmiserThermostat getNewHeatmiserThermostat(int version) {
        HeatmiserThermostat ht;
        switch (version) {
            case 2:
                ht = new HeatmiserThermostatV2();
                ht.protocolVersion = Versions.PROTVER2;
                break;
            case 3:
                ht = new HeatmiserThermostatV3();
                ht.protocolVersion = Versions.PROTVER2;
                break;
            default:
                ht = new HeatmiserThermostat();
                break;
        }
        return ht;
    }

    public static HeatmiserThermostat getNewHeatmiserThermostat(int version, Models type) {
        String className = "org.openhab.binding.heatmiser.internal.thermostat.HeatmiserThermostat" + type + "V"
                + Integer.toString(version);
        try {
            Class<?> cls = Class.forName(className);
            Object ht = cls.newInstance();
            return (HeatmiserThermostat) ht;
        } catch (ClassNotFoundException e) {
            if (type != Models.PRT) { // Attempt to get PRT type stat as a fallback
                return HeatmiserThermostat.getNewHeatmiserThermostat(version, Models.PRT);
            }
            logger.error("HEATMISER: Error loading class " + className);
        } catch (InstantiationException e) {
            logger.error("HEATMISER: Error instantiating object " + className);
        } catch (IllegalAccessException e) {
            logger.error("HEATMISER: Error accessing object " + className);
        }
        return null;
    }

    public void setAddress(byte newAddress) {
        address = newAddress;
    }

    public int getAddress() {
        return address;
    }

    protected byte[] getData() {
        return data;
    }

    protected byte getData(int index) {
        byte b = 0;
        if (data != null && index < data.length) {
            b = data[index];
        }
        return b;
    }

    public boolean setData(byte in[]) {
        data = in;
        if (in != null) {
            return true;
        } else {
            return false;
        }
    }

    public Models getType() {
        return dcbModel;
    }

    protected byte[] makePacket(boolean write, int start, int length, byte[] data) {
        return null;
    }

    /**
     * Produces a packet to poll this thermostat
     *
     * @return byte array with the packet
     */
    public byte[] pollThermostat() {
        return makePacket(false, 0, 0xffff, null);
    }

    /**
     * Formats a command to the thermostat
     *
     * @param function The command function
     * @param command The openHAB command parameter
     * @return byte array with the command packet
     */
    public byte[] formatCommand(Functions function, Command command) {
        switch (function) {
            case SETTEMP:
                return setRoomTemperature(command);
            case ONOFF:
                return setOnOff(command);
            case RUNMODE:
                return setRunMode(command);
            case FROSTTEMP:
                return setFrostTemperature(command);
            case HOLIDAYSET:
                return setHolidayTime(command);
            case TIME:
                return setTime(command);
            case LOCK:
                return setLock(command);
            default:
                return null;
        }
    }

    /**
     * Returns the current frost temperature
     *
     * @param itemType
     * @return
     */
    public State getFrostTemperature(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return StringType.valueOf(Double.toString(dcbFrostTemperature));
        }

        // Default to DecimalType
        return DecimalType.valueOf(Double.toString(dcbFrostTemperature));
    }

    /**
     * Returns the current floor temperature
     *
     * @param itemType
     * @return
     */
    public State getFloorTemperature(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return StringType.valueOf(Double.toString(dcbFloorTemperature));
        }

        // Default to DecimalType
        return DecimalType.valueOf(Double.toString(dcbFloorTemperature));
    }

    /**
     * Returns the current heating state
     *
     * @param itemType
     * @return
     */
    public State getOnOffState(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return dcbState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
        }
        if (itemType == SwitchItem.class) {
            return dcbState == 1 ? OnOffType.ON : OnOffType.OFF;
        }

        // Default to DecimalType
        return DecimalType.valueOf(Integer.toString(dcbState));
    }

    public State getHeatState(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return dcbHeatState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
        }
        if (itemType == SwitchItem.class) {
            return dcbHeatState == 1 ? OnOffType.ON : OnOffType.OFF;
        }
        if (itemType == ContactItem.class) {
            return dcbHeatState == 1 ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
        }
        // Default to DecimalType
        return DecimalType.valueOf(Integer.toString(dcbHeatState));
    }

    public State getTemperature(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return StringType.valueOf(Double.toString(dcbRoomTemperature));
        }

        // Default to DecimalType
        return DecimalType.valueOf(Double.toString(dcbRoomTemperature));
    }

    public State getSetTemperature(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return StringType.valueOf(Double.toString(dcbSetTemperature));
        }

        // Default to DecimalType
        return DecimalType.valueOf(Double.toString(dcbSetTemperature));
    }

    public State getWaterState(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return dcbWaterState == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
        }
        if (itemType == SwitchItem.class) {
            return dcbWaterState == 1 ? OnOffType.ON : OnOffType.OFF;
        }

        // Default to DecimalType
        return DecimalType.valueOf(Integer.toString(dcbWaterState));
    }

    public State getHolidayTime(Class<? extends Item> itemType) {
        return null;
    }

    public State getHolidaySet(Class<? extends Item> itemType) {
        return null;
    }

    public State getHolidayMode(Class<? extends Item> itemType) {
        return null;
    }

    public State getHoldTime(Class<? extends Item> itemType) {
        return null;
    }

    public State getHoldMode(Class<? extends Item> itemType) {
        return null;
    }

    public State getState(Class<? extends Item> itemType) {
        return null;
    }

    public State getTime(Class<? extends Item> itemType) {
        if (itemType == DateTimeItem.class) {
            return new DateTimeType(dcbTime);
        }
        return null;
    }

    public State getLockState(Class<? extends Item> itemType) {
        if (itemType == StringItem.class) {
            return dcbLock == 1 ? StringType.valueOf("ON") : StringType.valueOf("OFF");
        }
        if (itemType == SwitchItem.class) {
            return dcbLock == 1 ? OnOffType.ON : OnOffType.OFF;
        }
        if (itemType == ContactItem.class) {
            return dcbLock == 1 ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
        }
        // Default to DecimalType
        return DecimalType.valueOf(Integer.toString(dcbLock));
    }

    public byte[] setRoomTemperature(Command command) {
        return new byte[-1];
    }

    public byte[] setFrostTemperature(Command command) {
        return new byte[-1];
    }

    public byte[] setHolidayTime(Command command) {
        return new byte[-1];
    }

    public byte[] setOnOff(Command command) {
        return new byte[-1];
    }

    public byte[] setRunMode(Command command) {
        return new byte[-1];
    }

    public byte[] setTime(Command command) {
        return new byte[-1];
    }

    public byte[] setLock(Command command) {
        return new byte[-1];
    }

    public Models getModel() {
        return dcbModel;
    }

    public enum Functions {
        UNKNOWN,
        ROOMTEMP,
        FLOORTEMP,
        ONOFF,
        RUNMODE,
        SETTEMP,
        TIME,
        LOCK,
        FROSTTEMP,
        HOLIDAYTIME,
        HOLIDAYMODE,
        HOLIDAYSET,
        HEATSTATE,
        WATERSTATE,
        HOLDTIME,
        HOLDMODE,
        STATE;
    }

    public enum States {
        OFF,
        ON,
        HOLD,
        HOLIDAY;
    }

    public enum Models {
        PRT,
        PRTHW,
        DT,
        DTE,
        PRTE,
        FCV
    }

    public enum Versions {
        PROTVER2,
        PROTVER3
    }
}
