# super-projector

ROM personalizada para un proyector Linkmax (Android con Play Store, marca blanca),
**basada en el firmware de fábrica** para conservar sus drivers (panel, keystone, Wi‑Fi, mando).
Una GSI queda solo como opción al final.

## Reglas

- **Nunca** se flashea ni se escribe una partición sin confirmación explícita del dueño.
- **Nada de `pm uninstall`.** Para probar se usa solo `pm disable-user`, que es reversible.
- **Respaldo antes de cualquier cambio.**
- El repo es **público**: diagnósticos completos, respaldos y firmware **nunca** se suben
  (ver `.gitignore`). Solo se sube la carpeta `compartible/`, que va sin serie, MAC ni IDs.

## Hoja de ruta

- [ ] 1. Diagnóstico por ADB en red (`scripts/01-diagnostico.sh`)
- [ ] 2. Conseguir el firmware base y **comprobar que se puede reinstalar** (modo de flasheo)
- [ ] 3. Respaldo completo, incluidas las particiones únicas (calibración panel/keystone, MAC, claves)
- [ ] 4. Clasificar apps: imprescindibles / reemplazables / fuera (incluido el actualizador OTA)
- [ ] 5. Diseño de la ROM
- [ ] 6. Construcción con scripts reproducibles (Docker para las herramientas Linux)
- [ ] 7. Flasheo y pruebas con checklist
- [ ] 8. Launcher propio
- [ ] 9. Opcional: GSI

## Construcción de la ROM

Solo hace falta `brew install e2fsprogs`. Ningún script de `scripts/rom/` toca el proyector.

```bash
python3 scripts/rom/extract_firmware.py <respaldo> ../work/stock
python3 scripts/rom/build_rom.py scripts/rom/profile-stage1.json ../work/stock ../work/out/stage1
```

`profile-stage1.json` define qué se quita. Cada imagen se relee y se compara con la de fábrica,
incluidos permisos y SELinux, y el resultado es idéntico byte a byte en cada compilación.
Las imágenes aún **no se han probado en el proyector**.

## Paso 1 — Diagnóstico

Requisitos en el Mac:

```bash
brew install android-platform-tools
```

En el proyector: activar Opciones de desarrollador → Depuración ADB (por red / inalámbrica).

```bash
./scripts/01-diagnostico.sh 192.168.1.XX
```

La primera vez, en la pantalla del proyector sale un aviso de autorización: acéptalo y marca
"Permitir siempre". El script solo lee del proyector. La única acción que cambia algo en
tiempo de ejecución es probar `adb root`, que se deshace al final.

Salida en `diagnostico/<fecha>/`:

| Carpeta        | Contenido                                       | ¿Se sube a git? |
|----------------|-------------------------------------------------|-----------------|
| `privado/`     | Todo en bruto (serie, MAC, ajustes, IP)         | **No** (ignorada) |
| `compartible/` | Subconjunto tachado + `RESUMEN.txt`             | Sí, tras revisarla |
