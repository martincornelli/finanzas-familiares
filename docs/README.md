# Finanzas Familiares Web

Sitio estatico para publicar con GitHub Pages desde `main` usando la carpeta `/docs`.

## Publicacion

1. En GitHub, abrir `Settings` > `Pages`.
2. Elegir `Deploy from a branch`.
3. Seleccionar branch `main` y carpeta `/docs`.
4. En Firebase Console, abrir Authentication > Settings > Authorized domains y agregar el dominio de GitHub Pages.

La web usa Firebase Auth anonimo y Firestore directo desde el navegador. Para ver la misma base que Android, entrar con el codigo de familia que aparece en la pantalla de configuracion de la app.
