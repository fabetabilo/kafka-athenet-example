package cl.athenet.statistics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// un documento por encuentro con su ultimo marcador
// el _id lo genera MongoDB (ObjectId) para que mongo-express pueda abrirlo
@Document("marcadores")
public record ScoreDocument(@Id String id, Long idEncuentro, String equipoLocal, String equipoVisita,
                            int puntosLocal, int puntosVisita, String tipo) {
}
