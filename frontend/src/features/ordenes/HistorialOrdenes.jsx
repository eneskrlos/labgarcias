import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { listarEstados } from '../catalogos/api';
import { listarMisOrdenes } from './api';
import estilosEncabezado from '../../shared/components/EncabezadoPantalla.module.css';

const CLAVE_CONSULTA = 'historial-ordenes';

/** No es un estado del dominio: es la opción "sin filtro", que se traduce a no mandar el parámetro. */
const SIN_FILTRO = 'TODOS';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-12/§8: el historial del odontólogo, en `/historial`.
 *
 * Muestra sus trabajos ya cerrados. **Qué es "cerrado" lo decide el backend** con `historico=true`,
 * que filtra por `estado.es_terminal` (RN-04): en esta pantalla no hay ninguna lista de estados
 * terminales, igual que en el detalle de T-25. Si mañana el catálogo marca otra etapa como
 * terminal, el historial la incluye sin tocar el frontend.
 *
 * El selector de estado reutiliza el parámetro `estado` que §5.3 ya tenía —no se inventó uno—, y
 * se alimenta del catálogo filtrando por `esTerminal`, que es el mismo dato que usa el backend.
 *
 * **RN-01**: el dueño lo pone el token. **RN-22**: al paciente se lo identifica por iniciales y
 * código. **§8.1 Reglas 2 a 5**: paginación en el backend y en la URL, los tres estados de la
 * tabla y los componentes compartidos. La Regla 1 no aplica: no hay alta ni edición (RN-17).
 */
export default function HistorialOrdenes() {
  const { pagina, tamano, cambiarPagina, cambiarTamano, filtro, cambiarFiltro } = usePaginacion();

  const estado = filtro('estado') || SIN_FILTRO;

  const estados = useQuery({ queryKey: ['estados'], queryFn: listarEstados });
  const estadosTerminales = (estados.data ?? []).filter((opcion) => opcion.esTerminal);

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano, estado],
    queryFn: () => listarMisOrdenes({
      pagina,
      tamano,
      estado: estado === SIN_FILTRO ? null : estado,
      historico: true,
    }),
    placeholderData: keepPreviousData,
  });

  const columnas = [
    {
      clave: 'codigo',
      encabezado: 'Caso',
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
    // Sin símbolo de moneda: P-17 no está resuelto. Se muestra como lo devuelve el backend.
    { clave: 'precioTotal', encabezado: 'Total' },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) => <Link to={`/ordenes/${fila.id}`}>Ver seguimiento</Link>,
    },
  ];

  return (
    <div className="contenedor">
      <EncabezadoPantalla titulo="Historial">
        <label className={estilosEncabezado.filtro}>
          Estado
          <select value={estado} onChange={(evento) => cambiarFiltro('estado', evento.target.value)}>
            <option value={SIN_FILTRO}>Todos</option>
            {estadosTerminales.map((opcion) => (
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
        mensajeVacio="Todavía no tenés trabajos finalizados."
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
