package com.coaching_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds all data scraped from a single Eurobasket player profile page.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {

    /** Full name as shown on Eurobasket roster (e.g. "Lejla Omerbasic") */
    private String fullName;

    /** Eurobasket numeric player ID extracted from the profile URL (e.g. 309419) */
    private Integer playerID;

    /** Absolute profile URL (e.g. https://basketball.eurobasket.com/player/Lejla-Omerbasic/309419?Women=1) */
    private String profileUrl;

    /** Absolute photo URL (e.g. https://www.eurobasket.com/photos/Omerbasic_Lejla_1.png) */
    private String imageUrl;

    /** Height in cm parsed from the roster row data-order attribute (e.g. 182) */
    private Integer heightCm;

    /** Position string as shown on roster (e.g. "F", "G", "F/C") */
    private String position;

    /** Nationality / country code from the flag image alt attribute (e.g. "Bosnia", "USA") */
    private String nationality;

    // ── Fields scraped from the individual profile page ──────────────────────

    /** Birth date string as found in the FAQ section (e.g. "Mar.1, 1997") */
    private String birthDate;

    /** Birth city as found in the bio paragraph */
    private String birthCity;

    /** Weight in kg — present on some profiles, null otherwise */
    private Integer weightKg;
}