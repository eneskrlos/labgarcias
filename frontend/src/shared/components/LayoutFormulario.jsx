import estilos from './LayoutFormulario.module.css';

/**
 * spec.md §8.1 Regla 4: encabezado, cuerpo y botones Guardar/Cancelar comunes a todo
 * formulario de alta/edición. `noValidate` es obligatorio acá: sin esto, min/required
 * nativos del navegador bloquean el submit antes de que el backend pueda devolver el
 * mensaje real de la regla de negocio (bug detectado en T-15).
 */
export function LayoutFormulario({ titulo, onSubmit, onCancelar, guardando, error, children }) {
  return (
    <form onSubmit={onSubmit} className={estilos.formulario} noValidate>
      <h2>{titulo}</h2>

      {error && <p className={estilos.error}>{error}</p>}

      {children}

      <div className={estilos.acciones}>
        <button type="submit" className={estilos.boton} disabled={guardando}>
          {guardando ? 'Guardando...' : 'Guardar'}
        </button>
        <button type="button" className={estilos.botonSecundario} onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
