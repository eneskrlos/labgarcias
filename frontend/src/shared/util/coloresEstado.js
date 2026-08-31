/**
 * Colores de la etiqueta de estado de una orden, keyeados por `estadoCodigo` —nunca por
 * `estado`, el nombre que CU-22 deja editar—: un mapa por nombre se rompería en silencio al
 * renombrar una etapa. Vive en un solo lugar porque lo consumen tanto `EtiquetaEstado` como la
 * dona de distribución del bloque 4.
 *
 * Los seis primeros salen del prototipo (`StatusBadge`/`STATUS` en docs/prototipo). `CANCELADO`
 * no está ahí —el prototipo define seis estados y el sistema tiene siete— y se decidió aparte
 * (docs/ESTADO.md, 26/08/2026): no reutiliza `--color-error` porque una orden cancelada es un
 * estado de negocio legítimo, no un fallo de la aplicación.
 */
export const COLOR_POR_ESTADO = {
  RECIBIDO: { fondo: '#E1EAF7', texto: '#2B4C86' },
  EN_EVALUACION: { fondo: '#E9E0F7', texto: '#6B4BA8' },
  EN_PRODUCCION: { fondo: '#FCE6CC', texto: '#B4651A' },
  CONTROL_CALIDAD: { fondo: '#D8E9F8', texto: '#1F5F9C' },
  LISTO: { fondo: '#D9F0D6', texto: '#2E7D32' },
  ENTREGADO: { fondo: '#E4E8ED', texto: '#4A5560' },
  CANCELADO: { fondo: '#F6E2E1', texto: '#A03A32' },
};

const COLOR_DESCONOCIDO = { fondo: 'var(--color-borde)', texto: 'var(--color-texto-tenue)' };

/** RN-04/§4.2 fija los siete códigos; el valor por defecto es solo para no romper el render. */
export function colorPorEstado(estadoCodigo) {
  return COLOR_POR_ESTADO[estadoCodigo] ?? COLOR_DESCONOCIDO;
}
