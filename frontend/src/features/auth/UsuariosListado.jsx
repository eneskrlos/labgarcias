import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { useSesion } from '../../shared/hooks/useSesion';
import { cambiarEstadoUsuario, listarUsuarios } from './api';
import estilos from './Padron.module.css';

const CLAVE_CONSULTA = 'usuarios';

const ESTADO_ACTIVA = 'ACTIVA';
const ESTADO_INACTIVA = 'INACTIVA';

/**
 * CU-17/§7: el padrón completo de cuentas, para el SUPERADMIN.
 *
 * **Solo lista y cambia el estado**: §7 define para este recurso `GET /usuarios` y
 * `PATCH /usuarios/{id}/estado`, y nada más. **Sin alta ni edición** —no hay endpoints—, así que
 * de §8.1 rigen las Reglas 2 a 5 y de la Regla 1 no hay rutas `/nuevo` ni `/{id}/editar` que crear.
 * El alta de odontólogos vive en `/admin/odontologos/nuevo`; las de administración no se crean
 * desde la aplicación.
 *
 * **Es la única pantalla que puede reactivar una cuenta dada de baja.** Por eso existe: sin ella,
 * `/admin/odontologos` mostraría cuentas inactivas y no habría forma de devolverlas al servicio
 * sin salir de la aplicación.
 *
 * **La propia cuenta no se puede tocar**: el botón está deshabilitado y el backend lo rechaza con
 * `422 AUTODESACTIVACION_NO_PERMITIDA`. El SuperAdmin es quien reactiva a los demás.
 */
export default function UsuariosListado() {
  const { pagina, tamano, cambiarPagina, cambiarTamano } = usePaginacion();
  const { usuario } = useSesion();
  const queryClient = useQueryClient();

  const [confirmacion, setConfirmacion] = useState(null);

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano],
    queryFn: () => listarUsuarios({ pagina, tamano }),
    placeholderData: keepPreviousData,
  });

  const cambio = useMutation({
    mutationFn: ({ id, estadoCuenta }) => cambiarEstadoUsuario(id, estadoCuenta),
    onSuccess: (guardado) => {
      queryClient.invalidateQueries({ queryKey: [CLAVE_CONSULTA] });
      // El selector de nueva orden y el listado de odontólogos dependen del estado de la cuenta.
      queryClient.invalidateQueries({ queryKey: ['odontologos'] });
      queryClient.invalidateQueries({ queryKey: ['odontologos', 'activos'] });
      setConfirmacion(
        guardado.estadoCuenta === ESTADO_ACTIVA
          ? `Cuenta de ${guardado.nombreCompleto} activada.`
          : `Cuenta de ${guardado.nombreCompleto} desactivada.`,
      );
    },
  });

  const columnas = [
    { clave: 'nombreCompleto', encabezado: 'Nombre' },
    { clave: 'correo', encabezado: 'Correo' },
    { clave: 'nombreUsuario', encabezado: 'Usuario' },
    { clave: 'rol', encabezado: 'Rol' },
    { clave: 'estadoCuenta', encabezado: 'Estado' },
    {
      clave: 'acciones',
      encabezado: 'Acciones',
      render: (fila) => {
        if (fila.id === usuario?.id) {
          return <span className={estilos.propia}>Tu cuenta</span>;
        }
        const activa = fila.estadoCuenta === ESTADO_ACTIVA;
        return (
          <button
            type="button"
            className={estilos.accion}
            disabled={cambio.isPending}
            onClick={() => {
              setConfirmacion(null);
              cambio.mutate({ id: fila.id, estadoCuenta: activa ? ESTADO_INACTIVA : ESTADO_ACTIVA });
            }}
          >
            {activa ? 'Desactivar' : 'Activar'}
          </button>
        );
      },
    },
  ];

  return (
    <div className="contenedor">
      {cambio.isError && <p className={estilos.error}>{cambio.error.mensaje}</p>}

      <EncabezadoPantalla titulo="Usuarios" confirmacion={confirmacion} />

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="No hay cuentas para mostrar."
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
