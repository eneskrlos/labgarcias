import estilos from './ControlesPaginacion.module.css';

const TAMANOS = [10, 20, 30];

/** spec.md §8.1 Regla 4: navegación anterior/siguiente + selector de tamaño (10/20/30). */
export function ControlesPaginacion({ pagina, tamano, totalPaginas, onCambiarPagina, onCambiarTamano }) {
  const totalPaginasVisible = Math.max(totalPaginas, 1);

  return (
    <div className={estilos.controles}>
      <div className={estilos.navegacion}>
        <button
          type="button"
          onClick={() => onCambiarPagina(pagina - 1)}
          disabled={pagina <= 0}
        >
          Anterior
        </button>
        <span>
          Página {pagina + 1} de {totalPaginasVisible}
        </span>
        <button
          type="button"
          onClick={() => onCambiarPagina(pagina + 1)}
          disabled={pagina + 1 >= totalPaginasVisible}
        >
          Siguiente
        </button>
      </div>

      <label className={estilos.selector}>
        Mostrar
        <select value={tamano} onChange={(evento) => onCambiarTamano(Number(evento.target.value))}>
          {TAMANOS.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
