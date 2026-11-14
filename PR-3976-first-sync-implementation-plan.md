# PR 3976: refinamiento final del plan para el primer Sync

Fecha de revision: 2026-11-13. Estado: diagnostico, plan e implementacion local; no se hicieron commits.

## 1. Resumen ejecutivo

**Causa:** el primer `Sync` puede confirmar la clave publica y devolver `UNKNOWN` antes de entregar un token util. El flujo original solo hacia un `Sync` por invocacion; la siguiente ejecucion, ya autenticada con la clave reconocida, podia verificar el numero. Por eso el primer callback podia ser exitoso como RPC pero no contener un MSISDN token. El reporte de la segunda ejecucion respalda esta secuencia, pero no demuestra que la falta de autenticacion inicial sea la unica causa posible del `UNKNOWN`.

**Implementacion local:** modelar el ack con nombres semanticos sin cambiar los tags wire; persistirlo solo si corresponde a la clave enviada; y, ante un ack valido nuevo con respuestas raw exclusivamente `UNKNOWN`, construir un request autenticado nuevo y repetir `Sync` una sola vez. Procesar/cachear numeros una vez desde el resultado final, sin modificar callback ni status publico. La suite local completa pasa (130 tests); Kotlin, AIDL y ensamblado del modulo tambien pasan. Solo quedan verificaciones externas con Messages/backend y dispositivo real.

No asignar un `Status` reintentable nuevo: `Status.SUCCESS` indica exito de la operacion; los estados por SIM estan en `VerifyPhoneNumberResponse`. El repo no demuestra que un codigo de error existente provoque un retry util en Messages.

## 2. Diagnostico validado

### Hechos confirmados por el codigo

1. `runVerificationFlow` lee `isPublicKeyAcked` y lo pasa a `SyncRequest` como `includeClientAuth` ([VerifyPhoneNumber.kt:355-418](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt)).
2. `ClientInfoBuilder` incluye la clave publica en `ClientInfo` incluso sin `client_auth`. `RequestHeader` solo crea `ClientAuth` cuando `includeClientAuth` es true; entonces firma el IID token y serializa el dispositivo, la firma y el timestamp ([ClientInfoBuilder.kt:35-54](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/proto/builder/ClientInfoBuilder.kt), [CommonBuilders.kt:45-68](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/proto/builder/CommonBuilders.kt), [SyncRequestBuilder.kt:140-220](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/proto/builder/SyncRequestBuilder.kt)).
3. La clave esta guardada en `constellation_prefs`. Regenerar el par escribe `is_public_key_acked = false`; `isPublicKeyAcked` fuerza la carga/validacion del par antes de leer el flag ([AuthManager.kt:63-103](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/AuthManager.kt), [ConstellationStateStore.kt:131-139](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/ConstellationStateStore.kt)).
4. `executeSyncFlow` interpreta `SyncResponse.client_auth_status.public_key_status.status == 1` como ack, comprueba session id si viene informado y lo persiste atomicamente solo si la clave actual coincide con la enviada. Si la clave era nueva y todos los resultados raw son `UNKNOWN`, crea un `Sync` autenticado adicional, como maximo una vez; el procesamiento de numeros ocurre una vez desde el resultado final ([VerifyPhoneNumber.kt:485-645](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt), [AuthManager.kt:107-118](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/AuthManager.kt)).
5. El estado se deriva de la presencia de `verification_info`, `pending_verification_info` o `unverified_info`; ausencia de los tres da `UNKNOWN`. `UNKNOWN` se traduce a status por-SIM `0`, distinto de `PENDING` (`6`) y de los estados explicitamente no verificados. `includeUnverified=false` filtra todos los estados no `VERIFIED` antes de formar el array de callback ([VerificationMappings.kt:28-50,53-110,144-159](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerificationMappings.kt), [VerifyPhoneNumber.kt:541-557](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt)).
6. Para el flujo completo, el handler asigna `Status.SUCCESS` si no hubo excepcion. El boolean local `successful` exige verificaciones presentes y todas `VERIFIED`, pero solo se escribe en settings; no altera el status publico. El callback publico transporta ambos conceptos por separado: `Status` y `VerifyPhoneNumberResponse` ([VerifyPhoneNumber.kt:190-287](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt), [IConstellationCallbacks.aidl:10-13](play-services-constellation/src/main/aidl/com/google/android/gms/constellation/internal/IConstellationCallbacks.aidl)).
7. El `Status` local define `isSuccess()` como status code <= 0 y `SUCCESS` como 0. `5007`-`5009` aparecen en el mapper de errores RPC, pero el repo no define que signifiquen retry inmediato ni que Messages los reintente ([Status.java](play-services-basement/src/main/java/com/google/android/gms/common/api/Status.java), [CommonStatusCodes.java:26-44](play-services-basement/src/main/java/com/google/android/gms/common/api/CommonStatusCodes.java), [VerifyPhoneNumber.kt:342-352](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt)).
8. Una excepcion RPC no debe convertirse en retry de handshake. La cancelacion se vuelve a propagar; el dispatcher cancela el request al morir el Binder del caller. Hay una unica entrega de callback al final del handler ([VerifyPhoneNumber.kt:247-260](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt), [ConstellationRequestDispatcher.kt:45-79](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/ConstellationRequestDispatcher.kt)).
9. El response tambien puede actualizar tokens de verificacion, token DroidGuard y `next_sync_time`. El estado local de numeros verificados se reemplaza por IMSI dentro del procesamiento de verificaciones; procesar dos veces un response desconocido podria borrar un cache previo antes de que el segundo response lo reponga ([ConstellationStateStore.kt:74-83,237-260](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/ConstellationStateStore.kt), [VerifyPhoneNumber.kt:507-590](play-services-constellation/core/src/main/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumber.kt)).

