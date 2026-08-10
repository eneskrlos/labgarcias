import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { reenviarVerificacion, verificar } from './api';
import estilos from './Auth.module.css';

export default function Verificar() {
  const [parametros] = useSearchParams();
  const token = parametros.get('token');
  const [correoReenvio, setCorreoReenvio] = useState('');

  const consulta = useQuery({
    queryKey: ['verificar-cuenta', token],
    queryFn: () => verificar(token),
    enabled: Boolean(token),
    retry: false,
  });

  const mutacionReenvio = useMutation({ mutationFn: reenviarVerificacion });

  const enviarReenvio = (evento) => {
    evento.preventDefault();
    mutacionReenvio.mutate(correoReenvio);
  };

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Verificación de cuenta</h1>

        {!token && (
          <p className={estilos.error}>
            Falta el enlace de verificación. Revisá el link que recibiste por correo.
          </p>
        )}
        {token && consulta.isPending && <p>Verificando...</p>}
        {token && consulta.isSuccess && <p className={estilos.exito}>{consulta.data.mensaje}</p>}
        {token && consulta.isError && <p className={estilos.error}>{consulta.error.mensaje}</p>}

        {consulta.isSuccess ? (
          <p className={estilos.enlaces}>
            <Link to="/login">Iniciar sesión</Link>
          </p>
        ) : (
          <>
            <div className={estilos.separador}>¿El enlace venció o ya no funciona?</div>
            {mutacionReenvio.isSuccess ? (
              <p className={estilos.exito}>{mutacionReenvio.data.mensaje}</p>
            ) : (
              <form onSubmit={enviarReenvio}>
                <div className={estilos.campo}>
                  <label htmlFor="correoReenvio">Correo</label>
                  <input
                    id="correoReenvio"
                    type="email"
                    required
                    value={correoReenvio}
                    onChange={(evento) => setCorreoReenvio(evento.target.value)}
                  />
                </div>
                <button type="submit" className={estilos.boton} disabled={mutacionReenvio.isPending}>
                  {mutacionReenvio.isPending ? 'Enviando...' : 'Reenviar enlace'}
                </button>
              </form>
            )}
          </>
        )}
      </div>
    </div>
  );
}
