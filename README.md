# Algoritmos - Post-Contenido 1 U9

## Descripcion del Proyecto

Este proyecto implementa un laboratorio de escalabilidad y rendimiento para la materia Diseno de Algoritmos y Sistemas (Unidad 9). El sistema procesa registros de texto en un pipeline con cuatro etapas:

1. parse
2. enrich
3. transform
4. validate

Se construyen cuatro variantes del mismo flujo para comparar comportamiento:

1. Pipeline secuencial
2. Pipeline con parallelStream
3. Pipeline con CompletableFuture + pool dedicado
4. Pipeline con ForkJoinPool (divide y venceras)

El objetivo es medir throughput y latencia con JMH, identificar el cuello de botella real y justificar tecnicamente la mejor estrategia.

## Objetivo de la Unidad

Medir empiricamente el impacto del paralelismo en un workload mixto (CPU-bound + I/O-bound simulado), comparando resultados reales contra expectativas teoricas.

## Arquitectura del Sistema (Pipeline)

La arquitectura es lineal por etapas y aplica sobre lotes de lineas de entrada.

```text
Entrada (List<String>)
				|
				v
 [parse] ----> [enrich] ----> [transform] ----> [validate] ----> Salida (List<Record>)
	 CPU            I/O sim        CPU pesado         CPU liviano
```

## Tecnologias Utilizadas

| Tecnologia    | Version                  | Uso                                        |
| ------------- | ------------------------ | ------------------------------------------ |
| Java (JDK)    | 17+ (compatible con 21)  | Implementacion del pipeline y concurrencia |
| Apache Maven  | 3.8+ (probado en 3.9.12) | Build, tests y empaquetado                 |
| JMH           | 1.37                     | Microbenchmarks (throughput y latencia)    |
| JUnit Jupiter | 5.10.0                   | Pruebas unitarias                          |
| AssertJ       | 3.25.3                   | Asserts expresivos en pruebas              |

## Estructura del Proyecto

```text
algoritmos-castellanos-post1-u9/
|- capturas/
|  |- benchamrk.png
|- src/
|  |- main/
|  |  |- java/
|  |  |  |- co/edu/udes/algoritmos/u9/
|  |  |  |  |- benchmark/
|  |  |  |  |  |- PipelineBenchmark.java
|  |  |  |  |- model/
|  |  |  |  |  |- Record.java
|  |  |  |  |- service/
|  |  |  |  |  |- RecordProcessor.java
|  |  |  |  |- task/
|  |  |  |  |  |- RecordProcessorTask.java
|  |- test/
|  |  |- java/
|  |  |  |- co/edu/udes/algoritmos/u9/service/
|  |  |  |  |- RecordProcessorTest.java
|- .gitignore
|- pom.xml
|- README.md
```

## Prerrequisitos del Entorno

1. Java JDK 17 o superior instalado y configurado en PATH.
2. Maven 3.8 o superior instalado y configurado en PATH.
3. Terminal en la raiz del proyecto.

Comandos de verificacion recomendados:

```bash
java -version
mvn -version
```

## Instrucciones de Ejecucion (Paso a Paso)

1. Compilar y ejecutar pruebas unitarias:

```bash
mvn test
```

2. Empaquetar proyecto y benchmarks JMH:

```bash
mvn package
```

3. Ejecutar benchmarks:

```bash
java -jar target/benchmarks.jar
```

4. Revisar en consola las metricas de:
1. Throughput (ops/ms)
1. Average Time (ms/op)

## Funcionalidades Principales

1. Procesamiento de registros por etapas (parse, enrich, transform, validate).
2. Comparacion de cuatro estrategias de ejecucion del mismo pipeline.
3. Medicion reproducible de rendimiento con JMH para distintos tamanos de entrada (100, 1000, 5000).
4. Validacion de correccion funcional entre implementaciones paralelas y secuencial.
5. Test de regresion de rendimiento para `processAsync`.

## Hipotesis Previa al Benchmark

Antes de medir, la hipotesis fue:

1. `processAsync` seria la mas rapida al usar un pool dedicado para la etapa I/O-bound (`enrich`).
2. `parallelStream` mejoraria frente a secuencial, pero con limite por bloqueo en `commonPool`.
3. `forkJoin` seria competitivo en CPU puro, pero sin ventaja clara cuando domina I/O.

## Resultados JMH (Valores Reales)

### Throughput (ops/ms)

| Implementacion |   100 |  1000 |      5000 |
| -------------- | ----: | ----: | --------: |
| sequential     | 0.007 | 0.001 | ~0.0001\* |
| parallelStream | 0.077 | 0.007 |     0.001 |
| processAsync   | 0.134 | 0.029 |     0.006 |
| forkJoin       | 0.013 | 0.007 |     0.002 |

