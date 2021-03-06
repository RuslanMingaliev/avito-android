package com.avito.utils.gradle

import com.avito.kotlin.dsl.getMandatoryStringProperty
import com.avito.kotlin.dsl.getOptionalStringProperty
import org.gradle.api.Project
import java.io.Serializable

sealed class KubernetesCredentials : Serializable {

    data class Service(
        val token: String,
        val caCertData: String,
        val url: String
    ) : KubernetesCredentials(), Serializable

    data class Config(
        val context: String,
        val caCertFile: String? = kubeDefaultCaCertFile,
        val configFile: String = kubeConfigDefaultPath
    ) : KubernetesCredentials(), Serializable
}

// TODO: get rid of this default. autoConfig is enabled by default
private val kubeConfigDefaultPath: String by lazy {
    val userHome: String = requireUserHome()
    "${userHome}/.kube/config"
}

private val kubeDefaultCaCertFile: String by lazy {
    val userHome: String = requireUserHome()
    "${userHome}/.kube/avito_ca.crt"
}

private fun requireUserHome(): String {
    val userHome: String? = System.getProperty("user.home")
    require(!userHome.isNullOrBlank()) { "system property 'user.home' is not set" }
    return userHome
}

val Project.kubernetesCredentials: KubernetesCredentials
    get() {
        val context = getOptionalStringProperty("kubernetesContext", nullIfBlank = true)
        return if (context.isNullOrBlank()) {
            KubernetesCredentials.Service(
                token = getMandatoryStringProperty("kubernetesToken"),
                caCertData = getMandatoryStringProperty("kubernetesCaCertData"),
                url = getMandatoryStringProperty("kubernetesUrl")
            )
        } else {
            val caCertFile = getOptionalStringProperty("kubernetesCaCertFile", nullIfBlank = true)
            KubernetesCredentials.Config(context, caCertFile = caCertFile)
        }
    }
