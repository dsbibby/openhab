/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heatmiser.internal.thermostat;

import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

public class HeatmiserThermostatPRTHWV3 extends HeatmiserThermostatV3 {

    @Override
    public boolean setData(byte in[]) {
        if (super.setData(in) == false) {
            return false;
        }

        dcbState = getData(30);
        dcbHeatState = getData(44);
        dcbFrostTemperature = getData(26);
        dcbRoomTemperature = getTemp(41);
        dcbSetTemperature = getData(27);

        dcbHolidayTime = (getData(34) & 0xFF) + ((getData(33) & 0xFF) * 256);
        dcbHoldTime = (getData(36) & 0xFF) + ((getData(35) & 0xFF) * 256);

        dcbWaterState = getData(45);

        return true;
    }

    @Override
    public State getFloorTemperature(Class<? extends Item> itemType) {
        return null;
    }

    private byte[] setWaterState(Command command) {
        byte[] cmdByte = new byte[1];

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
}
