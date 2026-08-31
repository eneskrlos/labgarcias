import { useEffect, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { listarSolicitudes, rechazarSolicitud } from './api';
import estilos from './SolicitudesListado.module.css';
import estilosEncabezado from '../../shared/components/EncabezadoPantalla.module.css';

const CLAVE_CONSULTA = 'solicitudes-acceso';

/**
 * `TODAS` no es un estado del dominio: es la opción "sin filtro" del selector. Se traduce a no
 * mandar el parámetro, porque la API solo acepta los tres estados de D-17.
 */
const SIN_FILTRO = 'TODAS';
const ESTADO_POR_DEFECTO = 'PENDIENTE';

const OPCIONES_ESTADO = [
  { valor: 'PENDIENTE', etiqueta: 'Pendientes' },
  { valor: 'APROBADA', etiqueta: 'Aprobadas' },
  { valor: 'RECHAZADA', etiqueta: 'Rechazadas' },
  { valor: SIN_FILTRO, etiqueta: 'Todas' },
];

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

/**
 * D-17/§3.1.b: las solicitudes que le llegan al laboratorio.
 *
 * No es un CRUD: las solicitudes nacen del formulario público, así que no hay "Nuevo" ni
 * "Editar" (§8.1 Regla 1 no aplica). Sí rigen la paginación por backend, los tres estados de
 * la tabla y los componentes compartidos.
 *
 * Aprobar no está acá: aprobar una solicitud es crear la cuenta del odontólogo (§3.1.b).
 */
export default function SolicitudesListado() {
  const { pagina, tamano, cambiarPagina, cambiarTamano, filtro, cambiarFiltro } = usePaginacion();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();

  // Foto del mensaje con el que vuelve el alta: el efecto limpia location.state enseguida.
  const [mensajeConfirmacion] = useState(location.state?.mensaje ?? null);

  useEffect(() => {
    if (location.state?.mensaje) {
      navigate(location.pathname + location.search, { replace: true, state: {} });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const estado = filtro('estado') || ESTADO_POR_DEFECTO;

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano, estado],
    queryFn: () => listarSolicitudes({ pagina, tamano, estado: estado === SIN_FILTRO ? null : estado }),
    placeholderData: keepPreviousData,
  });

  const mutacionRechazar = useMutation({
    mutationFn: (id) => rechazarSolicitud(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [CLAVE_CONSULTA] }),
  });

  const crearCuentaDesde = (solicitud) => {
    navigate('/admin/odontologos/nuevo', {
      state: {
        solicitud: {
          id: solicitud.id,
          nombreCompleto: solicitud.nombreCompleto,
          correo: solicitud.correo,
          direccion: solicitud.direccion,
          telefono: solicitud.telefono,
        },
        origen: '/admin/solicitudes',
      },
    });
  };

  const columnas = [
    { clave: 'nombreCompleto', encabezado: 'Nombre' },
    { clave: 'correo', encabezado: 'Correo' },
    { clave: 'telefono', encabezado: 'Teléfono' },
    { clave: 'direccion', encabezado: 'Dirección' },
    { clave: 'estado', encabezado: 'Estado' },
    {
      clave: 'fechaCreacion',
      encabezado: 'Recibida',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaCreacion)),
    },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) =>
        fila.estado === 'PENDIENTE' ? (
          <>
            {/* §3.1.b: aprobar es crear la cuenta. El alta lleva el solicitudId y la deja APROBADA. */}
            <button type="button" onClick={() => crearCuentaDesde(fila)}>
              Crear cuenta
            </button>
            <button
              type="button"
              onClick={() => mutacionRechazar.mutate(fila.id)}
              disabled={mutacionRechazar.isPending}
            >
              Rechazar
            </button>
          </>
        ) : null,
    },
  ];

  return (
    <div className="contenedor">
      <EncabezadoPantalla titulo="Solicitudes de acceso" confirmacion={mensajeConfirmacion}>
        <label className={estilosEncabezado.filtro}>
          Estado
          <select value={estado} onChange={(evento) => cambiarFiltro('estado', evento.target.value)}>
            {OPCIONES_ESTADO.map((opcion) => (
              <option key={opcion.valor} value={opcion.valor}>
                {opcion.etiqueta}
              </option>
            ))}
          </select>
        </label>
      </EncabezadoPantalla>

      {mutacionRechazar.isError && <p className={estilos.error}>{mutacionRechazar.error.mensaje}</p>}

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay solicitudes para mostrar."
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
