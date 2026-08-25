import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { listarLicencias, obtenerLicenciaVigente } from './api';
import estilos from './Licencias.module.css';

const CLAVE_LISTADO = 'licencias';
const CLAVE_VIGENTE = 'licencia-vigente';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

/**
 * CU-23/§3.6: el histórico de períodos de licencia de esta instalación, para el SUPERADMIN.
 *
 * **Es un CRUD y aplica §8.1 completa**: listado y formulario en rutas separadas (Regla 1),
 * paginación resuelta en el backend y guardada en la URL (Regla 2), los tres estados de la tabla
 * (Regla 3), los componentes compartidos (Regla 4) y la disposición y los textos de siempre
 * (Regla 5). **Sin acción "Editar"**: un período registrado no se modifica —no hay `PUT`—, se
 * registra uno nuevo, así que la ruta `/{id}/editar` de la Regla 1 no existe.
 *
 * **Encabezado con el estado vigente**: es lo que el SuperAdmin necesita ver primero cuando llega
 * acá desde `/bloqueado`, y sale de `GET /licencias/vigente` — no se deduce del listado (§8).
 *
 * **P-11/P-12**: sin planes, sin precios y sin pasarela. Solo activación manual.
 */
export default function LicenciasListado() {
  const { pagina, tamano, cambiarPagina, cambiarTamano } = usePaginacion();
  const location = useLocation();
  const navigate = useNavigate();

  // §8.1 Regla 1: el alta vuelve acá con su confirmación. Foto del mensaje, porque el efecto
  // limpia location.state enseguida: recargar no debe repetir un aviso de algo ya hecho.
  const [mensajeConfirmacion] = useState(location.state?.mensaje ?? null);

  useEffect(() => {
    if (location.state?.mensaje) {
      navigate(location.pathname + location.search, { replace: true, state: {} });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const vigente = useQuery({ queryKey: [CLAVE_VIGENTE], queryFn: obtenerLicenciaVigente });

  const consulta = useQuery({
    queryKey: [CLAVE_LISTADO, pagina, tamano],
    queryFn: () => listarLicencias({ pagina, tamano }),
    placeholderData: keepPreviousData,
  });

  const columnas = [
    {
      clave: 'fechaInicio',
      encabezado: 'Inicio',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaInicio)),
    },
    {
      clave: 'fechaVencimiento',
      encabezado: 'Vencimiento',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaVencimiento)),
    },
    { clave: 'estado', encabezado: 'Estado' },
    { clave: 'activadaPorNombre', encabezado: 'Activada por' },
    {
      clave: 'fechaRegistro',
      encabezado: 'Registrada',
      render: (fila) => FORMATO_FECHA.format(new Date(fila.fechaRegistro)),
    },
    { clave: 'observacion', encabezado: 'Observación' },
  ];

  return (
    <div className="contenedor">
      {mensajeConfirmacion && <p role="status">{mensajeConfirmacion}</p>}

      <div className={estilos.encabezado}>
        <h1>Licencias</h1>
        <Link to="/admin/licencias/nueva" className={estilos.boton}>
          Nuevo
        </Link>
      </div>

      {/* El estado vigente lo dice el backend, no se deriva del listado. */}
      {vigente.data && (
        <p className={vigente.data.vigente ? estilos.vigente : estilos.vencida}>
          {vigente.data.vigente
            ? `Licencia vigente hasta el ${FORMATO_FECHA.format(new Date(vigente.data.licencia.fechaVencimiento))}.`
            : 'No hay ninguna licencia vigente: el sistema está bloqueado para todos los demás roles.'}
        </p>
      )}

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="Todavía no hay períodos de licencia registrados."
        accionVacio={<Link to="/admin/licencias/nueva">Nuevo</Link>}
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
