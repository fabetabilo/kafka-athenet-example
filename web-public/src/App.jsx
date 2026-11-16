import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import './App.css'

const URL_STATISTICS = 'http://localhost:8082'
const URL_WEBSOCKET = 'ws://localhost:8082/ws'

function App() {
  const [marcador, setMarcador] = useState(null)
  const [conectado, setConectado] = useState(false)

  useEffect(() => {
    // estado actual al cargar: el websocket solo trae los cambios
    fetch(`${URL_STATISTICS}/marcador/actual`)
      .then((respuesta) => (respuesta.status === 200 ? respuesta.json() : null))
      .then(setMarcador)

    const cliente = new Client({
      brokerURL: URL_WEBSOCKET,
      onConnect: () => {
        setConectado(true)
        // cada evento que ms-statistics lee de Kafka llega por este canal
        cliente.subscribe('/topic/marcador', (mensaje) => setMarcador(JSON.parse(mensaje.body)))
      },
      onWebSocketClose: () => setConectado(false),
    })
    cliente.activate()
    return () => cliente.deactivate()
  }, [])

  return (
    <main>
      <p className={conectado ? 'estado conectado' : 'estado'}>
        {conectado ? 'Conectado' : 'Desconectado'}
      </p>
      {marcador ? (
        <>
          <p className="etiqueta">{marcador.tipo === 'FINALIZADO' ? 'Finalizado' : 'En vivo'}</p>
          <div className="marcador">
            <div className="equipo">
              <h2>{marcador.equipoLocal}</h2>
              <span className="puntos">{marcador.puntosLocal}</span>
            </div>
            <span className="separador">-</span>
            <div className="equipo">
              <h2>{marcador.equipoVisita}</h2>
              <span className="puntos">{marcador.puntosVisita}</span>
            </div>
          </div>
        </>
      ) : (
        <p className="etiqueta">Sin partido en curso</p>
      )}
    </main>
  )
}

export default App
