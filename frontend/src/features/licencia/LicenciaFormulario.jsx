import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { LayoutFormulario } from '../../shared/components/LayoutFormulario';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { registrarLicencia } from './api';

const VALORES_INICIALES = { fechaInicio: '', fechaVencimiento: '', observacion: '' };
const CAMPOS_CONOCIDOS = ['fechaInicio', 'fechaVencimiento', 'observacion'];

/**
 * CU-23/RN-20: registro manual de un período de licencia por el SUPERADMIN.
 *
 * §8.1 Regla 1: el alta vive en su propia ruta, y al guardar vuelve al listado con la
 * confirmación. **Solo alta**: un período no se edita, se registra otro.
 *
 * **P-11/P-12**: no hay plan, ni precio, ni pasarela. Los tres campos son los que documenta §3.6.
 * El `422 FECHAS_LICENCIA_INVALIDAS` se muestra en el campo de vencimiento, que es el que devuelve
 * el backend; la pantalla **no adelanta la validación** (`Agente.md` §6.1).
 */
export default function LicenciaFormulario() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [datos, setDatos] = useState(VALORES_INICIALES);

  const volverAlListado = (mensaje) => {
    navigate('/admin/licencias', mensaje ? { state: { mensaje } } : undefined);
  };

  const mutacion = useMutation({
    mutationFn: (payload) => registrarLicencia(payload),
    onSuccess: () => {
      // El bloqueo depende del estado vigente: si este período lo levanta, hay que releerlo.
      queryClient.invalidateQueries({ queryKey: ['licencias'] });
      queryClient.invalidateQueries({ queryKey: ['licencia-vigente'] });
      volverAlListado('Período de licencia registrado.');
    },
  });

  const actualizarCampo = (campo) => (evento) => {
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    mutacion.mutate(datos);
  };

  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  return (
    <div className="contenedor">
      <LayoutFormulario
        titulo="Nuevo período de licencia"
        onSubmit={enviar}
        onCancelar={() => volverAlListado()}
        guardando={mutacion.isPending}
        error={errorGeneral}
      >
        <CampoFormulario id="fechaInicio" etiqueta="Inicio" error={errorDelCampo('fechaInicio')}>
          <input
            id="fechaInicio"
            type="date"
            required
            value={datos.fechaInicio}
            onChange={actualizarCampo('fechaInicio')}
          />
        </CampoFormulario>

        <CampoFormulario
          id="fechaVencimiento"
          etiqueta="Vencimiento"
          ayuda="Tiene que ser posterior a la fecha de inicio."
          error={errorDelCampo('fechaVencimiento')}
        >
          <input
            id="fechaVencimiento"
            type="date"
            required
            value={datos.fechaVencimiento}
            onChange={actualizarCampo('fechaVencimiento')}
          />
        </CampoFormulario>

        <CampoFormulario
          id="observacion"
          etiqueta="Observación"
          ayuda="Opcional. P-11/P-12: no hay planes ni cobro; esta activación es manual."
          error={errorDelCampo('observacion')}
        >
          <input id="observacion" value={datos.observacion} onChange={actualizarCampo('observacion')} />
        </CampoFormulario>
      </LayoutFormulario>
    </div>
  );
}
