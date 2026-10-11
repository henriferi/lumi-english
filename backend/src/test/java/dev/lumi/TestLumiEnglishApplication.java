package dev.lumi;

import org.springframework.boot.SpringApplication;

/** Runs the app against a throwaway Testcontainers database instead of docker compose. */
public class TestLumiEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.from(LumiEnglishApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
