package com.evolyn.evolyn_data_service.Activities.Catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One of the Compendium's 22 major headings (Bicycling, Running, Sports...). Table: V3. */
@Entity
@Table(name = "compendium_headings")
public class HeadingEntity {

    @Id
    @Column(name = "heading_code", length = 2)
    private String headingCode;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    public String getHeadingCode() { return headingCode; }
    public String getName() { return name; }
}
