import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { login, loginGoogle } from './api';
import { useSesion } from '../../shared/hooks/useSesion';
import { mostrarBotonGoogle } from '../../shared/api/googleIdentity';
import estilos from './Auth.module.css';

export default function Login() {
  const navigate = useNavigate();
  const { iniciarSesion } = useSesion();
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const contenedorGoogleRef = useRef(null);

  const alAutenticar = (datos) => {
    iniciarSesion(datos.token, datos.usuario);
    navigate('/');
  };

  const mutacionLogin = useMutation({ mutationFn: login, onSuccess: alAutenticar });
  const mutacionGoogle = useMutation({ mutationFn: loginGoogle, onSuccess: alAutenticar });

  useEffect(() => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!clientId) {
      return;
    }
    mostrarBotonGoogle({
      clientId,
      contenedor: contenedorGoogleRef.current,
      alObtenerCredencial: (credencial) => mutacionGoogle.mutate(credencial),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const enviar = (evento) => {
    evento.preventDefault();
    mutacionLogin.mutate({ correo, password });
  };

  const error = mutacionLogin.error ?? mutacionGoogle.error;

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Iniciar sesión</h1>

        {error && <p className={estilos.error}>{error.mensaje}</p>}

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

        <div className={estilos.separador}>o</div>
        <div ref={contenedorGoogleRef} />

        <p className={estilos.enlaces}>
          ¿No tenés cuenta? <Link to="/registro">Registrate</Link>
        </p>
      </div>
    </div>
  );
}
