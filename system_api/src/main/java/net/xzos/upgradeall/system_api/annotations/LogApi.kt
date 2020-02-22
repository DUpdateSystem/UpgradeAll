package net.xzos.upgradeall.system_api.annotations

/**
 * 自定义的日志打印工具类
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class LogApi {
    // 调用Log.v()方法打印日志
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class v

    // 调用Log.d()方法打印日志
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class d

    // 调用Log.i()方法打印日志
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class i

    // 调用Log.w()方法打印日志
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class w

    // 调用Log.e()方法打印日志
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class e

    // 提醒日志更新
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class change
}
