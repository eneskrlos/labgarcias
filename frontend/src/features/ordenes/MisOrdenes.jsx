import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { listarEstados } from '../catalogos/api';
import { listarMisOrdenes } from './api';
import estilosEncabezado from '../../shared/components/EncabezadoPantalla.module.css';

const CLAVE_CONSULTA = 'mis-ordenes';

/** No es un estado del dominio: es la opción "sin filtro", que se traduce a no mandar el parámetro. */
const SIN_FILTRO = 'TODAS';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-03/§5.3: las órdenes del odontólogo autenticado.
 *
 * **RN-01**: el dueño lo pone el token. Esta pantalla no tiene ni puede tener un selector de
 * odontólogo; el listado con filtro por odontólogo es el del laboratorio (CU-06, T-26).
 * **RN-22**: al paciente se lo identifica por iniciales y código, que es lo único que devuelve
 * §5.3. El nombre no está ni siquiera disponible acá.
 *
 * No es un CRUD: no hay "Nuevo" —la orden la registra el laboratorio (D-19)— ni "Editar"
 * (RN-17), así que §8.1 Regla 1 no aplica. Sí rigen la paginación por backend, los tres estados
 * de la tabla y los componentes compartidos.
 */
export default function MisOrdenes() {
  const { pagina, tamano, cambiarPagina, cambiarTamano, filtro, cambiarFiltro } = usePaginacion();

  const estado = filtro('estado') || SIN_FILTRO;

  const estados = useQuery({ queryKey: ['estados'], queryFn: listarEstados });

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano, estado],
    queryFn: () => listarMisOrdenes({ pagina, tamano, estado: estado === SIN_FILTRO ? null : estado }),
    placeholderData: keepPreviousData,
  });

  const columnas = [
    {
      clave: 'codigo',
      encabezado: 'Orden',
      render: (fila) => <Link to={`/ordenes/${fila.id}`}>{fila.codigo}</Link>,
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
    // Lo que muestra el backend, sin recomponer: no hay símbolo de moneda hasta que se confirme (P-17).
    { clave: 'precioTotal', encabezado: 'Total' },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) => <Link to={`/ordenes/${fila.id}`}>Ver seguimiento</Link>,
    },
  ];

  return (
    <div className="contenedor">
      <EncabezadoPantalla titulo="Mis trabajos">
        <label className={estilosEncabezado.filtro}>
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
      </EncabezadoPantalla>

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No tenés trabajos para mostrar."
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
