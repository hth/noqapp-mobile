package com.noqapp.mobile.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * User: hitender
 * Date: 3/22/17 8:28 PM
 */
public final class ExtractFirstLastName {

    private String firstName;
    private String lastName;

    public ExtractFirstLastName(String firstName) {
        this.firstName = firstName.trim();

        invoke();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    private void invoke() {
        String[] name = firstName.split(" ");
        if (name.length > 1) {
            lastName = name[name.length - 1];
            firstName = StringUtils.trim(firstName.substring(0, firstName.lastIndexOf(" " + lastName)));
        }
    }
}
