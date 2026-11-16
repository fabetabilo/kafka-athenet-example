package cl.athenet.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.athenet.statistics.model.ScoreDocument;
import cl.athenet.statistics.repository.ScoreRepository;

@CrossOrigin
@RestController
public class ScoreController {

    private final ScoreRepository repositorio;

    public ScoreController(ScoreRepository repositorio) {
        this.repositorio = repositorio;
    }

    // estado inicial para web-public: lo que llega despues viene por STOMP
    @GetMapping("/marcador/actual")
    public ResponseEntity<ScoreDocument> actual() {
        return repositorio.findFirstByTipoNot("FINALIZADO")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
