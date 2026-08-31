-- CU-10/§5.7, etapa 2 del rediseño (bloque 4): la vista suma el código de la etapa.
--
-- `v_ordenes_urgentes` solo exponía `e.nombre AS estado`. El bloque de urgentes del dashboard es
-- el único de los tres bloques de resumen que no puede colorear su etiqueta por `estadoCodigo`
-- (RECIBIDO/EN_EVALUACION/EN_PRODUCCION/CONTROL_CALIDAD/LISTO/ENTREGADO/CANCELADO): los otros dos
-- (proximasAEntregar, ordenesRecientes) ya lo tienen desde el bloque 0.
--
-- La columna nueva va al final: `CREATE OR REPLACE VIEW` no admite reordenar ni quitar columnas
-- existentes. No se toca ningún dato: es una vista, se redefine, no migra filas.
CREATE OR REPLACE VIEW v_ordenes_urgentes AS
SELECT o.id, o.codigo, o.paciente_nombre, o.fecha_estimada_entrega,
       u.nombre_completo AS odontologo,
       e.nombre          AS estado,
       e.codigo          AS estado_codigo
FROM orden o
JOIN tipo_orden t ON t.id = o.tipo_orden_id
JOIN usuario    u ON u.id = o.odontologo_id
JOIN estado     e ON e.id = o.estado_id
WHERE t.codigo = 'URGENTE'
  AND e.es_terminal = FALSE;
