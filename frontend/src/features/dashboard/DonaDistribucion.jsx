import { colorPorEstado } from '../../shared/util/coloresEstado';
import estilos from './DonaDistribucion.module.css';

const RADIO = 35;
const ANCHO_ANILLO = 16;
const CIRCUNFERENCIA = 2 * Math.PI * RADIO;

/**
 * CU-10/§5.7: la distribución por estado del dashboard del laboratorio, como dona.
 *
 * SVG a mano armando arcos con `stroke-dasharray`/`stroke-dashoffset`: sin librería de gráficos
 * (`Agente.md` §3.4, sin dependencias nuevas sin justificar). Los colores salen de
 * `coloresEstado.js` —el mismo mapa que `EtiquetaEstado`, no uno paralelo— para que la dona y la
 * píldora de un mismo estado siempre coincidan.
 *
 * **Presentación únicamente**: `distribucion` llega ya contada por el backend (§8); acá no se
 * recalcula ningún indicador, solo se suma el total para el centro de la dona, que es el mismo
 * tipo de aritmética de despliegue que ya hacía la barra proporcional que esto reemplaza.
 *
 * **Accesible**: una dona sola no le dice nada a un lector de pantalla —la cifra no está al lado
 * como en una barra—, así que el SVG lleva `role="img"` y un `aria-label` con la distribución
 * completa en texto. La leyenda de abajo repite lo mismo para quien mira.
 */
export function DonaDistribucion({ distribucion, cargando }) {
  const total = distribucion.reduce((suma, fila) => suma + fila.cantidad, 0);

  const resumenAccesible = total === 0
    ? 'Sin órdenes registradas.'
    : distribucion
        .filter((fila) => fila.cantidad > 0)
        .map((fila) => `${fila.estadoNombre}: ${fila.cantidad}`)
        .join(', ');

  let acumulado = 0;

  return (
    <div className={estilos.contenedor}>
      <div className={estilos.grafico}>
        <svg
          viewBox="0 0 100 100"
          className={estilos.dona}
          role="img"
          aria-label={`Distribución de órdenes por estado. ${resumenAccesible}`}
        >
          {total === 0 ? (
            <circle cx="50" cy="50" r={RADIO} className={estilos.vacia} />
          ) : (
            distribucion
              .filter((fila) => fila.cantidad > 0)
              .map((fila) => {
                const fraccion = fila.cantidad / total;
                const largo = fraccion * CIRCUNFERENCIA;
                const offset = -acumulado * CIRCUNFERENCIA;
                acumulado += fraccion;
                return (
                  <circle
                    key={fila.estadoCodigo}
                    cx="50"
                    cy="50"
                    r={RADIO}
                    fill="none"
                    stroke={colorPorEstado(fila.estadoCodigo).texto}
                    strokeWidth={ANCHO_ANILLO}
                    strokeDasharray={`${largo} ${CIRCUNFERENCIA - largo}`}
                    strokeDashoffset={offset}
                    transform="rotate(-90 50 50)"
                  />
                );
              })
          )}
        </svg>
        <div className={estilos.centro} aria-hidden="true">
          <span className={estilos.total}>{total}</span>
          {/* No "en total": con CANCELADO y ENTREGADO incluidos, esto es el total histórico de
              órdenes, no un indicador de trabajo activo. "órdenes" a secas no sugiere lo segundo. */}
          <span className={estilos.totalEtiqueta}>órdenes</span>
        </div>
      </div>

      <ul className={estilos.leyenda}>
        {distribucion.map((fila) => (
          <li key={fila.estadoCodigo} className={estilos.itemLeyenda}>
            <span
              className={estilos.marca}
              style={{ background: colorPorEstado(fila.estadoCodigo).texto }}
              aria-hidden="true"
            />
            <span className={estilos.nombreEstado}>{fila.estadoNombre}</span>
            <span className={estilos.cantidad}>{fila.cantidad}</span>
          </li>
        ))}
        {!cargando && distribucion.length === 0 && <li>No hay etapas cargadas.</li>}
      </ul>
    </div>
  );
}
