package net.xzos.upgradeall

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GetterBridgeRequestBuilderTest {
    @Test
    fun readOperationRequestPreservesOperationAndPayload() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.readOperationRequest(
                mapOf(
                    "operation" to "package_eval",
                    "payload" to mapOf("package_id" to "android/org.fdroid.fdroid"),
                ),
            ),
        )

        assertEquals("package_eval", json.getString("operation"))
        assertEquals(
            "android/org.fdroid.fdroid",
            json.getJSONObject("payload").getString("package_id"),
        )
    }

    @Test
    fun runtimeOperationRequestPreservesOperationAndPayload() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.runtimeOperationRequest(
                mapOf(
                    "operation" to "task_get",
                    "payload" to mapOf("task_id" to "task-1"),
                ),
            ),
        )

        assertEquals("task_get", json.getString("operation"))
        assertEquals("task-1", json.getJSONObject("payload").getString("task_id"))
    }

    @Test
    fun runtimeOperationRequestDefaultsMissingPayloadToEmptyObject() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.runtimeOperationRequest(
                mapOf("operation" to "task_list"),
            ),
        )

        assertEquals("task_list", json.getString("operation"))
        assertEquals(0, json.getJSONObject("payload").length())
    }

    @Test
    fun installedAutogenPreviewRequestPreservesOnlyScanOptions() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.installedAutogenPreviewRequest(
                mapOf(
                    "scan_options" to mapOf(
                        "include_system_apps" to true,
                        "include_self" to false,
                    ),
                    "index_xml" to "<fdroid />",
                    "mode" to "force_refresh",
                ),
            ),
        )

        val scanOptions = json.getJSONObject("scan_options")
        assertEquals(true, scanOptions.getBoolean("include_system_apps"))
        assertEquals(false, scanOptions.getBoolean("include_self"))
        assertEquals(false, json.has("index_xml"))
        assertEquals(false, json.has("mode"))
    }

    @Test
    fun startupRequestAddsDataDirAndForwardsOnlyScanOptions() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.startupRequest(
                "/app/files/getter",
                mapOf(
                    "scan_options" to mapOf(
                        "include_system_apps" to true,
                        "include_self" to false,
                    ),
                    "inventory" to mapOf("items" to emptyList<Any>()),
                    "provider" to "github",
                    "cache" to true,
                    "endpoint" to "https://example.invalid",
                    "transport" to "live",
                ),
            ),
        )

        assertEquals("/app/files/getter", json.getString("data_dir"))
        assertEquals(
            true,
            json.getJSONObject("scan_options").getBoolean("include_system_apps"),
        )
        assertEquals(false, json.has("inventory"))
        assertEquals(false, json.has("provider"))
        assertEquals(false, json.has("cache"))
        assertEquals(false, json.has("endpoint"))
        assertEquals(false, json.has("transport"))
        assertEquals(2, json.length())
    }

    @Test
    fun githubAutogenPreviewRequestCarriesOnlyProductFields() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.githubAutogenPreviewRequest(
                mapOf(
                    "owner" to "DUpdateSystem",
                    "repo" to "UpgradeAll",
                    "android_package" to "net.xzos.upgradeall",
                    "display_name" to "UpgradeAll",
                    "releases_json" to "[]",
                    "api_base_url" to "https://example.invalid",
                    "mode" to "force_refresh",
                    "asset" to mapOf("include" to ".*apk"),
                ),
            ),
        )

        assertEquals("DUpdateSystem", json.getString("owner"))
        assertEquals("UpgradeAll", json.getString("repo"))
        assertEquals("net.xzos.upgradeall", json.getString("android_package"))
        assertEquals("UpgradeAll", json.getString("display_name"))
        assertEquals(false, json.has("releases_json"))
        assertEquals(false, json.has("api_base_url"))
        assertEquals(false, json.has("mode"))
        assertEquals(false, json.has("asset"))
        assertEquals(4, json.length())
    }

    @Test
    fun fdroidCatalogRefreshRequestCarriesOnlyDataDir() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.fdroidCatalogRefreshRequest("/app/files/getter"),
        )

        assertEquals("/app/files/getter", json.getString("data_dir"))
        assertEquals(false, json.has("index_xml"))
        assertEquals(false, json.has("endpoint_url"))
        assertEquals(false, json.has("mode"))
        assertEquals(1, json.length())
    }

    @Test
    fun fdroidAutogenPreviewRequestPreservesGetterPayload() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.fdroidAutogenPreviewRequest(
                mapOf(
                    "payload" to mapOf(
                        "index_xml" to "<fdroid />",
                        "package_names" to listOf("org.fdroid.fdroid"),
                    ),
                ),
            ),
        )

        val payload = json.getJSONObject("payload")
        assertEquals("<fdroid />", payload.getString("index_xml"))
        assertEquals("org.fdroid.fdroid", payload.getJSONArray("package_names").getString(0))
    }

    @Test
    fun autogenApplyRequestPreservesPreviewAndAcceptance() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.autogenApplyRequest(
                mapOf(
                    "preview_json" to "{\"operation\":\"fdroid.autogen.preview\"}",
                    "acceptance" to mapOf(
                        "mode" to "packages",
                        "package_ids" to listOf("android/f-droid/app/org.fdroid.fdroid"),
                    ),
                ),
            ),
        )

        assertEquals(
            "fdroid.autogen.preview",
            json.getJSONObject("preview").getString("operation"),
        )
        val acceptance = json.getJSONObject("acceptance")
        assertEquals("packages", acceptance.getString("mode"))
        assertEquals(
            "android/f-droid/app/org.fdroid.fdroid",
            acceptance.getJSONArray("package_ids").getString(0),
        )
    }

    @Test
    fun autogenApplyRequestRequiresPreviewJson() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            GetterBridgeRequestBuilder.autogenApplyRequest(mapOf("acceptance" to emptyMap<String, String>()))
        }

        assertEquals("preview_json is required", error.message)
    }

    @Test
    fun runtimeOperationRequestRequiresOperation() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            GetterBridgeRequestBuilder.runtimeOperationRequest(mapOf("payload" to emptyMap<String, String>()))
        }

        assertEquals("operation is required", error.message)
    }

    @Test
    fun freshInstallSetupPreviewCarriesOnlyAndroidScanOptions() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.freshInstallSetupPreviewRequest(
                mapOf(
                    "scan_options" to mapOf(
                        "include_system_apps" to true,
                        "include_self" to false,
                    ),
                ),
            ),
        )

        assertEquals(setOf("scan_options"), json.keys().asSequence().toSet())
        assertEquals(
            setOf("include_system_apps", "include_self"),
            json.getJSONObject("scan_options").keys().asSequence().toSet(),
        )
        assertEquals(true, json.getJSONObject("scan_options").getBoolean("include_system_apps"))
    }

    @Test
    fun freshInstallSetupPreviewRejectsUnknownControlAndInventoryInjection() {
        listOf(
            "endpoint",
            "xml",
            "provider_payload",
            "cache_mode",
            "repository",
            "category",
            "normalization",
            "inventory",
            "unexpected",
        ).forEach { forbidden ->
            assertThrows("expected $forbidden to be rejected", IllegalArgumentException::class.java) {
                GetterBridgeRequestBuilder.freshInstallSetupPreviewRequest(
                    mapOf(forbidden to "not-product-safe"),
                )
            }
        }
    }

    @Test
    fun freshInstallSetupApplyCarriesOpaquePreviewIdAndAcceptanceOnly() {
        val json = JSONObject(
            GetterBridgeRequestBuilder.freshInstallSetupApplyRequest(
                mapOf(
                    "preview_id" to "opaque-1",
                    "accepted_package_ids" to listOf("android/app/com.example"),
                ),
            ),
        )

        assertEquals(setOf("preview_id", "accepted_package_ids"), json.keys().asSequence().toSet())
        assertEquals("opaque-1", json.getString("preview_id"))
        assertEquals(
            "android/app/com.example",
            json.getJSONArray("accepted_package_ids").getString(0),
        )
    }

    @Test
    fun freshInstallSetupApplyRejectsAmbiguousAcceptanceAndUnknownFields() {
        assertThrows(IllegalArgumentException::class.java) {
            GetterBridgeRequestBuilder.freshInstallSetupApplyRequest(
                mapOf(
                    "preview_id" to "opaque-1",
                    "accept_all" to true,
                    "accepted_package_ids" to emptyList<String>(),
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GetterBridgeRequestBuilder.freshInstallSetupApplyRequest(
                mapOf(
                    "preview_id" to "opaque-1",
                    "accepted_package_ids" to listOf(""),
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GetterBridgeRequestBuilder.freshInstallSetupApplyRequest(
                mapOf(
                    "preview_id" to "opaque-1",
                    "accept_all" to true,
                    "repository" to "autogen",
                ),
            )
        }
    }
}
