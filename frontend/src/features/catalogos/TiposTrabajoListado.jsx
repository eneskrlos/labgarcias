import { useEffect, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { cambiarEstado, listarPaginado } from './api';
import estilos from './TiposTrabajoListado.module.css';

const CLAVE_CONSULTA = 'tipos-trabajo';

/** spec.md §8.1 Regla 1: listado paginado, sin formulario. Alta y edición viven en sus propias rutas. */
export default function TiposTrabajoListado() {
  const { pagina, tamano, cambiarPagina, cambiarTamano } = usePaginacion();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();

  // Solo se lee el valor inicial: el efecto de abajo limpia location.state, así que hay que
  // quedarse con una foto del mensaje. Nunca se actualiza, por eso no se toma el setter.
  const [mensajeConfirmacion] = useState(location.state?.mensaje ?? null);

  useEffect(() => {
    if (location.state?.mensaje) {
      // Limpia el state de navegación para que el mensaje no reaparezca al volver con el botón atrás.
      navigate(location.pathname + location.search, { replace: true, state: {} });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano],
    queryFn: () => listarPaginado({ pagina, tamano }),
    placeholderData: keepPreviousData,
  });

  const mutacionCambiarEstado = useMutation({
    mutationFn: ({ id, activo }) => cambiarEstado(id, activo),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [CLAVE_CONSULTA] }),
  });

  const alternarActivo = (fila) => mutacionCambiarEstado.mutate({ id: fila.id, activo: !fila.activo });

  const columnas = [
    { clave: 'nombre', encabezado: 'Nombre' },
    { clave: 'diasEstimados', encabezado: 'Días' },
    { clave: 'precio', encabezado: 'Precio' },
    { clave: 'estado', encabezado: 'Estado', render: (fila) => (fila.activo ? 'Activo' : 'Inactivo') },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) => (
        <>
          <Link to={`/admin/tipos-trabajo/${fila.id}/editar`}>Editar</Link>
          <button type="button" onClick={() => alternarActivo(fila)}>
            {fila.activo ? 'Desactivar' : 'Activar'}
          </button>
        </>
      ),
    },
  ];

  return (
    <div className="contenedor">
      <div className={estilos.encabezado}>
        <h1>Tipos de trabajo</h1>
        <Link to="/admin/tipos-trabajo/nuevo" className={estilos.botonNuevo}>
          Nuevo
        </Link>
      </div>

      {mensajeConfirmacion && <p className={estilos.confirmacion}>{mensajeConfirmacion}</p>}

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay tipos de trabajo cargados."
        accionVacio={<Link to="/admin/tipos-trabajo/nuevo">Crear el primero</Link>}
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
