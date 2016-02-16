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

/**
 * Thermostat class for the PRT thermostat (Programmable Room Thermostat)
 * Most functions are handled by the base class
 * This thermostat does no include hot water, so these functions are overridden
 * and disabled
 *
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public class HeatmiserThermostatPRTV3 extends HeatmiserThermostatV3 {

    @Override
    public boolean setData(byte in[]) {
        if (super.setData(in) == false) {
            return false;
        }

        if ((getData(22) == 1) || (getData(22) == 4)) {
            dcbRoomTemperature = getTemp(37);
        } else {
            dcbRoomTemperature = getTemp(41);
        }
        dcbState = getData(30);
        dcbHeatState = getData(44);
        dcbFrostTemperature = getData(26);
        dcbSetTemperature = getData(27);
        dcbFloorTemperature = getTemp(39);
        dcbHolidayTime = (getData(34) & 0xFF) + ((getData(33) & 0xFF) * 256);
        dcbHoldTime = (getData(36) & 0xFF) + ((getData(35) & 0xFF) * 256);

        return true;
    }

    @Override
    public State getWaterState(Class<? extends Item> itemType) {
        return null;
    }

    @Override
    public byte[] formatCommand(Functions function, Command command) {
        switch (function) {
            default:
                // Default to calling the parent class.
                return super.formatCommand(function, command);
        }
    }
}
