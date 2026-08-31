import { describe, expect, it } from 'vitest';
import { colorPorEstado } from '../coloresEstado';

/** RN-04/§4.2: los siete códigos que trae la migración V1, ninguno más. */
const CODIGOS_ESTADO = [
  'RECIBIDO',
  'EN_EVALUACION',
  'EN_PRODUCCION',
  'CONTROL_CALIDAD',
  'LISTO',
  'ENTREGADO',
  'CANCELADO',
];

describe('colorPorEstado', () => {
  it.each(CODIGOS_ESTADO)('define un color para %s', (codigo) => {
    const { fondo, texto } = colorPorEstado(codigo);
    expect(fondo).toBeTruthy();
    expect(texto).toBeTruthy();
  });

  it('CANCELADO no reutiliza --color-error (docs/ESTADO.md, 26/08/2026)', () => {
    expect(colorPorEstado('CANCELADO')).toEqual({ fondo: '#F6E2E1', texto: '#A03A32' });
  });

  it('un código desconocido no rompe: devuelve un color neutro', () => {
    expect(colorPorEstado('CODIGO_INEXISTENTE')).toEqual({
      fondo: 'var(--color-borde)',
      texto: 'var(--color-texto-tenue)',
    });
  });
});
