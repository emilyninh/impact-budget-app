package com.impactbudget.categorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Cached impact scores for a normalized merchant. A lookup hit here means no LLM call is
 * needed — most spend repeats the same merchants, so this keeps the categorization cheap.
 */
@Entity
@Table(name = "merchant_score")
public class MerchantScore {

    @Id
    private UUID id;

    @Column(name = "normalized_merchant", nullable = false, unique = true)
    private String normalizedMerchant;

    @Column(name = "cleaned_merchant")
    private String cleanedMerchant;

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

    @Column(name = "rationale")
    private String rationale;

    @Column(name = "source", nullable = false)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNormalizedMerchant() {
        return normalizedMerchant;
    }

    public void setNormalizedMerchant(String normalizedMerchant) {
        this.normalizedMerchant = normalizedMerchant;
    }

    public String getCleanedMerchant() {
        return cleanedMerchant;
    }

    public void setCleanedMerchant(String cleanedMerchant) {
        this.cleanedMerchant = cleanedMerchant;
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

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
