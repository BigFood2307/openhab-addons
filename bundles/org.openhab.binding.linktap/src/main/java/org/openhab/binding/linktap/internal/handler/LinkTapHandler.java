/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.linktap.internal.handler;

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Time;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.linktap.internal.comunication.TapAPILink;
import org.openhab.binding.linktap.internal.config.LinkTapConfiguration;
import org.openhab.binding.linktap.internal.model.TapAPILinkResponse;
import org.openhab.binding.linktap.internal.model.TapModel;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LinkTapHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Lukas Pindl - Initial contribution
 */
@NonNullByDefault
public class LinkTapHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(LinkTapHandler.class);
    private final HttpClient httpClient;
    private final TapModel tapModel;
    private final TapAPILink apiLink;

    @SuppressWarnings("unused")
    private @Nullable ScheduledFuture<?> pollingJob;
    @SuppressWarnings("unused")
    private @Nullable ScheduledFuture<?> modeHandlingJob;

    public LinkTapHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
        this.tapModel = new TapModel();
        this.apiLink = new TapAPILink(this.httpClient, this.tapModel);
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String cd = command.toString();
        logger.debug("Handling command: {}", cd);
        logger.debug("Channel UID: {}", channelUID.getId());
        if (command instanceof RefreshType) {
            pollingCode();
        } else {
            logger.debug("Not Refresh");
            if (CHANNEL_MODE.equals(channelUID.getId())) {
                if (command instanceof StringType) {
                    updateTapMode((StringType) command);
                }
            }
            if (CHANNEL_RUNNING.equals(channelUID.getId())) {
                if (command instanceof OnOffType) {
                    updateRunning((OnOffType) command);
                }
            }
            if (CHANNEL_ON_DURATION.equals(channelUID.getId())) {
                if (command instanceof QuantityType<?>
                        && ((QuantityType<?>) command).getUnit().isCompatible(Units.SECOND)) {
                    updateOnDuration(((QuantityType<Time>) command).toUnit(Units.SECOND).intValue());
                } else if (command instanceof DecimalType) {
                    updateOnDuration(((DecimalType) command).intValue());
                }
            }
            if (CHANNEL_ECO_MODE.equals(channelUID.getId())) {
                if (command instanceof OnOffType) {
                    updateEcoMode((OnOffType) command);
                }
            }
            if (CHANNEL_ECO_ON.equals(channelUID.getId())) {
                if (command instanceof QuantityType<?>
                        && ((QuantityType<?>) command).getUnit().isCompatible(Units.SECOND)) {
                    updateEcoOn(((QuantityType<Time>) command).toUnit(Units.SECOND).intValue());
                } else if (command instanceof DecimalType) {
                    updateEcoOn(((DecimalType) command).intValue());
                }
            }
            if (CHANNEL_ECO_OFF.equals(channelUID.getId())) {
                if (command instanceof QuantityType<?>
                        && ((QuantityType<?>) command).getUnit().isCompatible(Units.SECOND)) {
                    updateEcoOff(((QuantityType<Time>) command).toUnit(Units.SECOND).intValue());
                } else if (command instanceof DecimalType) {
                    updateEcoOff(((DecimalType) command).intValue());
                }
            }
            if (CHANNEL_AUTO_BACK.equals(channelUID.getId())) {
                if (command instanceof OnOffType) {
                    updateAutoBack((OnOffType) command);
                }
            }
        }
    }

    @Override
    public void initialize() {
        LinkTapConfiguration config = getConfigAs(LinkTapConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollingCode, SECONDS_BETWEEN_MODE_HANDLING,
                SECONDS_BETWEEN_MODE_HANDLING, TimeUnit.SECONDS);
        modeHandlingJob = scheduler.scheduleWithFixedDelay(this::handleMode, config.refreshInterval,
                config.refreshInterval, TimeUnit.SECONDS);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = pollingCode(); // <background task with long running initialization here>
            // when done do:

            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    private void handleMode() {
        LinkTapConfiguration config = getConfigAs(LinkTapConfiguration.class);
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("No API Bridge Defined. Cannot connect.");
            return;
        }
        LinkTapBridgeHandler bridgeHandler = (LinkTapBridgeHandler) bridge.getHandler();

        apiLink.handleMode(config, bridgeHandler);
    }

    private boolean pollingCode() {
        LinkTapConfiguration config = getConfigAs(LinkTapConfiguration.class);
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("No API Bridge Defined. Cannot connect.");
            return false;
        }
        LinkTapBridgeHandler bridgeHandler = (LinkTapBridgeHandler) bridge.getHandler();

        TapAPILinkResponse linkResponse = apiLink.getWateringStatus(config, bridgeHandler);

        if (linkResponse.success) {
            updateState(CHANNEL_REMAINING_DURATION,
                    new QuantityType<Time>(linkResponse.remainingDuration, Units.MINUTE));
            updateState(CHANNEL_TOTAL_TIME, new QuantityType<Time>(linkResponse.totalTime, Units.MINUTE));
            updateState(CHANNEL_ECO_TOTAL, new QuantityType<Time>(linkResponse.ecoTotal, Units.MINUTE));
        }

        return linkResponse.success;
    }

    private void updateAutoBack(OnOffType command) {
        tapModel.setAutoBack(command.equals(OnOffType.ON));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_AUTO_BACK, command);
    }

    private void updateEcoOff(int command) {
        tapModel.setEcoOffDuration(Math.floorDiv(command, 60));
        tapModel.setEcoOffDurationSec(Math.floorMod(command, 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ECO_OFF, new QuantityType<Time>(command, Units.SECOND));
    }

    private void updateEcoOn(int command) {
        tapModel.setEcoOnDuration(Math.floorDiv(command, 60));
        tapModel.setEcoOnDurationSec(Math.floorMod(command, 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ECO_ON, new QuantityType<Time>(command, Units.SECOND));
    }

    private void updateEcoMode(OnOffType command) {
        tapModel.setEcoMode(command.equals(OnOffType.ON));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ECO_MODE, command);
    }

    private void updateOnDuration(int command) {
        tapModel.setOnDuration(Math.floorDiv(command, 60));
        tapModel.setOnDurationSec(Math.floorMod(command, 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ON_DURATION, new QuantityType<Time>(command, Units.SECOND));
    }

    private void updateRunning(OnOffType command) {
        tapModel.setRunning(command.equals(OnOffType.ON));

        if (!tapModel.getMode().equals(TAP_MODE_UNKNOWN)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            } else {
                deactivateTap();
            }
        }

        updateState(CHANNEL_RUNNING, command);
    }

    private void updateTapMode(StringType command) {
        tapModel.setMode(command.toString());
        logger.debug("Updating Tap Mode: {}", command.toString());

        if (tapModel.isRunning()) {
            activateNewMode();
        }

        updateState(CHANNEL_MODE, command);
    }

    public void deactivateTap() {
        apiLink.updateMode();
    }

    private void activateNewMode() {
        apiLink.updateMode();
    }
}
