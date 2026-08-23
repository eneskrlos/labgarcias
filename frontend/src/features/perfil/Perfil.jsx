import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { conectarTelegram, desvincularTelegram, obtenerPerfil } from './api';
import estilos from './Perfil.module.css';

const CLAVE_PERFIL = ['perfil'];

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

      <section className={estilos.tarjeta}>
        <DatoPerfil etiqueta="Nombre" valor={perfil.nombreCompleto} />
        <DatoPerfil etiqueta="Correo" valor={perfil.correo} />
        <DatoPerfil etiqueta="Usuario" valor={perfil.nombreUsuario} />
        <DatoPerfil etiqueta="Rol" valor={perfil.rol} />
        <DatoPerfil etiqueta="Dirección" valor={perfil.direccion} />
        <DatoPerfil etiqueta="Teléfono" valor={perfil.telefono} />
      </section>

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
