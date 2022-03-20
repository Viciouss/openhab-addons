/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.modbus.huaweisun2000.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The Huawei Sun 2000 Inverter Binding.
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
public class HuaweiSun2000BindingConstants {

    private static final String BINDING_ID = ModbusBindingConstants.BINDING_ID;

    // thing types
    public static final ThingTypeUID THING_TYPE_HUAWEI_SUN2000_INVERTER = new ThingTypeUID(BINDING_ID,
            "huawei-sun2000");

    // register groups which will be read as a block
    public static final String INVERTER_BASICS_NAME = "inverterBasics";
    public static final String INVERTER_STATE_NAME = "inverterState";
    public static final String INVERTER_ADJUSTMENTS_NAME = "inverterAdjustments";

    public static final HuaweiSun2000RegisterGroup INVERTER_BASICS = new HuaweiSun2000RegisterGroup(
            INVERTER_BASICS_NAME, 30000, 30083);
    public static final HuaweiSun2000RegisterGroup INVERTER_STATE = new HuaweiSun2000RegisterGroup(INVERTER_STATE_NAME,
            32000, 32116);
    public static final HuaweiSun2000RegisterGroup INVERTER_ADJUSTMENTS = new HuaweiSun2000RegisterGroup(
            INVERTER_ADJUSTMENTS_NAME, 35300, 35308);

    // other
    public static final int UNIT_ID = 1;
    public static final int MAX_TRIES = 3;

    public static final String VARIABLES_DEFINITION_FILE = "variables.json";
}
