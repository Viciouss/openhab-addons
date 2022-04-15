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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The Huawei Sun 2000 Inverter Binding.
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
public class HuaweiSun2000Variable {

    public static final String ACCESS_RO = "RO";
    public static final String ACCESS_WO = "WO";
    public static final String ACCESS_RW = "RW";

    public static final String TYPE_STRING = "STR";
    public static final String TYPE_SIGNED_INTEGER = "I32";
    public static final String TYPE_SIGNED_SHORT = "I16";
    public static final String TYPE_UNSIGNED_INTEGER = "U32";
    public static final String TYPE_UNSIGNED_SHORT = "U16";
    public static final String TYPE_BITFIELD16 = "Bitfield16";
    public static final String TYPE_BITFIELD32 = "Bitfield32";

    public static final List<String> NUMBER_TYPES = List.of(TYPE_SIGNED_INTEGER, TYPE_UNSIGNED_INTEGER,
            TYPE_SIGNED_SHORT, TYPE_UNSIGNED_SHORT);

    public static final String UNIT_VOLT = "V";
    public static final String UNIT_AMPERE = "A";
    public static final String UNIT_WATT = "W";
    public static final String UNIT_KILOWATT = "kW";
    public static final String UNIT_KILOWATT_HOURS = "kWh";
    public static final String UNIT_PERCENT = "%";
    public static final String UNIT_TEMP = "°C";
    public static final String UNIT_SEC = "s";
    public static final String UNIT_HZ = "Hz";

    private final int number;
    private final String name;
    private final String access;
    private final String type;
    private final @Nullable String unit;
    private final int gain;
    private final int address;
    private final int quantity;
    private final String group;

    public HuaweiSun2000Variable(int number, String name, String access, String type, @Nullable String unit, int gain,
            int address, int quantity, String group) {
        this.number = number;
        this.name = name;
        this.access = access;
        this.type = type;
        this.unit = unit;
        this.gain = gain;
        this.address = address;
        this.quantity = quantity;
        this.group = group;
    }

    public int getNumber() {
        return this.number;
    }

    public String getVariableString() {
        return String.format("%02d", number);
    }

    public String getName() {
        return this.name;
    }

    public @Nullable String getGroup() {
        return this.group;
    }

    public String getAccess() {
        return this.access;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public int getGain() {
        return this.gain;
    }

    public int getAddress() {
        return address;
    }

    public String getType() {
        return this.type;
    }

    public @Nullable String getUnit() {
        return this.unit;
    }

    public String getGroupAndName() {
        return this.group + "#" + this.name;
    }

    public boolean checkConsistency() {
        boolean check;

        // this.access has one of the allowed values
        check = (this.access.equals(HuaweiSun2000Variable.ACCESS_RO))
                || (this.access.equals(HuaweiSun2000Variable.ACCESS_WO))
                || (this.access.equals(HuaweiSun2000Variable.ACCESS_RW));

        // this.type has one of the allowed values
        check = check && ((this.type.equals(HuaweiSun2000Variable.TYPE_STRING))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_SIGNED_SHORT))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_UNSIGNED_SHORT))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_SIGNED_INTEGER))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_UNSIGNED_INTEGER))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_BITFIELD16))
                || (this.type.equals(HuaweiSun2000Variable.TYPE_BITFIELD32)));

        // length is set
        check = check && (this.quantity > 0);

        // gain is set
        check = check && (this.gain != 0);

        return check;
    }

    public boolean isNumber() {
        return NUMBER_TYPES.contains(this.type);
    }

    public boolean isString() {
        return HuaweiSun2000Variable.TYPE_STRING.equals(type);
    }

    @Override
    public String toString() {
        return "HuaweiSun2000Variable{" + "number=" + number + ", name='" + name + '\'' + ", access='" + access + '\''
                + ", type='" + type + '\'' + ", unit='" + unit + '\'' + ", gain=" + gain + ", address=" + address
                + ", quantity=" + quantity + ", group='" + group + '\'' + '}';
    }
}
