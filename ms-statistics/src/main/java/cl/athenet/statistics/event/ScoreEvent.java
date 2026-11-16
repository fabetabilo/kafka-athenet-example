package cl.athenet.statistics.event;

// misma forma que el evento de ms-competitions, pero clase propia
public record ScoreEvent(Long idEncuentro, String equipoLocal, String equipoVisita,
                         int puntosLocal, int puntosVisita, String tipo) {
}
