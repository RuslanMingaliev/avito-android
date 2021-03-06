package com.avito.android.lint

import com.avito.android.isAndroidApp
import com.avito.android.withAndroidApp
import com.avito.bitbucket.Bitbucket
import com.avito.bitbucket.atlassianCredentials
import com.avito.bitbucket.bitbucketConfig
import com.avito.utils.gradle.BuildEnvironment
import com.avito.utils.gradle.buildEnvironment
import com.avito.utils.gradle.envArgs
import com.avito.utils.logging.ciLogger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register

// TODO: support build variants
open class LintReportPlugin : Plugin<Project> {

    override fun apply(app: Project) {
        check(app.isAndroidApp()) { "Plugin must be applied to an application but was applied to ${app.path}" }

        val reportTask = app.tasks.register<LintReportTask>("lintReport") {

            val atlassianCredentials = app.atlassianCredentials
            if (atlassianCredentials.isPresent) {
                bitbucket.set(
                    project.bitbucketConfig.map { config ->
                        Bitbucket.create(
                            bitbucketConfig = config,
                            logger = app.ciLogger,
                            pullRequestId = null
                        )
                    }
                )
            }

            buildId.set(project.envArgs.buildId)
        }

        if (app.buildEnvironment is BuildEnvironment.CI) {
            // На CI гоняем только lint в application-модуле с checkDependencies=true
            setupDependencies(app, reportTask)
        } else {
            // Локально может понадобиться запустить lint в нескольких модулях, поэтому report-таска
            // должна запускаться точно последней
            setupOnlyOrdering(app, reportTask)
        }
    }

    private fun setupDependencies(app: Project, reportTask: TaskProvider<LintReportTask>) {
        reportTask.configure {
            it.dependsOn(app.lintTask())
        }
        app.withAndroidApp { appExtension ->

            appExtension.testVariants.all { _ ->

                app.lintTask().configure {

                    // see LintWorkerApiWorkaround.md
                    it.mustRunAfter("preInstrumentation")
                }
            }
        }
    }

    private fun setupOnlyOrdering(app: Project, reportTask: TaskProvider<LintReportTask>) {
        app.gradle.projectsEvaluated {
            app.rootProject.subprojects { module ->
                module.pluginManager.withPlugin("com.android.library") {
                    reportTask {
                        mustRunAfter(module.lintTask())
                    }
                }
                module.pluginManager.withPlugin("com.android.application") {
                    reportTask {
                        mustRunAfter(module.lintTask())
                    }
                }
            }
        }
    }

    private fun Project.lintTask(): TaskProvider<out Task> {
        return try {
            tasks.named("lintRelease")
        } catch (ex: UnknownTaskException) {
            tasks.named("lint")
        }
    }

}