### Hechos observados por el tester

- En el primer intento se registro una respuesta con una verificacion `UNKNOWN` y el log local “Server acknowledged client public key”; Messages no obtuvo el MSISDN token y entro en un backoff aproximado de dos horas.
- Ejecutar de nuevo el provisioning poco despues, con la clave ya reconocida, verifico el numero y conecto RCS.
- La misma prueba reporto que, luego de corregir la configuracion Cloud Messaging, IID/Asterism funciono. Eso no vincula la falla inicial con IID.

Son observaciones del reporte, no una reproduccion independiente en este entorno.

## 3. Evidencias de codigo y protocolo

### Lo que implementa el esquema local

`constellation.proto` ahora declara `ClientAuthStatus` en el campo wire 3 de `SyncResponse` y en el campo wire 2 de `ProceedResponse`, con `PublicKeyStatus` anidado. Se conservaron los tags y la estructura wire que antes se interpretaba como `ResponseHeader.status.code`; el test nuevo comprueba decode de ambos campos. La semantica del campo sigue apoyada en evidencia reverse-engineered, no en una especificacion oficial ([constellation.proto:26-34,221-238](play-services-constellation/core/src/main/proto/constellation.proto), [VerifyPhoneNumberSyncRetryTest.kt](play-services-constellation/core/src/test/kotlin/org/microg/gms/constellation/core/VerifyPhoneNumberSyncRetryTest.kt)).

### Evidencia secundaria externa

