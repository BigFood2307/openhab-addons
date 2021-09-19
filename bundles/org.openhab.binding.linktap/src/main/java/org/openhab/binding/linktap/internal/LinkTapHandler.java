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
package org.openhab.binding.linktap.internal;

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    private @Nullable ScheduledFuture pollingJob;

    private @Nullable LinkTapConfiguration config;

    public LinkTapHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
        this.tapModel = new TapModel();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String cd = command.toString();
        logger.debug("Handling command: {}", cd);
        logger.debug("Channel UID: {}", channelUID.getId());
        if (command instanceof RefreshType) {
            // TODO: handle data refresh
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
                if (command instanceof DecimalType) {
                    updateOnDuration((DecimalType) command);
                }
            }
            if (CHANNEL_ECO_MODE.equals(channelUID.getId())) {
                if (command instanceof OnOffType) {
                    updateEcoMode((OnOffType) command);
                }
            }
            if (CHANNEL_ECO_ON.equals(channelUID.getId())) {
                if (command instanceof DecimalType) {
                    updateEcoOn((DecimalType) command);
                }
            }
            if (CHANNEL_ECO_OFF.equals(channelUID.getId())) {
                if (command instanceof DecimalType) {
                    updateEcoOff((DecimalType) command);
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
        config = getConfigAs(LinkTapConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollingCode, config.refreshInterval, config.refreshInterval,
                TimeUnit.MINUTES);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = getWateringStatus(); // <background task with long running initialization here>
            // when done do:

            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    private void pollingCode() {
        getWateringStatus();
    }

    private boolean getWateringStatus() {
        boolean success = false;

        Request httpReq = httpClient.newRequest("https://www.link-tap.com/api/getWateringStatus")
                .method(HttpMethod.POST);

        LinkTapBridgeHandler bridge = (LinkTapBridgeHandler) (getBridge().getHandler());

        JsonObject dataObject = bridge.getAuthObject();

        dataObject.addProperty("taplinkerId", config.tapId);

        String requestBodyPayload = dataObject.toString();

        httpReq = httpReq.content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug(responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if (responseBody.equals("")) {
            // No additional action for now
        } else {
            JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonElement result = responseObject.get("result");

            if (result.isJsonPrimitive()) {
                if (result.getAsString().equals("ok")) {
                    success = true;
                    logger.debug("Watering Status OK");
                    JsonElement status = responseObject.get("status");
                    if (status.isJsonNull()) {
                        logger.debug("Watering Status NULL");
                        updateState(CHANNEL_REMAINING_DURATION, new DecimalType(0));
                        updateState(CHANNEL_TOTAL_TIME, new DecimalType(0));
                        updateState(CHANNEL_ECO_TOTAL, new DecimalType(0));
                    } else {
                        JsonObject statusObject = status.getAsJsonObject();
                        updateState(CHANNEL_REMAINING_DURATION,
                                new DecimalType(statusObject.get("onDuration").getAsInt()));
                        updateState(CHANNEL_TOTAL_TIME, new DecimalType(statusObject.get("total").getAsInt()));
                        JsonElement ecoTotalElement = statusObject.get("ecoTotal");
                        if (!(ecoTotalElement == null || ecoTotalElement.isJsonNull())) {
                            updateState(CHANNEL_ECO_TOTAL, new DecimalType(statusObject.get("ecoTotal").getAsInt()));
                        } else {
                            updateState(CHANNEL_ECO_TOTAL, new DecimalType(0));
                        }
                    }
                }
            }
        }
        return success;
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

    private void updateEcoOff(DecimalType command) {
        tapModel.setEcoOffDuration(Math.floorDiv(command.intValue(), 60));
        tapModel.setEcoOffDurationSec(Math.floorMod(command.intValue(), 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ECO_OFF, command);
    }

    private void updateEcoOn(DecimalType command) {
        tapModel.setEcoOnDuration(Math.floorDiv(command.intValue(), 60));
        tapModel.setEcoOnDurationSec(Math.floorMod(command.intValue(), 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ECO_ON, command);
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

    private void updateOnDuration(DecimalType command) {
        tapModel.setOnDuration(Math.floorDiv(command.intValue(), 60));
        tapModel.setOnDurationSec(Math.floorMod(command.intValue(), 60));

        if (tapModel.getMode().equals(TAP_MODE_INSTANT)) {
            if (tapModel.isRunning()) {
                activateNewMode();
            }
        }
        updateState(CHANNEL_ON_DURATION, command);
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

        if (tapModel.isRunning()) {
            activateNewMode();
        }

        updateState(CHANNEL_MODE, command);
    }

    private boolean deactivateTap() {
        LinkTapBridgeHandler bridge = (LinkTapBridgeHandler) (getBridge().getHandler());
        JsonObject dataObject = bridge.getAuthObject();
        config = getConfigAs(LinkTapConfiguration.class);

        dataObject.addProperty("gatewayId", config.gatewayId);
        dataObject.addProperty("taplinkerId", config.tapId);
        dataObject.addProperty("action", false);
        dataObject.addProperty("duration", 0);

        String requestBodyPayload = dataObject.toString();

        Request httpReq = httpClient.newRequest("https://www.link-tap.com/api/activateInstantMode")
                .method(HttpMethod.POST).content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug(responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if (responseBody.equals("")) {
            // No additional action for now
        } else {
            JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonElement result = responseObject.get("result");

            if (result.isJsonPrimitive()) {
                if (result.getAsString().equals("ok")) {
                    logger.debug("Successfully Turned Tap off");
                    return true;
                } else {
                    String errorMessage = responseObject.get("message").getAsString();
                    logger.debug(errorMessage);
                }
            }
        }
        return false;
    }

    private boolean activateNewMode() {
        LinkTapBridgeHandler bridge = (LinkTapBridgeHandler) (getBridge().getHandler());
        String uri = "";
        JsonObject dataObject = bridge.getAuthObject();
        config = getConfigAs(LinkTapConfiguration.class);

        dataObject.addProperty("gatewayId", config.gatewayId);
        dataObject.addProperty("taplinkerId", config.tapId);

        switch (tapModel.getMode()) {
            case TAP_MODE_INSTANT: {
                dataObject.addProperty("action", true);
                dataObject.addProperty("duration", tapModel.getOnDuration());
                dataObject.addProperty("durationSec", tapModel.getOnDurationSec());
                dataObject.addProperty("autoBack", tapModel.isAutoBack());

                if (tapModel.isEcoMode()) {
                    dataObject.addProperty("eco", true);
                    dataObject.addProperty("ecoOn", tapModel.getEcoOnDuration());
                    dataObject.addProperty("ecoOnSec", tapModel.getEcoOnDurationSec());
                    dataObject.addProperty("ecoOff", tapModel.getEcoOffDuration());
                    dataObject.addProperty("ecoOffSec", tapModel.getEcoOnDurationSec());
                }

                uri = "https://www.link-tap.com/api/activateInstantMode";

                break;
            }
            case TAP_MODE_INTERVAL: {
                uri = "https://www.link-tap.com/api/activateIntervalMode";

                break;
            }
            case TAP_MODE_ODD_EVEN: {
                uri = "https://www.link-tap.com/api/activateOddEvenMode";

                break;
            }
            case TAP_MODE_SEVEN_DAY: {
                uri = "https://www.link-tap.com/api/activateSevenDayMode";

                break;
            }
            case TAP_MODE_MONTH: {
                uri = "https://www.link-tap.com/api/activateMonthMode";

                break;
            }
            default:
                break;
        }

        String requestBodyPayload = dataObject.toString();

        Request httpReq = httpClient.newRequest(uri).method(HttpMethod.POST)
                .content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug(responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if (responseBody.equals("")) {
            // No additional action for now
        } else {
            JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonElement result = responseObject.get("result");

            if (result.isJsonPrimitive()) {
                if (result.getAsString().equals("ok")) {
                    logger.debug("Successful Mode change");
                    return true;
                } else {
                    String errorMessage = responseObject.get("message").getAsString();
                    logger.debug(errorMessage);
                }
            }
        }

        return false;
    }
}
