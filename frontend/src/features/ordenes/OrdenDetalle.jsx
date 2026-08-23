import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { listarEstados } from '../catalogos/api';
import { cancelarOrden, descargarArchivo, obtenerOrden } from './api';
import estilos from './OrdenDetalle.module.css';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });
const FORMATO_FECHA_HORA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });

/** §5.1 paso 9: el registro inicial no tiene autor porque lo asigna el sistema. */
const AUTOR_SISTEMA = 'Sistema';

function Dato({ etiqueta, valor }) {
  return (
    <div className={estilos.dato}>
      <span className={estilos.etiqueta}>{etiqueta}</span>
      <span>{valor ?? '—'}</span>
    </div>
  );
}

/**
 * CU-04/§5.4: el seguimiento de una orden propia, con su línea de tiempo fechada.
 *
 * **RN-22**: al paciente se lo identifica por iniciales y código. El backend ni siquiera manda
 * `pacienteNombre` cuando quien pregunta es el odontólogo, así que acá no hay nada que ocultar:
 * lo que no llega, no se puede mostrar.
 * **RN-01**: una orden ajena responde 404, igual que una inexistente, y así se muestra — decir
 * "no tenés permiso" revelaría que existe.
 * **D-11**: sin sección de mensajes.
 */
export default function OrdenDetalle() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [confirmandoCancelacion, setConfirmandoCancelacion] = useState(false);

  const consulta = useQuery({ queryKey: ['orden', id], queryFn: () => obtenerOrden(id) });
  const estados = useQuery({ queryKey: ['estados'], queryFn: listarEstados });

  const cancelacion = useMutation({
    // TanStack Query v5 le pasa un segundo argumento a mutationFn: se envuelve para que no llegue.
    mutationFn: () => cancelarOrden(id),
    onSuccess: () => {
      setConfirmandoCancelacion(false);
      queryClient.invalidateQueries({ queryKey: ['orden', id] });
    },
  });

  const descarga = useMutation({
    mutationFn: (archivo) => descargarArchivo(archivo.id),
    onSuccess: (blob, archivo) => abrirDescarga(blob, archivo.nombreOriginal),
  });

  if (consulta.isPending) {
    return <p className="contenedor">Cargando el trabajo...</p>;
  }
  if (consulta.isError) {
    return (
      <div className="contenedor">
        {/* RN-01: la orden ajena y la inexistente dan lo mismo, y se cuentan igual. */}
        <p>{consulta.error.status === 404 ? 'No encontramos ese trabajo.' : consulta.error.mensaje}</p>
        <Link to="/ordenes">Volver a mis trabajos</Link>
      </div>
    );
  }

  const orden = consulta.data;
  const esTerminal = estadoTerminal(estados.data, orden.estado);

  return (
    <div className="contenedor">
      <div className={estilos.encabezado}>
        <h1>Trabajo {orden.codigo}</h1>
        <Link to="/ordenes">Volver a mis trabajos</Link>
      </div>

      <section className={estilos.tarjeta}>
        <Dato etiqueta="Paciente" valor={orden.pacienteIdentificacion} />
        <Dato etiqueta="Trabajo" valor={orden.tipoTrabajo} />
        <Dato etiqueta="Tipo" valor={orden.tipoOrden} />
        <Dato etiqueta="Estado" valor={orden.estado} />
        <Dato etiqueta="Descripción" valor={orden.descripcion} />
        <Dato etiqueta="Ingreso" valor={FORMATO_FECHA.format(new Date(orden.fechaIngreso))} />
        <Dato
          etiqueta="Entrega estimada"
          valor={FORMATO_FECHA.format(new Date(orden.fechaEstimadaEntrega))}
        />
        {/* Los tres importes vienen calculados por el backend; acá no se suma nada (Agente.md 6.1). */}
        <Dato etiqueta="Precio base" valor={orden.precioBase} />
        <Dato etiqueta="Recargo por urgencia" valor={orden.recargoUrgencia} />
        <Dato etiqueta="Total" valor={orden.precioTotal} />
      </section>

      <section className={estilos.tarjeta}>
        <h2>Seguimiento</h2>
        <ol className={estilos.lineaTiempo}>
          {orden.lineaTiempo.map((etapa) => (
            <li key={`${etapa.estado}-${etapa.fechaHora}`}>
              <strong>{etapa.estado}</strong>
              <span>{FORMATO_FECHA_HORA.format(new Date(etapa.fechaHora))}</span>
              <span className={estilos.autor}>{etapa.autor ?? AUTOR_SISTEMA}</span>
            </li>
          ))}
        </ol>
      </section>

      <section className={estilos.tarjeta}>
        <h2>Archivos</h2>
        {orden.archivos.length === 0 ? (
          <p className={estilos.ayuda}>Este trabajo no tiene archivos adjuntos.</p>
        ) : (
          <ul className={estilos.archivos}>
            {orden.archivos.map((archivo) => (
              <li key={archivo.id}>
                <span>{archivo.nombreOriginal}</span>
                <button type="button" onClick={() => descarga.mutate(archivo)} disabled={descarga.isPending}>
                  Descargar
                </button>
              </li>
            ))}
          </ul>
        )}
        {descarga.isError && <p className={estilos.error}>{descarga.error.mensaje}</p>}
      </section>

      {/* CU-20/§5.6: cancelar es del propietario. Sin cargo por cancelación (P-14). */}
      {!esTerminal && (
        <section className={estilos.tarjeta}>
          <h2>Cancelar el trabajo</h2>
          {confirmandoCancelacion ? (
            <>
              <p>Una vez cancelado no se puede reabrir ni editar. ¿Confirmás la cancelación?</p>
              <button
                type="button"
                className={estilos.boton}
                onClick={() => cancelacion.mutate()}
                disabled={cancelacion.isPending}
              >
                {cancelacion.isPending ? 'Cancelando...' : 'Confirmar cancelación'}
              </button>
              <button type="button" onClick={() => setConfirmandoCancelacion(false)}>
                Volver
              </button>
            </>
          ) : (
            <button type="button" className={estilos.boton} onClick={() => setConfirmandoCancelacion(true)}>
              Cancelar el trabajo
            </button>
          )}
          {cancelacion.isError && <p className={estilos.error}>{cancelacion.error.mensaje}</p>}
        </section>
      )}
    </div>
  );
}

/**
 * RN-04: qué estados son terminales lo define la tabla `estado` (`esTerminal`), no una lista
 * escrita acá. Si el catálogo todavía no llegó, se muestra el botón: la última palabra la tiene
 * el backend, que responde `409 ORDEN_NO_CANCELABLE`.
 */
function estadoTerminal(estados, nombreEstado) {
  return (estados ?? []).some((estado) => estado.nombre === nombreEstado && estado.esTerminal);
}

/**
 * El adjunto llega como blob porque la ruta exige el token (ver `apiFetchArchivo`). Se abre desde
 * memoria con un enlace temporal, y la URL se libera enseguida para no retener el archivo.
 */
function abrirDescarga(blob, nombreArchivo) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
