package ru.mirea.railways;

import org.junit.jupiter.api.Test;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.security.PasswordPolicy;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test void acceptsValid() { assertDoesNotThrow(() -> policy.validate("Strong1Pass")); }
    @Test void rejectsShort() { assertThrows(BusinessException.class, () -> policy.validate("Ab1")); }
    @Test void rejectsNoDigits() { assertThrows(BusinessException.class, () -> policy.validate("NoDigitsHere")); }
}