/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.hardware;

import java.io.File;
import org.cyanogenmod.hardware.util.FileUtils;

/*
 * Button backlight brightness adjustment
 *
 * Exports methods to get the valid value boundaries, the
 * default and current backlight brightness, and a method to set
 * the backlight brightness.
 *
 * Values exported by min/max can be the direct values required
 * by the hardware, or a local (to ButtonBacklightBrightness) abstraction
 * that's internally converted to something else prior to actual use. The
 * Settings user interface will normalize these into a 0-100 (percentage)
 * scale before showing them to the user, but all values passed to/from
 * the client (Settings) are in this class' scale.
 */

public class ButtonBacklight {
    private static final String BRIGHTNESS_FILE = "/sys/class/leds/button-backlight/button_brightness";

    /*
     * All HAF classes should export this boolean.
     * Real implementations must, of course, return true
     */

    public static boolean isSupported() {
        try {
            int brightness = getCurBrightness();
            return setBrightness(brightness);
        } catch (Exception e) {
            return false;
        }
    }

    /*
     * Set the button backlight brightness to given integer input. That'll
     * be a value between the boundaries set by get(Max|Min)Intensity
     * (see below), and it's meant to be locally interpreted/used.
     */

    public static boolean setBrightness(int brightness)  {
        return FileUtils.writeLine(BRIGHTNESS_FILE, String.valueOf(brightness));
    }

    /*
     * What's the maximum integer value we take for setBrightness()?
     */

    public static int getMaxBrightness()  {
        return 255;
    }

    /*
     * What's the minimum integer value we take for setBrightness()?
     */

    public static int getMinBrightness()  {
        // 0 resets to default value in LED kernel driver
        return 1;
    }

    /*
     * What's the current brightness value?
     */

    public static int getCurBrightness()  {
        return Integer.parseInt(FileUtils.readOneLine(BRIGHTNESS_FILE));
    }

    /*
     * What's the shipping brightness value?
     */

    public static int getDefaultBrightness()  {
        return 140;
    }
}