El esquema reverse-engineered [phonedeviceverification_named.proto](https://github.com/doug-leith/android-protobuf-decoding/blob/c869440ae43bf2bd28acf003023e29285db4dfe5/phonedeviceverification_named.proto) describe, para GMS 24.06.16, `SyncResponse.client_auth_status = 3`, `ClientAuthStatus.public_key_status = 1` y `PublicKeyStatus.status = 1` como acuse de clave. Sus nombres/semantica estan estimados a partir de un APK, no son documentacion oficial. Tambien indica que el `ClientAuth` del request se agrega despues de reconocer la clave.

El anidamiento de ese esquema parece compatible con la forma wire que el `.proto` local interpreta como `header.status.code`; por eso el valor no parece ser simplemente un status generico de la llamada. La compatibilidad y la semantica exacta siguen siendo **PROBABLES** hasta verificar la version real de Messages/backend con un fixture o una captura controlada. El esquema tambien atribuye `next_sync` a la programacion del siguiente sync, lo que deja pendiente si un segundo sync inmediato respeta todas las politicas del servidor.

## 4. Hipotesis no verificadas

| Afirmacion | Clasificacion | Evidencia / limite |
|---|---|---|
| El primer request no incluye `client_auth` cuando el flag local es falso. | **CONFIRMADA** | Condicion en `VerifyPhoneNumber.kt`; builder asigna `client_auth = null` si no se incluye. |
| El campo de respuesta interpretado como `code == 1` significa que el servidor reconoce la clave publica. | **PROBABLE** | Codigo y log local, mas esquema reverse-engineered; falta contrato oficial/captura wire independiente. |
| La ausencia inicial de `client_auth` causa el `UNKNOWN` reportado. | **PROBABLE** | La segunda ejecucion con clave reconocida tuvo exito; `UNKNOWN` puede tener otras causas. |
| El servidor admite otro `Sync` inmediato con autenticacion y sin rate limit. | **PROBABLE** | La segunda ejecucion manual inmediata funciono; no prueba un retry en la misma sesion ni todas las politicas `next_sync_time`. |
| Google Messages reintentaria antes si microG devolviera un status de error. | **PENDIENTE** | El consumer de Messages no esta en el repo y no se conoce su tabla de retries. |
| `Status.SUCCESS` en este callback exige tener MSISDN token. | **PENDIENTE / no respaldada** | El contrato local separa status de respuesta; no hay consumer para verificar como lo usa Messages. |

El mensaje “Server acknowledged client public key” no es por si solo evidencia independiente: hoy lo emite el propio codigo al ver el valor numerico `1`.

## 5. Comparacion de alternativas

| Alternativa | Ventajas | Riesgos / limites | Decision |
|---|---|---|---|
| **A. Segundo `Sync` inmediato y condicionado** | Resuelve en la misma llamada el handshake observado; conserva la API publica; el tester ya observo que una segunda ejecucion inmediata puede funcionar. | Requiere validar la semantica del status y el efecto de `next_sync_time`; puede repetir telemetria/trabajo del servidor; riesgo en concurrencia. | **Recomendada PROBABLE**, detras de los criterios estrictos de la seccion 8 y de validacion de protocolo. Maximo una vez. |
| **B. No repetir; exponer solamente el estado correcto** | Menor riesgo de trafico duplicado y respeta que `Status.SUCCESS` signifique RPC exitoso; no inventa errores. | No obtiene token en el primer intento ni garantiza salir del backoff de Messages; con `includeUnverified=false`, el cliente puede recibir array vacio. | Fallback si A no supera la validacion externa. No cambiar `Status` publico sin evidencia de contrato. |
| **C. Esperar al siguiente sync programado por el servidor** | Potencialmente respeta `next_sync_time` y evita una llamada inmediata. | El repo no implementa un scheduler que garantice invocacion; puede conservar el backoff largo observado. El significado de `next_sync_time` requiere validacion. | Solo si la respuesta establece una fecha minima y el backend rechaza/recomienda no hacer el retry inmediato. |

## 6. Decisiones de arquitectura

- **[IMPLEMENTADO, validacion externa pendiente] Retry A:** un segundo `Sync` solo con ack explicito, clave coincidente, primera llamada no autenticada, resultados raw exclusivamente `UNKNOWN` y ausencia de errores; maximo uno. El builder existente crea un request fresco autenticado. `next_sync_time` todavia debe validarse con el backend.
- **[CONFIRMADA] No confundir estados:** exito RPC, acuse de clave y verificacion de numero son estados diferentes; persistirlos por separado.
- **[CONFIRMADA] Mantener intacta el API AIDL:** no cambiar parametros ni estructura publica del callback.
- **[CONFIRMADA] Mantener `Status.SUCCESS` como exito de RPC:** el resultado de verificacion vive en `VerifyPhoneNumberResponse`; no se ha probado que Messages espere un error para reintentar.
- **[IMPLEMENTADO, semantica externa pendiente] Corregir el nombre wire del status de clave:** modelar `ClientAuthStatus/PublicKeyStatus` con los tags actuales; el fixture local comprueba decode wire, pero no confirma por si solo el significado del campo en el backend.
- **[PENDIENTE externo] Reintento frente a `next_sync_time`:** el esquema reverse-engineered indica que el cliente lo usa para programar el proximo sync, no prueba que sea un hard not-before del backend. Confirmar con una traza controlada antes del rollout; el codigo no lo usa como gate.
- **[CONFIRMADA] No repetir desafios ni estados conocidos:** `PENDING`, `VERIFIED`, `NONE` con razon explicita, errores, respuestas vacias y resultados mixtos no disparan el retry global.
- **[CONFIRMADA] No registrar datos sensibles:** logs solo de transiciones/estados, nunca firmas, tokens, numeros, IMSI o ICCID.

No se recomienda mapear `UNKNOWN` a `Status(5007)`, `INTERNAL_ERROR` ni otro error. El nombre del codigo no prueba comportamiento de retry de Messages y cambiaria el significado de una llamada RPC que si se completo.

## 7. Maquina de estados

Conceptos independientes:

- **Exito RPC:** `Sync` devolvio response sin error de transporte/protocolo.
- **Exito de handshake:** response contiene un acuse valido de la clave enviada y se persiste asociado a esa misma clave.
- **Exito de verificacion:** el resultado final por SIM es `VERIFIED` y contiene los datos/token validos. No se infiere del acuse ni del exito RPC.

| Clave antes | Response inicial relevante | Accion | Estado persistido | Callback |
|---|---|---|---|---|
| Reconocida | Cualquier estado | Flujo existente; no retry por handshake. | Mantener acuse de la clave actual. | `SUCCESS` solo significa RPC exitoso; respuesta por SIM sin cambios. |
| No reconocida | Sin acuse, acuse ausente/invalido o de otra clave | No retry; no persistir acuse. | Mantener no reconocida. | Usar la respuesta recibida y el status actual de RPC. |
| No reconocida | Acuse nuevo valido + todos los resultados targeteados son `UNKNOWN` raw | Persistir acuse de esa clave; construir un `Sync` nuevo autenticado; maximo una vez. | Clave reconocida; numeros se actualizan desde el resultado final, no desde el intermedio. | Callback unico con resultado final; `SUCCESS` si ambos RPC terminaron, estado `UNKNOWN` sigue siendo visible si aplica. |
| No reconocida | Acuse valido + `VERIFIED`, `PENDING` o estado explicito `NONE` | No retry. Procesar `PENDING` con `ChallengeProcessor` como hoy. | Acuse reconocido; persistir numeros solo desde resultado final. | Mantener status y detalle por SIM actuales. |
| No reconocida | Acuse valido + mezcla de `UNKNOWN` con un estado conocido | No retry global en la primera version; no repetir SIMs con desafios/resultado. | Acuse reconocido; conservar todos los resultados elegibles. | Callback con la mezcla sin perder la informacion conocida. |
| No reconocida | Response vacio | No retry: falta evidencia de que `UNKNOWN` fue la causa; decision conservadora. | Persistir solo acuse valido y metadatos de response. | `SUCCESS` de RPC y array acorde al contrato. |
| Cualquiera | Error RPC, error por-verificacion, timeout o cancelacion | No retry de handshake. Aplicar rutas de error/cancelacion existentes. | Acuse solo si se valido antes del fallo del segundo RPC; no marcar numero verificado. | Error existente o ninguna entrega ante cancelacion; nunca dos callbacks. |
| Cualquiera | Segundo response `UNKNOWN` o vacio | Terminar; no encadenar mas requests. | Acuse permanece si fue valido; `successful=false`. | No inventar status nuevo; respetar `includeUnverified` y reportar `UNKNOWN` cuando el contrato lo pide. |

`Verification.State.UNKNOWN` es la condicion de retry propuesta. No usar `effectiveStatus == STATUS_UNKNOWN` como equivalente: `State.NONE` con `UnverifiedInfo.Reason.UNKNOWN_REASON` es un resultado explicito y no debe reintentarse como handshake.

## 8. Diseño detallado de implementacion

### Archivos/funciones

1. **Proto y ack — implementado:** modelar `ClientAuthStatus`/`PublicKeyStatus` conservando tags; comprobar decode wire para `SyncResponse` y `ProceedResponse`; asociar ack atomicamente a la clave que recibio la respuesta.
2. **Flujo Sync — implementado localmente:** separar RPC y procesamiento final; persistir metadatos intermedios; construir un `Sync` autenticado nuevo solo bajo la condicion estricta; no cambiar el callback publico ni el `Status`.
3. **Pruebas — agregadas, ejecucion pendiente:** seam interno para simular los RPC y builder del retry. Los tests cubren retry exitoso/persistente `UNKNOWN`, ack ausente o de otra sesion, estados `VERIFIED`/`PENDING`/mixtos, clave distinta, wire decode y cancelacion. Falta validar el builder real en compilacion/dispositivo.

### Pseudocodigo

```text
keySnapshot = cargar clave actual y su identidad
hadAck = leer ack asociado a keySnapshot
firstRequest = construir Sync fresco(includeClientAuth = hadAck)
first = ejecutar Sync (propagar excepcion/cancelacion por la ruta existente)
guardar metadatos del response que el segundo request necesita (tokens/DroidGuard)

ackValid = response contiene PublicKeyStatus ACK para esta respuesta
            AND la clave actual sigue siendo keySnapshot
if ackValid:
    persistir ack para keySnapshot

if not hadAck
   AND ackValid
   AND hay resultados y todos son Verification.State.UNKNOWN
   AND los resultados cubren los IMSI solicitados
   AND no hay error, PENDING, VERIFIED ni estado explicito NONE
   AND coroutine sigue activa
       secondRequest = reconstruir Sync fresco(includeClientAuth = true)
       verificar que ClientAuth existe y firma con keySnapshot
       second = ejecutar Sync una sola vez
       final = mergear resultados por IMSI; conservar del primero solo
               los IMSI que falten en el segundo
else:
       final = first

procesar desafios/cache de numeros una sola vez desde final
mapear una sola respuesta publica; no derivar Status del ack de clave
```

El codigo permite actualmente el segundo `Sync` sin una compuerta de `next_sync_time`. El reporte de una segunda ejecucion manual inmediata exitosa es evidencia de que el backend acepto ese flujo en ese caso, pero no prueba que un retry en la misma invocacion respete todas las politicas. Validar esta condicion antes de produccion y revertir a la alternativa B/C si el servidor impone una espera. El response intermedio guarda metadatos antes del retry, pero el cache de numeros se modifica una sola vez desde el resultado final.

### Concurrencia y atomicidad

- No agregar un lock global: el dispatcher permite requests concurrentes y serializar todas las verificaciones podria bloquear llamadas no relacionadas.
- Capturar la identidad/public key del primer request; antes de persistir el ack y antes de firmar el retry, verificar que no hubo regeneracion. La prueba existente confirma que un par corrupto invalida el ack; no cubre rotacion concurrente.
- Garantizar **un retry por invocacion**, no necesariamente uno total entre invocaciones concurrentes. Dos jobs simultaneos pueden producir dos retries; no hay evidencia de que sea un caso habitual ni de que el backend los rechace. Cubrir la intercalacion y dejar este riesgo documentado; introducir single-flight por clave solo si la prueba demuestra un problema real.
- Mantener el mismo `sessionId` del flujo para correlacionar el retry, pero reconstruir el request completo. Validar esta reutilizacion con el contrato/captura; si el servidor usa el identificador como clave de idempotencia para `Sync`, resolverlo antes del cambio.

## 9. Matriz exhaustiva de pruebas deterministas

En las filas, `N` significa request sin `client_auth`, `A` con `client_auth`, `S` callback `Status.SUCCESS` por RPC exitoso y `E` el status producido por el mapper de error existente. Los detalles por SIM respetan `includeUnverified`. `ack=true/false` y `successful=true/false` son persistencias distintas.

| # | Estado inicial y responses simulados | Sync esperados / auth | Persistencia final | Callback e invariantes |
|---|---|---|---|---|
| 1 | Clave no reconocida; first ack valido + un `UNKNOWN`; second `VERIFIED` con token. | 2: `N`, luego `A`. | `ack=true`, numero/token final, `successful=true`. | Una llamada; `S`; devuelve token final. |
| 2 | Igual, second solo `UNKNOWN`. | 2: `N`, `A`. | `ack=true`, sin cache de numero nuevo, `successful=false`. | Una llamada; `S` de RPC y `UNKNOWN` visible si se incluye. Sin tercer request ni codigo inventado. |
| 3 | Clave no reconocida; response sin acuse. | 1: `N`. | `ack=false`. | Una llamada; status/estado segun response; no retry. |
| 4 | Campo de ack ausente, desconocido o valor distinto de ack. | 1: `N`. | `ack=false`. | No interpretar status generico/error como acuse. |
| 5 | Response con ack que no corresponde a la sesion/request, si el campo de sesion se confirma en el esquema. | 1: `N`. | `ack=false`. | No usar ack no correlacionado; fixture cubre el campo. |
| 6 | Clave ya reconocida; respuesta `UNKNOWN`. | 1: `A`. | `ack=true`, verificacion no exitosa. | `S` por RPC, detalle `UNKNOWN`; no retry por handshake. |
| 7 | Clave no reconocida; primera respuesta `VERIFIED`. | 1: `N`. | Ack valido se guarda; numero/token, `successful=true`. | Una llamada, `S`; no segunda llamada. |
| 8 | Primera respuesta `PENDING` con desafio. | 1 `Sync: N`, luego solo los `Proceed` que el procesador actual requiera. | Ack valido; estado final de desafio segun flujo actual. | No segundo `Sync`; un callback; no duplicar desafio. |
| 9 | Primera respuesta `NONE`/terminal (`FAILED`, `THROTTLED`, `DENIED`, `INELIGIBLE`, etc.). | 1: `N`. | Ack si es valido; sin numero exitoso. | `S` por RPC y status por-SIM/retryAfter preservados; no retry. |
| 10 | Response sin registros, con ack valido. | 1: `N`. | Ack/meta persistidos; numeros sin cambio. | `S`, response vacio; sin retry por falta de evidencia. |
| 11 | Resultados mixtos: `UNKNOWN` + `VERIFIED`. | 1: `N`. | Ack valido y solo cache final; conserva el verificado. | No repetir todos los SIMs; callback conserva el resultado util. |
| 12 | Resultados mixtos: `UNKNOWN` + `PENDING` o terminal. | 1: `N`, mas `Proceed` si corresponde. | Sin cache erroneo; estado terminal/desafio intacto. | No retry global; no perder ni duplicar estados. |
| 13 | `includeUnverified=true`; raw first `UNKNOWN` + ack, second verified. | 2: `N`, `A`. | Como #1. | La decision de retry usa el raw response; resultado final verificado. |
| 14 | `includeUnverified=false`; raw first `UNKNOWN` + ack, second verified. | 2: `N`, `A`. | Como #1. | El filtrado no oculta el trigger; callback final incluye solo lo permitido. |
| 15 | Primer RPC arroja `GrpcException`/error de transporte. | 1 intento `N`. | No ack nuevo ni numero exitoso. | `E` segun mapper; no retry de handshake. |
| 16 | Segundo RPC arroja `GrpcException`. | 2: `N`, `A`. | Ack del primer response queda; `successful=false`. | Una entrega `E`; ningun callback parcial exitoso. |
| 17 | Timeout del primer RPC. | 1 intento. | Sin ack no confirmado. | Status por la ruta actual; no segundo request. |
| 18 | Timeout del segundo RPC. | 2 intentos maximo. | Ack valido persiste; no numero verificado. | Error actual; no loop ni callback duplicado. |
| 19 | Cancelacion despues del primer response y antes del retry. | 1 RPC; segundo no se inicia. | No marcar numero exitoso; ack solo si ya se valido/persistio antes de cancelarse. | Propagar `CancellationException`; cero callback y cero retry. |
| 20 | Cancelacion durante el segundo RPC. | 2 iniciados; el segundo se cancela. | Ack inicial se conserva; no marcar numero exitoso. | Cancelacion propagada; cero callback de resultado. |
| 21 | Persistencia del ack falla (seam que fuerza excepcion). | 1 `N`; no firmar retry con ack no persistido. | No afirmar ack. | Error por ruta explicita, no success-shaped fallback ni retry. |
| 22 | Se regenera la clave entre request y response/retry. | Como maximo 1 retry solo si la identidad no cambio; si cambio, solo 1 RPC. | No asociar ack viejo a clave nueva. | Sin firma con una clave distinta; estado coherente. |
| 23 | Dos llamadas concurrentes empiezan con `ack=false`, ambas reciben ack/UNKNOWN. | Cada llamada: maximo 2; pueden existir dos `N` y dos `A`. | Flag asociado a par valido; no sobrescritura de numeros entre SIMs. | Un callback por llamada; documentar que no hay single-flight. |
| 24 | El primer response es `PENDING`; desafio se completa parcialmente y sigue pendiente. | 1 `Sync`; `Proceed` actuales, sin retry `Sync`. | Persistencia existente; sin doble cache. | Un callback con pending final; challenge processor se ejecuta una vez. |
| 25 | El segundo devuelve VERIFIED sin/ con token, o se mezclan entradas ausentes. | Maximo 2; `N` luego `A`. | Merge por IMSI; segundo reemplaza al primero, primer UNKNOWN solo como fallback para IMSI faltante. | No perder MSISDN token ni respuesta por SIM. |
| 26 | Camino read-only/local-read (cualquier flag de clave). | 0 Sync de verificacion completa; mantener RPC read-only actual. | Cache/registro actual. | Sin retry; regresion de callback/Status ausente. |
| 27 | Response raw UNKNOWN pero no coincide IMSI targeteado. | 1: `N`. | No mutar SIM no relacionada ni reconocer respuesta ajena. | Sin retry basado en registro descartado por filtro. |
| 28 | Se repite el helper de decision con second UNKNOWN/vacio. | Total exactamente 2. | Ack puede quedar true; `successful=false`. | Demuestra limite de retry y ausencia de recursion. |

`VerifyPhoneNumberSyncRetryTest` contiene 12 pruebas focalizadas y todas pasan. Cubre retry exitoso y persistente `UNKNOWN`, ausencia/mismatch del ack, request ya autenticado, estados `VERIFIED`/`PENDING`/mixtos, preservacion de IMSI no cubierto en una respuesta multi-SIM, clave distinta, wire decode y cancelacion. No ejecuta el builder real ni la matriz completa de 28 filas. La tabla es la especificacion de casos a completar segun lo requiera CI/revision; no debe interpretarse como cobertura ya alcanzada. Para `PENDING`, el test confirma que no se dispara el retry; no pretende validar el flujo de `Proceed`.

## 10. Validacion y compatibilidad

### Local y CI

1. Agregar primero pruebas del esquema y ack; ejecutar `:play-services-constellation-core:testDebugUnitTest`.
2. Ejecutar `:play-services-constellation-core:compileDebugKotlin` y construir el APK del modulo si el proyecto lo permite.
3. Ejecutar `:play-services-constellation-core:connectedDebugAndroidTest` en API soportada, incluyendo la prueba de regeneracion/ack y transporte AIDL relevante.
4. Repetir `:play-services-auth:compileDebugAidl` y la tarea que ensambla el APK para aislar el fallo AIDL que describio el body del PR.
5. Comparar contra la base del PR y registrar cada fallo como preexistente o nuevo; no atribuir un error del modulo auth a este cambio Constellation sin evidencia.

Para validar se preparo un JDK 17 y Android SDK 35 temporales fuera del repositorio. Resultado local:

- `:play-services-constellation-core:testDebugUnitTest`: **130 tests, 0 fallos**.
- `:play-services-constellation-core:compileDebugKotlin`: **pasa**.
- `:play-services-auth:compileDebugAidl`: **pasa**; no reproduce el fallo AIDL reportado.
- `:play-services-constellation-core:assembleDebug`: **pasa**.
- `git diff --check`: **pasa**.

No se ejecutaron tests instrumentales porque `adb devices` no reporto dispositivos conectados. La inspeccion estatica anterior encontro `DeviceManagementInfoResponse.aidl` en `play-services-auth-base` y `play-services-auth` declara dependencia API de ese modulo; el build actual pasa, aunque solo valida la variante/target local.

### Validacion con Messages y SIM real

Antes del rollout:

1. Registrar version exacta de Messages, GmsCore, Android/build, carrier, tipo de SIM y policy; no incluir IMSI/numero/token en el informe.
2. En un dispositivo de prueba preparado por el tester, observar un provisioning de clave no reconocida y guardar solo un response/fixture saneado que permita verificar `ClientAuthStatus`, `PublicKeyStatus`, session id y `next_sync_time`.
3. Confirmar con una captura saneada que el backend acepta el segundo `Sync` autenticado en la misma invocacion sin violar rate limits. La fuente reverse-engineered indica que `next_sync` programa el siguiente sync del cliente, pero no define si el backend tambien lo usa como restriccion.
4. Ejecutar exactamente un job inicial sin forzarlo otra vez; comprobar requests `N` y `A`, un callback, MSISDN token, conexion RCS y ausencia del backoff largo.
5. Probar tambien el caso donde el segundo response sigue `UNKNOWN`; comprobar que no hay loop y que Messages recibe el estado real.
6. Solo afirmar interoperabilidad tras verificar envio y recepcion de mensajes RCS.

No borrar datos ni cambiar configuracion de un dispositivo durante este refinamiento.

## 11. Riesgos residuales y refutacion

1. **`UNKNOWN` no prueba handshake incompleto.** Cubierto con retry solo cuando se valida un ack nuevo y todos los resultados relevantes son raw `UNKNOWN`; causas carrier/backend siguen posibles.
2. **`code == 1` puede estar mal modelado.** Mitigado al renombrar el esquema y probar wire; sigue dependiendo de una fuente reverse-engineered hasta obtener spec/captura.
3. **`next_sync_time` y rate limits.** La evidencia reverse-engineered describe programacion del cliente, no una prohibicion del servidor; confirmar la politica real en backend antes del rollout.
4. **Segundo Sync podria consumir rate limit o tener side effects.** Acotado a uno y unicamente en el estado observado; validar con backend real.
5. **Rotacion de clave durante el RPC podria asociar ack equivocado.** Capturar y comparar la identidad de la clave antes de persistir y firmar; test de carrera.
6. **Dos operaciones concurrentes podrian hacer retries duplicados.** Cada una queda acotada, pero no se garantiza single-flight; no agregar lock global sin evidencia.
7. **Retry segundo exitoso pero respuesta parcial podria ocultar un resultado del primero.** Merge por IMSI y pruebas mixtas/faltantes; no procesar cache dos veces.
8. **Messages podria decidir backoff por `Status.SUCCESS` aun con `UNKNOWN`.** No hay consumer disponible. El retry interno intenta resolver el caso sin cambiar el contrato; el caso persistente requiere validacion externa.
9. **Timeout/cancelacion puede ocurrir despues de persistir ack.** El flag de clave puede quedar correctamente reconocido aunque no haya callback; no marcar numero exitoso y nunca suprimir la cancelacion.

La prueba que mas directamente podria refutar A es una captura donde el primer response da ack y `next_sync_time` futuro, pero un segundo Sync inmediato falla/throttles o devuelve exactamente el mismo UNKNOWN. Si ocurre, elegir C o B; no ampliar el numero de retries.

## 12. Criterios de aceptacion

- El ack se deriva de un campo con semantica de clave documentada por el esquema local, no de un literal cuyo nombre sugiera exito RPC.
- No se persiste ack para una clave distinta de la enviada.
- Un solo retry autenticado como maximo por invocacion, solo ante ack nuevo + resultados targeteados todos `UNKNOWN` + ausencia de error/desafio/estado conocido.
- No se repiten ni duplican desafios; no se pierden estados por SIM ni token.
- El cache de numeros y `recordPhoneNumberVerification` reflejan unicamente el resultado final; el ack de clave no marca `successful=true`.
- Callback publico exactamente una vez; cancellation no genera callback.
- `Status.SUCCESS` y los codigos existentes conservan su semantica. No se inventan errores para inducir un retry no verificado.
- Se prueba `includeUnverified` en ambas variantes; la decision de retry se toma sobre la respuesta raw.
- Pruebas unitarias (130), compilacion Kotlin, ensamblado del modulo y tarea AIDL de Auth pasan. Instrumentacion y prueba real con Messages siguen pendientes por falta de dispositivo y backend accesible.
- Una prueba documentada con Messages/SIM demuestra que un unico provisioning inicial obtiene MSISDN token y RCS, o el plan se revierte a la alternativa compatible con el protocolo.

## 13. Secuencia propuesta de commits

1. **`fix(constellation): model sync public key acknowledgement`** — corregir nombres/semantica del response proto sin cambiar wire tags; anadir fixture y prueba de reconocimiento/serializacion. No incluir retry aun.
2. **`fix(constellation): retry unknown sync after new key acknowledgement`** — seam interno minimo, retry condicionado, request nuevo autenticado, persistencia/merge final y matriz determinista. Mantener el callback publico intacto.
3. **`test(constellation): cover first sync retry boundaries`** — completar pruebas de excepciones, cancelacion, concurrencia, `includeUnverified`, token y build/compatibilidad si conviene separarlas para revision.

La implementacion actual sigue estos grupos de cambios en un worktree sin commits. No se hicieron commits. La validacion local de build/tests pasa; las pruebas externas indicadas arriba quedan como pasos de rollout.

## 14. Checklist final de maintainer

- [x] Actualizar el esquema local para response field wire 3 `ClientAuthStatus` y comprobar el decode del tag wire.
- [ ] Confirmar con protocolo/backend de la version objetivo que `PublicKeyStatus.status == 1` significa ack.
- [x] Comprobar decode wire local para `SyncResponse` y `ProceedResponse`; falta captura independiente del backend.
- [ ] Confirmar si el segundo `Sync` puede ejecutarse inmediatamente y sin rate limit pese a `next_sync_time`.
- [x] Revisar y probar que el retry requiere primera llamada no autenticada, ack/session/clave validos y resultados raw `UNKNOWN`.
- [x] Revisar que `PENDING`, errores, resultados mixtos y request ya autenticado no disparan retry.
- [x] Revisar merge por IMSI y que el cache de numeros se procesa una sola vez desde el resultado final.
- [x] Confirmar que `Status` publico conserva la semantica RPC y no sustituye el estado por-SIM.
- [x] Revisar logs del cambio: no incluyen PII, tokens, firmas ni identificadores de SIM.
- [x] Ejecutar suite (130 tests), compile Kotlin, assemble debug y AIDL de Auth; todos pasan.
- [ ] Ejecutar instrumentacion y prueba real con Messages cuando haya un dispositivo disponible.

## 15. Fuera de alcance

- IID/GCM y los ajustes de Cloud Messaging del tester.
- Umbral de version de GmsCore para TS.43.
- Cambios generales a Asterism/consent, a la politica de retries de Messages o a otros servicios.
