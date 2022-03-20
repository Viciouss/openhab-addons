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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.Thing;

/**
 * The Huawei Sun 2000 Inverter Binding.
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
public class HuaweiSun2000HandlerTest {

    @Test
    void test_readVariables() {
        Thing thing = mock(Thing.class);
        HuaweiSun2000Handler huaweiSun2000Handler = new HuaweiSun2000Handler(thing);
        Map<String, HuaweiSun2000Variable> stringHuaweiSun2000VariableMap = huaweiSun2000Handler
                .readVariableDefinition();

        assertNotNull(stringHuaweiSun2000VariableMap);
        assertFalse(stringHuaweiSun2000VariableMap.isEmpty());
    }
}
