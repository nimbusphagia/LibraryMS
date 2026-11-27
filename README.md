# Red de Gestión Bibliotecaria

Servicio web para administrar una red de bibliotecas desde un solo servidor,
desarrollado para un equipo de bibliotecología que clasifica, organiza y
gestiona las bibliotecas de distintas instituciones.

Cada institución cliente tiene sus propias bibliotecas, su personal, sus
lectores y sus reglas de préstamo, y el equipo trabaja sobre todas ellas con un
catálogo común. El sistema lleva el inventario, la catalogación, los tejuelos y
los préstamos, y expone todo a través de una API REST.

## Contexto

El equipo ofrece la renovación y la gestión de bibliotecas como un servicio:
catalogar la colección, ordenar la estantería, etiquetar los ejemplares y dejar
a la institución lista para prestar. Después, el personal de cada institución
se encarga del día a día y el equipo sigue a cargo de la catalogación y de las
decisiones que afectan a la colección.

Por eso el sistema separa dos niveles de trabajo. El equipo tiene acceso a toda
la red y libertad para configurar esquemas de tejuelos, plantillas y reglas.
El personal de las instituciones solo ve lo suyo y opera dentro de límites
claros: puede prestar, ingresar ejemplares e imprimir tejuelos, pero las
operaciones que cambian la colección quedan reservadas a quien tiene la
formación para hacerlas.

El servicio se puede publicar en internet o en una red privada. Cada
organización queda aislada de las demás, así que un mismo servidor puede
atender a varios equipos sin que compartan datos.

## Funcionalidades

### Organizaciones, instituciones y bibliotecas

Los datos se ordenan en tres niveles. La organización es el equipo que presta
el servicio, la institución es el cliente (por ejemplo, un colegio) y la
biblioteca es cada espacio físico de la institución, que puede tener varias.

Los lectores, sus roles, los grados escolares y las reglas de préstamo
pertenecen a la institución, de modo que un lector puede usar cualquiera de sus
bibliotecas. La organización mantiene plantillas de roles de lector y grados
que se copian a cada institución al crearla y que luego se ajustan por cliente.

Cada institución tiene su propia zona horaria, que se usa para calcular los
vencimientos y los días de retraso.

### Cuentas y roles

Hay cuatro roles:

- **Owner**: responsable de la organización. Se crea al iniciar el servidor,
  administra las cuentas del equipo y puede hacer todo lo que hace un admin.
- **Admin**: miembros del equipo. Trabajan sobre todas las instituciones de la
  organización, mantienen el catálogo compartido y configuran los esquemas de
  tejuelos.
- **Bibliotecario**: encargado de una institución. Cataloga dentro de su
  institución, organiza la estantería y registra entradas, salidas y
  traspasos.
- **Auxiliar**: personal de apoyo de una institución. Presta, devuelve,
  registra lectores, ingresa ejemplares e imprime tejuelos.

Cuando un auxiliar intenta algo que no le corresponde, como dar de baja un
ejemplar, prestar uno dañado o modificar un tejuelo, el sistema rechaza la
operación con un error que indica que debe hacerlo un bibliotecario.

Las sesiones se guardan en la base de datos y quien entra con una contraseña
asignada por otra cuenta debe cambiarla antes de seguir.

### Catálogo compartido y exclusivo

Cada libro se registra una vez con sus autores, editorial, colección, serie,
categoría, subcategoría y clasificación Dewey. El catálogo tiene dos alcances:

- El **compartido** lo mantiene el equipo y lo leen todas las instituciones de
  la organización.
- El **exclusivo** pertenece a una institución, que puede crear y editar sus
  propios registros y usar en ellos los registros compartidos, pero no al
  revés.

Las consultas se pueden filtrar por alcance. Los datos que ya aparecen en
tejuelos impresos no se pueden cambiar desde una institución, para que lo que
está pegado en el lomo siga coincidiendo con el sistema.

El sistema incluye las clases y divisiones del sistema Dewey y la tabla de
Málaga, cargada tal como aparece en su edición impresa.

### Inventario

Cada libro tiene ejemplares físicos en una o más bibliotecas. Cada ejemplar
tiene un número, un estante y un estado: disponible, prestado, perdido,
descartado o traspasado.

Los movimientos de inventario quedan registrados como entradas y salidas
firmadas por quien las hizo:

- **Entradas** por compra o donación.
- **Salidas** por pérdida o descarte. Un descarte manual solo aplica a
  ejemplares que no están en un préstamo.
- **Recuperaciones** de ejemplares perdidos. Los descartados no se recuperan.
- **Traspasos** entre bibliotecas, que crean la entrada correspondiente en la
  biblioteca de destino.

