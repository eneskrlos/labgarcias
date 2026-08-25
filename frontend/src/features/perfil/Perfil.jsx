import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { useSesion } from '../../shared/hooks/useSesion';
import { obtenerToken } from '../../shared/api/token';
import { actualizarPerfil, conectarTelegram, desvincularTelegram, obtenerPerfil } from './api';
import estilos from './Perfil.module.css';

const CLAVE_PERFIL = ['perfil'];

/** §7: los dos únicos campos editables. El resto se muestra como dato. */
const CAMPOS_EDITABLES = ['nombreCompleto', 'direccion'];

/**
 * §6.5 paso 4: el chat lo captura el backend cuando el usuario toca Iniciar en el bot, así que
 * la pantalla no se entera sola. Mientras hay un enlace abierto y la cuenta sigue sin vincular,
 * se vuelve a preguntar por el perfil: es lo que hace que el estado cambie "en segundos"
 * (criterio 1) sin que el usuario tenga que refrescar.
 */
const INTERVALO_ESPERA_VINCULACION_MS = 5_000;

function DatoPerfil({ etiqueta, valor }) {
  return (
    <div className={estilos.dato}>
      <span className={estilos.etiqueta}>{etiqueta}</span>
      <span>{valor || '—'}</span>
    </div>
  );
}

export default function Perfil() {
  const queryClient = useQueryClient();
  const { usuario, iniciarSesion } = useSesion();

  const [datos, setDatos] = useState({ nombreCompleto: '', direccion: '' });
  const [confirmacion, setConfirmacion] = useState(null);

  // TanStack Query v5 le pasa un segundo argumento a mutationFn: se envuelve para que no llegue.
  const conexion = useMutation({ mutationFn: () => conectarTelegram() });
  const desvinculacion = useMutation({
    mutationFn: () => desvincularTelegram(),
    onSuccess: () => {
      conexion.reset();
      queryClient.invalidateQueries({ queryKey: CLAVE_PERFIL });
    },
  });

  const enlace = conexion.data?.enlace;

  const consulta = useQuery({
    queryKey: CLAVE_PERFIL,
    queryFn: obtenerPerfil,
    refetchInterval: (actual) =>
      enlace && !actual.state.data?.telegramVinculado ? INTERVALO_ESPERA_VINCULACION_MS : false,
  });
  const perfil = consulta.data;

  useEffect(() => {
    if (perfil) {
      setDatos({ nombreCompleto: perfil.nombreCompleto, direccion: perfil.direccion ?? '' });
    }
  }, [perfil]);

  const edicion = useMutation({
    mutationFn: (payload) => actualizarPerfil(payload),
    onSuccess: (guardado) => {
      queryClient.setQueryData(CLAVE_PERFIL, guardado);
      // El nombre se muestra en el saludo del panel y en el encabezado, que leen la sesión
      // guardada: sin esto, el usuario cambia su nombre y sigue viendo el anterior hasta salir.
      if (usuario) {
        iniciarSesion(obtenerToken(), { ...usuario, nombreCompleto: guardado.nombreCompleto });
      }
      setConfirmacion('Perfil actualizado.');
    },
  });

  const actualizarCampo = (campo) => (evento) => {
    setConfirmacion(null);
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const guardar = (evento) => {
    evento.preventDefault();
    setConfirmacion(null);
    edicion.mutate(datos);
  };

  const errorDelCampo = (campo) =>
    edicion.isError && edicion.error.campo === campo ? edicion.error.mensaje : null;
  const errorGeneral =
    edicion.isError && !CAMPOS_EDITABLES.includes(edicion.error.campo) ? edicion.error.mensaje : null;

  if (consulta.isPending) {
    return <p className={estilos.estado}>Cargando tu perfil...</p>;
  }
  if (consulta.isError) {
    return (
      <div className={estilos.estado}>
        <p>No pudimos traer tu perfil.</p>
        <button type="button" onClick={() => consulta.refetch()}>
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className={estilos.pantalla}>
      <h1>Mi perfil</h1>

      {/*
        §7: **solo nombre y dirección son editables.** Correo, usuario, rol y teléfono se muestran
        como dato y no como campo — el rol decide la autorización de cada endpoint (RN-14) y el
        correo identifica la cuenta en el login. Que no sean editables se ve en la pantalla, no
        depende de que el backend los rechace.
      */}
      <form className={estilos.tarjeta} onSubmit={guardar} noValidate>
        <h2>Mis datos</h2>

        {confirmacion && <p role="status">{confirmacion}</p>}
        {errorGeneral && <p className={estilos.error}>{errorGeneral}</p>}

        <CampoFormulario id="nombreCompleto" etiqueta="Nombre" error={errorDelCampo('nombreCompleto')}>
          <input
            id="nombreCompleto"
            required
            maxLength={150}
            value={datos.nombreCompleto}
            onChange={actualizarCampo('nombreCompleto')}
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

        <DatoPerfil etiqueta="Correo" valor={perfil.correo} />
        <DatoPerfil etiqueta="Usuario" valor={perfil.nombreUsuario} />
        <DatoPerfil etiqueta="Rol" valor={perfil.rol} />
        <DatoPerfil etiqueta="Teléfono" valor={perfil.telefono} />

        <button type="submit" className={estilos.boton} disabled={edicion.isPending}>
          {edicion.isPending ? 'Guardando...' : 'Guardar'}
        </button>
      </form>

      <section className={estilos.tarjeta}>
        <h2>Telegram</h2>

        {perfil.telegramVinculado ? (
          <>
            <p className={estilos.vinculado}>Telegram: vinculado ✅</p>
            <p className={estilos.ayuda}>
              Recibís los avisos del laboratorio por el bot. Si desvinculás, seguís recibiéndolos
              por correo y en la campana.
            </p>
            <button
              type="button"
              className={estilos.boton}
              disabled={desvinculacion.isPending}
              onClick={() => desvinculacion.mutate()}
            >
              {desvinculacion.isPending ? 'Desvinculando...' : 'Desvincular'}
            </button>
          </>
        ) : (
          <>
            <p>Telegram: no vinculado</p>
            <p className={estilos.ayuda}>
              El bot no puede escribirte primero: para recibir los avisos por Telegram tenés que
              abrirlo vos una vez.
            </p>
            <button
              type="button"
              className={estilos.boton}
              disabled={conexion.isPending}
              onClick={() => conexion.mutate()}
            >
              {conexion.isPending ? 'Generando enlace...' : 'Conectar Telegram'}
            </button>

            {enlace && (
              <p className={estilos.enlace}>
                <a href={enlace} target="_blank" rel="noreferrer">
                  Abrir el bot de Telegram
                </a>
                <span className={estilos.ayuda}>
                  Tocá <strong>Iniciar</strong> en la conversación. El enlace vale 15 minutos y
                  una sola vez.
                </span>
              </p>
            )}
          </>
        )}

        {conexion.isError && <p className={estilos.error}>{conexion.error.mensaje}</p>}
        {desvinculacion.isError && <p className={estilos.error}>{desvinculacion.error.mensaje}</p>}
      </section>
    </div>
  );
}
