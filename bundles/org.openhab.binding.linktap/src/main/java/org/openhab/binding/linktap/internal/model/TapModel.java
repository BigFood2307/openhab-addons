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

import static org.openhab.binding.linktap.internal.LinkTapBindingConstants.TAP_MODE_UNKNOWN;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TapModel} stores data on the tap to use at a later point.
 *
 * @author Lukas Pindl - Initial contribution
 *
 */
@NonNullByDefault
public class TapModel {
    private String mode;
    private boolean running;
    private int onDuration;
    private int onDurationSec;
    private boolean ecoMode;
    private int ecoOnDuration;
    private int ecoOnDurationSec;
    private int ecoOffDuration;
    private int ecoOffDurationSec;
    private boolean autoBack;

    public TapModel() {
        this.mode = TAP_MODE_UNKNOWN;
        this.setRunning(false);
        this.setOnDuration(1);
        this.setOnDurationSec(0);
        this.setEcoMode(false);
        this.setEcoOnDuration(1);
        this.setEcoOnDurationSec(0);
        this.setEcoOffDuration(1);
        this.setEcoOnDurationSec(0);
        this.setAutoBack(false);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getOnDuration() {
        return onDuration;
    }

    public void setOnDuration(int onDuration) {
        this.onDuration = onDuration;
    }

    public int getOnDurationSec() {
        return onDurationSec;
    }

    public void setOnDurationSec(int onDurationSec) {
        this.onDurationSec = onDurationSec;
    }

    public boolean isEcoMode() {
        return ecoMode;
    }

    public void setEcoMode(boolean ecoMode) {
        this.ecoMode = ecoMode;
    }

    public int getEcoOnDuration() {
        return ecoOnDuration;
    }

    public void setEcoOnDuration(int ecoOnDuration) {
        this.ecoOnDuration = ecoOnDuration;
    }

    public int getEcoOnDurationSec() {
        return ecoOnDurationSec;
    }

    public void setEcoOnDurationSec(int ecoOnDurationSec) {
        this.ecoOnDurationSec = ecoOnDurationSec;
    }

    public int getEcoOffDuration() {
        return ecoOffDuration;
    }

    public void setEcoOffDuration(int ecoOffDuration) {
        this.ecoOffDuration = ecoOffDuration;
    }

    public int getEcoOffDurationSec() {
        return ecoOffDurationSec;
    }

    public void setEcoOffDurationSec(int ecoOffDurationSec) {
        this.ecoOffDurationSec = ecoOffDurationSec;
    }

    public boolean isAutoBack() {
        return autoBack;
    }

    public void setAutoBack(boolean autoBack) {
        this.autoBack = autoBack;
    }
}
