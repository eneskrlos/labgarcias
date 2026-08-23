import { Link } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import estilos from './PanelNotificaciones.module.css';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

const ROL_ODONTOLOGO = 'ODONTOLOGO';

/**
 * Una notificación dentro del panel.
 *
 * `ordenId` puede venir nulo (§6.4: hay avisos que no son de una orden). Cuando viene, es enlace
 * al seguimiento **solo para el odontólogo**: `/ordenes/:id` es su pantalla según §8 y muestra el
 * botón de cancelar, que §5.6 reserva al propietario. El administrador también recibe avisos con
 * `ordenId` —las órdenes urgentes—, pero su destino es el detalle del laboratorio, que construye
 * T-26; hasta entonces lo ve como dato.
 *
 * El mensaje ya viene armado por el backend con el código del paciente (RN-03/RN-22): acá no se
 * compone ningún texto.
 */
export default function ItemNotificacion({ notificacion, onLeer, deshabilitado }) {
  const { usuario } = useSesion();
  const clases = notificacion.leida ? estilos.item : `${estilos.item} ${estilos.noLeida}`;
  const enlazable = notificacion.ordenId != null && usuario?.rol === ROL_ODONTOLOGO;

  return (
    <li className={clases}>
      <p className={estilos.mensaje}>{notificacion.mensaje}</p>

      <p className={estilos.detalle}>
        <span>{FORMATO_FECHA.format(new Date(notificacion.fechaCreacion))}</span>
        {enlazable && <Link to={`/ordenes/${notificacion.ordenId}`}>Orden #{notificacion.ordenId}</Link>}
        {notificacion.ordenId != null && !enlazable && <span>Orden #{notificacion.ordenId}</span>}
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
