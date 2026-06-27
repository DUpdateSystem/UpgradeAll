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
}
