package com.noqapp.mobile.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * hitender
 * 12/4/17 11:11 AM
 */
class ExtractFirstLastNameTest {

    @Test
    void getFirstName() {
        ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName("Sammy S");
        assertEquals("Sammy", extractFirstLastName.getFirstName());
        assertEquals("S", extractFirstLastName.getLastName());
    }

    @Test
    void getFirstName_LastName_As_Null() {
        ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName(" S ");
        assertEquals("S", extractFirstLastName.getFirstName());
        assertNull(extractFirstLastName.getLastName());
    }

    @Test
    void getFirstName_With_MiddleName() {
        ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName("Sammy D S");
        assertEquals("Sammy D", extractFirstLastName.getFirstName());
        assertEquals("S", extractFirstLastName.getLastName());
    }

    @Test
    void getFirstName_With_MiddleName_SameAs_LastName() {
        ExtractFirstLastName extractFirstLastName = new ExtractFirstLastName("Sammy S S");
        assertEquals("Sammy S", extractFirstLastName.getFirstName());
        assertEquals("S", extractFirstLastName.getLastName());
    }
}
