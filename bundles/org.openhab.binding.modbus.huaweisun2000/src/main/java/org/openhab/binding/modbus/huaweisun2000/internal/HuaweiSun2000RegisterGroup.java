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

/**
 * The Huawei Sun 2000 Inverter Binding.
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
public class HuaweiSun2000RegisterGroup {

    /**
     * the group name that matches the channel group name
     */
    private final String groupeName;
    /**
     * start address of a register area to be read
     */
    private final int startAddress;
    /**
     * the end address includes the last items offset and may not be higher than startAddress + 125
     */
    private final int endAddress;

    public HuaweiSun2000RegisterGroup(String groupeName, int startAddress, int endAddress) {
        this.groupeName = groupeName;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    public String getGroupeName() {
        return groupeName;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getEndAddress() {
        return endAddress;
    }
}
