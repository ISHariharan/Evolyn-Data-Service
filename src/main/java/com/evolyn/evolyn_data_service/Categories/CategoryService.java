package com.evolyn.evolyn_data_service.Categories;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules for categories, in one place:
 *  - a user's first visit creates the default set;
 *  - names are unique per user and type (ignoring capital letters);
 *  - a category with transactions can only be deleted by moving them elsewhere;
 *  - each type keeps at least one category, so the Add screen always has a choice.
 */
@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final CategoryUsage usage;

    public CategoryService(CategoryRepository categories, CategoryUsage usage) {
        this.categories = categories;
        this.usage = usage;
    }

    /** Thrown for rule violations; the controller turns it into a 4xx response. */
    public static class CategoryException extends RuntimeException {
        final int status;
        final Map<String, Object> body;

        CategoryException(int status, String message) {
            this(status, Map.of("message", message));
        }

        CategoryException(int status, Map<String, Object> body) {
            super(String.valueOf(body.get("message")));
            this.status = status;
            this.body = body;
        }
    }

    public record CategoryInput(UUID userId, String name, String type, String icon, String color, String description) {}

    public record CategoryView(UUID categoryId, String name, String type, String icon, String color,
                               String description, long transactionCount) {}

    @Transactional
    public List<CategoryView> list(UUID userId) {
        if (categories.countByUserId(userId) == 0) seedDefaults(userId);
        Map<UUID, Long> counts = usage.countsByCategory(userId).stream()
                .collect(Collectors.toMap(CategoryUsage.CategoryCount::getCategoryId, CategoryUsage.CategoryCount::getCount));
        return categories.findByUserIdOrderByTypeAscSortOrderAscNameAsc(userId).stream()
                .map(c -> view(c, counts.getOrDefault(c.getCategoryId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryView create(CategoryInput in) {
        String name = in.name().trim();
        ensureNameFree(in.userId(), in.type(), name, null);
        CategoryEntity c = new CategoryEntity();
        c.setCategoryId(UUID.randomUUID());
        c.setUserId(in.userId());
        c.setType(in.type());
        c.setName(name);
        c.setIcon(in.icon());
        c.setColor(in.color());
        c.setDescription(blankToNull(in.description()));
        c.setSortOrder(categories.maxSortOrder(in.userId(), in.type()) + 1);
        c.setCreatedAt(Instant.now());
        return view(categories.save(c), 0);
    }

    @Transactional
    public CategoryView update(UUID categoryId, CategoryInput in) {
        CategoryEntity c = find(categoryId, in.userId());
        long used = usage.countUsing(c);
        String name = in.name().trim();

        if (!c.getType().equals(in.type()) && used > 0) {
            throw new CategoryException(409, "This category has " + used + " transaction(s), so it can't switch between expense and income.");
        }
        ensureNameFree(in.userId(), in.type(), name, c.getCategoryId());

        boolean renamed = !c.getName().equals(name);
        c.setName(name);
        c.setType(in.type());
        c.setIcon(in.icon());
        c.setColor(in.color());
        c.setDescription(blankToNull(in.description()));
        categories.save(c);
        if (renamed) usage.syncName(c, name);
        return view(c, used);
    }

    /** Deletes a category. If it has transactions, they're moved to reassignTo first. */
    @Transactional
    public void delete(UUID categoryId, UUID userId, UUID reassignTo) {
        CategoryEntity c = find(categoryId, userId);
        if (categories.countByUserIdAndType(userId, c.getType()) <= 1) {
            throw new CategoryException(409, "Keep at least one " + c.getType().toLowerCase() + " category.");
        }
        long used = usage.countUsing(c);
        if (used > 0) {
            if (reassignTo == null) {
                throw new CategoryException(409, Map.of(
                        "message", "This category has " + used + " transaction(s). Choose a category to move them to.",
                        "transactionCount", used));
            }
            CategoryEntity target = find(reassignTo, userId);
            if (target.getCategoryId().equals(c.getCategoryId()) || !target.getType().equals(c.getType())) {
                throw new CategoryException(400, "Move the transactions to a different " + c.getType().toLowerCase() + " category.");
            }
            usage.reassign(c, target, target.getName());
        }
        categories.delete(c);
    }

    /** Adds any default category the user doesn't already have (matched by type + name). */
    @Transactional
    public int seedDefaults(UUID userId) {
        int added = 0;
        int order = 0;
        for (var d : CategoryDefaults.ALL) {
            order++;
            if (categories.findFirstByUserIdAndTypeAndNameIgnoreCase(userId, d.type(), d.name()).isPresent()) continue;
            CategoryEntity c = new CategoryEntity();
            c.setCategoryId(UUID.randomUUID());
            c.setUserId(userId);
            c.setType(d.type());
            c.setName(d.name());
            c.setIcon(d.icon());
            c.setColor(d.color());
            c.setDescription(d.description());
            c.setSortOrder(order);
            c.setCreatedAt(Instant.now());
            categories.save(c);
            added++;
        }
        return added;
    }

    /** Used by the migration: the user's category with this name, created if missing. */
    @Transactional
    public CategoryEntity findOrCreate(UUID userId, String type, String name) {
        return categories.findFirstByUserIdAndTypeAndNameIgnoreCase(userId, type, name).orElseGet(() -> {
            CategoryEntity c = new CategoryEntity();
            c.setCategoryId(UUID.randomUUID());
            c.setUserId(userId);
            c.setType(type);
            c.setName(name);
            c.setIcon(CategoryDefaults.FALLBACK_ICON);
            c.setColor(CategoryDefaults.FALLBACK_COLOR);
            c.setSortOrder(categories.maxSortOrder(userId, type) + 1);
            c.setCreatedAt(Instant.now());
            return categories.save(c);
        });
    }

    public CategoryEntity find(UUID categoryId, UUID userId) {
        if (categoryId == null) throw new CategoryException(400, "Category is required");
        return categories.findByCategoryIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryException(404, "Category not found"));
    }

    private void ensureNameFree(UUID userId, String type, String name, UUID exceptId) {
        categories.findFirstByUserIdAndTypeAndNameIgnoreCase(userId, type, name)
                .filter(existing -> !existing.getCategoryId().equals(exceptId))
                .ifPresent(existing -> {
                    // "an expense" / "an income": both types start with a vowel
                    throw new CategoryException(409, "You already have an " + type.toLowerCase() + " category called \"" + existing.getName() + "\".");
                });
    }

    private static CategoryView view(CategoryEntity c, long count) {
        return new CategoryView(c.getCategoryId(), c.getName(), c.getType(), c.getIcon(), c.getColor(),
                c.getDescription(), count);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
