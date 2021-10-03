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
package org.openhab.binding.linktap.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * is a Container for information received and forwarded during API Calls.
 *
 * @author Lukas Pindl - Initial contribution
 */

@NonNullByDefault
public class TapAPILinkResponse {

    public boolean success;
    public int remainingDuration;
    public int totalTime;
    public int ecoTotal;

    public TapAPILinkResponse(boolean success, int remainingDuration, int totalTime, int ecoTotal) {

        this.success = success;
        this.remainingDuration = remainingDuration;
        this.totalTime = totalTime;
        this.ecoTotal = ecoTotal;
    }
}
