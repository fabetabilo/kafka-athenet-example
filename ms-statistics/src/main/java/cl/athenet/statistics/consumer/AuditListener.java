package cl.athenet.statistics.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.athenet.statistics.event.ScoreEvent;

// segundo grupo: lee los mismos mensajes que estadisticas, con su propia marca
@Component
public class AuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuditListener.class);

    @KafkaListener(topics = "marcador", groupId = "auditoria")
    public void auditar(ConsumerRecord<String, ScoreEvent> registro) {
        log.info("[AUDITORIA] grupo=auditoria particion={} offset={} clave={} tipo={}",
                registro.partition(), registro.offset(), registro.key(), registro.value().tipo());
    }
}
