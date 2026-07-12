package com.impactbudget.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A user's goal to shift a dimension of their discretionary spending over time — e.g.
 * "move local spending from 18% to 30% by year end."
 */
@Entity
@Table(name = "goal")
public class Goal {

    public enum Dimension {LOCAL, SUSTAINABLE}

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false)
    private Dimension dimension;

    @Column(name = "baseline_pct", nullable = false)
    private int baselinePct;

    @Column(name = "target_pct", nullable = false)
    private int targetPct;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public int getBaselinePct() {
        return baselinePct;
    }

    public void setBaselinePct(int baselinePct) {
        this.baselinePct = baselinePct;
    }

    public int getTargetPct() {
        return targetPct;
    }

    public void setTargetPct(int targetPct) {
        this.targetPct = targetPct;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
