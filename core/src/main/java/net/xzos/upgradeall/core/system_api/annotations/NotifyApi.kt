package net.xzos.upgradeall.core.system_api.annotations

/**
 * 自定义的日志打印工具类
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class LogApi {
    // 新增日志提醒
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class printLog

    // 日志修改（删除动作）提醒
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class logChanged
}

/**
 * 数据库更改通知
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class DatabaseApi {
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class databaseChanged
}

/**
 * 更新服务状态通知
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class UpdateManagerApi {
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class statusRefresh
}

/**
 * 跟踪项库变更通知
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class AppManagerApi {
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class appListChanged
}
