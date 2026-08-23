import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { LayoutFormulario } from '../../shared/components/LayoutFormulario';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { listarOdontologosActivos } from '../auth/api';
import { listarActivos, listarTiposOrden } from '../catalogos/api';
import { crearOrden } from './api';

const VALORES_INICIALES = {
  odontologoId: '',
  pacienteNombre: '',
  fechaIngreso: '',
  tipoTrabajoId: '',
  tipoOrdenCodigo: '',
  descripcion: '',
};

const CAMPOS_CONOCIDOS = Object.keys(VALORES_INICIALES);

/** §8 y §5.1: el listado de órdenes del admin es de T-26; hasta que exista, se vuelve al inicio. */
const DESTINO = '/';

/**
 * CU-09/§5.1 con D-19: **el laboratorio registra la orden** a nombre de un odontólogo. La pantalla
 * de "Nueva orden" del odontólogo quedó retirada de la navegación; el flujo original se conserva
 * documentado por P-19.
 *
 * No calcula nada (Agente.md 6.1): el precio, el recargo, el estado inicial y la fecha estimada de
 * entrega los deriva el backend y se muestran tal como llegan en la respuesta. Tampoco muestra el
 * nombre del paciente en la confirmación (§5.1 criterio 4, RN-22).
 */
export default function OrdenFormulario() {
  const navigate = useNavigate();
  const [datos, setDatos] = useState(VALORES_INICIALES);

  const odontologos = useQuery({ queryKey: ['odontologos', 'activos'], queryFn: listarOdontologosActivos });
  const tiposTrabajo = useQuery({ queryKey: ['tipos-trabajo', 'activos'], queryFn: listarActivos });
  const tiposOrden = useQuery({ queryKey: ['tipos-orden'], queryFn: listarTiposOrden });

  const mutacion = useMutation({
    // TanStack Query v5 le pasa un segundo argumento a mutationFn: se envuelve para que no llegue.
    mutationFn: (payload) => crearOrden(payload),
    onSuccess: (orden) => {
      navigate(DESTINO, {
        state: {
          mensaje: `Orden ${orden.codigo} registrada. Entrega estimada: ${orden.fechaEstimadaEntrega}. `
            + `Total: ${orden.precioTotal}.`,
        },
      });
    },
  });

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate({
      ...datos,
      // El backend espera números; los selects entregan texto. Convertir no es calcular.
      odontologoId: datos.odontologoId ? Number(datos.odontologoId) : null,
      tipoTrabajoId: datos.tipoTrabajoId ? Number(datos.tipoTrabajoId) : null,
      descripcion: datos.descripcion || null,
    });
  };

  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  const cargando = odontologos.isPending || tiposTrabajo.isPending || tiposOrden.isPending;
  const errorDeCarga = odontologos.isError || tiposTrabajo.isError || tiposOrden.isError;

  if (cargando) {
    return <p className="contenedor">Cargando los datos del formulario...</p>;
  }
  if (errorDeCarga) {
    return (
      <div className="contenedor">
        <p>No pudimos cargar los datos del formulario.</p>
        <button
          type="button"
          onClick={() => {
            odontologos.refetch();
            tiposTrabajo.refetch();
            tiposOrden.refetch();
          }}
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="contenedor">
      <LayoutFormulario
        titulo="Nueva orden"
        onSubmit={enviar}
        onCancelar={() => navigate(DESTINO)}
        guardando={mutacion.isPending}
        error={errorGeneral}
      >
        <CampoFormulario
          id="odontologoId"
          etiqueta="Odontólogo"
          ayuda="D-19: la orden se registra a nombre del odontólogo que la encargó, que es quien recibe los avisos."
          error={errorDelCampo('odontologoId')}
        >
          <select id="odontologoId" value={datos.odontologoId} onChange={actualizarCampo('odontologoId')}>
            <option value="">Elegí un odontólogo</option>
            {odontologos.data.map((odontologo) => (
              <option key={odontologo.id} value={odontologo.id}>
                {odontologo.nombreCompleto}
              </option>
            ))}
          </select>
        </CampoFormulario>

        <CampoFormulario
          id="pacienteNombre"
          etiqueta="Nombre del paciente"
          ayuda="RN-22: es de uso interno del laboratorio. Al odontólogo se le muestra por iniciales y código."
          error={errorDelCampo('pacienteNombre')}
        >
          <input id="pacienteNombre" type="text" value={datos.pacienteNombre} onChange={actualizarCampo('pacienteNombre')} />
        </CampoFormulario>

        <CampoFormulario
          id="fechaIngreso"
          etiqueta="Fecha de ingreso"
          ayuda="RN-18: la entrega estimada se calcula desde esta fecha, en días hábiles."
          error={errorDelCampo('fechaIngreso')}
        >
          <input id="fechaIngreso" type="date" value={datos.fechaIngreso} onChange={actualizarCampo('fechaIngreso')} />
        </CampoFormulario>

        <CampoFormulario
          id="tipoTrabajoId"
          etiqueta="Tipo de trabajo"
          ayuda="RN-21: el precio y los días estimados quedan congelados con el valor del catálogo de hoy."
          error={errorDelCampo('tipoTrabajoId')}
        >
          <select id="tipoTrabajoId" value={datos.tipoTrabajoId} onChange={actualizarCampo('tipoTrabajoId')}>
            <option value="">Elegí un tipo de trabajo</option>
            {tiposTrabajo.data.map((tipo) => (
              <option key={tipo.id} value={tipo.id}>
                {tipo.nombre}
              </option>
            ))}
          </select>
        </CampoFormulario>

        <CampoFormulario
          id="tipoOrdenCodigo"
          etiqueta="Tipo de orden"
          ayuda="RN-11: el tipo urgente adelanta el estado inicial y agrega el recargo definido en el catálogo."
          error={errorDelCampo('tipoOrdenCodigo')}
        >
          <select id="tipoOrdenCodigo" value={datos.tipoOrdenCodigo} onChange={actualizarCampo('tipoOrdenCodigo')}>
            <option value="">Elegí un tipo de orden</option>
            {tiposOrden.data.map((tipo) => (
              <option key={tipo.codigo} value={tipo.codigo}>
                {tipo.nombre}
              </option>
            ))}
          </select>
        </CampoFormulario>

        <CampoFormulario id="descripcion" etiqueta="Descripción (opcional)" error={errorDelCampo('descripcion')}>
          <textarea id="descripcion" rows={3} value={datos.descripcion} onChange={actualizarCampo('descripcion')} />
        </CampoFormulario>
      </LayoutFormulario>
    </div>
  );
}
