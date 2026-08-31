import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { usePaginacion } from '../../shared/hooks/usePaginacion';
import { TablaPaginada } from '../../shared/components/TablaPaginada';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { listarOdontologos } from './api';
import estilosEncabezado from '../../shared/components/EncabezadoPantalla.module.css';

const CLAVE_CONSULTA = 'odontologos';

/**
 * CU-11/§7: la tabla administrable de cuentas de odontólogo, para ADMIN y SUPERADMIN.
 *
 * **Es un CRUD y aplica §8.1 completa**: listado y formulario en rutas separadas —el alta ya vive
 * en `/admin/odontologos/nuevo` desde T-31—, paginación resuelta en el backend y guardada en la
 * URL, los tres estados de la tabla, los componentes compartidos y la disposición de siempre.
 *
 * **Sin acción "Editar"**: §7 no define ningún `PUT /odontologos/{id}`. Los datos de la cuenta los
 * cambia su dueño desde `/perfil`, y el estado lo maneja el SUPERADMIN en `/admin/usuarios`
 * (CU-17). Inventar acá una edición sería inventar un endpoint.
 *
 * **Muestra también las cuentas dadas de baja**, marcadas: son las que hay que poder ver para
 * saber que existen. Reactivarlas es de `/admin/usuarios`, y esta pantalla lo dice.
 */
export default function OdontologosListado() {
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

  const consulta = useQuery({
    queryKey: [CLAVE_CONSULTA, pagina, tamano],
    queryFn: () => listarOdontologos({ pagina, tamano }),
    placeholderData: keepPreviousData,
  });

  const columnas = [
    { clave: 'nombreCompleto', encabezado: 'Nombre' },
    { clave: 'correo', encabezado: 'Correo' },
    { clave: 'nombreUsuario', encabezado: 'Usuario' },
    { clave: 'telefono', encabezado: 'Teléfono' },
    { clave: 'direccion', encabezado: 'Dirección' },
    { clave: 'estadoCuenta', encabezado: 'Estado' },
  ];

  return (
    <div className="contenedor">
      <EncabezadoPantalla titulo="Odontólogos" confirmacion={mensajeConfirmacion}>
        <Link to="/admin/odontologos/nuevo" className={estilosEncabezado.botonAccion}>
          Nuevo
        </Link>
      </EncabezadoPantalla>

      <TablaPaginada
        columnas={columnas}
        filas={consulta.data?.contenido ?? []}
        cargando={consulta.isLoading}
        actualizando={consulta.isFetching && !consulta.isLoading}
        error={consulta.isError ? consulta.error.mensaje : null}
        onReintentar={() => consulta.refetch()}
        mensajeVacio="Todavía no hay cuentas de odontólogo."
        accionVacio={<Link to="/admin/odontologos/nuevo">Nuevo</Link>}
        pagina={pagina}
        tamano={tamano}
        totalPaginas={consulta.data?.totalPaginas ?? 0}
        onCambiarPagina={cambiarPagina}
        onCambiarTamano={cambiarTamano}
      />
    </div>
  );
}
