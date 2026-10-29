package org.telegram.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.telegram.tasks.GenerateStringResourceIdsAssetTask
import org.telegram.tasks.GenerateLottieMetadataAssetFileTask
import org.telegram.tasks.TelegramStringsTask

private val STRING_FILES = listOf("strings.xml", "ayu.xml", "extera.xml")

class TelegramBuildAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val telegramModule = project.project(":TMessagesProj")
        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: error("Apply com.android.application/library before org.telegram.build-app-plugin")

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }

            val task = project.tasks.register<TelegramStringsTask>(
                "generate${suffix}TelegramStrings"
            ) {
                // LocaleController reads every string from the generated assets, never from
                // Android resources, so each values file that defines strings must be listed.
                // ayu.xml and extera.xml hold the AyuGram and exteraGram screens' strings.
                stringsXml.from(
                    telegramModule.fileTree("src/main/res/values") {
                        include(STRING_FILES)
                    },
                    project.fileTree("src/main/res/values") {
                        include(STRING_FILES)
                    }
                )

                localizationFiles.from(
                    telegramModule.fileTree("src/main/res") {
                        include(STRING_FILES.map { "values-*/$it" })
                    },
                    project.fileTree("src/main/res") {
                        include(STRING_FILES.map { "values-*/$it" })
                    }
                )

                stringsOutputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/telegramStrings/${variant.name}/res"
                    )
                )

                assetsOutputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/telegramStrings/${variant.name}/assets"
                    )
                )

                stableIdsFile.set(
                    project.layout.buildDirectory.file(
                        "generated/telegramStrings/${variant.name}/stable-ids.txt"
                    )
                )

                resourcePackageName.set((variant as ApplicationVariant).applicationId)
            }

            variant.sources.res?.addGeneratedSourceDirectory(
                task,
                TelegramStringsTask::stringsOutputDir
            )

            variant.sources.assets?.addGeneratedSourceDirectory(
                task,
                TelegramStringsTask::assetsOutputDir
            )

            (variant as ApplicationVariant).androidResources.aaptAdditionalParameters.addAll(
                task.flatMap { telegramStringsTask ->
                    telegramStringsTask.stableIdsFile.map { stableIdsFile ->
                        listOf(
                            "--stable-ids",
                            stableIdsFile.asFile.absolutePath
                        )
                    }
                }
            )

            project.tasks.withType(LinkApplicationAndroidResourcesTask::class.java).configureEach {
                if (name == "process${suffix}Resources") {
                    dependsOn(task)

                    doFirst {
                        println("=== $path ===")
                        println("AAPT additional parameters:")
                        aaptAdditionalParameters.get().forEach {
                            println("  $it")
                        }
                    }
                }
            }
        }

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }
            val task = project.tasks.register<GenerateStringResourceIdsAssetTask>("generate${suffix}StringResourceIdsAsset") {
                runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
                outputDir.set(project.layout.buildDirectory.dir("generated/stringResourceIds/${variant.name}/assets"))
            }
            variant.sources.assets?.addGeneratedSourceDirectory(task, GenerateStringResourceIdsAssetTask::outputDir)
        }

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }
            val lottieTask = project.tasks.register<GenerateLottieMetadataAssetFileTask>("generate${suffix}LottieMeta") {
                runtimeSymbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
                variant.sources.res?.all?.let { _ ->
                    rawResourceDirs.from(
                        telegramModule.fileTree("src/main/res") {
                            include("raw*/*.json")
                        },
                        project.fileTree("src/main/res") {
                            include("raw*/*.json")
                        }
                    )
                }
            }

            variant.sources.assets?.addGeneratedSourceDirectory(lottieTask, GenerateLottieMetadataAssetFileTask::outputDir)
        }
    }
}
