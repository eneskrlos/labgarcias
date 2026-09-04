import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { listarEstados } from '../catalogos/api';
import { abrirDescarga } from '../../shared/util/descargaArchivo';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { LineaTiempo } from '../../shared/components/LineaTiempo';
import { cancelarOrden, descargarArchivo, obtenerOrden } from './api';
import estilos from './OrdenDetalle.module.css';

const FORMATO_FECHA = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short' });

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
      <EncabezadoPantalla titulo={`Trabajo ${orden.codigo}`}>
        <Link to="/ordenes">Volver a mis trabajos</Link>
      </EncabezadoPantalla>

      <section className={estilos.tarjeta}>
        <Dato etiqueta="Paciente" valor={orden.pacienteIdentificacion} />
        <Dato etiqueta="Trabajo" valor={orden.tipoTrabajo} />
        <Dato etiqueta="Tipo" valor={orden.tipoOrden} />
        <Dato
          etiqueta="Estado"
          valor={<EtiquetaEstado estado={orden.estado} estadoCodigo={orden.estadoCodigo} />}
        />
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
        <LineaTiempo etapas={orden.lineaTiempo} />
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

