package com.evolyn.evolyn_data_service.Categories;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A user's own category ("Food & Dining", "Salary", ...). Every user gets a set of
 * defaults they can rename, recolour, delete or add to. Transactions point at a
 * category by id, so renaming one updates every transaction that uses it.
 */
@Entity
@Table(
    name = "categories",
    indexes = @Index(name = "idx_categories_user", columnList = "user_id"),
    uniqueConstraints = @UniqueConstraint(name = "uk_categories_user_type_name", columnNames = { "user_id", "type", "name" })
)
public class CategoryEntity {

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // EXPENSE or INCOME
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    // Boxicons class, e.g. "bx bx-restaurant"
    @Column(name = "icon", nullable = false, length = 60)
    private String icon;

    // Hex colour, e.g. "#f97316"
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    // Optional hint, e.g. "Restaurants, groceries, cafes"
    @Column(name = "description", length = 120)
    private String description;

    // Keeps the defaults in a sensible order; new categories go to the end
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
