import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { LayoutFormulario } from '../../shared/components/LayoutFormulario';
import { CampoFormulario } from '../../shared/components/CampoFormulario';
import { guardarConfiguracionNotificaciones, obtenerConfiguracionNotificaciones } from './api';
import estilos from './ConfiguracionNotificaciones.module.css';

const CLAVE_CONSULTA = 'configuracion-notificaciones';

/** Los campos que el formulario sabe pintar: un error de otro campo va al encabezado. */
const CAMPOS_CONOCIDOS = ['canalAppActivo', 'canalCorreoActivo', 'canalTelegramActivo', 'telegramChatId'];

const VALORES_INICIALES = {
  canalAppActivo: false,
  canalCorreoActivo: false,
  canalTelegramActivo: false,
  telegramChatId: '',
};

/**
 * CU-21/RN-19, §6.4: por qué canales recibe sus notificaciones el administrador autenticado.
 *
 * **No es un CRUD**: es un formulario único de edición sobre un registro que ya existe, así que
 * §8.1 **Regla 1 no aplica** —no hay `/nuevo` ni `/{id}/editar`— y tampoco la Regla 2, porque no
 * hay nada que paginar. Sí rigen la **Regla 3** (cargando y error con reintento; *vacío* no
 * existe: el `GET` nunca devuelve 404, quien no configuró nada recibe los canales por defecto de
 * §6.3), la **Regla 4** (`LayoutFormulario` y `CampoFormulario` de `shared/`) y la **Regla 5**
 * (los textos "Guardar" y "Cancelar").
 *
 * **Guardar deja al usuario acá**, con la confirmación y el formulario refrescado con lo que
 * devolvió el `PUT`: es una pantalla de ajustes que se toca varias veces seguidas, no un alta que
 * se completa y se cierra. **Cancelar vuelve a `/admin`**, que es de donde se llega por el menú.
 *
 * **P-18**: WhatsApp se informa y no se puede activar — el request del backend ni siquiera lo
 * acepta. La casilla está deshabilitada y el texto dice por qué, sin sugerir que el canal
 * funcione.
 */
export default function ConfiguracionNotificaciones() {
  const navigate = useNavigate();

  const [datos, setDatos] = useState(VALORES_INICIALES);
  const [confirmacion, setConfirmacion] = useState(null);

  const consulta = useQuery({ queryKey: [CLAVE_CONSULTA], queryFn: obtenerConfiguracionNotificaciones });

  useEffect(() => {
    if (consulta.data) {
      setDatos(aFormulario(consulta.data));
    }
  }, [consulta.data]);

  const mutacion = useMutation({
    // Envuelta y no pasada directo: TanStack Query agrega un segundo argumento con su propio
    // contexto, y el cliente de API no tiene por qué recibirlo.
    mutationFn: (payload) => guardarConfiguracionNotificaciones(payload),
    onSuccess: (guardada) => {
      // §8.1 Regla 1 no aplica —no hay listado al que volver—, así que la confirmación se muestra
      // acá. El formulario se refresca con lo que devolvió el backend, no con lo que se mandó.
      setDatos(aFormulario(guardada));
      setConfirmacion('Configuración guardada.');
    },
  });

  const actualizarCasilla = (campo) => (evento) => {
    setConfirmacion(null);
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.checked }));
  };

  const actualizarTexto = (campo) => (evento) => {
    setConfirmacion(null);
    setDatos((anterior) => ({ ...anterior, [campo]: evento.target.value }));
  };

  const enviar = (evento) => {
    evento.preventDefault();
    setConfirmacion(null);
    // Las tres banderas viajan siempre: el PUT reemplaza la configuración entera (CU-21).
    // canalWhatsappActivo no se manda — P-18, el backend no lo acepta.
    mutacion.mutate({
      canalAppActivo: datos.canalAppActivo,
      canalCorreoActivo: datos.canalCorreoActivo,
      canalTelegramActivo: datos.canalTelegramActivo,
      telegramChatId: datos.telegramChatId,
    });
  };

  // El 422 TELEGRAM_SIN_DESTINO viene con campo "telegramChatId": se pinta ahí, con el mensaje
  // que mandó el backend. La pantalla no reimplementa la regla de CU-21 ni adelanta el rechazo.
  const errorDelCampo = (campo) =>
    mutacion.isError && mutacion.error.campo === campo ? mutacion.error.mensaje : null;
  const errorGeneral =
    mutacion.isError && !CAMPOS_CONOCIDOS.includes(mutacion.error.campo) ? mutacion.error.mensaje : null;

  if (consulta.isLoading) {
    return (
      <div className="contenedor">
        <p>Cargando...</p>
      </div>
    );
  }

  // §8.1 Regla 3: el error se muestra y se puede reintentar.
  if (consulta.isError) {
    return (
      <div className="contenedor">
        <p className={estilos.error}>{consulta.error.mensaje}</p>
        <button type="button" onClick={() => consulta.refetch()}>
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="contenedor">
      {confirmacion && <p role="status">{confirmacion}</p>}

      <LayoutFormulario
        titulo="Configuración de notificaciones"
        onSubmit={enviar}
        onCancelar={() => navigate('/admin')}
        guardando={mutacion.isPending}
        error={errorGeneral}
      >
        <CampoFormulario
          id="canalAppActivo"
          etiqueta="Aplicación"
          ayuda="RN-19: canales por los que recibís las notificaciones del laboratorio."
          error={errorDelCampo('canalAppActivo')}
        >
          <input
            id="canalAppActivo"
            type="checkbox"
            checked={datos.canalAppActivo}
            onChange={actualizarCasilla('canalAppActivo')}
          />
        </CampoFormulario>

        <CampoFormulario id="canalCorreoActivo" etiqueta="Correo" error={errorDelCampo('canalCorreoActivo')}>
          <input
            id="canalCorreoActivo"
            type="checkbox"
            checked={datos.canalCorreoActivo}
            onChange={actualizarCasilla('canalCorreoActivo')}
          />
        </CampoFormulario>

        <CampoFormulario id="canalTelegramActivo" etiqueta="Telegram" error={errorDelCampo('canalTelegramActivo')}>
          <input
            id="canalTelegramActivo"
            type="checkbox"
            checked={datos.canalTelegramActivo}
            onChange={actualizarCasilla('canalTelegramActivo')}
          />
        </CampoFormulario>

        <CampoFormulario
          id="telegramChatId"
          etiqueta="Chat de Telegram"
          ayuda="CU-21: obligatorio si activás Telegram."
          error={errorDelCampo('telegramChatId')}
        >
          <input
            id="telegramChatId"
            maxLength={100}
            value={datos.telegramChatId}
            onChange={actualizarTexto('telegramChatId')}
          />
        </CampoFormulario>

        <CampoFormulario
          id="canalWhatsappActivo"
          etiqueta="WhatsApp"
          ayuda="P-18: todavía no hay proveedor de WhatsApp, así que este canal no se puede activar."
        >
          <input
            id="canalWhatsappActivo"
            type="checkbox"
            disabled
            checked={consulta.data.canalWhatsappActivo}
            readOnly
          />
        </CampoFormulario>
      </LayoutFormulario>
    </div>
  );
}

/** El `telegramChatId` nulo se muestra vacío: un input controlado no admite `null`. */
function aFormulario(configuracion) {
  return {
    canalAppActivo: configuracion.canalAppActivo,
    canalCorreoActivo: configuracion.canalCorreoActivo,
    canalTelegramActivo: configuracion.canalTelegramActivo,
    telegramChatId: configuracion.telegramChatId ?? '',
  };
}
