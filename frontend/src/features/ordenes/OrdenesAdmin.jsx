import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { listarEstados, listarTiposOrden } from '../catalogos/api';
import { listarOdontologosActivos } from '../auth/api';
import { listarOrdenesAdmin } from './api';
import estilos from './OrdenesAdmin.module.css';
import estilosEncabezado from '../../shared/components/EncabezadoPantalla.module.css';

const CLAVE_CONSULTA = 'admin-ordenes';

/** No es un valor del dominio: es la opción "sin filtro", que se traduce a no mandar el parámetro. */
const SIN_FILTRO = '';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-06/§5.7: las órdenes de todo el laboratorio.
 *
 * Es la contracara de `MisOrdenes`: acá el odontólogo **sí** es un filtro, porque quien mira es
 * la administración y ve todas por rol. **RN-22 igual**: el listado identifica al paciente por
 * iniciales y código; el nombre solo aparece en el detalle, que es donde se opera (§5.4).
 *
 * No es un CRUD: el alta vive en `/admin/ordenes/nueva` (D-19) y no hay edición (RN-17), así que
 * §8.1 Regla 1 no aplica. Sí rigen la paginación por backend, los tres estados de la tabla y los
 * componentes compartidos.
 */
export default function OrdenesAdmin() {
  const { pagina, tamano, cambiarPagina, cambiarTamano, filtro, cambiarFiltro } = usePaginacion();
  const navigate = useNavigate();
  const location = useLocation();

  // §8.1 Regla 1: el alta vuelve acá con su confirmación. Foto del mensaje, porque el efecto
  // limpia location.state enseguida: recargar no debe repetir un aviso de algo ya hecho.
  const [mensajeConfirmacion] = useState(location.state?.mensaje ?? null);

  useEffect(() => {
    if (location.state?.mensaje) {
      navigate(location.pathname + location.search, { replace: true, state: {} });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const estado = filtro('estado');
  const tipoOrden = filtro('tipoOrden');
  const odontologoId = filtro('odontologoId');

  const estados = useQuery({ queryKey: ['estados'], queryFn: listarEstados });
  const tiposOrden = useQuery({ queryKey: ['tipos-orden'], queryFn: listarTiposOrden });
  const odontologos = useQuery({ queryKey: ['odontologos', 'activos'], queryFn: listarOdontologosActivos });

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano, estado, tipoOrden, odontologoId],
    queryFn: () => listarOrdenesAdmin({
      pagina,
      tamano,
      estado: estado || null,
      tipoOrden: tipoOrden || null,
      odontologoId: odontologoId || null,
    }),
    placeholderData: keepPreviousData,
  });

  const columnas = [
    {
      clave: 'codigo',
      encabezado: 'Orden',
      render: (fila) => <Link to={`/admin/ordenes/${fila.id}`}>{fila.codigo}</Link>,
    },
    { clave: 'pacienteIdentificacion', encabezado: 'Paciente' },
    { clave: 'tipoTrabajo', encabezado: 'Trabajo' },
    { clave: 'tipoOrden', encabezado: 'Tipo' },
    {
      clave: 'estado',
      encabezado: 'Estado',
      render: (fila) => <EtiquetaEstado estado={fila.estado} estadoCodigo={fila.estadoCodigo} />,
    },
    {
      clave: 'fechaIngreso',
      encabezado: 'Ingreso',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaIngreso)),
    },
    {
      clave: 'fechaEstimadaEntrega',
      encabezado: 'Entrega estimada',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaEstimadaEntrega)),
    },
    { clave: 'precioTotal', encabezado: 'Total' },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) => <Link to={`/admin/ordenes/${fila.id}`}>Gestionar</Link>,
    },
  ];

  return (
    <div className="contenedor">
      <EncabezadoPantalla titulo="Trabajos" confirmacion={mensajeConfirmacion}>
        <Link to="/admin/ordenes/nueva" className={estilosEncabezado.botonAccion}>
          Nueva orden
        </Link>
      </EncabezadoPantalla>

      <div className={estilos.filtros}>
        <label>
          Estado
          <select value={estado} onChange={(evento) => cambiarFiltro('estado', evento.target.value)}>
            <option value={SIN_FILTRO}>Todos</option>
            {(estados.data ?? []).map((opcion) => (
              <option key={opcion.codigo} value={opcion.codigo}>
                {opcion.nombre}
              </option>
            ))}
          </select>
        </label>

        <label>
          Tipo
          <select value={tipoOrden} onChange={(evento) => cambiarFiltro('tipoOrden', evento.target.value)}>
            <option value={SIN_FILTRO}>Todos</option>
            {(tiposOrden.data ?? []).map((opcion) => (
              <option key={opcion.codigo} value={opcion.codigo}>
                {opcion.nombre}
              </option>
            ))}
          </select>
        </label>

        <label>
          Odontólogo
          <select
            value={odontologoId}
            onChange={(evento) => cambiarFiltro('odontologoId', evento.target.value)}
          >
            <option value={SIN_FILTRO}>Todos</option>
            {(odontologos.data ?? []).map((opcion) => (
              <option key={opcion.id} value={opcion.id}>
                {opcion.nombreCompleto}
              </option>
            ))}
          </select>
        </label>
      </div>

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay trabajos para mostrar."
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
