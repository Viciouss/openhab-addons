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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.modbus.handler.ModbusEndpointThingHandler;
import org.openhab.core.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.core.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusRegisterArray;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * The Huawei Sun 2000 Inverter Binding.
 * <p>
 * Based on HeliosEasyControlsHandler by Bernhard Bauer
 *
 * @author Martin Juecker - Initial contribution
 */
@NonNullByDefault
public class HuaweiSun2000Handler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HuaweiSun2000Handler.class);

    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable Map<String, HuaweiSun2000Variable> variableMap;

    private final Gson gson = new Gson();

    private @Nullable ModbusCommunicationInterface comms;

    private final Map<ModbusSlaveEndpoint, Semaphore> transactionLocks = new ConcurrentHashMap<>();

    private final Map<String, Integer> lastState = new HashMap<>();

    public HuaweiSun2000Handler(Thing thing) {
        super(thing);
    }

    @Nullable
    Map<String, HuaweiSun2000Variable> readVariableDefinition() {
        Type vMapType = new TypeToken<List<HuaweiSun2000Variable>>() {
        }.getType();
        List<HuaweiSun2000Variable> entries = null;
        try (InputStreamReader jsonFile = new InputStreamReader(
                getClass().getResourceAsStream(HuaweiSun2000BindingConstants.VARIABLES_DEFINITION_FILE));
                BufferedReader reader = new BufferedReader(jsonFile)) {
            entries = gson.fromJson(reader, vMapType);

        } catch (IOException e) {
            this.handleError("Error reading variable definition file", ThingStatusDetail.CONFIGURATION_ERROR);
        }
        if (entries != null) {
            Map<String, HuaweiSun2000Variable> variableMap = new HashMap<>();
            entries.forEach(e -> {
                if (e.checkConsistency()) {
                    variableMap.put(e.getName(), e);
                } else {
                    this.handleError(
                            "Variable definition for variable " + e.getName() + " is not supported or inconsistent.",
                            ThingStatusDetail.CONFIGURATION_ERROR);
                }
            });
            return variableMap;
        } else {
            this.handleError("Variables definition file not found or of illegal format",
                    ThingStatusDetail.CONFIGURATION_ERROR);
        }
        return null;
    }

    /**
     * Get the endpoint handler from the bridge this handler is connected to
     * Checks that we're connected to the right type of bridge
     *
     * @return the endpoint handler or null if the bridge does not exist
     */
    private @Nullable ModbusEndpointThingHandler getEndpointThingHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Bridge is null");
            return null;
        }
        if (bridge.getStatus() != ThingStatus.ONLINE) {
            logger.debug("Bridge is not online");
            return null;
        }

        ThingHandler handler = bridge.getHandler();
        if (handler == null) {
            logger.debug("Bridge handler is null");
            return null;
        }

        if (handler instanceof ModbusEndpointThingHandler) {
            return (ModbusEndpointThingHandler) handler;
        } else {
            logger.debug("Unexpected bridge handler: {}", handler);
            return null;
        }
    }

    /**
     * Get a reference to the modbus endpoint
     */
    private void connectEndpoint() {
        if (this.comms != null) {
            return;
        }

        ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        if (slaveEndpointThingHandler == null) {
            String label = Optional.ofNullable(getBridge()).map(Thing::getLabel).orElse("<null>");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' is offline", label));
            logger.debug("No bridge handler available -- aborting init for {}", label);
            return;
        }

        comms = slaveEndpointThingHandler.getCommunicationInterface();

        if (comms == null) {
            String label = Optional.ofNullable(getBridge()).map(Thing::getLabel).orElse("<null>");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' not completely initialized", label));
            logger.debug("Bridge not initialized fully (no endpoint) -- aborting init for {}", this);
            return;
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        HuaweiSun2000Configuration config = getConfigAs(HuaweiSun2000Configuration.class);

        @Nullable
        Map<String, HuaweiSun2000Variable> variableMap = this.readVariableDefinition();
        if (variableMap == null) {
            this.handleError("Variable definition is unavailable", ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }
        this.variableMap = variableMap;

        this.connectEndpoint();
        @Nullable
        ModbusCommunicationInterface comms = this.comms;
        if (comms == null) {
            this.handleError("Modbus communication interface is unavailable", ThingStatusDetail.COMMUNICATION_ERROR);
            return;
        }

        this.transactionLocks.putIfAbsent(comms.getEndpoint(), new Semaphore(1, true));

        // these values will never change, so we only read them once on init
        scheduler.execute(() -> {
            readValue(HuaweiSun2000BindingConstants.INVERTER_BASICS, this::consumeInverterBasics);
        });

        // poll for status updates regularly
        this.pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            readValue(HuaweiSun2000BindingConstants.INVERTER_STATE, this::consumeInverterState);
            readValue(HuaweiSun2000BindingConstants.INVERTER_ADJUSTMENTS, this::consumeInverterAdjustments);
        }, 5000, config.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    @Nullable
    private HuaweiSun2000RegisterGroup getGroup(@NotNull String groupName) {
        switch (groupName) {
            case HuaweiSun2000BindingConstants.INVERTER_BASICS_NAME:
                return HuaweiSun2000BindingConstants.INVERTER_BASICS;
            case HuaweiSun2000BindingConstants.INVERTER_STATE_NAME:
                return HuaweiSun2000BindingConstants.INVERTER_STATE;
            case HuaweiSun2000BindingConstants.INVERTER_ADJUSTMENTS_NAME:
                return HuaweiSun2000BindingConstants.INVERTER_ADJUSTMENTS;
        }
        handleError("Group name " + groupName + " could not be resolved.", ThingStatusDetail.CONFIGURATION_ERROR);
        return null;
    }

    private Consumer<ModbusRegisterArray> getConsumerForGroup(@NotNull String groupName) {
        switch (groupName) {
            case HuaweiSun2000BindingConstants.INVERTER_BASICS_NAME:
                return this::consumeInverterBasics;
            case HuaweiSun2000BindingConstants.INVERTER_STATE_NAME:
                return this::consumeInverterState;
            case HuaweiSun2000BindingConstants.INVERTER_ADJUSTMENTS_NAME:
                return this::consumeInverterAdjustments;
        }

        handleError("Group name " + groupName + " could not be resolved to a consumer.",
                ThingStatusDetail.CONFIGURATION_ERROR);
        return a -> {
        };
    }

    private void consumeInverterBasics(ModbusRegisterArray modbusRegisterArray) {
        HuaweiSun2000RegisterGroup registerGroup = HuaweiSun2000BindingConstants.INVERTER_BASICS;
        updateStates(modbusRegisterArray, registerGroup);
    }

    /**
     * This method iterates over all available variables and matches those in the group, it then looks at the returned
     * byte array and with the help of the offset and length of the variable, we can get the bytes that match this
     * variable.
     */
    private void updateStates(ModbusRegisterArray modbusRegisterArray, HuaweiSun2000RegisterGroup registerGroup) {
        int groupStartAddress = registerGroup.getStartAddress();
        @Nullable
        Map<String, HuaweiSun2000Variable> variableMap = this.variableMap;
        if (variableMap == null) {
            return;
        }
        variableMap.values().stream()
                .filter(currentVariable -> registerGroup.getGroupeName().equals(currentVariable.getGroup()))
                .forEach(currentVariable -> {
                    HuaweiSun2000Variable variable = variableMap.get(currentVariable.getName());
                    if (variable != null) {
                        int variableOffset = currentVariable.getAddress() - groupStartAddress;
                        byte[] bytes = modbusRegisterArray.getBytes();
                        byte[] target = new byte[2 * currentVariable.getQuantity()];
                        System.arraycopy(bytes, 2 * variableOffset, target, 0, 2 * currentVariable.getQuantity());
                        updateState(variable, target);
                    }
                });
    }

    private void consumeInverterState(ModbusRegisterArray modbusRegisterArray) {
        HuaweiSun2000RegisterGroup registerGroup = HuaweiSun2000BindingConstants.INVERTER_STATE;
        updateStates(modbusRegisterArray, registerGroup);
    }

    private void consumeInverterAdjustments(ModbusRegisterArray modbusRegisterArray) {
        HuaweiSun2000RegisterGroup registerGroup = HuaweiSun2000BindingConstants.INVERTER_ADJUSTMENTS;
        updateStates(modbusRegisterArray, registerGroup);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null) {
            pollingJob.cancel(true);
        }
        this.comms = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // we only read values for now
        if (command instanceof RefreshType) {
            String groupId = channelUID.getGroupId();
            if (groupId != null) {
                scheduler.submit(() -> {
                    @Nullable
                    HuaweiSun2000RegisterGroup group = getGroup(groupId);
                    if (group != null) {
                        readValue(group, getConsumerForGroup(groupId));
                    }
                });
            } else {
                handleError("Group Id for channel " + channelUID.getId() + " is null!",
                        ThingStatusDetail.CONFIGURATION_ERROR);
            }
        }
    }

    /**
     * Read a register group and update all attached variables
     */
    public void readValue(HuaweiSun2000RegisterGroup registerGroup, Consumer<ModbusRegisterArray> consumer) {
        ModbusCommunicationInterface comms = this.comms;
        if (comms == null) {
            this.handleError("Modbus communication interface is unavailable", ThingStatusDetail.COMMUNICATION_ERROR);
            return;
        }

        final Semaphore lock = transactionLocks.get(comms.getEndpoint());
        if (lock != null) {
            try {
                lock.acquire(); // will block until lock is available
            } catch (InterruptedException e) {
                logger.warn("{} encountered Exception when trying to read variable group {} from the device: {}",
                        HuaweiSun2000Handler.class.getSimpleName(), registerGroup.getGroupeName(), e.getMessage());
                return;
            }
            int registerAddress = registerGroup.getStartAddress();
            int registerLength = registerGroup.getEndAddress() - registerAddress;
            comms.submitOneTimePoll(new ModbusReadRequestBlueprint(HuaweiSun2000BindingConstants.UNIT_ID,
                    ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, registerAddress, registerLength,
                    HuaweiSun2000BindingConstants.MAX_TRIES), pollResult -> {
                        lock.release();
                        Optional<ModbusRegisterArray> registers = pollResult.getRegisters();
                        registers.ifPresent(consumer);
                    }, failureInfo -> {
                        lock.release();
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                "Error reading from device: " + failureInfo.getCause().getMessage());
                    });
        }
    }

    private @Nullable QuantityType<?> toQuantityType(double value, @Nullable String unit) {
        if (unit == null) {
            return null;
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_AMPERE)) {
            return new QuantityType<>(value, Units.AMPERE);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_WATT)) {
            return new QuantityType<>(value, Units.WATT);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_HZ)) {
            return new QuantityType<>(value, Units.HERTZ);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_SEC)) {
            return new QuantityType<>(value, Units.SECOND);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_VOLT)) {
            return new QuantityType<>(value, Units.VOLT);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_KILOWATT)) {
            return new QuantityType<>(value * 1000, Units.WATT);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_KILOWATT_HOURS)) {
            return new QuantityType<>(value, Units.KILOWATT_HOUR);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_PERCENT)) {
            return new QuantityType<>(value, Units.PERCENT);
        } else if (unit.equals(HuaweiSun2000Variable.UNIT_TEMP)) {
            return new QuantityType<>(value, SIUnits.CELSIUS);
        } else {
            return null;
        }
    }

    private void updateState(HuaweiSun2000Variable variable, byte[] content) {
        // System date and time

        if (HuaweiSun2000Variable.TYPE_BITFIELD16.equals(variable.getType())
                || HuaweiSun2000Variable.TYPE_BITFIELD32.equals(variable.getType())) {
            updateBitfieldState(variable, content);
        } else {
            Channel channel = getThing().getChannel(variable.getGroupAndName());
            String itemType;
            if (channel != null) {
                itemType = channel.getAcceptedItemType();
                if (itemType != null) {
                    if (itemType.startsWith("Number:")) {
                        itemType = "Number";
                    }

                    switch (itemType) {
                        case "Number":
                            if (variable.isNumber()) {
                                State state = null;
                                int result = 0;
                                for (int i = 0; i < content.length; i++) {
                                    result = (result << 8) | (content[i] & 0xFF);
                                }
                                if (variable.getUnit() == null) {
                                    state = new DecimalType(result);
                                } else {
                                    String unit = variable.getUnit();
                                    BigDecimal dividend = BigDecimal.valueOf(result);
                                    BigDecimal divisor = BigDecimal.valueOf(variable.getGain());
                                    BigDecimal quotient = dividend.divide(divisor);
                                    state = toQuantityType(quotient.doubleValue(), unit);
                                }
                                if (state != null) {
                                    updateState(variable.getGroupAndName(), state);
                                    updateStatus(ThingStatus.ONLINE);
                                }
                            }
                            break;
                        case "String":
                            if (variable.isString()) {
                                ByteBuffer buffer = ByteBuffer.wrap(content);
                                updateState(variable.getGroupAndName(), StringType.valueOf(toString(buffer)));
                                updateStatus(ThingStatus.ONLINE);
                            }
                            break;
                    }
                } else { // itemType was null
                    logger.warn("{} couldn't determine item type of variable {}",
                            HuaweiSun2000Handler.class.getSimpleName(), variable.getName());
                }
            } else { // channel was null
                logger.warn("{} couldn't find channel for variable {}", HuaweiSun2000Handler.class.getSimpleName(),
                        variable.getName());
            }
        }
    }

    private void updateBitfieldState(HuaweiSun2000Variable variable, byte[] content) {
        int currentValue = 0;
        for (int i = 0; i < content.length; i++) {
            currentValue = (currentValue << 8) | (content[i] & 0xFF);
        }
        String groupAndName = variable.getGroupAndName();
        Integer lastValue = lastState.get(groupAndName);

        int bitfieldLength = content.length * 8;
        for (int i = 0; i < bitfieldLength; i++) {
            String channelName = groupAndName + "_" + (i + 1);
            Channel channel = getThing().getChannel(channelName);
            if (channel == null) {
                break;
            }

            boolean wasSet = lastValue != null && (((0b1 << i) & lastValue) > 0);
            boolean isSet = ((0b1 << i) & currentValue) > 0;
            if (wasSet != isSet || lastValue == null) {
                if (channelName.contains("alarm")) {
                    if (isSet && !wasSet) {
                        triggerChannel(channel.getUID(), "1");
                    } else if (!isSet && wasSet) {
                        triggerChannel(channel.getUID(), "0");
                    }
                } else {
                    updateState(channel.getUID(), new DecimalType(isSet ? 1 : 0));
                }
            }
        }
        lastState.put(groupAndName, currentValue);
    }

    private String toString(ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    /**
     * Logs an error (as a warning entry) and updates the thing status
     *
     * @param errorMsg The error message to be logged and provided with the Thing's status update
     * @param status The Thing's new status
     */
    private void handleError(String errorMsg, ThingStatusDetail status) {
        updateStatus(ThingStatus.OFFLINE, status, errorMsg);
    }
}
