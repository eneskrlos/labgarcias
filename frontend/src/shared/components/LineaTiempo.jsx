import { Icono } from './Icono';
import estilos from './LineaTiempo.module.css';

const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

/** §5.1 paso 9: el registro inicial no tiene autor porque lo asigna el sistema. */
const AUTOR_SISTEMA = 'Sistema';

/**
 * RN-04: el flujo normal, en el orden en que `orden_secuencia` los define. Una etapa alcanzada
 * (no la última) **siempre** es la de su posición: el flujo es estrictamente hacia adelante y de
 * a un paso (§5.5), así que la posición basta para saber qué ícono le corresponde sin mirar el
 * texto de la etapa — exactamente el mismo criterio con el que §5.4 decide alcanzada o actual.
 */
const ICONOS_FLUJO = [
  'estadoRecibido',
  'estadoEnEvaluacion',
  'estadoEnProduccion',
  'estadoControlCalidad',
  'estadoListo',
  'estadoEntregado',
];

/**
 * CU-04/CU-06, §5.4: el seguimiento de una orden, con la forma del prototipo —riel, punto,
 * conector, ícono por etapa y el círculo de verificación— pero **sin su interacción**: ahí las
 * etapas se hacen clic para simular el avance; acá el avance es una acción real y exclusiva del
 * admin (RN-04, §5.5), así que ninguna fila es un `<button>` ni lleva `cursor: pointer` u hover
 * de clic. Una afordancia de "esto se toca" sobre algo que no se toca sería peor que ninguna.
 *
 * **Solo dos colores, no tres.** `lineaTiempo` únicamente trae lo que ya ocurrió —no hay entrada
 * para una etapa futura—, así que se colorea la última como "actual" y las anteriores como
 * "alcanzada". No se sintetiza un tramo "pendiente" cruzando con el catálogo de estados: para una
 * orden cancelada no hay un camino futuro real que mostrar, e inventarlo sería mostrar información
 * falsa. Esto es justamente lo que pide §5.4: el color sale de la **posición en la lista**, no del
 * código de cada etapa —por eso `EtapaSeguimientoResponse` no lo lleva.
 *
 * **`estadoActualCodigo` es aparte, y es solo para el ícono de la última fila.** No es un código
 * por etapa —eso seguiría faltando en `EtapaSeguimientoResponse`, sin uso—, es el código de la
 * orden entera, que la pantalla ya tiene. Sirve para una sola pregunta binaria y estable: ¿la
 * última etapa es `CANCELADO`? Sin él, la única alternativa sería adivinar por posición, y una
 * cancelación no respeta la posición del flujo lineal (puede caer en cualquier paso).
 */
export function LineaTiempo({ etapas, estadoActualCodigo }) {
  return (
    <ol className={estilos.linea}>
      {etapas.map((etapa, indice) => {
        const esActual = indice === etapas.length - 1;
        const esCancelada = esActual && estadoActualCodigo === 'CANCELADO';
        const nombreIcono = esCancelada ? 'estadoCancelado' : ICONOS_FLUJO[indice] ?? 'estadoEntregado';
        return (
          <li
            key={`${etapa.estado}-${etapa.fechaHora}`}
            className={`${estilos.item} ${esActual ? estilos.actual : estilos.alcanzada}`}
          >
            <span className={estilos.riel} aria-hidden="true">
              <span className={estilos.punto} />
              {indice < etapas.length - 1 && <span className={estilos.conector} />}
            </span>
            <span className={estilos.cuerpo}>
              <span className={estilos.nombreEstado}>
                <Icono nombre={nombreIcono} tamano={14} />
                {etapa.estado}
              </span>
              <span className={estilos.fecha}>{FORMATO_FECHA_HORA.format(new Date(etapa.fechaHora))}</span>
              <span className={estilos.autor}>{etapa.autor ?? AUTOR_SISTEMA}</span>
            </span>
            <span className={estilos.marca} aria-hidden="true">
              <Icono nombre="verificado" tamano={12} />
            </span>
          </li>
        );
      })}
    </ol>
  );
}
