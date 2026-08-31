/**
 * Iconografía del cromo: un trazo SVG uniforme, sin dependencias ni librería de iconos
 * (`Agente.md` §3.4 y §5.7). El lenguaje visual sale del prototipo de `docs/prototipo/`.
 *
 * **Solo están los iconos que hoy se usan** —los dos menús de §8, la campana de §6.4 y el
 * cierre de sesión de CU-14—. `Agente.md` §6.2 prohíbe las abstracciones sin uso actual: los
 * demás entran cuando su pantalla los necesite, no antes.
 *
 * `aria-hidden` es deliberado: el icono acompaña a una etiqueta de texto que ya nombra al
 * enlace o al botón. Anunciarlo duplicaría el nombre accesible.
 */
const TRAZOS = {
  /* Menú del odontólogo (§8) */
  inicio: 'M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z',
  trabajos: 'M3 8h18v12H3zM8 8V5h8v3M3 13h18',
  historial: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M12 7.5V12l3 2',
  perfil: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4.5 20.5c0-4 3.4-6 7.5-6s7.5 2 7.5 6',

  /* Menú del laboratorio (§8) */
  panel: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  odontologos:
    'M7 4c-2.5 0-4 2-4 5s1 8 3 11c1 1.5 2.5 1 3-1l1.5-4h1l1.5 4c.5 2 2 2.5 3 1 2-3 3-8 3-11s-1.5-5-4-5c-1.5 0-2.5 1-4 1s-2.5-1-4-1z',
  solicitudes: 'M3 6h18v12H3zM3 7l9 6 9-6',
  tiposTrabajo: 'M3 8h18v12H3zM3 8l3-4h12l3 4M12 4v16',
  configuracion:
    'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6M12 2.5v2.6M12 18.9v2.6M2.5 12h2.6M18.9 12h2.6M5.2 5.2l1.9 1.9M16.9 16.9l1.9 1.9M18.8 5.2l-1.9 1.9M7.1 16.9l-1.9 1.9',
  usuarios:
    'M9 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M2 20c0-3.6 3-5.5 7-5.5s7 1.9 7 5.5M17 5.5a3.5 3.5 0 0 1 0 7M18 20c0-2.6-.8-4.2-2-5',
  licencias: 'M6 3h8l4 4v14H6zM14 3v4h4',

  /* Encabezado */
  campana: 'M18 9a6 6 0 0 0-12 0c0 6-2 7-2 7h16s-2-1-2-7M10.5 20a2 2 0 0 0 3 0',
  cerrarSesion: 'M9 21H4V3h5m7 13 5-4-5-4m5 4H9',
};

export function Icono({ nombre, tamano = 17 }) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={TRAZOS[nombre]} />
    </svg>
  );
}
