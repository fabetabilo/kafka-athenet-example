package cl.athenet.statistics.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cl.athenet.statistics.model.ScoreDocument;

public interface ScoreRepository extends MongoRepository<ScoreDocument, String> {

    Optional<ScoreDocument> findByIdEncuentro(Long idEncuentro);
}
