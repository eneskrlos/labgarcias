import estilos from './CampoFormulario.module.css';

/** spec.md §8.1 Regla 4: etiqueta, control, texto de ayuda (RN-xx) y mensaje de error del campo. */
export function CampoFormulario({ id, etiqueta, ayuda, error, children }) {
  return (
    <div className={estilos.campo}>
      <label htmlFor={id}>{etiqueta}</label>
      {children}
      {ayuda && <p className={estilos.ayuda}>{ayuda}</p>}
      {error && <p className={estilos.error}>{error}</p>}
    </div>
  );
}
