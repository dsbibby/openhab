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
public class HeatmiserThermostatPRTV2 extends HeatmiserThermostatV2 {

    @Override
    public boolean setData(byte in[]) {
        if (super.setData(in) == false) {
            return false;
        }

        dcbRoomTemperature = getData(6);
        dcbState = getBit(getData(8), 7);
        dcbLock = getBit(getData(8), 6);
        dcbHeatState = getBit(getData(8), 4);
        dcbFrostTemperature = getData(10);
        dcbSetTemperature = getData(9);
        dcbFloorTemperature = getData(12);
        dcbTime = Calendar.getInstance();
        dcbTime.set(Calendar.DAY_OF_WEEK, (getData(3) & 0x0F) + 1);
        dcbTime.set(Calendar.HOUR_OF_DAY, getData(4));
        dcbTime.set(Calendar.MINUTE, getData(5));
        dcbTime.set(Calendar.SECOND, 0);

        /*
         * dcbHolidayTime = (getData(34) & 0xFF) + ((getData(33) & 0xFF) * 256);
         * dcbHoldTime = (getData(36) & 0xFF) + ((getData(35) & 0xFF) * 256);
         */

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
