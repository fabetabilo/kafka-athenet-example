package cl.athenet.competitions.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import cl.athenet.competitions.config.KafkaConfig;
import cl.athenet.competitions.event.ScoreEvent;
import cl.athenet.competitions.model.Match;
import cl.athenet.competitions.model.ScoreRequest;
import cl.athenet.competitions.repository.MatchRepository;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository repositorio;
    private final KafkaTemplate<String, ScoreEvent> productor;

    public MatchService(MatchRepository repositorio, KafkaTemplate<String, ScoreEvent> productor) {
        this.repositorio = repositorio;
        this.productor = productor;
    }

    public List<Match> listar() {
        return repositorio.findAll();
    }

    public Match iniciar(Long idEncuentro) {
        Match encuentro = repositorio.findById(idEncuentro).orElseThrow();
        encuentro.setEstado("EN_CURSO");
        repositorio.save(encuentro);
        publicar(encuentro, 0, 0, "INICIADO");
        return encuentro;
    }

    // cada click del delegado: solo publica, no escribe en la BD
    public void registrarMarcador(Long idEncuentro, ScoreRequest marcador) {
        Match encuentro = repositorio.findById(idEncuentro).orElseThrow();
        publicar(encuentro, marcador.puntosLocal(), marcador.puntosVisita(), "PUNTO");
    }

    public Match finalizar(Long idEncuentro, ScoreRequest marcador) {
        Match encuentro = repositorio.findById(idEncuentro).orElseThrow();
        encuentro.setPuntosLocal(marcador.puntosLocal());
        encuentro.setPuntosVisita(marcador.puntosVisita());
        encuentro.setEstado("FINALIZADO");
        repositorio.save(encuentro);
        publicar(encuentro, marcador.puntosLocal(), marcador.puntosVisita(), "FINALIZADO");
        return encuentro;
    }

    private void publicar(Match encuentro, int puntosLocal, int puntosVisita, String tipo) {
        ScoreEvent evento = new ScoreEvent(encuentro.getId(), encuentro.getEquipoLocal(),
                encuentro.getEquipoVisita(), puntosLocal, puntosVisita, tipo);
        // la clave elige la particion: un encuentro siempre cae en la misma
        String clave = String.valueOf(encuentro.getId());
        productor.send(KafkaConfig.TOPIC, clave, evento).thenAccept(resultado ->
                log.info("[PUBLICA] topic={} clave={} particion={} offset={} tipo={} marcador={}-{}",
                        KafkaConfig.TOPIC, clave,
                        resultado.getRecordMetadata().partition(),
                        resultado.getRecordMetadata().offset(),
                        tipo, puntosLocal, puntosVisita));
    }
}
