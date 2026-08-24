import { useQuery } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { FilaContadores, TarjetaContador } from '../../shared/components/TarjetaContador';
import { obtenerPanelOdontologo } from './api';
import estilos from './Panel.module.css';

const CLAVE_CONSULTA = 'panel-odontologo';

/** Filas que devuelve cada bloque de resumen del backend. Acá solo dimensiona el esqueleto. */
const FILAS_POR_BLOQUE = 5;

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-02/§8: el panel de inicio del odontólogo, en `/inicio`.
 *
 * Muestra el saludo personalizado, los indicadores y la tabla de trabajos recientes que enumera
 * CU-02. **Sin el contador de "mensajes nuevos"** que también enumera: D-11 pospuso la mensajería
 * entera, así que no hay dato ni pantalla que mostrar.
 *
 * **RN-01**: todo lo que se ve es del odontólogo autenticado, y quien lo garantiza es el backend
 * —el endpoint no acepta un id—. **RN-22**: los trabajos recientes identifican al paciente por
 * iniciales y código; el nombre no llega a esta pantalla.
 *
 * **Ningún contador se calcula acá** (§8): los cuatro números salen tal cual del backend. El
 * saludo es el único dato local, y sale de la sesión.
 */
export default function PanelOdontologo() {
  const { usuario } = useSesion();
  const location = useLocation();

  const consulta = useQuery({ queryKey: [CLAVE_CONSULTA], queryFn: obtenerPanelOdontologo });

  const contadores = consulta.data?.contadores;
  const cargando = consulta.isLoading;

  const columnas = [
    {
      clave: 'codigo',
      encabezado: 'Caso',
      render: (fila) => <Link to={`/ordenes/${fila.id}`}>{fila.codigo}</Link>,
    },
    { clave: 'pacienteIdentificacion', encabezado: 'Paciente' },
    { clave: 'tipoTrabajo', encabezado: 'Trabajo' },
    { clave: 'estado', encabezado: 'Estado' },
    {
      clave: 'fechaEstimadaEntrega',
      encabezado: 'Entrega estimada',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaEstimadaEntrega)),
    },
  ];

  return (
    <div className="contenedor">
      {/* §8.1 Regla 1: los formularios que vuelven acá traen su confirmación en location.state. */}
      {location.state?.mensaje && <p role="status">{location.state.mensaje}</p>}

      <h1>Hola, {usuario.nombreCompleto}</h1>

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
      </FilaContadores>

      <div className={estilos.encabezadoBloque}>
        <h2>Trabajos recientes</h2>
        <Link to="/ordenes">Ver todos</Link>
      </div>

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.ordenesRecientes ?? []}
        cargando={cargando}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="Todavía no tenés trabajos registrados."
        tamano={FILAS_POR_BLOQUE}
      />
    </div>
  );
}
