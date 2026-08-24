import { apiFetch, apiFetchArchivo } from '../../shared/api/cliente';

/**
 * CU-03/§5.3: las órdenes **del usuario autenticado**. El backend filtra por el token: acá no
 * hay ni puede haber un id de odontólogo (RN-01). `estado` es opcional y también lo resuelve el
 * backend; nunca se filtra sobre una colección ya traída (Agente.md 6.2).
 *
 * CU-12: con `historico` el backend deja solo las órdenes cerradas —las de estado terminal—, que
 * es lo que consume la pantalla de historial. Es el mismo endpoint con un filtro más, así que se
 * consume con la misma función.
 */
export function listarMisOrdenes({ pagina, tamano, estado, historico = false }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  if (estado) {
    parametros.set('estado', estado);
  }
  if (historico) {
    parametros.set('historico', 'true');
  }
  return apiFetch(`/ordenes?${parametros.toString()}`);
}

/**
 * CU-06/§5.7: las órdenes de todo el laboratorio, con los tres filtros opcionales. A diferencia
 * de `listarMisOrdenes`, acá `odontologoId` **sí** es un filtro: quien consulta ve todas por rol.
 */
export function listarOrdenesAdmin({ pagina, tamano, estado, tipoOrden, odontologoId }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  if (estado) {
    parametros.set('estado', estado);
  }
  if (tipoOrden) {
    parametros.set('tipoOrden', tipoOrden);
  }
  if (odontologoId) {
    parametros.set('odontologoId', String(odontologoId));
  }
  return apiFetch(`/admin/ordenes?${parametros.toString()}`);
}

/**
 * CU-06/§5.5: avanza la orden a la etapa siguiente. El código que se manda es el que vino en
 * `siguienteEstado`: la transición la decide el backend, no la pantalla (§8).
 */
export function avanzarEstado(id, estadoCodigo) {
  return apiFetch(`/ordenes/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estadoCodigo }) });
}

/** CU-04/§5.4: detalle con línea de tiempo y adjuntos. Una orden ajena responde 404, no 403. */
export function obtenerOrden(id) {
  return apiFetch(`/ordenes/${id}`);
}

/** CU-20/§5.6: cancelación por el propietario. Sin cargo (P-14) y sin vuelta atrás. */
export function cancelarOrden(id) {
  return apiFetch(`/ordenes/${id}/cancelar`, { method: 'PATCH' });
}

/** RN-13: el binario del adjunto. Va por `fetch` porque la ruta exige el token (ver cliente). */
export function descargarArchivo(id) {
  return apiFetchArchivo(`/archivos/${id}`);
}

/**
 * CU-09/§5.1: alta de una orden. **La registra el laboratorio** (D-19), a nombre del odontólogo
 * que elige el administrador.
 *
 * El cliente manda solo lo que la persona escribe: código, iniciales, precios, estado inicial y
 * fecha estimada los deriva el backend (Agente.md 6.1: ningún cálculo de negocio en el frontend).
 */
export function crearOrden(datos) {
  return apiFetch('/ordenes', { method: 'POST', body: JSON.stringify(datos) });
}
