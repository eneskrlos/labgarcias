import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { CampoFormulario } from '../CampoFormulario';

describe('CampoFormulario', () => {
  it('asocia la etiqueta con el control por id', () => {
    render(
      <CampoFormulario id="nombre" etiqueta="Nombre">
        <input id="nombre" />
      </CampoFormulario>,
    );

    expect(screen.getByLabelText('Nombre')).toBeInTheDocument();
  });

  it('muestra el texto de ayuda (RN-12/RN-21) cuando se provee', () => {
    render(
      <CampoFormulario id="diasEstimados" etiqueta="Días estimados" ayuda="RN-12: mínimo 7 días hábiles.">
        <input id="diasEstimados" />
      </CampoFormulario>,
    );

    expect(screen.getByText('RN-12: mínimo 7 días hábiles.')).toBeInTheDocument();
  });

  it('muestra el mensaje de error del campo cuando se provee', () => {
    render(
      <CampoFormulario id="precio" etiqueta="Precio" error="El precio debe ser al menos 250.">
        <input id="precio" />
      </CampoFormulario>,
    );

    expect(screen.getByText('El precio debe ser al menos 250.')).toBeInTheDocument();
  });

  it('sin ayuda ni error, no renderiza esos párrafos', () => {
    render(
      <CampoFormulario id="nombre" etiqueta="Nombre">
        <input id="nombre" />
      </CampoFormulario>,
    );

    expect(screen.queryByText(/RN-/)).not.toBeInTheDocument();
  });
});
