package com.impactbudget.categorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * The resolved impact score for a single transaction (after cache / LLM / curated
 * overrides). Uniquely keyed by transaction so re-processing is idempotent.
 */
@Entity
@Table(name = "impact_score")
public class ImpactScore {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category")
    private String category;

    @Column(name = "local_score", nullable = false)
    private int localScore;

    @Column(name = "local_independent", nullable = false)
    private boolean localIndependent;

    @Column(name = "sustainability_score", nullable = false)
    private int sustainabilityScore;

    @Column(name = "material_flags")
    private String materialFlags;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "source", nullable = false)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getLocalScore() {
        return localScore;
    }

    public void setLocalScore(int localScore) {
        this.localScore = localScore;
    }

    public boolean isLocalIndependent() {
        return localIndependent;
    }

    public void setLocalIndependent(boolean localIndependent) {
        this.localIndependent = localIndependent;
    }

    public int getSustainabilityScore() {
        return sustainabilityScore;
    }

    public void setSustainabilityScore(int sustainabilityScore) {
        this.sustainabilityScore = sustainabilityScore;
    }

    public String getMaterialFlags() {
        return materialFlags;
    }

    public void setMaterialFlags(String materialFlags) {
        this.materialFlags = materialFlags;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
