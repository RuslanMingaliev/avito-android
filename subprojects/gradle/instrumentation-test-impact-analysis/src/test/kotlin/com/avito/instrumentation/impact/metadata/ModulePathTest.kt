package com.avito.instrumentation.impact.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class ModulePathTest {

    @TestFactory
    fun `modulePath - created without exception - for valid gradle path`() = listOf(
        ":lib",
        ":lib:one:two",
        ":lib_x",
        ":lib:one_x:two",
        ":kotlinModuleDependency",
        ":avito-app:publish"
    )
        .map {
            DynamicTest.dynamicTest("path = $it") {
                val modulePath = ModulePath(it)
                assertThat(modulePath.path).isEqualTo(it)
            }
        }

    @TestFactory
    fun `modulePath - raise exception on init - for invalid gradle paths`() = listOf(
        "lib",
        ":lib_",
        ":lib.x",
        "x:one"
    ).map {
        DynamicTest.dynamicTest("path = $it") {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                ModulePath(it)
            }
        }
    }
}
