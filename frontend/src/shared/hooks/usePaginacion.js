import { useSearchParams } from 'react-router-dom';

const TAMANOS_VALIDOS = [10, 20, 30];
const TAMANO_DEFECTO = 10;

/** spec.md §8.1 Regla 2: page/size viven en la URL (?page=&size=) y sobreviven a un refresco. */
export function usePaginacion() {
  const [searchParams, setSearchParams] = useSearchParams();

  const pageParam = Number(searchParams.get('page'));
  const sizeParam = Number(searchParams.get('size'));

  const pagina = Number.isInteger(pageParam) && pageParam >= 0 ? pageParam : 0;
  const tamano = TAMANOS_VALIDOS.includes(sizeParam) ? sizeParam : TAMANO_DEFECTO;

  const cambiarPagina = (nuevaPagina) => {
    setSearchParams((anteriores) => {
      const siguientes = new URLSearchParams(anteriores);
      siguientes.set('page', String(Math.max(nuevaPagina, 0)));
      siguientes.set('size', String(tamano));
      return siguientes;
    });
  };

  const cambiarTamano = (nuevoTamano) => {
    setSearchParams((anteriores) => {
      const siguientes = new URLSearchParams(anteriores);
      // Regla 2: cambiar el tamaño de página vuelve a page=0.
      siguientes.set('page', '0');
      siguientes.set('size', String(nuevoTamano));
      return siguientes;
    });
  };

  /**
   * Un filtro de la vista, leído de la URL. Vale lo mismo que page y size: refrescar el
   * navegador o compartir el enlace tiene que devolver la misma lista (§8.1 Regla 2).
   */
  const filtro = (nombre) => searchParams.get(nombre) ?? '';

  const cambiarFiltro = (nombre, valor) => {
    setSearchParams((anteriores) => {
      const siguientes = new URLSearchParams(anteriores);
      if (valor) {
        siguientes.set(nombre, valor);
      } else {
        siguientes.delete(nombre);
      }
      // Por el mismo motivo que cambiar el tamaño: con otro filtro, la página 3 puede no existir.
      siguientes.set('page', '0');
      return siguientes;
    });
  };

  return { pagina, tamano, cambiarPagina, cambiarTamano, filtro, cambiarFiltro };
}
