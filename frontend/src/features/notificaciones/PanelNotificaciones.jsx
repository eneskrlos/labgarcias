import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ControlesPaginacion } from '../../shared/components/ControlesPaginacion';
import ItemNotificacion from './ItemNotificacion';
import { listarPaginado, marcarLeida, marcarTodasLeidas } from './api';
import { CLAVE_CONTADOR, CLAVE_LISTADO } from './claves';
import estilos from './PanelNotificaciones.module.css';

/**
 * Agente.md §6.2: la paginación la resuelve el backend. El panel pide una página por vez y
 * nunca trae la colección completa para cortarla en el cliente.
 * Sin selector de tamaño: no es una vista CRUD de §8.1 (no tiene alta ni edición), así que
 * navega con anterior/siguiente sobre un tamaño fijo.
 */
const TAMANO_PAGINA = 10;

export default function PanelNotificaciones({ onCerrar }) {
  const [pagina, setPagina] = useState(0);
  const queryClient = useQueryClient();

  const consulta = useQuery({
    queryKey: [...CLAVE_LISTADO, pagina],
    queryFn: () => listarPaginado({ pagina, tamano: TAMANO_PAGINA }),
    placeholderData: keepPreviousData,
  });

  const mutacionLeer = useMutation({
    // Envuelto a propósito: useMutation pasa un segundo argumento con su contexto y el cliente
    // de API no lo espera.
    mutationFn: (id) => marcarLeida(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_LISTADO });
      queryClient.invalidateQueries({ queryKey: CLAVE_CONTADOR });
    },
  });

  const mutacionLeerTodas = useMutation({
    mutationFn: () => marcarTodasLeidas(),
    onSuccess: (contador) => {
      // §6.4: leer-todas ya devuelve el contador al día; volver a pedirlo sería una llamada de más.
      queryClient.setQueryData(CLAVE_CONTADOR, contador);
      queryClient.invalidateQueries({ queryKey: CLAVE_LISTADO });
    },
  });

  const notificaciones = consulta.data?.contenido ?? [];
  const hayError = consulta.isError && !consulta.isLoading;

  return (
    <div className={estilos.panel} role="dialog" aria-label="Notificaciones">
      <div className={estilos.encabezado}>
        <h2>Notificaciones</h2>
        <button type="button" onClick={onCerrar} aria-label="Cerrar notificaciones">
          ✕
        </button>
      </div>

      <button
        type="button"
        className={estilos.leerTodas}
        onClick={() => mutacionLeerTodas.mutate()}
        disabled={mutacionLeerTodas.isPending}
      >
        Marcar todas como leídas
      </button>

      {consulta.isLoading && <p className={estilos.estado}>Cargando notificaciones...</p>}

      {hayError && (
        <div className={estilos.estado}>
          <p className={estilos.error}>{consulta.error.mensaje}</p>
          <button type="button" onClick={() => consulta.refetch()}>
            Reintentar
          </button>
        </div>
      )}

      {!consulta.isLoading && !hayError && notificaciones.length === 0 && (
        <p className={estilos.estado}>No tenés notificaciones.</p>
      )}

      {!hayError && notificaciones.length > 0 && (
        <ul className={estilos.lista}>
          {notificaciones.map((notificacion) => (
            <ItemNotificacion
              key={notificacion.id}
              notificacion={notificacion}
              onLeer={() => mutacionLeer.mutate(notificacion.id)}
              deshabilitado={mutacionLeer.isPending}
            />
          ))}
        </ul>
      )}

      <ControlesPaginacion
        pagina={pagina}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={setPagina}
      />
    </div>
  );
}
