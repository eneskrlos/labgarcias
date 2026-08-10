const ID_SCRIPT = 'google-identity-services';
let promesaCarga = null;

function cargarScript() {
  if (!promesaCarga) {
    promesaCarga = new Promise((resolve) => {
      const existente = document.getElementById(ID_SCRIPT);
      if (existente) {
        if (window.google) {
          resolve();
        } else {
          existente.addEventListener('load', () => resolve(), { once: true });
        }
        return;
      }
      const script = document.createElement('script');
      script.id = ID_SCRIPT;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      document.body.appendChild(script);
    });
  }
  return promesaCarga;
}

/**
 * Carga el SDK de Google Identity Services una sola vez por página (el
 * <script> no se duplica aunque el componente se monte más de una vez, ej.:
 * React StrictMode en desarrollo) e inicializa/renderiza el botón.
 */
export async function mostrarBotonGoogle({ clientId, contenedor, alObtenerCredencial }) {
  await cargarScript();
  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: (respuesta) => alObtenerCredencial(respuesta.credential),
  });
  if (contenedor) {
    window.google.accounts.id.renderButton(contenedor, { theme: 'outline', size: 'large', width: 320 });
  }
}
