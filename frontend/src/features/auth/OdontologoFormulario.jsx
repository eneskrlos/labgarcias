import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import { LayoutFormulario } from '../../shared/components/LayoutFormulario';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { crearOdontologo } from './api';

const VALORES_INICIALES = { nombreCompleto: '', correo: '', nombreUsuario: '', direccion: '', telefono: '' };
const CAMPOS_CONOCIDOS = ['nombreCompleto', 'correo', 'nombreUsuario', 'direccion', 'telefono'];

/**
 * Sin listado de odontólogos todavía (CU-11 es de T-28): el alta directa vuelve al dashboard, que
 * es donde se muestra la confirmación. Apuntaba a `/` hasta que T-27 convirtió esa ruta en un
 * redirect por rol: el `location.state` con el mensaje no sobrevive a un redirect.
 */
const DESTINO_POR_DEFECTO = '/admin';

/**
 * D-18/§3.1.b: el administrador da de alta la cuenta de un odontólogo.
 *
 * **No pide contraseña**: la genera el backend, la manda por correo y el odontólogo la cambia en
 * su primer ingreso. Cuando se llega desde una solicitud de acceso, los datos vienen precargados
 * y el `solicitudId` viaja con el alta: crear la cuenta es lo que aprueba la solicitud.
 */
export default function OdontologoFormulario() {
  const navigate = useNavigate();
  const location = useLocation();

  const solicitud = location.state?.solicitud ?? null;
  const origen = location.state?.origen ?? DESTINO_POR_DEFECTO;

  const [datos, setDatos] = useState(() => ({
    ...VALORES_INICIALES,
    nombreCompleto: solicitud?.nombreCompleto ?? '',
    correo: solicitud?.correo ?? '',
    direccion: solicitud?.direccion ?? '',
    telefono: solicitud?.telefono ?? '',
  }));

  const mutacion = useMutation({
    mutationFn: (payload) => crearOdontologo(payload),
    onSuccess: () => {
      navigate(origen, {
        state: { mensaje: 'Cuenta creada. Las credenciales se enviaron por correo al odontólogo.' },
      });
    },
  });

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate({ ...datos, solicitudId: solicitud?.id ?? null });
  };

  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  return (
    <div className="contenedor">
      <LayoutFormulario
        titulo="Nuevo odontólogo"
        onSubmit={enviar}
        onCancelar={() => navigate(origen)}
        guardando={mutacion.isPending}
        error={errorGeneral}
      >
        {solicitud && <p>Alta a partir de la solicitud de acceso #{solicitud.id}.</p>}

        <CampoFormulario id="nombreCompleto" etiqueta="Nombre completo" error={errorDelCampo('nombreCompleto')}>
          <input
            id="nombreCompleto"
            maxLength={150}
            value={datos.nombreCompleto}
            onChange={actualizarCampo('nombreCompleto')}
          />
        </CampoFormulario>

        <CampoFormulario id="correo" etiqueta="Correo" error={errorDelCampo('correo')}>
          <input id="correo" type="email" maxLength={255} value={datos.correo} onChange={actualizarCampo('correo')} />
        </CampoFormulario>

        <CampoFormulario
          id="nombreUsuario"
          etiqueta="Nombre de usuario"
          ayuda="Con este nombre va a ingresar al sistema. No se puede repetir."
          error={errorDelCampo('nombreUsuario')}
        >
          <input
            id="nombreUsuario"
            maxLength={60}
            value={datos.nombreUsuario}
            onChange={actualizarCampo('nombreUsuario')}
          />
        </CampoFormulario>

        <CampoFormulario id="direccion" etiqueta="Dirección" error={errorDelCampo('direccion')}>
          <input id="direccion" maxLength={255} value={datos.direccion} onChange={actualizarCampo('direccion')} />
        </CampoFormulario>

        <CampoFormulario
          id="telefono"
          etiqueta="Teléfono"
          ayuda="Formato internacional, por ejemplo +59891234567."
          error={errorDelCampo('telefono')}
        >
          <input id="telefono" value={datos.telefono} onChange={actualizarCampo('telefono')} />
        </CampoFormulario>

        <p>
          D-18: la contraseña la genera el sistema y se la envía por correo al odontólogo, que
          deberá cambiarla en su primer ingreso.
        </p>
      </LayoutFormulario>
    </div>
  );
}
