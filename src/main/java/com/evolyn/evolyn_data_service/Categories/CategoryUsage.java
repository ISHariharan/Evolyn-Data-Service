package com.evolyn.evolyn_data_service.Categories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.evolyn.evolyn_data_service.Transactions.TransactionEntity;

/** Queries about how transactions use categories (counts, moves, renames, migration). */
public interface CategoryUsage extends Repository<TransactionEntity, UUID> {

    interface CategoryCount { UUID getCategoryId(); Long getCount(); }
    interface PendingName { UUID getUserId(); String getType(); String getName(); }

    @Query("""
        select t.category.categoryId as categoryId, count(t) as count
        from TransactionEntity t
        where t.userId = :userId and t.category is not null
        group by t.category.categoryId""")
    List<CategoryCount> countsByCategory(@Param("userId") UUID userId);

    @Query("select count(t) from TransactionEntity t where t.category = :category")
    long countUsing(@Param("category") CategoryEntity category);

    // Move every transaction from one category to another (used before deleting)
    @Modifying
    @Query("update TransactionEntity t set t.category = :to, t.categoryName = :toName where t.category = :from")
    int reassign(@Param("from") CategoryEntity from, @Param("to") CategoryEntity to, @Param("toName") String toName);

    // Keep the backup name column in sync after a rename
    @Modifying
    @Query("update TransactionEntity t set t.categoryName = :name where t.category = :category")
    int syncName(@Param("category") CategoryEntity category, @Param("name") String name);

    // ── One-time migration of transactions created before categories existed ──
    @Query("""
        select distinct t.userId as userId, t.type as type, t.categoryName as name
        from TransactionEntity t
        where t.category is null""")
    List<PendingName> pendingNames();

    @Modifying
    @Query("""
        update TransactionEntity t set t.category = :category
        where t.userId = :userId and t.type = :type and t.categoryName = :name and t.category is null""")
    int link(@Param("userId") UUID userId, @Param("type") String type, @Param("name") String name,
             @Param("category") CategoryEntity category);
}
