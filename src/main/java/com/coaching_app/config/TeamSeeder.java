package com.coaching_app.config;

import com.coaching_app.models.Team;
import com.coaching_app.repositories.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TeamSeeder {

    private record TeamSeed(String teamName, String rosterUrl, String logoUrl) {}

    private static final List<TeamSeed> TEAMS = List.of(
        new TeamSeed(
            "KK Jumper Zenica",
            "https://basketball.eurobasket.com/team/KK-Jumper-Zenica/61962/Roster?Women=1",
            "https://www.eurobasket.com/logos/kjumpe.png"
        ),
        new TeamSeed(
            "OKK Zenica",
            "https://basketball.eurobasket.com/team/OKK-Zenica/73301/Roster?Women=1",
            null
        ),
        new TeamSeed(
            "ZKK Celik BH Telekom Zenica",
            "https://basketball.eurobasket.com/team/ZKK-Celik-BH-Telekom-Zenica/7981/Roster?Women=1",
            "https://www.eurobasket.com/logos/zcelik.png"
        ),
        new TeamSeed(
            "ZKK Igman Ilidza",
            "https://basketball.eurobasket.com/team/ZKK-Igman-Ilidza/14586/Roster?Women=1",
            "https://www.eurobasket.com/logos/ziili.png"
        ),
        new TeamSeed(
            "ZKK Jedinstvo Dzenex Tuzla",
            "https://basketball.eurobasket.com/team/ZKK-Jedinstvo-Dzenex-Tuzla/7977/Roster?Women=1",
            "https://www.eurobasket.com/logos/jedins.png"
        ),
        new TeamSeed(
            "ZKK Konjic",
            "https://basketball.eurobasket.com/team/ZKK-Konjic/11115/Roster?Women=1",
            "https://www.eurobasket.com/logos/zkonji.png"
        ),
        new TeamSeed(
            "ZKK Lavovi Brcko",
            "https://basketball.eurobasket.com/team/ZKK-Lavovi-Brcko/66016/Roster?Women=1",
            "https://www.eurobasket.com/logos/zlbrc.png"
        ),
        new TeamSeed(
            "ZKK Leotar 03 Trebinje",
            "https://basketball.eurobasket.com/team/ZKK-Leotar-03-Trebinje/10633/Roster?Women=1",
            "https://www.eurobasket.com/logos/nt03.png"
        ),
        new TeamSeed(
            "ZKK Mladi Krajisnik Banja Luka",
            "https://basketball.eurobasket.com/team/ZKK-Mladi-Krajisnik-Banja-Luka/7980/Roster?Women=1",
            "https://www.eurobasket.com/logos/mlkraji.png"
        ),
        new TeamSeed(
            "ZKK Orlovi Banja Luka",
            "https://basketball.eurobasket.com/team/ZKK-Orlovi-Banja-Luka/18723/Roster?Women=1",
            "https://www.eurobasket.com/logos/zobluk.png"
        ),
        new TeamSeed(
            "ZKK Play Off Happy Sarajevo",
            "https://basketball.eurobasket.com/team/ZKK-Play-Off-Happy-Sarajevo/18179/Roster?Women=1",
            "https://www.eurobasket.com/logos/playoff.png"
        ),
        new TeamSeed(
            "ZKK RMU Banovici",
            "https://basketball.eurobasket.com/team/ZKK-RMU-Banovici/7986/Roster?Women=1",
            "https://www.eurobasket.com/logos/rmubanov.png"
        )
    );

    @Bean
    public CommandLineRunner seedTeams(TeamRepository teamRepository) {
        return args -> {
            int inserted = 0;
            for (TeamSeed seed : TEAMS) {
                if (!teamRepository.existsByTeamName(seed.teamName())) {
                    Team team = new Team();
                    team.setTeamName(seed.teamName());
                    team.setRosterUrl(seed.rosterUrl());
                    team.setLogoUrl(seed.logoUrl());
                    teamRepository.save(team);
                    inserted++;
                }
            }
            if (inserted > 0) {
                log.info("Seeded {} new team(s) into the teams table", inserted);
            } else {
                log.info("All {} teams already present, skipping seed", TEAMS.size());
            }
        };
    }
}
