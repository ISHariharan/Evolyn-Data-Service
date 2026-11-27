package com.evolyn.evolyn_data_service.Activities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One calorie calculation for an activity, with every input it used.
 * Never updated in place: an edit adds a new row and sets isCurrent=false on the old one.
 */
@Entity
@Table(name = "activity_calculations")
public class ActivityCalculationEntity {

    @Id
    @Column(name = "calculation_id")
    private UUID calculationId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "metric_type", nullable = false, length = 30)
    private String metricType;

    @Column(name = "metric_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal metricValue;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "formula_version", nullable = false, length = 30)
    private String formulaVersion;

    @Column(name = "met_value", nullable = false, precision = 4, scale = 1)
    private BigDecimal metValue;

    @Column(name = "weight_kg_used", precision = 5, scale = 2) // empty for entered calories without a weight
    private BigDecimal weightKgUsed;

    @Column(name = "weight_recorded_on")
    private LocalDate weightRecordedOn;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "reference_edition", nullable = false, length = 20)
    private String referenceEdition;

    @Column(name = "reference_activity_code", nullable = false, length = 5)
    private String referenceActivityCode;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public UUID getCalculationId() { return calculationId; }
    public void setCalculationId(UUID calculationId) { this.calculationId = calculationId; }

    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getFormulaVersion() { return formulaVersion; }
    public void setFormulaVersion(String formulaVersion) { this.formulaVersion = formulaVersion; }

    public BigDecimal getMetValue() { return metValue; }
    public void setMetValue(BigDecimal metValue) { this.metValue = metValue; }

    public BigDecimal getWeightKgUsed() { return weightKgUsed; }
    public void setWeightKgUsed(BigDecimal weightKgUsed) { this.weightKgUsed = weightKgUsed; }

    public LocalDate getWeightRecordedOn() { return weightRecordedOn; }
    public void setWeightRecordedOn(LocalDate weightRecordedOn) { this.weightRecordedOn = weightRecordedOn; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getReferenceEdition() { return referenceEdition; }
    public void setReferenceEdition(String referenceEdition) { this.referenceEdition = referenceEdition; }

    public String getReferenceActivityCode() { return referenceActivityCode; }
    public void setReferenceActivityCode(String referenceActivityCode) { this.referenceActivityCode = referenceActivityCode; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
