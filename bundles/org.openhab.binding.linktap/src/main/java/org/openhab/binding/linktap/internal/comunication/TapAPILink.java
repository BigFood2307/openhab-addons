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

package org.openhab.binding.linktap.internal.comunication;

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.*;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.linktap.internal.config.LinkTapConfiguration;
import org.openhab.binding.linktap.internal.handler.LinkTapBridgeHandler;
import org.openhab.binding.linktap.internal.model.TapAPILinkResponse;
import org.openhab.binding.linktap.internal.model.TapModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link TapAPILink} is responsible for communication with the LinkTap API. It also makes sure, API Rate Limits are
 * not reached.
 *
 * @author Lukas Pindl - Initial contribution
 */

@NonNullByDefault
public class TapAPILink {

    private final Logger logger = LoggerFactory.getLogger(TapAPILink.class);
    private final HttpClient httpClient;
    private final TapModel tapModel;

    private LocalDateTime lastPoll;
    private LocalDateTime lastModeChange;

    private boolean updateModeFlag = false;

    public TapAPILink(HttpClient httpClient, TapModel tapModel) {
        this.httpClient = httpClient;
        this.tapModel = tapModel;
        lastPoll = LocalDateTime.MIN;
        lastModeChange = LocalDateTime.MIN;
    }

    public boolean handleMode(LinkTapConfiguration config, @Nullable LinkTapBridgeHandler bridgeHandler) {
        if (updateModeFlag && lastModeChange.plusSeconds(config.refreshInterval).isBefore(LocalDateTime.now())) {
            updateModeFlag = false;
            lastModeChange = LocalDateTime.now();
            if (tapModel.isRunning()) {
                return activateNewMode(config, bridgeHandler);
            } else {
                return deactivateTap(config, bridgeHandler);
            }
        }
        return false;
    }

    public void updateMode() {
        updateModeFlag = true;
    }

    @SuppressWarnings("null")
    private boolean activateNewMode(LinkTapConfiguration config, @Nullable LinkTapBridgeHandler bridgeHandler) {

        String uri = "";
        JsonObject dataObject = bridgeHandler.getAuthObject();

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

                uri = URI_ACTIVATE_INSTANT;

                break;
            }
            case TAP_MODE_INTERVAL: {
                uri = URI_ACTIVATE_INTERVAL;

                break;
            }
            case TAP_MODE_ODD_EVEN: {
                uri = URI_ACTIVATE_ODD_EVEN;

                break;
            }
            case TAP_MODE_SEVEN_DAY: {
                uri = URI_ACTIVATE_SEVEN_DAY;

                break;
            }
            case TAP_MODE_MONTH: {
                uri = URI_ACTIVATE_MONTH;

                break;
            }
            default:
                logger.warn("Invalid Mode String: {}", tapModel.getMode());
                return false;
        }

        String requestBodyPayload = dataObject.toString();

        Request httpReq = httpClient.newRequest(uri).method(HttpMethod.POST)
                .content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug("Got response: {} ", responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if ("".equals(responseBody)) {
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
                    logger.warn("Got Error: {} ", errorMessage);
                }
            }
        }

        return false;
    }

    @SuppressWarnings("null")
    private boolean deactivateTap(LinkTapConfiguration config, @Nullable LinkTapBridgeHandler bridgeHandler) {

        JsonObject dataObject = bridgeHandler.getAuthObject();

        dataObject.addProperty("gatewayId", config.gatewayId);
        dataObject.addProperty("taplinkerId", config.tapId);
        dataObject.addProperty("action", false);
        dataObject.addProperty("duration", 0);

        String requestBodyPayload = dataObject.toString();

        Request httpReq = httpClient.newRequest(URI_ACTIVATE_INSTANT).method(HttpMethod.POST)
                .content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug("Got response: {} ", responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if ("".equals(responseBody)) {
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
                    logger.warn("Got Error: {} ", errorMessage);
                }
            }
        }
        return false;
    }

    @SuppressWarnings("null")
    public TapAPILinkResponse getWateringStatus(LinkTapConfiguration config,
            @Nullable LinkTapBridgeHandler bridgeHandler) {
        boolean success = false;
        TapAPILinkResponse linkResponse = new TapAPILinkResponse(success, 0, 0, 0);

        if (lastPoll.plusSeconds(config.refreshInterval).isAfter(LocalDateTime.now())) {
            return linkResponse;
        }

        lastPoll = LocalDateTime.now();

        Request httpReq = httpClient.newRequest(URI_GET_WATERING_STATUS).method(HttpMethod.POST);

        JsonObject dataObject = bridgeHandler.getAuthObject();

        dataObject.addProperty("taplinkerId", config.tapId);

        String requestBodyPayload = dataObject.toString();

        httpReq = httpReq.content(new StringContentProvider(requestBodyPayload), "application/json");

        String responseBody = "";

        try {
            ContentResponse response = httpReq.send();
            responseBody = response.getContentAsString();
            logger.debug("Got response: {} ", responseBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.warn("Http Error");
        }

        if ("".equals(responseBody)) {
            // No additional action for now
        } else {
            JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonElement result = responseObject.get("result");

            if (result.isJsonPrimitive()) {
                if (result.getAsString().equals("ok")) {
                    success = true;
                    logger.debug("Watering Result OK");
                    JsonElement status = responseObject.get("status");
                    if (status.isJsonNull()) {
                        logger.debug("Watering Result NULL");
                        linkResponse.success = success;
                    } else {
                        JsonObject statusObject = status.getAsJsonObject();
                        linkResponse.success = success;
                        linkResponse.remainingDuration = statusObject.get("onDuration").getAsInt();
                        linkResponse.totalTime = statusObject.get("total").getAsInt();

                        JsonElement ecoTotalElement = statusObject.get("ecoTotal");
                        if (!(ecoTotalElement == null || ecoTotalElement.isJsonNull())) {
                            linkResponse.ecoTotal = ecoTotalElement.getAsInt();
                        }
                    }
                }
            }
        }
        return linkResponse;
    }
}
