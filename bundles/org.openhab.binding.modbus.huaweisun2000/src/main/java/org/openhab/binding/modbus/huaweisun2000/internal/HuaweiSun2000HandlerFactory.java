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

import static org.openhab.binding.modbus.huaweisun2000.internal.HuaweiSun2000BindingConstants.THING_TYPE_HUAWEI_SUN2000_INVERTER;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * The Huawei Sun 2000 Inverter Binding.
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.huawei-sun2000", service = ThingHandlerFactory.class)
public class HuaweiSun2000HandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(THING_TYPE_HUAWEI_SUN2000_INVERTER);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (THING_TYPE_HUAWEI_SUN2000_INVERTER.equals(thingTypeUID)) {
            return new HuaweiSun2000Handler(thing);
        }
        return null;
    }
}
