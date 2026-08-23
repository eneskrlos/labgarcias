import estilos from './LayoutAutenticado.module.css';

/**
 * Encabezado común de las pantallas con sesión iniciada.
 *
 * `acciones` y `navegacion` son huecos: por ahí entran la campana (§6.4) y el menú de §8, que
 * viven en `features/`. Así este componente no importa nada de `features/`, coherente con la
 * regla de acoplamiento de Agente.md §5.4.3.
 *
 * `navegacion` es opcional a propósito: el menú de §8 lo monta la tarea que crea sus destinos.
 * T-25 arma el del odontólogo; el del admin lo arman T-26 y T-27, cuando existan sus pantallas.
 */
export function LayoutAutenticado({ acciones, navegacion, children }) {
  return (
    <div className={estilos.layout}>
      <header className={estilos.encabezado}>
        <div className={estilos.barra}>
          <span className={estilos.marca}>Lab. Garcia&apos;s Connect</span>
          {acciones}
        </div>
        {navegacion && <nav className={estilos.navegacion}>{navegacion}</nav>}
      </header>

      <main className={estilos.contenido}>{children}</main>
    </div>
  );
}
