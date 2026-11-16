package cl.athenet.competitions.event;

// INICIADO, PUNTO o FINALIZADO
public record ScoreEvent(
        Long idEncuentro,
        String equipoLocal,
        String equipoVisita,
        int puntosLocal,
        int puntosVisita,
        String tipo) {
}
