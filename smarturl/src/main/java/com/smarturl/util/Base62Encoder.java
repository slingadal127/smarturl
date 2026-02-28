package com.smarturl.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    // 62 possible characters: 0-9, a-z, A-Z
    private static final String CHARACTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int MIN_LENGTH = 6;

    /**
     * Encodes a long ID to a Base62 short code
     *
     * Example:
     * encode(1)        → "000001"
     * encode(12345678) → "dnh75K"
     * encode(56000000) → "ZZZZZZ" (near max for 6 chars)
     *
     * How it works:
     * Repeatedly divide the number by 62, use remainder as index into
     * CHARACTERS string. Like converting decimal to another base.
     */
    public String encode(long id) {
        StringBuilder sb = new StringBuilder();

        // Edge case — ID of 0 encodes to "000000"
        if (id == 0) return "000000";

        while (id > 0) {
            // Remainder gives us which character to use
            sb.append(CHARACTERS.charAt((int)(id % BASE)));
            id /= BASE;
        }

        // Pad to minimum length with leading zeros
        while (sb.length() < MIN_LENGTH) {
            sb.append(CHARACTERS.charAt(0));
        }

        // Reverse because we built it backwards
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 short code back to the original ID
     *
     * Example:
     * decode("dnh75K") → 12345678
     *
     * How it works:
     * Same as reading a number in any base — multiply running
     * total by base and add value of current character
     */
    public long decode(String shortCode) {
        long result = 0;
        for (char c : shortCode.toCharArray()) {
            result = result * BASE + CHARACTERS.indexOf(c);
        }
        return result;
    }
}