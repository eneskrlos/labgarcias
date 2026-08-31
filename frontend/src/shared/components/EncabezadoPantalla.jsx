import estilos from './EncabezadoPantalla.module.css';

/**
 * §8.1 Regla 4/5: el título, una acción o un filtro a la derecha y el mensaje de confirmación
 * que vuelve de un alta se repetían casi idénticos en cada listado. Vive acá una sola vez.
 *
 * `children` es el contenido a la derecha del título —un enlace de acción, un filtro, o nada—;
 * cada pantalla decide qué va ahí porque no hay un único caso que cubrir.
 */
export function EncabezadoPantalla({ titulo, confirmacion, children }) {
  return (
    <>
      <div className={estilos.encabezado}>
        <h1>{titulo}</h1>
        {children}
      </div>
      {confirmacion && (
        <p className={estilos.confirmacion} role="status">
          {confirmacion}
        </p>
      )}
    </>
  );
}
