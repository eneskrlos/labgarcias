import { colorPorEstado } from '../util/coloresEstado';
import estilos from './EtiquetaEstado.module.css';

/**
 * CU-22/docs/ESTADO.md (26/08/2026): la píldora de estado de una orden. Se colorea por
 * `estadoCodigo`, no por `estado` —el nombre que ese caso de uso deja renombrar—, así que el
 * color no depende de un texto editable. El texto que se muestra sigue siendo `estado`: es lo
 * que el usuario reconoce.
 */
export function EtiquetaEstado({ estado, estadoCodigo }) {
  const { fondo, texto } = colorPorEstado(estadoCodigo);
  return (
    <span className={estilos.etiqueta} style={{ background: fondo, color: texto }}>
      {estado}
    </span>
  );
}
