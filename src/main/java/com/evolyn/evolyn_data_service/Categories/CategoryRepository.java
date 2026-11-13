package com.evolyn.evolyn_data_service.Categories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByUserIdOrderByTypeAscSortOrderAscNameAsc(UUID userId);

    // Always look up by id AND owner, so users can't touch each other's categories
    Optional<CategoryEntity> findByCategoryIdAndUserId(UUID categoryId, UUID userId);

    Optional<CategoryEntity> findFirstByUserIdAndTypeAndNameIgnoreCase(UUID userId, String type, String name);

    long countByUserId(UUID userId);

    long countByUserIdAndType(UUID userId, String type);

    @Query("select coalesce(max(c.sortOrder), 0) from CategoryEntity c where c.userId = :userId and c.type = :type")
    int maxSortOrder(@Param("userId") UUID userId, @Param("type") String type);
}
