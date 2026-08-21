import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { useSesion } from '../../shared/hooks/useSesion';
import { cambiarPassword } from './api';
import estilos from './Auth.module.css';

const CAMPOS_CONOCIDOS = ['passwordActual', 'passwordNueva'];

/**
 * §3.1.b: cambio obligatorio del primer ingreso.
 *
 * No tiene "Cancelar" a propósito: hasta cambiarla, el token no habilita ninguna otra pantalla,
 * así que salir de acá sin cambiarla no lleva a ningún lado. La única salida es cerrar sesión.
 */
export default function CambiarPassword() {
  const navigate = useNavigate();
  const { usuario, iniciarSesion, cerrarSesion } = useSesion();
  const [passwordActual, setPasswordActual] = useState('');
  const [passwordNueva, setPasswordNueva] = useState('');

  const mutacion = useMutation({
    mutationFn: (datos) => cambiarPassword(datos),
    onSuccess: (datos) => {
      // El backend devuelve un token nuevo, ya sin la restricción: se reemplaza la sesión entera.
      iniciarSesion(datos.token, { ...datos.usuario, debeCambiarPassword: false });
      navigate('/', { replace: true });
    },
  });

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate({ passwordActual, passwordNueva });
  };

  const salir = () => {
    cerrarSesion();
    navigate('/login', { replace: true });
  };

  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Cambiá tu contraseña</h1>
        <p className={estilos.ayuda}>
          {usuario?.nombreCompleto
            ? `Hola, ${usuario.nombreCompleto}. `
            : ''}
          Antes de empezar a usar el sistema tenés que reemplazar la contraseña temporal que te
          llegó por correo.
        </p>

        {errorGeneral && <p className={estilos.error}>{errorGeneral}</p>}

        <form onSubmit={enviar} noValidate>
          <CampoFormulario
            id="passwordActual"
            etiqueta="Contraseña temporal"
            error={errorDelCampo('passwordActual')}
          >
            <input
              id="passwordActual"
              type="password"
              autoComplete="current-password"
              value={passwordActual}
              onChange={(evento) => setPasswordActual(evento.target.value)}
            />
          </CampoFormulario>

          <CampoFormulario
            id="passwordNueva"
            etiqueta="Contraseña nueva"
            ayuda="RN-15: mínimo 9 caracteres, con mayúsculas, minúsculas, números y caracteres especiales."
            error={errorDelCampo('passwordNueva')}
          >
            <input
              id="passwordNueva"
              type="password"
              autoComplete="new-password"
              value={passwordNueva}
              onChange={(evento) => setPasswordNueva(evento.target.value)}
            />
          </CampoFormulario>

          <button type="submit" className={estilos.boton} disabled={mutacion.isPending}>
            {mutacion.isPending ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
        </form>

        <p className={estilos.enlaces}>
          <button type="button" onClick={salir}>
            Cerrar sesión
          </button>
        </p>
      </div>
    </div>
  );
}
