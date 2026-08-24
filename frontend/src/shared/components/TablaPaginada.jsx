import { ControlesPaginacion } from './ControlesPaginacion';
import estilos from './TablaPaginada.module.css';

/**
 * spec.md §8.1 Regla 3 y Regla 4: tabla genérica con los tres estados obligatorios
 * (cargando, vacío, error) más los controles de paginación. `cargando` debe reflejar
 * solo la carga inicial (sin datos previos); mientras se trae la página siguiente con
 * datos previos visibles, usar `actualizando` para no producir salto de layout.
 *
 * **Sin `onCambiarPagina` no se dibujan los controles.** Los bloques de resumen de los paneles
 * (CU-02, CU-10) son una lista corta y cerrada —hasta 5 filas que decide el backend—, no un
 * listado navegable: mostrarles una paginación de una sola página sería ofrecer un control que no
 * lleva a ningún lado. Se resolvió extendiendo este componente y no con una tabla propia de esos
 * paneles, que es lo que manda §8.1 Regla 4.
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

      {onCambiarPagina && (
        <ControlesPaginacion
          pagina={pagina}
          tamano={tamano}
          totalPaginas={totalPaginas}
          onCambiarPagina={onCambiarPagina}
          onCambiarTamano={onCambiarTamano}
        />
      )}
    </div>
  );
}
