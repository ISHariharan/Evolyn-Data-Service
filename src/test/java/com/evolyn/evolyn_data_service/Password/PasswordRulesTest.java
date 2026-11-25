package com.evolyn.evolyn_data_service.Password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordRulesTest {

    @Test
    void aStrongPasswordPasses() {
        assertTrue(PasswordRules.problems("Secret#123").isEmpty());
    }

    @Test
    void eachMissingRuleIsNamed() {
        assertEquals(3, PasswordRules.problems("abc").size()); // too short, no upper-case, no special
        assertTrue(PasswordRules.problems("short#A").contains("Password must be at least 8 characters"));
        assertTrue(PasswordRules.problems("secret#123").contains("Add an upper-case letter"));
        assertTrue(PasswordRules.problems("SECRET#123").contains("Add a lower-case letter"));
        assertTrue(PasswordRules.problems("Secret1234").contains("Add a special character, like ! @ # $ %"));
        assertFalse(PasswordRules.problems(null).isEmpty());
    }

    @Test
    void longerThanBcryptAllowsIsRejected() {
        assertTrue(PasswordRules.problems("Aa#" + "x".repeat(70)).contains("Password is too long"));
    }

    @Test
    void codesAreGroupedRandomAndFreeOfLookAlikes() {
        String a = PasswordRules.newCode();
        String b = PasswordRules.newCode();
        assertNotEquals(a, b);
        assertTrue(a.matches("^[A-Z2-9]{4}(-[A-Z2-9]{4}){3}$"), a);
        assertFalse(a.matches(".*[01OILU].*"), a);
        assertTrue(PasswordRules.looksLikeCode(a));
    }

    @Test
    void typedCodesMatchWhateverTheCaseSpacesOrDashes() {
        String code = "K7QF-9XRM-2HPA-W4TD";
        assertEquals(PasswordRules.hash(code), PasswordRules.hash("k7qf 9xrm 2hpa w4td"));
        assertEquals(PasswordRules.hash(code), PasswordRules.hash("K7QF9XRM2HPAW4TD"));
        assertNotEquals(PasswordRules.hash(code), PasswordRules.hash("K7QF-9XRM-2HPA-W4TE"));
        assertEquals(64, PasswordRules.hash(code).length());
    }

    @Test
    void wrongLengthsAreNotCodes() {
        assertFalse(PasswordRules.looksLikeCode(null));
        assertFalse(PasswordRules.looksLikeCode("ABCD-EFGH"));
        assertFalse(PasswordRules.looksLikeCode("K7QF-9XRM-2HPA-W4TD-X"));
    }
}
