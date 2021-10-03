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

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.URI_GET_ALL_DEVICES;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.linktap.internal.config.LinkTapBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link LinkTapBridgeHandler} is responsible for initalizing the Bridge and giving necessary information to the
 * Things for API calls.
 *
 * @author Lukas Pindl - Initial contribution
 */
@NonNullByDefault
public class LinkTapBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(LinkTapBridgeHandler.class);
    private final HttpClient httpClient;

    public LinkTapBridgeHandler(Bridge thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not used in the Bridge
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Bridge");
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = false; // <background task with long running initialization here>
            // when done do:
            Request httpReq = httpClient.newRequest(URI_GET_ALL_DEVICES).method(HttpMethod.POST);

            JsonObject dataObject = getAuthObject();
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

            if ("".equals(responseBody)) {
                // No additional action for now
            } else {
                JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonElement result = responseObject.get("result");

                if (result.isJsonPrimitive()) {
                    if (result.getAsString().equals("ok")) {
                        thingReachable = true;
                    }
                }
            }

            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    public JsonObject getAuthObject() {
        LinkTapBridgeConfiguration config = getConfigAs(LinkTapBridgeConfiguration.class);
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("username", config.username);
        dataObject.addProperty("apiKey", config.apikey);

        return dataObject;
    }
}
