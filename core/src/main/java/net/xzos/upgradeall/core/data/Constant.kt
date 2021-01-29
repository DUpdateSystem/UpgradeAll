package net.xzos.upgradeall.core.data

const val ANDROID_APP_TYPE = "android_app_package"
const val ANDROID_MAGISK_MODULE_TYPE = "android_magisk_module"
const val ANDROID_CUSTOM_SHELL = "android_custom_shell"
const val ANDROID_CUSTOM_SHELL_ROOT = "android_custom_shell_root"

const val DEF_UPDATE_SERVER_URL = "update-server.xzos.net:5255"
internal val URL_ARG_REGEX = "(%.*?)\\w*".toRegex()
internal val VERSION_NUMBER_STRICT_MATCH_REGEX = "\\d+(\\.\\d+)+([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
internal val VERSION_NUMBER_MATCH_REGEX = "\\d+(\\.\\d+)*([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
