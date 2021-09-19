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

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.CHANNEL_1;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
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

    private @Nullable LinkTapConfiguration config;

    public LinkTapHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(LinkTapConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
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

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}
