import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { FilaContadores, TarjetaContador } from '../../shared/components/TarjetaContador';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { DonaDistribucion } from './DonaDistribucion';
import { obtenerDashboardAdmin } from './api';
import estilos from './Panel.module.css';

const CLAVE_CONSULTA = 'dashboard-admin';

/** Filas que devuelve cada bloque de resumen del backend. Acá solo dimensiona el esqueleto. */
const FILAS_POR_BLOQUE = 5;

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-10/§5.7: el dashboard del laboratorio, en `/admin`.
 *
 * Los cuatro indicadores, la distribución por estado, las próximas a entregar, las recientes y
 * las urgentes, **todo tal como lo devuelve `GET /admin/dashboard`**: ni un número se arma acá
 * (§8, `Agente.md` §6.1). La distribución se dibuja como dona (`DonaDistribucion`, bloque 4 de la
 * etapa 2); el total del centro y el largo de cada arco son presentación sobre cifras ya contadas
 * por el backend, no cálculo de negocio.
 *
 * **RN-22**: ningún bloque muestra el nombre del paciente, ni siquiera el de urgentes, cuya vista
 * sí lo tiene. El laboratorio lo ve en el detalle de la orden (§5.4).
 *
 * **Sin reportes ni estadísticas** más allá de estos contadores: CU-13 es Fase 4.
 */
export default function DashboardAdmin() {
  const consulta = useQuery({ queryKey: [CLAVE_CONSULTA], queryFn: obtenerDashboardAdmin });

  const contadores = consulta.data?.contadores;
  const cargando = consulta.isLoading;
  const error = consulta.isError ? consulta.error.mensaje : null;
  const distribucion = consulta.data?.distribucionPorEstado ?? [];

  const columnasOrden = [
    {
      clave: 'codigo',
      encabezado: 'Orden',
      render: (fila) => <Link to={`/admin/ordenes/${fila.id}`}>{fila.codigo}</Link>,
    },
    { clave: 'pacienteIdentificacion', encabezado: 'Paciente' },
    { clave: 'tipoTrabajo', encabezado: 'Trabajo' },
    {
      clave: 'estado',
      encabezado: 'Estado',
      render: (fila) => <EtiquetaEstado estado={fila.estado} estadoCodigo={fila.estadoCodigo} />,
    },
    {
      clave: 'fechaEstimadaEntrega',
      encabezado: 'Entrega estimada',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaEstimadaEntrega)),
    },
  ];

  const columnasUrgentes = [
    {
      clave: 'codigo',
      encabezado: 'Orden',
      render: (fila) => <Link to={`/admin/ordenes/${fila.id}`}>{fila.codigo}</Link>,
    },
    { clave: 'odontologo', encabezado: 'Odontólogo' },
    {
      clave: 'estado',
      encabezado: 'Estado',
      render: (fila) => <EtiquetaEstado estado={fila.estado} estadoCodigo={fila.estadoCodigo} />,
    },
    {
      clave: 'fechaEstimadaEntrega',
      encabezado: 'Entrega estimada',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaEstimadaEntrega)),
    },
  ];

  return (
    <div className="contenedor">
      <h1>Dashboard</h1>

      <FilaContadores>
        <TarjetaContador etiqueta="Trabajos en curso" valor={contadores?.enCurso} cargando={cargando} />
        <TarjetaContador
          etiqueta="Listos para retirar"
          valor={contadores?.listasParaRetirar}
          cargando={cargando}
        />
        <TarjetaContador
          etiqueta="Entregados esta semana"
          valor={contadores?.entregadasEstaSemana}
          cargando={cargando}
        />
        <TarjetaContador
          etiqueta="Urgentes activos"
          valor={contadores?.urgentesActivas}
          cargando={cargando}
        />
      </FilaContadores>

      <section>
        <h2>Distribución por estado</h2>
        {error && <p className={estilos.error}>{error}</p>}
        {!error && <DonaDistribucion distribucion={distribucion} cargando={cargando} />}
      </section>

      <div className={estilos.encabezadoBloque}>
        <h2>Próximos a entregar</h2>
        <Link to="/admin/ordenes">Ver todos</Link>
      </div>
      <TablaPaginada
        columnas={columnasOrden}
        filas={consulta.data?.proximasAEntregar ?? []}
        cargando={cargando}
        error={error}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay trabajos por entregar."
        tamano={FILAS_POR_BLOQUE}
      />

      <h2>Urgentes</h2>
      <TablaPaginada
        columnas={columnasUrgentes}
        filas={consulta.data?.urgentes ?? []}
        cargando={cargando}
        error={error}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay trabajos urgentes sin terminar."
        tamano={FILAS_POR_BLOQUE}
      />

      <h2>Trabajos recientes</h2>
      <TablaPaginada
        columnas={columnasOrden}
        filas={consulta.data?.ordenesRecientes ?? []}
        cargando={cargando}
        error={error}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="Todavía no hay trabajos registrados."
        tamano={FILAS_POR_BLOQUE}
      />
    </div>
  );
}
