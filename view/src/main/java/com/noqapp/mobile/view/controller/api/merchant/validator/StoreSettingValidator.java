package com.noqapp.mobile.view.controller.api.merchant.validator;

import com.noqapp.domain.json.JsonHour;

import org.apache.commons.text.WordUtils;

import java.time.DayOfWeek;
import java.util.List;

/**
 * hitender
 * 9/20/20 12:20 PM
 */
public class StoreSettingValidator {
    public static String validateBusinessHours(List<JsonHour> jsonHours) {
        for (JsonHour jsonHour : jsonHours) {
            if (!jsonHour.isDayClosed()) {
                if (jsonHour.getStartHour() == 0) {
                    return "Specify Store Start Time for " + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name());
                }

                if (jsonHour.getEndHour() == 0) {
                    return "Specify Store Close Time for " + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name());
                }

                if (jsonHour.getTokenAvailableFrom() == 0) {
                    return "Specify Token Available Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + ". This is the time from when Token would be available to users.";
                }

                if (jsonHour.getStartHour() > 2359) {
                    return "Store Start Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getEndHour() > 2359) {
                    return "Store Close Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getTokenAvailableFrom() > 2359) {
                    return "Token Available Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getTokenNotAvailableFrom() > 2359) {
                    return "Token Not Available After for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getLunchTimeStart() > 2359) {
                    return "Lunch Start Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getLunchTimeEnd() > 2359) {
                    return "Lunch End Time for  "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " cannot exceed 2359";
                }

                if (jsonHour.getLunchTimeStart() != 0 && jsonHour.getLunchTimeStart() < jsonHour.getStartHour()) {
                    return "Lunch Start Time for  "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " should be after store start time. Or set value to 0.";
                }

                if (jsonHour.getLunchTimeEnd() != 0 && (jsonHour.getLunchTimeEnd() > jsonHour.getEndHour() || jsonHour.getLunchTimeEnd() < jsonHour.getStartHour())) {
                    if (jsonHour.getLunchTimeEnd() > jsonHour.getEndHour()) {
                        return "Lunch End Time for  "
                            + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                            + " should be before store close time.";
                    }

                    if (jsonHour.getLunchTimeEnd() < jsonHour.getStartHour()) {
                        return "Lunch End Time for  "
                            + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                            + " should be after store start time.";
                    }
                }

                if (jsonHour.getLunchTimeStart() > 0) {
                    if (jsonHour.getLunchTimeEnd() == 0) {
                        return "Lunch End Time for  "
                            + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                            + " should be set or remove Lunch Start Time";
                    }
                }

                if (jsonHour.getLunchTimeEnd() > 0) {
                    if (jsonHour.getLunchTimeStart() == 0) {
                        return "Lunch Start Time for  "
                            + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                            + " should be set or remove Lunch End Time.";
                    }
                }

                if (jsonHour.getLunchTimeStart() == jsonHour.getStartHour() || jsonHour.getLunchTimeEnd() == jsonHour.getEndHour()) {
                    return "Lunch Start Time or End Time for  "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " should not match. Nor Start Time or Close Time should match.";
                }

                if (jsonHour.getLunchTimeStart() > jsonHour.getLunchTimeEnd()) {
                    return "Lunch Start Time for "
                        + WordUtils.capitalizeFully(DayOfWeek.of(jsonHour.getDayOfWeek()).name())
                        + " should be before Lunch End Time.";
                }
            }
        }

        return null;
    }
}
