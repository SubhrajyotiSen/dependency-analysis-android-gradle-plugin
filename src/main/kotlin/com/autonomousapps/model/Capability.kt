package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Capability : Comparable<Capability> {
  override fun compareTo(other: Capability): Int = javaClass.simpleName.compareTo(other.javaClass.simpleName)
}

@TypeLabel("linter")
@JsonClass(generateAdapter = false)
data class AndroidLinterCapability(
  val lintRegistry: String,
  /** True if this dependency contains _only_ an Android lint jar/registry. */
  val isLintJar: Boolean
) : Capability()

@TypeLabel("manifest")
@JsonClass(generateAdapter = false)
data class AndroidManifestCapability(
  val packageName: String,
  val componentMap: Map<Component, Set<String>>
) : Capability() {

  enum class Component(val tagName: String, val mapKey: String) {
    ACTIVITY("activity", "activities"),
    SERVICE("service", "services"),
    RECEIVER("receiver", "receivers"),
    PROVIDER("provider", "providers");

    val attrName = "android:name"

    companion object {
      internal fun of(mapKey: String): Component {
        return values().find {
          it.mapKey == mapKey
        } ?: error("Could not find Manifest.Component for $mapKey")
      }
    }
  }
}

@TypeLabel("res")
@JsonClass(generateAdapter = false)
data class AndroidResCapability(
  val rImport: String,
  val lines: List<Line>
) : Capability() {

  data class Line(val type: String, val value: String)
}

@TypeLabel("proc")
@JsonClass(generateAdapter = false)
data class AnnotationProcessorCapability(
  val processor: String,
  val supportedAnnotationTypes: Set<String>
) : Capability()

@TypeLabel("class")
@JsonClass(generateAdapter = false)
data class ClassCapability(
  val classes: Set<String>
) : Capability()

@TypeLabel("const")
@JsonClass(generateAdapter = false)
data class ConstantCapability(
  /** Map of fully-qualified class names to constant field names. */
  val constants: Map<String, Set<String>>,
  /** Kotlin classes with top-level declarations. */
  val ktFiles: Set<KtFile>
) : Capability()

@TypeLabel("inferred")
@JsonClass(generateAdapter = false)
data class InferredCapability(
  /**
   * True if this dependency contains only annotations that are only needed at compile-time (`CLASS`
   * and `SOURCE` level retention policies). False otherwise.
   */
  val isCompileOnlyAnnotations: Boolean
) : Capability()

@TypeLabel("inline")
@JsonClass(generateAdapter = false)
data class InlineMemberCapability(
  val inlineMembers: Set<InlineMember>
) : Capability() {

  data class InlineMember(
    val packageName: String,
    val inlineMembers: Set<String>
  ) : Comparable<InlineMember> {
    override fun compareTo(other: InlineMember): Int = compareBy(InlineMember::packageName)
      .thenBy(LexicographicIterableComparator()) { it.inlineMembers }
      .compare(this, other)
  }
}

@TypeLabel("native")
@JsonClass(generateAdapter = false)
data class NativeLibCapability(
  val fileNames: Set<String>
) : Capability()

@TypeLabel("service_loader")
@JsonClass(generateAdapter = false)
data class ServiceLoaderCapability(
  val providerFile: String,
  val providerClasses: Set<String>
) : Capability()

@TypeLabel("security_provider")
@JsonClass(generateAdapter = false)
data class SecurityProviderCapability(
  val securityProviders: Set<String>
) : Capability()
