package net.xzos.upgradeall.core.utils.versioning

internal val VERSION_NUMBER_STRICT_MATCH_REGEX = "\\d+(\\.\\d+)+([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
internal val VERSION_NUMBER_MATCH_REGEX = "\\d+(\\.\\d+)*([.|\\-|+|_| ]*[A-Za-z0-9]+)*".toRegex()
