CREATE TABLE encuentros (
    id            BIGSERIAL PRIMARY KEY,
    equipo_local  VARCHAR(100) NOT NULL,
    equipo_visita VARCHAR(100) NOT NULL,
    puntos_local  INT NOT NULL DEFAULT 0,
    puntos_visita INT NOT NULL DEFAULT 0,
    estado        VARCHAR(20) NOT NULL
);

INSERT INTO encuentros (equipo_local, equipo_visita, estado) VALUES
    ('Leones', 'Halcones', 'PROGRAMADO'),
    ('Pumas', 'Condores', 'PROGRAMADO'),
    ('Toros', 'Zorros', 'PROGRAMADO');
