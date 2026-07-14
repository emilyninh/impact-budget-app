package com.impactbudget.categorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Curated ground truth for a well-known merchant. Overlaid on top of LLM output — the
 * curated values win on conflict. Nullable score columns override only the dimensions
 * they specify (e.g. a brand may pin sustainability but leave local scoring to the LLM).
 */
@Entity
@Table(name = "curated_merchant")
public class CuratedMerchant {

    @Id
    private UUID id;

    /** Uppercase token matched against the normalized merchant (substring match). */
    @Column(name = "match_key", nullable = false, unique = true)
    private String matchKey;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "local_score")
    private Integer localScore;

    @Column(name = "local_independent")
    private Boolean localIndependent;

    @Column(name = "sustainability_score")
    private Integer sustainabilityScore;

    @Column(name = "material_flags")
    private String materialFlags;

    @Column(name = "note")
    private String note;

    /** Provenance: MANUAL | B-CORP | … */
    @Column(name = "source", nullable = false)
    private String source = "MANUAL";

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatchKey() {
        return matchKey;
    }

    public void setMatchKey(String matchKey) {
        this.matchKey = matchKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getLocalScore() {
        return localScore;
    }

    public void setLocalScore(Integer localScore) {
        this.localScore = localScore;
    }

    public Boolean getLocalIndependent() {
        return localIndependent;
    }

    public void setLocalIndependent(Boolean localIndependent) {
        this.localIndependent = localIndependent;
    }

    public Integer getSustainabilityScore() {
        return sustainabilityScore;
    }

    public void setSustainabilityScore(Integer sustainabilityScore) {
        this.sustainabilityScore = sustainabilityScore;
    }

    public String getMaterialFlags() {
        return materialFlags;
    }

    public void setMaterialFlags(String materialFlags) {
        this.materialFlags = materialFlags;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
