import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { solicitarAcceso } from './api';
import estilos from './Auth.module.css';

const VALORES_INICIALES = { nombreCompleto: '', correo: '', direccion: '', telefono: '' };
const CAMPOS_CONOCIDOS = ['nombreCompleto', 'correo', 'direccion', 'telefono'];

/**
 * D-17/§3.1: formulario público que reemplaza al auto-registro. Enviarlo no crea ninguna cuenta
 * —la crea el administrador (§3.1.b)—, así que la pantalla no promete acceso ni pide contraseña.
 * Sin captcha en esta versión (§3.1).
 */
export default function SolicitarAcceso() {
  const [datos, setDatos] = useState(VALORES_INICIALES);

  // Envuelto a propósito: useMutation pasa un segundo argumento con su contexto y el cliente
  // de API no lo espera.
  const mutacion = useMutation({ mutationFn: (datosSolicitud) => solicitarAcceso(datosSolicitud) });

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate(datos);
  };

  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  if (mutacion.isSuccess) {
    return (
      <div className={estilos.pantalla}>
        <div className={estilos.tarjeta}>
          <h1>Solicitud enviada</h1>
          {/* El texto lo fija §3.1 y lo devuelve el backend: no se reescribe en el cliente. */}
          <p className={estilos.exito}>{mutacion.data.mensaje}</p>
          <p className={estilos.enlaces}>
            <Link to="/login">Volver al inicio de sesión</Link>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Solicitar acceso</h1>
        <p className={estilos.ayuda}>
          Completá tus datos y el laboratorio se va a comunicar con vos para crear tu cuenta.
        </p>

        {errorGeneral && <p className={estilos.error}>{errorGeneral}</p>}

        <form onSubmit={enviar} noValidate>
          <CampoFormulario id="nombreCompleto" etiqueta="Nombre completo" error={errorDelCampo('nombreCompleto')}>
            <input
              id="nombreCompleto"
              maxLength={150}
              value={datos.nombreCompleto}
              onChange={actualizarCampo('nombreCompleto')}
            />
          </CampoFormulario>

          <CampoFormulario id="correo" etiqueta="Correo" error={errorDelCampo('correo')}>
            <input
              id="correo"
              type="email"
              maxLength={255}
              value={datos.correo}
              onChange={actualizarCampo('correo')}
            />
          </CampoFormulario>

          <CampoFormulario id="direccion" etiqueta="Dirección" error={errorDelCampo('direccion')}>
            <input
              id="direccion"
              maxLength={255}
              value={datos.direccion}
              onChange={actualizarCampo('direccion')}
            />
          </CampoFormulario>

          <CampoFormulario
            id="telefono"
            etiqueta="Teléfono"
            ayuda="Formato internacional, por ejemplo +59891234567."
            error={errorDelCampo('telefono')}
          >
            <input id="telefono" value={datos.telefono} onChange={actualizarCampo('telefono')} />
          </CampoFormulario>

          <button type="submit" className={estilos.boton} disabled={mutacion.isPending}>
            {mutacion.isPending ? 'Enviando...' : 'Enviar solicitud'}
          </button>
        </form>

        <p className={estilos.enlaces}>
          <Link to="/login">Volver al inicio de sesión</Link>
        </p>
      </div>
    </div>
  );
}
