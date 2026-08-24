/**
 * Entrega al usuario un archivo que ya está en memoria (RN-13/CU-04).
 *
 * Va de la mano de `apiFetchArchivo`: como la sesión viaja en el header `Authorization`, el
 * adjunto no se puede abrir con un enlace directo —saldría sin token y daría 401—, así que se
 * descarga primero como blob y se entrega desde acá con un enlace temporal.
 *
 * La URL se libera enseguida: si no, el navegador retiene el archivo hasta recargar la página.
 */
export function abrirDescarga(blob, nombreArchivo) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
