package com.alixandremz.encurtador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.alixandremz.encurtador.model.Link;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // incremento atômico no banco: dois cliques simultâneos não perdem contagem
    @Modifying
    @Query("update Link l set l.cliques = l.cliques + 1 where l.slug = :slug")
    void incrementarCliques(@Param("slug") String slug);
}
