package net.xzos.upgradeall.core.system_api

import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.log.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


open class RegisterApi(vararg annotationClass: Class<out Annotation>) {

    private val annotationClassList = annotationClass.toList()
    private val methodMap: HashMap<Class<out Annotation>, MutableList<MethodRunTime>> = hashMapOf()

    fun register(any: Any) {
        val classObj = any::class.java
        val method: Array<Method> = classObj.declaredMethods
        for (m in method) {
            for (annotationClass in annotationClassList) {
                if (m.isAnnotationPresent(annotationClass)) {
                    m.isAccessible = true
                    (methodMap[annotationClass]
                        ?: mutableListOf<MethodRunTime>().apply {
                            methodMap[annotationClass] = this
                        }).add(MethodRunTime(any, m))
                }
            }
        }
    }

    fun unregister(any: Any) {
        for (list in methodMap.values) {
            for (methodRunTime in list) {
                val classObj = methodRunTime.classObject
                if (classObj == any) {
                    list.remove(methodRunTime)
                }
            }
        }
    }

    /**
     * 获取所有符合要求的已注册函数
     * @return MethodRunTime
     */
    internal fun getFun(annotationClass: Class<out Annotation>): List<MethodRunTime> {
        return methodMap[annotationClass] ?: listOf()
    }

    /**
     * 运行符合要求的所有已注册函数
     * 适用于广播操作
     * @return NULL
     */
    internal fun runNoReturnFun(annotationClass: Class<out Annotation>, vararg vars: Any = emptyArray()) {
        methodMap[annotationClass]?.run {
            for (methodRunTime in this) {
                val classObject = methodRunTime.classObject
                val function = methodRunTime.function
                try {
                    function.invoke(classObject, *vars)
                } catch (e: InvocationTargetException) {
                    Log.e(objectTag, TAG, "ERROR_MESSAGE: $e, error_cause: ${e.cause}")
                    e.cause?.run {
                        throw this
                    }
                }
            }
        }
    }

    class MethodRunTime(
        val classObject: Any,
        val function: Method
    )

    companion object {
        private const val TAG = "RegisterApi"
        private val objectTag = ObjectTag(core, TAG)
    }
}
