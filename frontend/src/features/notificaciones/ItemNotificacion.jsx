import { Link } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import estilos from './PanelNotificaciones.module.css';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

const ROL_ODONTOLOGO = 'ODONTOLOGO';

/**
 * Una notificación dentro del panel.
 *
 * `ordenId` puede venir nulo (§6.4: hay avisos que no son de una orden). Cuando viene, es enlace
 * a la orden, **y el destino depende del rol**: el odontólogo va a su seguimiento (`/ordenes/:id`,
 * §8) y el laboratorio a su propio detalle (`/admin/ordenes/:id`, §5.7). Son dos pantallas
 * distintas a propósito: la del odontólogo tiene el botón de cancelar, que §5.6 le reserva al
 * propietario, y la del admin muestra el nombre del paciente, que §5.4 le da solo a él.
 *
 * El mensaje ya viene armado por el backend con el código del paciente (RN-03/RN-22): acá no se
 * compone ningún texto.
 */
export default function ItemNotificacion({ notificacion, onLeer, deshabilitado }) {
  const { usuario } = useSesion();
  const clases = notificacion.leida ? estilos.item : `${estilos.item} ${estilos.noLeida}`;
  const rutaOrden = usuario?.rol === ROL_ODONTOLOGO
    ? `/ordenes/${notificacion.ordenId}`
    : `/admin/ordenes/${notificacion.ordenId}`;
  const enlazable = notificacion.ordenId != null;

  return (
    <li className={clases}>
      <p className={estilos.mensaje}>{notificacion.mensaje}</p>

      <p className={estilos.detalle}>
        <span>{FORMATO_FECHA.format(new Date(notificacion.fechaCreacion))}</span>
        {enlazable && <Link to={rutaOrden}>Orden #{notificacion.ordenId}</Link>}
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
