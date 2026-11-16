package cl.athenet.statistics.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import cl.athenet.statistics.event.ScoreEvent;
import cl.athenet.statistics.model.ScoreDocument;
import cl.athenet.statistics.repository.ScoreRepository;

@Component
public class ScoreListener {

    private static final Logger log = LoggerFactory.getLogger(ScoreListener.class);

    private final ScoreRepository repositorio;
    private final SimpMessagingTemplate mensajeria;

    public ScoreListener(ScoreRepository repositorio, SimpMessagingTemplate mensajeria) {
        this.repositorio = repositorio;
        this.mensajeria = mensajeria;
    }

    @KafkaListener(topics = "marcador", groupId = "estadisticas")
    public void escuchar(ConsumerRecord<String, ScoreEvent> registro) {
        ScoreEvent evento = registro.value();
        log.info("[LEE] grupo=estadisticas particion={} offset={} clave={} tipo={} marcador={}-{}",
                registro.partition(), registro.offset(), registro.key(),
                evento.tipo(), evento.puntosLocal(), evento.puntosVisita());
        // si el encuentro ya tiene documento se reemplaza: queda el ultimo marcador
        String id = repositorio.findByIdEncuentro(evento.idEncuentro()).map(ScoreDocument::id).orElse(null);
        ScoreDocument documento = repositorio.save(new ScoreDocument(id, evento.idEncuentro(), evento.equipoLocal(),
                evento.equipoVisita(), evento.puntosLocal(), evento.puntosVisita(), evento.tipo()));
        // empuja el marcador a los navegadores suscritos por STOMP
        mensajeria.convertAndSend("/topic/marcador", documento);
        // al terminar sin error, Spring confirma el offset del grupo
    }
}
