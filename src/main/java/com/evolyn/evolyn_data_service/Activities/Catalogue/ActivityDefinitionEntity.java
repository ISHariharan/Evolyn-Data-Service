package com.evolyn.evolyn_data_service.Activities.Catalogue;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One activity from the Compendium, e.g. 15330 "Handball, team", MET 8.0.
 * Read-only for the app: rows only come from Flyway seeds (V4).
 */
@Entity
@Table(name = "activity_definitions")
public class ActivityDefinitionEntity {

    @Id
    @Column(name = "activity_definition_id")
    private Long activityDefinitionId;

    @Column(name = "edition", nullable = false, length = 20)
    private String edition;

    @Column(name = "activity_code", nullable = false, length = 5)
    private String activityCode;

    @Column(name = "heading_code", nullable = false, length = 2)
    private String headingCode;

    @Column(name = "met_value", nullable = false, precision = 4, scale = 1)
    private BigDecimal metValue;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public Long getActivityDefinitionId() { return activityDefinitionId; }
    public String getEdition() { return edition; }
    public String getActivityCode() { return activityCode; }
    public String getHeadingCode() { return headingCode; }
    public BigDecimal getMetValue() { return metValue; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
