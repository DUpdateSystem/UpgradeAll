package net.xzos.upgradeall.system_api.annotations

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class IoApi {
   // 注释相应平台的下载软件
   @MustBeDocumented
   @Retention(AnnotationRetention.RUNTIME)
   @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
   annotation class download
   @MustBeDocumented
   @Retention(AnnotationRetention.RUNTIME)
   @kotlin.annotation.Target(AnnotationTarget.FUNCTION)
   annotation class getAppVersionNumber
}