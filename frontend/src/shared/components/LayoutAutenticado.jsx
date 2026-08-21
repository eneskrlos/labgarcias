import estilos from './LayoutAutenticado.module.css';

/**
 * Encabezado común de las pantallas con sesión iniciada.
 *
 * `acciones` es el hueco donde el consumidor monta la campana (§6.4): así este componente no
 * importa nada de `features/`, coherente con la regla de acoplamiento de Agente.md §5.4.3.
 *
 * Sin menú de navegación a propósito: el menú de §8 lo arma la tarea que cree sus destinos
 * (T-25, T-26, T-33b). Hoy enlazaría a rutas que todavía no existen.
 */
export function LayoutAutenticado({ acciones, children }) {
  return (
    <div className={estilos.layout}>
      <header className={estilos.encabezado}>
        <div className={estilos.barra}>
          <span className={estilos.marca}>Lab. Garcia&apos;s Connect</span>
          {acciones}
        </div>
      </header>

      <main className={estilos.contenido}>{children}</main>
    </div>
  );
}
