package com.impactbudget.budget;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for {@link CategoryMonthlyRollup}. */
public class CategoryRollupId implements Serializable {

    private String userId;
    private String yearMonth;
    private String category;

    public CategoryRollupId() {
    }

    public CategoryRollupId(String userId, String yearMonth, String category) {
        this.userId = userId;
        this.yearMonth = yearMonth;
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoryRollupId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(yearMonth, that.yearMonth)
                && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, yearMonth, category);
    }
}
