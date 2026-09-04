import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { avanzarEstado, descargarArchivo, obtenerOrden } from './api';
import { abrirDescarga } from '../../shared/util/descargaArchivo';
import { EncabezadoPantalla } from '../../shared/components/EncabezadoPantalla';
import { EtiquetaEstado } from '../../shared/components/EtiquetaEstado';
import { LineaTiempo } from '../../shared/components/LineaTiempo';
import { Icono } from '../../shared/components/Icono';
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
 * CU-06/§5.5: la orden vista por el laboratorio, que es quien la hace avanzar.
 *
 * Dos diferencias con el detalle del odontólogo, y las dos son de spec:
 * - **Muestra `pacienteNombre`** (§5.4: "lo necesita para operar"). Es la única pantalla del
 *   sistema donde ese dato aparece; el backend solo lo manda a ADMIN y SUPERADMIN.
 * - **No tiene botón de cancelar**: §5.5 es explícito en que el laboratorio no cancela, la
 *   cancelación es del odontólogo (RN-17/CU-20).
 *
 * El botón de avance ofrece **una sola** transición, la que viene en `siguienteEstado`. La
 * pantalla no calcula cuál es (§8): si el campo viene vacío —`ENTREGADO`, `CANCELADO`— no hay
 * botón, y no hay forma de saltear etapas ni de retroceder (RN-04, P-02).
 */
export default function OrdenDetalleAdmin() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const consulta = useQuery({ queryKey: ['orden-admin', id], queryFn: () => obtenerOrden(id) });

  const avance = useMutation({
    // TanStack Query v5 le pasa un segundo argumento a mutationFn: se envuelve para que no llegue.
    mutationFn: (estadoCodigo) => avanzarEstado(id, estadoCodigo),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['orden-admin', id] }),
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
        <p>{consulta.error.status === 404 ? 'No encontramos ese trabajo.' : consulta.error.mensaje}</p>
        <Link to="/admin/ordenes">Volver a los trabajos</Link>
      </div>
    );
  }

  const orden = consulta.data;

  return (
    <div className="contenedor">
      <Link to="/admin/ordenes" className={estilos.volver}>
        <Icono nombre="flechaIzquierda" tamano={16} />
        Volver a los trabajos
      </Link>

      <EncabezadoPantalla titulo={`Trabajo ${orden.codigo}`}>
        <EtiquetaEstado estado={orden.estado} estadoCodigo={orden.estadoCodigo} />
      </EncabezadoPantalla>

      {/* §5.4: el laboratorio sí ve el nombre; al odontólogo el backend no se lo manda (RN-22). */}
      <p className={estilos.meta}>
        <span>
          <strong>Paciente:</strong> {orden.pacienteNombre}
        </span>
        <span>
          <strong>Trabajo:</strong> {orden.tipoTrabajo}
        </span>
        <span>
          <strong>Ingreso:</strong> {FORMATO_FECHA.format(new Date(orden.fechaIngreso))}
        </span>
        <span>
          <strong>Entrega estimada:</strong> {FORMATO_FECHA.format(new Date(orden.fechaEstimadaEntrega))}
        </span>
      </p>

      <div className={estilos.grilla}>
        <div className={estilos.columna}>
          <section className={estilos.tarjeta}>
            <h2>Avance</h2>
            {orden.siguienteEstado ? (
              <>
                <p className={estilos.ayuda}>
                  RN-04: el flujo es lineal. Desde acá solo se puede avanzar a la etapa siguiente.
                </p>
                <button
                  type="button"
                  className={estilos.boton}
                  disabled={avance.isPending}
                  onClick={() => avance.mutate(orden.siguienteEstado.codigo)}
                >
                  {avance.isPending ? 'Avanzando...' : `Avanzar a ${orden.siguienteEstado.nombre}`}
                </button>
              </>
            ) : (
              <p className={estilos.ayuda}>
                El trabajo está en {orden.estado} y ya no admite cambios de estado.
              </p>
            )}
            {avance.isError && <p className={estilos.error}>{avance.error.mensaje}</p>}
          </section>

          <section className={estilos.tarjeta}>
            <h2>Seguimiento del trabajo</h2>
            <LineaTiempo etapas={orden.lineaTiempo} estadoActualCodigo={orden.estadoCodigo} />
          </section>
        </div>

        <div className={estilos.columna}>
          <section className={estilos.tarjeta}>
            <h2>Información del trabajo</h2>
            <Dato etiqueta="Identificación" valor={orden.pacienteIdentificacion} />
            <Dato etiqueta="Tipo" valor={orden.tipoOrden} />
            <Dato etiqueta="Descripción" valor={orden.descripcion} />
            <Dato etiqueta="Precio base" valor={orden.precioBase} />
            <Dato etiqueta="Recargo por urgencia" valor={orden.recargoUrgencia} />
            <Dato etiqueta="Total" valor={orden.precioTotal} />
          </section>

          <section className={estilos.tarjeta}>
            <h2>Archivos</h2>
            {orden.archivos.length === 0 ? (
              <p className={estilos.ayuda}>Este trabajo no tiene archivos adjuntos.</p>
            ) : (
              <div className={estilos.archivosGrid}>
                {orden.archivos.map((archivo) => (
                  <div key={archivo.id} className={estilos.archivo}>
                    <span className={estilos.archivoNombre}>{archivo.nombreOriginal}</span>
                    <button
                      type="button"
                      className={estilos.botonSecundario}
                      onClick={() => descarga.mutate(archivo)}
                      disabled={descarga.isPending}
                    >
                      Descargar
                    </button>
                  </div>
                ))}
              </div>
            )}
            {descarga.isError && <p className={estilos.error}>{descarga.error.mensaje}</p>}
          </section>
        </div>
      </div>
    </div>
  );
}