Los daños y reparaciones de cada ejemplar se guardan como un historial aparte.
Un ejemplar dañado solo lo puede prestar un bibliotecario.

### Tejuelos

El formato del tejuelo se define con un esquema: una lista de líneas, cada una
formada por partes como la categoría, el código Dewey, las letras del autor, el
código de Málaga, el título, el volumen de la colección o de la serie y el
número de ejemplar. Cada parte puede ser obligatoria u opcional, editable o
fija, y llevar un prefijo (`v.`, `t.`, `c.`).

El sistema trae dos esquemas de base que el equipo puede copiar y adaptar:

- **Spring**: varias líneas con Dewey, el código de Málaga del autor y los
  volúmenes, para bibliotecas catalogadas por un profesional.
- **Native**: una sola línea generada automáticamente a partir de la
  categoría, el autor, la subcategoría y el título, sin campos editables, para
  bibliotecas sin personal especializado.

Cada biblioteca usa un esquema, y cada libro tiene en ella un solo patrón de
tejuelo que siguen todos sus ejemplares. Si dos libros generarían el mismo
tejuelo, el sistema propone una alternativa. Cuando cambia un dato del catálogo
que forma parte del tejuelo, los ejemplares ya etiquetados quedan marcados
como desactualizados.

El estado de cada ejemplar se calcula a partir de lo que tiene pegado:
pendiente, al día o desactualizado. Las etiquetas de los ejemplares elegidos se
imprimen en PDF, en hojas A4 de 24 tejuelos, y luego se marcan como colocadas.

### Préstamos

Las reglas se configuran por rol de lector: días de préstamo, cantidad máxima
de ejemplares a la vez, cantidad de préstamos vencidos que bloquea nuevos
préstamos y días de gracia antes de contar un retraso. Un rol también puede
exigir que el lector tenga un grado escolar.

Cada ejemplar de un préstamo se resuelve por separado como devuelto, devuelto
dañado, perdido o descartado. Una pérdida o un descarte generan la salida de
inventario correspondiente. Un lector con préstamos abiertos no se puede
desactivar.

### Reputación de lectores

A partir de los últimos 12 meses de préstamos, el sistema clasifica a cada
lector como nuevo, confiable, regular o en riesgo. Un retraso suma un punto, un
daño o un descarte dos y una pérdida cuatro, y el puntaje se divide por la
cantidad de préstamos resueltos, así que un retraso pesa más para quien pidió
dos libros que para quien pidió cuarenta.

Además de los préstamos, cada institución define tipos de incidencia (por
ejemplo, mal comportamiento en la sala) con su propio peso. Las incidencias
graves cuentan como una pérdida para decidir si un lector está en riesgo.

## Aspectos técnicos

El servicio está hecho con Spring Boot 4.1 sobre Java 25, con PostgreSQL 18 como
base de datos y Flyway para las migraciones. La API se documenta con OpenAPI y
Swagger UI, y los PDF de tejuelos se generan con PDFBox.

El código se organiza por área (`account`, `organization`, `catalog`,
`inventory`, `loan`). Cada área expone una interfaz pública que usan las demás
y guarda en `internal/` sus controladores, servicios, entidades y
repositorios.

El aislamiento entre organizaciones e instituciones se resuelve en dos capas:
un interceptor comprueba que cada ruta bajo una institución o biblioteca
pertenezca al alcance de la cuenta, y las claves foráneas compuestas impiden en
la base de datos que un registro apunte a otra organización.

Las reglas importantes también están en la base de datos, con restricciones
`CHECK` e índices únicos. La base usa dos usuarios: `migrator`, dueño del
esquema, que solo usa Flyway, y `app_user`, con permisos de lectura y escritura
para la aplicación. Las sesiones se guardan con Spring Session JDBC, en cookies
`HttpOnly` y `SameSite=Strict`.

## Ejecutar el proyecto

Requiere Docker. Primero se copian las variables de entorno y se completan las
contraseñas:

    cp .env.example .env
    docker compose up --build

La API queda en `http://localhost:8080` y la documentación en
`http://localhost:8080/swagger-ui.html`. En el primer inicio se crean la
organización y la cuenta owner con los datos de `.env`, y Flyway carga los
datos de referencia (Dewey, tabla de Málaga y esquemas de tejuelos).

`REGISTRATION_OPEN=true` permite que cualquiera cree su propia organización
desde `/api/auth/register`. Solo se usa para la demo pública.

Las pruebas levantan su propia base PostgreSQL con Testcontainers, así que solo
necesitan Docker en ejecución:

    ./mvnw test
