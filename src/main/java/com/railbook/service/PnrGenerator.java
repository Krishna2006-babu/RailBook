
package com.railbook.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PnrGenerator {

    private static final int PNR_LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder pnr = new StringBuilder(PNR_LENGTH);

        // Generate a 10-digit numeric PNR
        for (int i = 0; i < PNR_LENGTH; i++) {
            int digit = random.nextInt(10);
            pnr.append(digit);
        }

        return pnr.toString();
    }
}
