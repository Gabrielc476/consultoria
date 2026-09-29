package br.com.govflow.core.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptTest {

    @Test
    void testBCryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String raw = "GovFlow2026!";
        String hash = encoder.encode(raw);
        System.out.println("GENERATED_BCRYPT_HASH=" + hash);
        assertTrue(encoder.matches(raw, hash));
    }
}
