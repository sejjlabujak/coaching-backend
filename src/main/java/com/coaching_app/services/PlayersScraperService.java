package com.coaching_app.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.jsoup.nodes.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlayersScraperService {

    public void scrapeRoster() throws IOException {

        String url =
                "https://basketball.eurobasket.com/team/ZKK-Play-Off-Happy-Sarajevo/18179/Roster?Women=1";

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        Elements links = doc.select("a[href*=/player/]");

        Set<String> processed = new HashSet<>();

        for (Element link : links) {

            String href = link.attr("href");

            if (!href.contains("/player/")) {
                continue;
            }

            // izbjegni duplikate
            if (processed.contains(href)) {
                continue;
            }

            processed.add(href);

            String fullUrl = href.startsWith("http")
                    ? href
                    : "https://basketball.eurobasket.com" + href;

            String playerName = link.text();

            Pattern pattern =
                    Pattern.compile("/player/.+?/(\\d+)");

            Matcher matcher = pattern.matcher(href);

            if (matcher.find()) {

                Integer playerId =
                        Integer.parseInt(matcher.group(1));

                System.out.println("NAME: " + playerName);
                System.out.println("ID: " + playerId);
                System.out.println("URL: " + fullUrl);
                System.out.println("----------------");
            }
        }
    }
}