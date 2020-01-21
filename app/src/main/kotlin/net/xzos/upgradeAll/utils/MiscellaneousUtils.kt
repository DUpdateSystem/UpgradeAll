package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.google.gson.Gson
import com.jaredrummler.android.shell.CommandResult
import com.jaredrummler.android.shell.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication.Companion.context
import net.xzos.upgradeAll.server.app.engine.js.utils.JSUtils
import org.json.JSONException
import java.io.StringReader
import java.util.*

object MiscellaneousUtils {
    private var suAvailable: Boolean? = null
        get() = field ?: Shell.SU.available().also { field = it }

    fun accessByBrowser(url: String?, context: Context?) {
        if (url != null && context != null)
            context.startActivity(
                    Intent.createChooser(
                            Intent(Intent.ACTION_VIEW).apply {
                                this.data = Uri.parse(url)
                            }, "请选择浏览器以打开网页").apply {
                        if (context == context)
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
            )

    }

    fun getCurrentLocale(context: Context): Locale? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.resources.configuration.locales[0]
            else
                @Suppress("DEPRECATION")
                context.resources.configuration.locale

    fun parsePropertiesString(s: String): Properties =
            Properties().apply {
                this.load(StringReader(s))
            }

    fun runShellCommand(command: String, su: Boolean = false): CommandResult? =
            if (command.isNotBlank())
                if (su)
                    if (suAvailable!!)
                        Shell.SU.run(command)
                    else {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.no_root_and_restart_to_use_root,
                                    Toast.LENGTH_LONG).show()
                        }
                        null
                    }
                else Shell.run(command)
            else null

    fun mapOfJsonObject(jsonObjectString: String): Map<*, *> {
        return try {
            Gson().fromJson(jsonObjectString, Map::class.java)
        } catch (e: JSONException) {
            mapOf<Any, Any>()
        }
    }
}