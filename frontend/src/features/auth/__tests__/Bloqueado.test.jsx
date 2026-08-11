import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Bloqueado from '../Bloqueado';

describe('Bloqueado', () => {
  it('muestra el mensaje de sistema bloqueado por licencia vencida', () => {
    render(<Bloqueado />);

    expect(screen.getByText('Sistema bloqueado')).toBeInTheDocument();
    expect(screen.getByText(/licencia/i)).toBeInTheDocument();
  });
});
