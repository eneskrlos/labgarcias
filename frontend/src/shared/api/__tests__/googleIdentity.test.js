import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderizarBotonGoogle } from '../googleIdentity';

describe('renderizarBotonGoogle', () => {
  let renderButton;

  beforeEach(() => {
    renderButton = vi.fn();
    window.google = { accounts: { id: { renderButton } } };
  });

  afterEach(() => {
    delete window.google;
  });

  it('no hace nada si no hay contenedor', () => {
    renderizarBotonGoogle(null, 300);
    expect(renderButton).not.toHaveBeenCalled();
  });

  it('no hace nada si el SDK de Google todavía no cargó', () => {
    delete window.google;
    renderizarBotonGoogle({}, 300);
    expect(renderButton).not.toHaveBeenCalled();
  });

  it('usa el ancho disponible cuando está dentro del rango permitido', () => {
    const contenedor = {};
    renderizarBotonGoogle(contenedor, 300);
    expect(renderButton).toHaveBeenCalledWith(contenedor, expect.objectContaining({ width: 300 }));
  });

  it('no baja de 200px aunque el contenedor sea más angosto (caso reportado: pantallas < 320px)', () => {
    const contenedor = {};
    renderizarBotonGoogle(contenedor, 150);
    expect(renderButton).toHaveBeenCalledWith(contenedor, expect.objectContaining({ width: 200 }));
  });

  it('no supera los 400px aunque el contenedor sea más ancho (tope documentado por Google)', () => {
    const contenedor = {};
    renderizarBotonGoogle(contenedor, 900);
    expect(renderButton).toHaveBeenCalledWith(contenedor, expect.objectContaining({ width: 400 }));
  });

  it('redondea anchos con decimales', () => {
    const contenedor = {};
    renderizarBotonGoogle(contenedor, 237.6);
    expect(renderButton).toHaveBeenCalledWith(contenedor, expect.objectContaining({ width: 238 }));
  });
});

describe('iniciarGoogle', () => {
  beforeEach(() => {
    vi.resetModules();
    document.body.innerHTML = '';
    delete window.google;
  });

  it('agrega el script del SDK una sola vez e inicializa con el clientId y el callback', async () => {
    const { iniciarGoogle } = await import('../googleIdentity');
    const credencialRecibida = vi.fn();

    const promesa = iniciarGoogle({ clientId: 'client-123', alObtenerCredencial: credencialRecibida });

    const script = document.getElementById('google-identity-services');
    expect(script).not.toBeNull();
    expect(script.src).toBe('https://accounts.google.com/gsi/client');

    const initialize = vi.fn();
    window.google = { accounts: { id: { initialize } } };
    script.onload();
    await promesa;

    expect(initialize).toHaveBeenCalledTimes(1);
    const opciones = initialize.mock.calls[0][0];
    expect(opciones.client_id).toBe('client-123');

    opciones.callback({ credential: 'id-token-simulado' });
    expect(credencialRecibida).toHaveBeenCalledWith('id-token-simulado');
  });
});
