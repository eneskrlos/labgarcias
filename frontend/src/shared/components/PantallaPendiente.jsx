import { Link } from 'react-router-dom';
import estilos from './PantallaPendiente.module.css';

/**
 * Destino de un ítem del menú de §8 cuya pantalla todavía no se construyó.
 *
 * El menú de §8 se monta completo desde la tarea que crea el primero de sus destinos, así que
 * algunos ítems apuntan a pantallas que llegan después. Un ítem que lleva a una página en blanco
 * es un defecto visible: acá dice qué va a haber ahí y ofrece una salida.
 *
 * **No tiene funcionalidad y no debe adquirirla.** La tarea dueña de cada pantalla reemplaza la
 * ruta por la suya; ver los puntos abiertos de ESTADO.md.
 */
export function PantallaPendiente({ titulo, detalle }) {
  return (
    <div className={`contenedor ${estilos.pantalla}`}>
      <h1>{titulo}</h1>
      <p className={estilos.aviso}>Disponible próximamente.</p>
      {detalle && <p className={estilos.detalle}>{detalle}</p>}
      <Link to="/">Volver al inicio</Link>
    </div>
  );
}
