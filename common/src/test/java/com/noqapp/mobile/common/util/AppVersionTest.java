package com.noqapp.mobile.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * hitender
 * 2/7/18 5:42 AM
 */
class AppVersionTest {

    @Test
    void compare() {
        assertFalse(AppVersion.compare("1.0", "1.1"));
        assertFalse(AppVersion.compare("1.0.1", "1.1"));
        assertFalse(AppVersion.compare("1.9", "1.10"));
        assertTrue(AppVersion.compare("1.a", "1.9"));
        assertFalse(AppVersion.compare("1.1.0", "1.1.1"));
        assertTrue(AppVersion.compare("1.0.89", "1.0.89"));
        assertTrue(AppVersion.compare("1.1.1", "1.0.89"));
        assertTrue(AppVersion.compare("1.1.100", "1.0.89"));
        assertTrue(AppVersion.compare("1.1.10000", "1.0.89"));
    }
}