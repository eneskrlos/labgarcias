import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Icono } from '../../shared/components/Icono';
import { contarNoLeidas } from './api';
import { CLAVE_CONTADOR } from './claves';
import PanelNotificaciones from './PanelNotificaciones';
import estilos from './Campana.module.css';

/** spec.md §6.4: el contador se refresca por polling cada 60 s. Prohibido WebSocket y SSE. */
const INTERVALO_REFRESCO_MS = 60_000;

function etiquetaAccesible(noLeidas) {
  return noLeidas > 0
    ? `Notificaciones: ${noLeidas} sin leer`
    : 'Notificaciones: ninguna sin leer';
}

export default function Campana() {
  const [abierto, setAbierto] = useState(false);

  const consulta = useQuery({
    queryKey: CLAVE_CONTADOR,
    queryFn: contarNoLeidas,
    refetchInterval: INTERVALO_REFRESCO_MS,
  });

  // Un contador que no se pudo traer no es motivo para romper el encabezado: se muestra sin globo.
  const noLeidas = consulta.data?.noLeidas ?? 0;

  return (
    <div className={estilos.campana}>
      <button
        type="button"
        className={estilos.boton}
        aria-expanded={abierto}
        aria-label={etiquetaAccesible(noLeidas)}
        onClick={() => setAbierto((anterior) => !anterior)}
      >
        <Icono nombre="campana" tamano={18} />
        {noLeidas > 0 && <span className={estilos.contador}>{noLeidas}</span>}
      </button>

      {abierto && <PanelNotificaciones onCerrar={() => setAbierto(false)} />}
    </div>
  );
}
