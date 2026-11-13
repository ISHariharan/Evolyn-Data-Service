package com.evolyn.evolyn_data_service.Categories;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time migration, run at start-up.
 *
 * Before categories were editable, each transaction only stored a category NAME.
 * For every transaction that isn't linked to a category yet, this:
 *   1. gives its owner the default categories (if they don't have them),
 *   2. finds the owner's category with that name (creating it if it's not a default),
 *   3. links the transaction to it.
 *
 * Nothing is deleted or renamed, and the name column is left as it was.
 * It only touches unlinked rows, so after the first successful run it does nothing;
 * running it again is safe.
 */
@Component
public class CategoryBackfill implements ApplicationRunner {

    private final CategoryUsage usage;
    private final CategoryService service;

    public CategoryBackfill(CategoryUsage usage, CategoryService service) {
        this.usage = usage;
        this.service = service;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var pending = usage.pendingNames();
        if (pending.isEmpty()) return;

        Set<UUID> seeded = new HashSet<>();
        int linked = 0;
        for (var p : pending) {
            if (seeded.add(p.getUserId())) service.seedDefaults(p.getUserId());
            String name = p.getName() == null || p.getName().isBlank() ? "Others" : p.getName();
            CategoryEntity category = service.findOrCreate(p.getUserId(), p.getType(), name);
            linked += usage.link(p.getUserId(), p.getType(), p.getName(), category);
        }
        System.out.println("Category migration: linked " + linked + " transaction(s) for "
                + seeded.size() + " user(s)");
    }
}
