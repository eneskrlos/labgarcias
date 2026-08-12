import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { actualizar, cambiarEstado, crear, listarTodos } from './api';
import estilos from './TiposTrabajo.module.css';

const CLAVE_CONSULTA = ['tipos-trabajo-todos'];
const VALORES_INICIALES = { nombre: '', diasEstimados: '', precio: '' };

export default function TiposTrabajo() {
  const queryClient = useQueryClient();
  const consulta = useQuery({ queryKey: CLAVE_CONSULTA, queryFn: listarTodos });

  const [editandoId, setEditandoId] = useState(null);
  const [datos, setDatos] = useState(VALORES_INICIALES);

  const invalidarListado = () => queryClient.invalidateQueries({ queryKey: CLAVE_CONSULTA });

  const cancelarEdicion = () => {
    setEditandoId(null);
    setDatos(VALORES_INICIALES);
  };

  const mutacionCrear = useMutation({
    mutationFn: crear,
    onSuccess: () => {
      invalidarListado();
      setDatos(VALORES_INICIALES);
    },
  });

  const mutacionActualizar = useMutation({
    mutationFn: ({ id, datosActualizados }) => actualizar(id, datosActualizados),
    onSuccess: () => {
      invalidarListado();
      cancelarEdicion();
    },
  });

  const mutacionCambiarEstado = useMutation({
    mutationFn: ({ id, activo }) => cambiarEstado(id, activo),
    onSuccess: invalidarListado,
  });

  const mutacionDelFormulario = editandoId ? mutacionActualizar : mutacionCrear;

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const editar = (tipo) => {
    setEditandoId(tipo.id);
    setDatos({
      nombre: tipo.nombre,
      diasEstimados: String(tipo.diasEstimados),
      precio: String(tipo.precio),
    });
  };

  const enviar = (evento) => {
    evento.preventDefault();
    const payload = {
      nombre: datos.nombre,
      diasEstimados: Number(datos.diasEstimados),
      precio: Number(datos.precio),
    };
    if (editandoId) {
      mutacionActualizar.mutate({ id: editandoId, datosActualizados: payload });
    } else {
      mutacionCrear.mutate(payload);
    }
  };

  const alternarActivo = (tipo) => {
    mutacionCambiarEstado.mutate({ id: tipo.id, activo: !tipo.activo });
  };

  return (
    <div className="contenedor">
      <h1>Tipos de trabajo</h1>

      {/*
        noValidate: la validación real de RN-12/RN-21 la hace el backend (Agente.md:
        "sin lógica de negocio en el frontend"). min/required abajo son solo affordance
        visual; sin noValidate el navegador bloquea el submit y nunca llega a mostrarse
        el mensaje del backend.
      */}
      <form onSubmit={enviar} className={estilos.formulario} noValidate>
        <h2>{editandoId ? 'Editar tipo de trabajo' : 'Nuevo tipo de trabajo'}</h2>

        {mutacionDelFormulario.isError && (
          <p className={estilos.error}>{mutacionDelFormulario.error.mensaje}</p>
        )}

        <div className={estilos.campo}>
          <label htmlFor="nombre">Nombre</label>
          <input id="nombre" required value={datos.nombre} onChange={actualizarCampo('nombre')} />
        </div>

        <div className={estilos.campo}>
          <label htmlFor="diasEstimados">Días estimados</label>
          <input
            id="diasEstimados"
            type="number"
            min="7"
            required
            value={datos.diasEstimados}
            onChange={actualizarCampo('diasEstimados')}
          />
        </div>
        <p className={estilos.ayuda}>RN-12: mínimo 7 días hábiles.</p>

        <div className={estilos.campo}>
          <label htmlFor="precio">Precio</label>
          <input
            id="precio"
            type="number"
            min="250"
            step="0.01"
            required
            value={datos.precio}
            onChange={actualizarCampo('precio')}
          />
        </div>
        <p className={estilos.ayuda}>RN-21: mínimo 250.</p>

        <div className={estilos.acciones}>
          <button type="submit" className={estilos.boton} disabled={mutacionDelFormulario.isPending}>
            {mutacionDelFormulario.isPending ? 'Guardando...' : editandoId ? 'Guardar cambios' : 'Crear'}
          </button>
          {editandoId && (
            <button type="button" className={estilos.botonSecundario} onClick={cancelarEdicion}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      {consulta.isPending && <p>Cargando...</p>}
      {consulta.isError && <p className={estilos.error}>{consulta.error.mensaje}</p>}

      {consulta.isSuccess && (
        <table className={estilos.tabla}>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Días</th>
              <th>Precio</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {consulta.data.map((tipo) => (
              <tr key={tipo.id} className={tipo.activo ? undefined : estilos.inactivo}>
                <td>{tipo.nombre}</td>
                <td>{tipo.diasEstimados}</td>
                <td>{tipo.precio}</td>
                <td>{tipo.activo ? 'Activo' : 'Inactivo'}</td>
                <td>
                  <button type="button" onClick={() => editar(tipo)}>
                    Editar
                  </button>
                  <button type="button" onClick={() => alternarActivo(tipo)}>
                    {tipo.activo ? 'Desactivar' : 'Activar'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
