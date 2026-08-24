import estilos from './TarjetaContador.module.css';

/**
 * CU-02/CU-10: un indicador de panel — un número grande con su etiqueta.
 *
 * Vive en `shared/` porque los dos paneles muestran la misma fila de indicadores y §8.1 Regla 4
 * pide que la uniformidad no dependa de la disciplina. **La cifra llega hecha del backend**: acá
 * no se suma, promedia ni deriva nada (§8, `Agente.md` §6.1).
 *
 * `cargando` dibuja un hueco del mismo alto que el número para que la fila no salte cuando llega
 * la respuesta (§8.1 Regla 3).
 */
export function TarjetaContador({ etiqueta, valor, cargando = false }) {
  return (
    <div className={estilos.tarjeta}>
      {cargando ? (
        <span className={estilos.esqueleto} aria-hidden="true" />
      ) : (
        <strong className={estilos.valor}>{valor}</strong>
      )}
      <span className={estilos.etiqueta}>{etiqueta}</span>
    </div>
  );
}

/** La fila de indicadores del panel. Existe para que los dos paneles la dispongan igual. */
export function FilaContadores({ children }) {
  return <div className={estilos.fila}>{children}</div>;
}