\* En la salida de consola el valor de `sequential` para 5000 en throughput aparece ilegible. Se estima desde `avgt` como $1 / 9518.162 \approx 0.000105$ ops/ms.

### Latencia Promedio (ms/op)

| Implementacion |     100 |     1000 |     5000 |
| -------------- | ------: | -------: | -------: |
| sequential     | 148.738 | 1474.327 | 9518.162 |
| parallelStream |  12.841 |  133.729 |  662.428 |
| processAsync   |   6.877 |   33.950 |  151.879 |
| forkJoin       |  75.053 |  144.286 |  677.296 |

## Analisis Comparativo (Teoria vs Medicion)

### Cuello de Botella Identificado

La etapa dominante es `enrich`, porque simula I/O con espera (`sleep`) por registro. Esa etapa bloquea hilos y condiciona el rendimiento global.

### Por que `processAsync` supera a `parallelStream`

`parallelStream` usa `ForkJoinPool.commonPool()` (paralelismo acotado, tipicamente cercano a nucleos CPU). Cuando `enrich` bloquea hilos, el pool se satura rapido. En `processAsync`, el pool dedicado amplia la concurrencia para I/O-bound, por eso reduce mucho la latencia total.

### Speedup Empirico (respecto a secuencial)

Se calcula como:

$$
S = \frac{T_{secuencial}}{T_{paralelo}}
$$

Con latencia `avgt`:

1. `parallelStream` (1000): $1474.327 / 133.729 \approx 11.02\times$
2. `processAsync` (1000): $1474.327 / 33.950 \approx 43.43\times$
3. `forkJoin` (1000): $1474.327 / 144.286 \approx 10.22\times$

### Comparacion con Ley de Amdahl

Ley de Amdahl:

$$
S(N) = \frac{1}{(1-p) + \frac{p}{N}}
$$

Con 12 procesadores logicos, incluso con $p=0.95$:

$$
S(12) = \frac{1}{0.05 + 0.95/12} \approx 7.76\times
$$

El speedup observado en `processAsync` supera ese valor porque no es solo paralelismo CPU clasico: se optimiza un tramo I/O-bound aumentando concurrencia efectiva de operaciones bloqueantes, lo cual sale del modelo simplificado de Amdahl puro para CPU.

### Bottleneck Remanente

Despues de optimizar, el cuello de botella sigue asociado a:

1. Costo total de `enrich` (espera simulada acumulada).
2. Overhead de coordinacion de tareas/futuros cuando crece la entrada.
3. Variabilidad alta en cargas grandes (se evidencia en el error de `sequential` para 5000).

## Complejidad y Justificacion Tecnica

1. `parse`: $O(m)$ por registro (longitud de cadena).
2. `enrich`: $O(1)$ computacional por registro, pero con latencia bloqueante simulada.
3. `transform`: $O(m)$ por registro.
4. `validate`: $O(1)$ por registro.
5. Pipeline completo: $O(n \cdot m)$, donde $n$ es numero de registros y $m$ longitud promedio de `rawData`.

En versiones paralelas, la complejidad asintotica no cambia; mejora el tiempo efectivo por concurrencia, con costo de coordinacion adicional.

## Pruebas y Validacion

El proyecto incluye pruebas unitarias para:

1. `parse`
2. `enrich`
3. `transform`
4. `validate`
5. Equivalencia de cantidad de resultados entre las 4 implementaciones
6. Regresion de rendimiento (`throughputRegressionTest`)

## Evidencia Visual

Resultado de ejecucion de benchmarks JMH:

![Salida de benchmarks JMH](capturas/benchamrk.png)

## Solucion de Problemas Frecuentes

1. `java -jar target/benchmarks.jar` falla porque no se ejecuto empaquetado:
1. Ejecutar primero `mvn package`.
1. Tests lentos o inestables por carga del sistema:
1. Cerrar aplicaciones pesadas antes del benchmark.
1. Repetir corrida y comparar tendencia, no un unico valor aislado.
1. Error por version de Java:
1. Confirmar JDK (no JRE) y version 17+.

## Conclusiones

1. La mejor implementacion para este workload mixto fue `processAsync`.
2. El resultado confirma que, cuando domina I/O-bound, un pool dedicado supera a `parallelStream` y `forkJoin`.
3. El speedup empirico fue muy superior al baseline secuencial y coherente con la estrategia aplicada.
4. El modelo de Amdahl sirve como referencia teorica, pero no captura completamente optimizaciones de concurrencia sobre espera bloqueante.
