package com.coaching_app.services;

import com.coaching_app.dto.PlayerDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PlayersScraperService {

    private static final String ROSTER_URL =
            "https://basketball.eurobasket.com/team/ZKK-Play-Off-Happy-Sarajevo/18179/Roster?Women=1";

    private static final String BASE_URL = "https://basketball.eurobasket.com";

    /** Regex to extract numeric ID from /player/Name/532397?Women=1 */
    private static final Pattern PLAYER_ID_PATTERN =
            Pattern.compile("/player/[^/]+/(\\d+)");

    /** Regex to extract birth date from bio: "born on March 1 1997" or "born on Mar.1, 1997" */
    private static final Pattern BIRTH_DATE_PATTERN =
            Pattern.compile("born on ([A-Za-z.]+\\.?\\s*\\d{1,2},?\\s*\\d{4})");

    /** Regex to extract birth city: "born on ... in Sarajevo (Bosnia)" */
    private static final Pattern BIRTH_CITY_PATTERN =
            Pattern.compile("born on .+? in ([^(.,]+?)\\s*(?:\\(|\\.|,|$)");

    public List<PlayerDTO> scrapeRoster() {
        List<PlayerDTO> players = new ArrayList<>();

        Document doc;
        try {
            doc = Jsoup.connect(ROSTER_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .timeout(15_000)
                    .get();
        } catch (IOException e) {
            log.error("Failed to fetch Eurobasket roster: {}", e.getMessage());
            return players;
        }

        // Build a map of playerUrl -> photoUrl from the top thumbnail table
        // The top table (#tableCoach) has <a href="/player/..."><img src="..."></a>
        java.util.Map<String, String> photoMap = new java.util.HashMap<>();
        for (Element link : doc.select("#tableCoach a[href*=/player/]")) {
            String href = normalizeHref(link.attr("href"));
            Element img = link.selectFirst("img");
            if (img != null && href != null) {
                photoMap.put(href, absoluteUrl(img.attr("src")));
            }
        }

        // Select both senior and dual-registered rows
        Elements rows = doc.select("tr.clssenior, tr.clsboth");
        log.info("Found {} roster rows on Eurobasket", rows.size());

        for (Element row : rows) {
            try {
                PlayerDTO dto = parseRosterRow(row, photoMap);
                if (dto != null) {
                    players.add(dto);
                    log.debug("Parsed roster player: {} (id={})", dto.getFullName(), dto.getPlayerID());
                }
            } catch (Exception e) {
                log.warn("Failed to parse roster row: {}", e.getMessage());
            }
        }

        log.info("Scraped {} players from Eurobasket roster", players.size());
        return players;
    }


    public void scrapeProfile(PlayerDTO dto) {
        if (dto.getProfileUrl() == null) return;

        Document doc;
        try {
            doc = Jsoup.connect(dto.getProfileUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .timeout(15_000)
                    .get();
        } catch (IOException e) {
            log.warn("Failed to fetch profile for {}: {}", dto.getFullName(), e.getMessage());
            return;
        }

        // ── Bio paragraph: "born on Mar.1, 1997 in Sarajevo (Bosnia)" ──────
        Element bioEl = doc.selectFirst("div.newseotxt");
        if (bioEl != null) {
            String bioText = bioEl.text();

            Matcher dateMatcher = BIRTH_DATE_PATTERN.matcher(bioText);
            if (dateMatcher.find()) {
                dto.setBirthDate(dateMatcher.group(1).trim());
            }

            Matcher cityMatcher = BIRTH_CITY_PATTERN.matcher(bioText);
            if (cityMatcher.find()) {
                dto.setBirthCity(cityMatcher.group(1).trim());
            }
        }


        Elements h3s = doc.select("div#div_faq h3");
        for (Element h3 : h3s) {
            String question = h3.text().toLowerCase();
            Element answer = h3.nextElementSibling(); // the <p> right after
            if (answer == null) continue;
            String answerText = answer.text();

            if (question.contains("when was") && question.contains("born")) {
                // e.g. "Lejla Omerbasic was born on Mar.1, 1997."
                if (dto.getBirthDate() == null) {
                    Matcher m = Pattern.compile("born on ([A-Za-z.]+\\.?\\s*\\d{1,2},?\\s*\\d{4})")
                            .matcher(answerText);
                    if (m.find()) dto.setBirthDate(m.group(1).trim());
                }
            }

            if (question.contains("how tall")) {
                // e.g. "182cm / 6'0'' tall."
                if (dto.getHeightCm() == null || dto.getHeightCm() == 0) {
                    Matcher m = Pattern.compile("(\\d{2,3})\\s*cm").matcher(answerText);
                    if (m.find()) dto.setHeightCm(Integer.parseInt(m.group(1)));
                }
            }

            if (question.contains("weight") || question.contains("how heavy")) {
                // e.g. "74 kg"
                Matcher m = Pattern.compile("(\\d{2,3})\\s*kg").matcher(answerText);
                if (m.find()) dto.setWeightKg(Integer.parseInt(m.group(1)));
            }
        }

        log.debug("Profile enriched for {}: birthDate={}, city={}, weight={}",
                dto.getFullName(), dto.getBirthDate(), dto.getBirthCity(), dto.getWeightKg());
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private PlayerDTO parseRosterRow(Element row,
                                               java.util.Map<String, String> photoMap) {
        // Player link and name
        Element playerLink = row.selectFirst("a[href*=/player/]");
        if (playerLink == null) return null;

        String href = playerLink.attr("href");
        Integer eurobasketId = extractPlayerId(href);
        if (eurobasketId == null) return null;

        // Prefer the desktop span, fall back to link text
        Element nameSpan = playerLink.selectFirst(".spnplnamedesktop");
        String fullName = nameSpan != null ? nameSpan.text().trim() : playerLink.text().trim();
        if (fullName.isEmpty()) return null;

        String profileUrl = normalizeHref(href);

        // Height: td.tdhightcls data-order attribute
        Element heightTd = row.selectFirst("td.tdhightcls");
        Integer heightCm = null;
        if (heightTd != null) {
            String dataOrder = heightTd.attr("data-order");
            try {
                int h = Integer.parseInt(dataOrder);
                heightCm = h > 0 ? h : null;
            } catch (NumberFormatException ignored) {}
        }

       String position = null;
        Elements centerTds = row.select("td[align=center]");
        for (Element td : centerTds) {
            // Skip the nationality td (it contains an img, not plain text)
            if (td.selectFirst("img") != null) continue;
            String text = td.text().trim();
            // Position values: G, SG, F, C, F/C etc. — short and alphabetic
            if (!text.isEmpty() && text.length() <= 5 && text.matches("[A-Z/]+")) {
                position = text;
                break;
            }
        }

        String nationality = null;
        Element natImg = row.selectFirst("td.tdrsimg img");
        if (natImg != null) {
            nationality = natImg.attr("alt");
        }

        String imageUrl = photoMap.get(profileUrl);

        PlayerDTO dto = new PlayerDTO();
        dto.setFullName(fullName);
        dto.setPlayerID(eurobasketId);
        dto.setProfileUrl(profileUrl);
        dto.setImageUrl(imageUrl);
        dto.setHeightCm(heightCm);
        dto.setPosition(position);
        dto.setNationality(nationality);

        return dto;
    }

    private Integer extractPlayerId(String href) {
        if (href == null) return null;
        Matcher m = PLAYER_ID_PATTERN.matcher(href);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Ensure the href is absolute and strip query string for map keying. */
    private String normalizeHref(String href) {
        if (href == null || href.isEmpty()) return null;
        // Make absolute
        String absolute = href.startsWith("http") ? href : BASE_URL + href;
        // Keep ?Women=1 so the profile page loads correctly
        return absolute;
    }

    private String absoluteUrl(String src) {
        if (src == null || src.isEmpty()) return null;
        return src.startsWith("http") ? src : "https://www.eurobasket.com" + src;
    }
}