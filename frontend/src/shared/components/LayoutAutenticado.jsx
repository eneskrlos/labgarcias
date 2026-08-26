import estilos from './LayoutAutenticado.module.css';

/**
 * Cromo común de las pantallas con sesión iniciada: barra lateral con la marca y el menú de
 * §8, y una barra superior con las acciones.
 *
 * `acciones` y `navegacion` son huecos: por ahí entran la campana (§6.4) y el menú de §8, que
 * viven en `features/`. Así este componente no importa nada de `features/`, coherente con la
 * regla de acoplamiento de Agente.md §5.4.3.
 *
 * `navegacion` sigue siendo opcional: sin él no se renderiza ningún `<nav>`. La marca es un
 * `<span>` y no un enlace a propósito —el destino de la raíz depende del rol (§8) y este
 * componente no conoce roles—.
 *
 * **La estructura de navegación de §8 no cambió**: los mismos dos menús, con los mismos ítems
 * y el mismo orden. Lo que cambió es dónde se dibujan.
 */
export function LayoutAutenticado({ acciones, navegacion, children }) {
  return (
    <div className={estilos.layout}>
      <aside className={estilos.barraLateral}>
        <span className={estilos.marca}>Lab. Garcia&apos;s Connect</span>
        {navegacion && <nav className={estilos.navegacion}>{navegacion}</nav>}
      </aside>

      <div className={estilos.columna}>
        <header className={estilos.barraSuperior}>{acciones}</header>
        <main className={estilos.contenido}>{children}</main>
      </div>
    </div>
  );
}
