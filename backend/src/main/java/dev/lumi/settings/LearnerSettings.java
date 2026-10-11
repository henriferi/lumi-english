package dev.lumi.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The single learner profile. The table holds exactly one row (id = 1), created by the first migration. */
@Entity
@Table(name = "learner_settings")
public class LearnerSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_band", nullable = false)
    private AgeBand ageBand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    protected LearnerSettings() {
        // for JPA
    }

    public void update(String name, AgeBand ageBand, Level level) {
        this.name = name;
        this.ageBand = ageBand;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public AgeBand getAgeBand() {
        return ageBand;
    }

    public Level getLevel() {
        return level;
    }
}
