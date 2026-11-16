package cl.athenet.competitions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.athenet.competitions.model.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
