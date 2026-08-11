import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { registrar } from './api';
import estilos from './Auth.module.css';

const VALORES_INICIALES = {
  nombreCompleto: '',
  correo: '',
  nombreUsuario: '',
  password: '',
  direccion: '',
};

export default function Registro() {
  const [datos, setDatos] = useState(VALORES_INICIALES);

  const mutacion = useMutation({ mutationFn: registrar });

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate(datos);
  };

  if (mutacion.isSuccess) {
    return (
      <div className={estilos.pantalla}>
        <div className={estilos.tarjeta}>
          <h1>Cuenta creada</h1>
          <p className={estilos.exito}>{mutacion.data.mensaje}</p>
          <p className={estilos.enlaces}>
            <Link to="/verificar">Ya tengo un enlace de verificación</Link>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Crear cuenta de odontólogo</h1>

        {mutacion.isError && <p className={estilos.error}>{mutacion.error.mensaje}</p>}

        <form onSubmit={enviar}>
          <div className={estilos.campo}>
            <label htmlFor="nombreCompleto">Nombre completo</label>
            <input
              id="nombreCompleto"
              required
              value={datos.nombreCompleto}
              onChange={actualizarCampo('nombreCompleto')}
            />
          </div>
          <div className={estilos.campo}>
            <label htmlFor="correo">Correo</label>
            <input
              id="correo"
              type="email"
              required
              value={datos.correo}
              onChange={actualizarCampo('correo')}
            />
          </div>
          <div className={estilos.campo}>
            <label htmlFor="nombreUsuario">Nombre de usuario</label>
            <input
              id="nombreUsuario"
              required
              value={datos.nombreUsuario}
              onChange={actualizarCampo('nombreUsuario')}
            />
          </div>
          <div className={estilos.campo}>
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              required
              value={datos.password}
              onChange={actualizarCampo('password')}
            />
          </div>
          {/* RN-15 */}
          <p className={estilos.ayuda}>
            Mínimo 9 caracteres, con al menos una mayúscula, una minúscula, un número y un carácter especial.
          </p>
          <div className={estilos.campo}>
            <label htmlFor="direccion">Dirección</label>
            <input
              id="direccion"
              required
              value={datos.direccion}
              onChange={actualizarCampo('direccion')}
            />
          </div>
          <button type="submit" className={estilos.boton} disabled={mutacion.isPending}>
            {mutacion.isPending ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <p className={estilos.enlaces}>
          ¿Ya tenés cuenta? <Link to="/login">Iniciar sesión</Link>
        </p>
      </div>
    </div>
  );
}
