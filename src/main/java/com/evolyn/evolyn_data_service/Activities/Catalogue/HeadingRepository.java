package com.evolyn.evolyn_data_service.Activities.Catalogue;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeadingRepository extends JpaRepository<HeadingEntity, String> {
    List<HeadingEntity> findAllByOrderByHeadingCodeAsc();
}
