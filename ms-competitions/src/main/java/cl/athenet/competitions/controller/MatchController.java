package cl.athenet.competitions.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.athenet.competitions.model.Match;
import cl.athenet.competitions.model.ScoreRequest;
import cl.athenet.competitions.service.MatchService;

@RestController
@RequestMapping("/encuentros")
public class MatchController {

    private final MatchService servicio;

    public MatchController(MatchService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Match> listar() {
        return servicio.listar();
    }

    @PostMapping("/{idEncuentro}/iniciar")
    public Match iniciar(@PathVariable Long idEncuentro) {
        return servicio.iniciar(idEncuentro);
    }

    @PostMapping("/{idEncuentro}/marcador")
    public ResponseEntity<Void> marcador(@PathVariable Long idEncuentro, @RequestBody ScoreRequest marcador) {
        servicio.registrarMarcador(idEncuentro, marcador);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{idEncuentro}/finalizar")
    public Match finalizar(@PathVariable Long idEncuentro, @RequestBody ScoreRequest marcador) {
        return servicio.finalizar(idEncuentro, marcador);
    }
}
