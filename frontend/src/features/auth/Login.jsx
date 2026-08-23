import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { login } from './api';
import { useSesion } from '../../shared/hooks/useSesion';
import estilos from './Auth.module.css';

/**
 * CR-01: se retiró el botón de Google (D-17) y el enlace al auto-registro (D-18).
 * Los reemplaza "Solicitar acceso" (D-17, spec.md §3.1), que no crea cuenta: la crea el
 * administrador.
 */
export default function Login() {
  const navigate = useNavigate();
  const { iniciarSesion } = useSesion();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');

  const mutacionLogin = useMutation({
    mutationFn: (credenciales) => login(credenciales),
    onSuccess: (datos) => {
      // §3.1.b: la bandera viaja fuera del objeto usuario, pero la sesión la necesita adentro
      // para que RutaProtegida pueda decidir en cada ruta.
      iniciarSesion(datos.token, { ...datos.usuario, debeCambiarPassword: datos.debeCambiarPassword });
      navigate(datos.debeCambiarPassword ? '/cambiar-password' : '/');
    },
  });

  const enviar = (evento) => {
    evento.preventDefault();
    mutacionLogin.mutate({ correo, password });
  };

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Iniciar sesión</h1>

        {mutacionLogin.isError && <p className={estilos.error}>{mutacionLogin.error.mensaje}</p>}

        <form onSubmit={enviar}>
          <div className={estilos.campo}>
            <label htmlFor="correo">Correo</label>
            <input
              id="correo"
              type="email"
              required
              value={correo}
              onChange={(evento) => setCorreo(evento.target.value)}
            />
          </div>
          <div className={estilos.campo}>
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(evento) => setPassword(evento.target.value)}
            />
          </div>
          <button type="submit" className={estilos.boton} disabled={mutacionLogin.isPending}>
            {mutacionLogin.isPending ? 'Ingresando...' : 'Ingresar'}
          </button>
        </form>

        <p className={estilos.enlaces}>
          ¿No tenés cuenta? <Link to="/solicitar-acceso">Solicitar acceso</Link>
        </p>
      </div>
    </div>
  );
}
