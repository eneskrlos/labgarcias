import { ControlesPaginacion } from './ControlesPaginacion';
import estilos from './TablaPaginada.module.css';

/**
 * spec.md §8.1 Regla 3 y Regla 4: tabla genérica con los tres estados obligatorios
 * (cargando, vacío, error) más los controles de paginación. `cargando` debe reflejar
 * solo la carga inicial (sin datos previos); mientras se trae la página siguiente con
 * datos previos visibles, usar `actualizando` para no producir salto de layout.
 */
export function TablaPaginada({
  columnas,
  filas,
  claveFila = (fila) => fila.id,
  cargando,
  actualizando,
  error,
  onReintentar,
  mensajeVacio = 'No hay registros para mostrar.',
  accionVacio,
  pagina,
  tamano,
  totalPaginas,
  onCambiarPagina,
  onCambiarTamano,
}) {
  const hayFilas = filas && filas.length > 0;

  return (
    <div className={estilos.contenedor}>
      <div className={estilos.envoltorioTabla}>
        <table className={estilos.tabla}>
          <thead>
            <tr>
              {columnas.map((columna) => (
                <th key={columna.clave}>{columna.encabezado}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {cargando &&
              Array.from({ length: tamano }).map((_, indice) => (
                <tr key={`esqueleto-${indice}`}>
                  {columnas.map((columna) => (
                    <td key={columna.clave}>
                      <span className={estilos.esqueleto} />
                    </td>
                  ))}
                </tr>
              ))}

            {!cargando && error && (
              <tr>
                <td colSpan={columnas.length} className={estilos.estadoCelda}>
                  <p className={estilos.error}>{error}</p>
                  {onReintentar && (
                    <button type="button" onClick={onReintentar}>
                      Reintentar
                    </button>
                  )}
                </td>
              </tr>
            )}

            {!cargando && !error && !hayFilas && (
              <tr>
                <td colSpan={columnas.length} className={estilos.estadoCelda}>
                  <p>{mensajeVacio}</p>
                  {accionVacio}
                </td>
              </tr>
            )}

            {!cargando &&
              !error &&
              hayFilas &&
              filas.map((fila) => (
                <tr key={claveFila(fila)} className={fila.activo === false ? estilos.inactivo : undefined}>
                  {columnas.map((columna) => (
                    <td key={columna.clave}>{columna.render ? columna.render(fila) : fila[columna.clave]}</td>
                  ))}
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {actualizando && <p className={estilos.actualizando}>Actualizando...</p>}

      <ControlesPaginacion
        pagina={pagina}
        tamano={tamano}
        totalPaginas={totalPaginas}
        onCambiarPagina={onCambiarPagina}
        onCambiarTamano={onCambiarTamano}
      />
    </div>
  );
}
