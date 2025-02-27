package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ANNOTATION_PROCESSOR_PATH
import com.autonomousapps.internal.SERVICE_LOADER_PATH
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.intermediates.ServiceLoaderDependency
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.BufferedReader
import java.util.zip.ZipFile

/**
 * See also [ServiceLoader][https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html].
 *
 * Some dependencies are loaded at runtime, and cannot reasonably be detected by looking at compiled
 * bytecode. We will special-case all such dependencies discovered on the classpath.
 */
@CacheableTask
abstract class FindServiceLoadersTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all dependencies that include Java ServiceLoaders"
  }

  private lateinit var compileClasspath: ArtifactCollection

  fun setCompileClasspath(artifacts: ArtifactCollection) {
    this.compileClasspath = artifacts
  }

  @Classpath
  fun getCompileClasspath(): FileCollection = compileClasspath.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val serviceLoaders = compileClasspath
      .filterNonGradle()
      .filter { it.file.name.endsWith(".jar") }
      .flatMapToSet { findServiceLoaders(it) }

    outputFile.writeText(serviceLoaders.toJson())
  }

  // E.g. org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5 -->
  // 1. META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory
  // 2. META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler
  private fun findServiceLoaders(artifact: ResolvedArtifactResult): Set<ServiceLoaderDependency> {
    val zip = ZipFile(artifact.file)

    return zip.entries().toList()
      .filter { it.name.startsWith(SERVICE_LOADER_PATH) }
      .filterNot { it.name.startsWith(ANNOTATION_PROCESSOR_PATH) }
      .filterNot { it.isDirectory }
      .mapToOrderedSet { serviceFile ->
        val providerClasses = zip.getInputStream(serviceFile)
          .bufferedReader().use(BufferedReader::readLines)
          // remove whitespace
          .map { it.trim() }
          // Ignore comments
          .filterToSet { !it.startsWith("#") }

        ServiceLoaderDependency(
          providerFile = serviceFile.name.removePrefix(SERVICE_LOADER_PATH),
          providerClasses = providerClasses,
          componentIdentifier = artifact.id.componentIdentifier
        )
      }
  }
}
