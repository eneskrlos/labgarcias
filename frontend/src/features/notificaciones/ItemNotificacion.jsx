import estilos from './PanelNotificaciones.module.css';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

/**
 * Una notificación dentro del panel.
 *
 * `ordenId` puede venir nulo (§6.4: hay avisos que no son de una orden) y se muestra como dato,
 * sin enlace: la pantalla `/ordenes/:id` la crea T-25 y hasta entonces el enlace estaría muerto.
 * El mensaje ya viene armado por el backend con el código del paciente (RN-03/RN-22): acá no se
 * compone ningún texto.
 */
export default function ItemNotificacion({ notificacion, onLeer, deshabilitado }) {
  const clases = notificacion.leida ? estilos.item : `${estilos.item} ${estilos.noLeida}`;

  return (
    <li className={clases}>
      <p className={estilos.mensaje}>{notificacion.mensaje}</p>

      <p className={estilos.detalle}>
        <span>{FORMATO_FECHA.format(new Date(notificacion.fechaCreacion))}</span>
        {notificacion.ordenId != null && <span>Orden #{notificacion.ordenId}</span>}
        {!notificacion.leida && <span className={estilos.etiquetaNoLeida}>Sin leer</span>}
      </p>

      {!notificacion.leida && (
        <button type="button" onClick={onLeer} disabled={deshabilitado}>
          Marcar como leída
        </button>
      )}
    </li>
  );
}
