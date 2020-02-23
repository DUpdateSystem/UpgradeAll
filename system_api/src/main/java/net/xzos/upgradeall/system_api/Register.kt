package net.xzos.upgradeall.system_api

import java.lang.reflect.Method


open class Register(private val annotationClassList: List<Class<out Annotation>>) {
    private val methodMap: HashMap<Class<out Annotation>, MutableList<Pair<Any, Method>>> = hashMapOf()

    fun register(any: Any) {
        val classObj = any::class.java
        val method: Array<Method> = classObj.declaredMethods
        for (m in method) {
            for (annotationClass in annotationClassList) {
                if (m.isAnnotationPresent(annotationClass)) {
                    m.isAccessible = true
                    (methodMap[annotationClass]
                            ?: mutableListOf<Pair<Any, Method>>().apply {
                                methodMap[annotationClass] = this
                            }).add(Pair(any, m))
                }
            }
        }
    }

    fun unregister(any: Any) {
        for (list in methodMap.values) {
            for (pair in list) {
                val classObj = pair.first
                if (classObj == any) {
                    list.remove(pair)
                }
            }
        }
    }

    internal fun runFun(annotationClass: Class<out Annotation>, vararg vars: Any? = emptyArray()): Any? {
        methodMap[annotationClass]?.run {
            for (pair in this) {
                val classObject = pair.first
                val function = pair.second
                return function.invoke(classObject, *vars)
            }
        }
        return null
    }
}
