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

const ANCHO_MINIMO = 200;
const ANCHO_MAXIMO = 400; // tope documentado por Google para renderButton

/**
 * Carga el SDK de Google Identity Services una sola vez por página (el
 * <script> no se duplica aunque el componente se monte más de una vez, ej.:
 * React StrictMode en desarrollo) e inicializa el cliente. Se llama una sola
 * vez; para dibujar o redibujar el botón usar renderizarBotonGoogle.
 */
export async function iniciarGoogle({ clientId, alObtenerCredencial }) {
  await cargarScript();
  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: (respuesta) => alObtenerCredencial(respuesta.credential),
  });
}

/**
 * (Re)dibuja el botón con el ancho disponible. Google no soporta un ancho
 * porcentual/responsive nativo (renderButton exige un valor fijo en
 * píxeles), así que quien la use debe volver a llamarla cuando cambie el
 * tamaño del contenedor (ej.: con ResizeObserver) para que el botón
 * acompañe el resto del layout en pantallas angostas.
 */
export function renderizarBotonGoogle(contenedor, anchoDisponible) {
  if (!contenedor || !window.google) {
    return;
  }
  const ancho = Math.round(Math.min(ANCHO_MAXIMO, Math.max(ANCHO_MINIMO, anchoDisponible)));
  window.google.accounts.id.renderButton(contenedor, { theme: 'outline', size: 'large', width: ancho });
}
