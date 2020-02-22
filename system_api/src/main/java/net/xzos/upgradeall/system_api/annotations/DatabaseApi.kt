package net.xzos.upgradeall.system_api.annotations

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class DatabaseApi {
    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class getAppDatabases

    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class saveAppDatabase

    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class deleteAppDatabase

    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class getHubDatabases

    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class saveHubDatabase

    @MustBeDocumented
    @Retention(AnnotationRetention.RUNTIME)
    @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
    annotation class deleteHubDatabase
}