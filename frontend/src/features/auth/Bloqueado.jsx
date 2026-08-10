import estilos from './Auth.module.css';

/** RN-20: se muestra cuando el cliente HTTP intercepta un 423 (licencia vencida). */
export default function Bloqueado() {
  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Sistema bloqueado</h1>
        <p>
          La licencia de este laboratorio está vencida. Contactá al SuperAdmin para regularizar la
          situación; mientras tanto, el sistema no puede operar.
        </p>
      </div>
    </div>
  );
}
