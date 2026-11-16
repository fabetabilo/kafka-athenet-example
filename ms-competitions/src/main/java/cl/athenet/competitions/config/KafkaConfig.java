package cl.athenet.competitions.config;

import java.time.Duration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC = "marcador";

    @Bean
    NewTopic marcador() {
        // 3 copias de cada particion: un lider y dos seguidores en brokers distintos
        return TopicBuilder.name(TOPIC).partitions(3).replicas(3)
                // borrar por tiempo, no por lectura
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                // 7 dias: los mensajes viven aunque ya se hayan leido
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }
}
