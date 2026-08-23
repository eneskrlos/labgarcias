import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { LayoutFormulario } from '../../shared/components/LayoutFormulario';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { actualizar, crear, obtenerPorId } from './api';

const VALORES_INICIALES = { nombre: '', diasEstimados: '', precio: '' };
const CAMPOS_CONOCIDOS = ['nombre', 'diasEstimados', 'precio'];

/** spec.md §8.1 Regla 1: mismo componente para alta y edición, parametrizado por la presencia de :id. */
export default function TipoTrabajoFormulario() {
  const { id } = useParams();
  const esEdicion = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [datos, setDatos] = useState(VALORES_INICIALES);

  const consulta = useQuery({
    queryKey: ['tipo-trabajo', id],
    queryFn: () => obtenerPorId(id),
    enabled: esEdicion,
  });

  useEffect(() => {
    if (consulta.data) {
      setDatos({
        nombre: consulta.data.nombre,
        diasEstimados: String(consulta.data.diasEstimados),
        precio: String(consulta.data.precio),
      });
    }
  }, [consulta.data]);

  const volverAlListado = (mensaje) => {
    navigate('/admin/tipos-trabajo', mensaje ? { state: { mensaje } } : undefined);
  };

  const invalidarListado = () => queryClient.invalidateQueries({ queryKey: ['tipos-trabajo'] });

  const mutacionCrear = useMutation({
    mutationFn: crear,
    onSuccess: () => {
      invalidarListado();
      volverAlListado('Tipo de trabajo creado.');
    },
  });

  const mutacionActualizar = useMutation({
    mutationFn: (payload) => actualizar(id, payload),
    onSuccess: () => {
      invalidarListado();
      volverAlListado('Tipo de trabajo actualizado.');
    },
  });

  const mutacionActiva = esEdicion ? mutacionActualizar : mutacionCrear;

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacionActiva.mutate({
      nombre: datos.nombre,
      diasEstimados: Number(datos.diasEstimados),
      precio: Number(datos.precio),
    });
  };

  const errorDelCampo = (campo) =>
    mutacionActiva.isError && mutacionActiva.error.campo === campo ? mutacionActiva.error.mensaje : null;
  const errorGeneral =
    mutacionActiva.isError && !CAMPOS_CONOCIDOS.includes(mutacionActiva.error.campo)
      ? mutacionActiva.error.mensaje
      : null;

  if (esEdicion && consulta.isLoading) {
    return (
      <div className="contenedor">
        <p>Cargando...</p>
      </div>
    );
  }

  if (esEdicion && consulta.isError) {
    return (
      <div className="contenedor">
        <p>{consulta.error.mensaje}</p>
      </div>
    );
  }

  return (
    <div className="contenedor">
      <LayoutFormulario
        titulo={esEdicion ? 'Editar tipo de trabajo' : 'Nuevo tipo de trabajo'}
        onSubmit={enviar}
        onCancelar={() => volverAlListado()}
        guardando={mutacionActiva.isPending}
        error={errorGeneral}
      >
        <CampoFormulario id="nombre" etiqueta="Nombre" error={errorDelCampo('nombre')}>
          <input id="nombre" required value={datos.nombre} onChange={actualizarCampo('nombre')} />
        </CampoFormulario>

        <CampoFormulario
          id="diasEstimados"
          etiqueta="Días estimados"
          ayuda="RN-12: mínimo 7 días hábiles."
          error={errorDelCampo('diasEstimados')}
        >
          <input
            id="diasEstimados"
            type="number"
            min="7"
            required
            value={datos.diasEstimados}
            onChange={actualizarCampo('diasEstimados')}
          />
        </CampoFormulario>

        <CampoFormulario id="precio" etiqueta="Precio" ayuda="RN-21: mínimo 250." error={errorDelCampo('precio')}>
          <input
            id="precio"
            type="number"
            min="250"
            step="0.01"
            required
            value={datos.precio}
            onChange={actualizarCampo('precio')}
          />
        </CampoFormulario>
      </LayoutFormulario>
    </div>
  );
}
