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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link LinkTapBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Lukas Pindl - Initial contribution
 */
@NonNullByDefault
public class LinkTapBindingConstants {

    private static final String BINDING_ID = "linktap";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_TAP = new ThingTypeUID(BINDING_ID, "tap");
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // List of all Channel ids
    public static final String CHANNEL_MODE = "mode";
    public static final String CHANNEL_RUNNING = "running";
    public static final String CHANNEL_ON_DURATION = "on-duration";
    public static final String CHANNEL_ECO_MODE = "eco-mode";
    public static final String CHANNEL_ECO_ON = "eco-on";
    public static final String CHANNEL_ECO_OFF = "eco-off";
    public static final String CHANNEL_AUTO_BACK = "auto-back";
    public static final String CHANNEL_REMAINING_DURATION = "remaining-duration";
    public static final String CHANNEL_TOTAL_TIME = "total-time";
    public static final String CHANNEL_ECO_TOTAL = "eco-total";

    // Tap Modes
    public static final String TAP_MODE_INSTANT = "TAP_MODE_INSTANT";
    public static final String TAP_MODE_INTERVAL = "TAP_MODE_INTERVAL";
    public static final String TAP_MODE_ODD_EVEN = "TAP_MODE_ODD_EVEN";
    public static final String TAP_MODE_SEVEN_DAY = "TAP_MODE_SEVEN_DAY";
    public static final String TAP_MODE_MONTH = "TAP_MODE_MONTH";
    public static final String TAP_MODE_UNKNOWN = "TAP_MODE_UNKNOWN";
}
