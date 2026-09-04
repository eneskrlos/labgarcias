import estilos from './LineaTiempo.module.css';

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

/** §5.1 paso 9: el registro inicial no tiene autor porque lo asigna el sistema. */
const AUTOR_SISTEMA = 'Sistema';

/**
 * CU-04/CU-06, §5.4: el seguimiento de una orden, con la forma del prototipo —riel, punto,
 * conector— pero **sin su interacción**: ahí las etapas se hacen clic para simular el avance;
 * acá el avance es una acción real y exclusiva del admin (RN-04, §5.5), así que ninguna fila es
 * un `<button>` ni lleva `cursor: pointer` u hover de clic. Una afordancia de "esto se toca"
 * sobre algo que no se toca sería peor que ninguna.
 *
 * **Solo dos colores, no tres.** `lineaTiempo` únicamente trae lo que ya ocurrió —no hay entrada
 * para una etapa futura—, así que se colorea la última como "actual" y las anteriores como
 * "alcanzada". No se sintetiza un tramo "pendiente" cruzando con el catálogo de estados: para una
 * orden cancelada no hay un camino futuro real que mostrar, e inventarlo sería mostrar información
 * falsa. Esto es justamente lo que pide §5.4: el color sale de la **posición en la lista**, no del
 * código de cada etapa —por eso `EtapaSeguimientoResponse` no lo lleva.
 */
export function LineaTiempo({ etapas }) {
  return (
    <ol className={estilos.linea}>
      {etapas.map((etapa, indice) => {
        const esActual = indice === etapas.length - 1;
        return (
          <li
            key={`${etapa.estado}-${etapa.fechaHora}`}
            className={esActual ? estilos.actual : estilos.alcanzada}
          >
            <span className={estilos.riel} aria-hidden="true">
              <span className={estilos.punto} />
              {indice < etapas.length - 1 && <span className={estilos.conector} />}
            </span>
            <span className={estilos.cuerpo}>
              <span className={estilos.nombreEstado}>{etapa.estado}</span>
              <span className={estilos.fecha}>{FORMATO_FECHA_HORA.format(new Date(etapa.fechaHora))}</span>
              <span className={estilos.autor}>{etapa.autor ?? AUTOR_SISTEMA}</span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
