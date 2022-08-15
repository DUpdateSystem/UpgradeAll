##---------------Begin: proguard configuration for GPlay API ----------
-keep public class com.aurora.gplayapi.** { *; }
##---------------End: proguard configuration for GPlay API ----------

##---------------Begin: proguard configuration for dom4j ----------
-keep class org.dom4j.** { *; }
-keep interface org.dom4j.** { *; }
-keep class javax.xml.** { *; }
-keep class org.w3c.** { *; }
-keep class org.xml.** { *; }
-keep class org.xmlpull.** { *; }
-keep class org.jaxen.** { *; }
##---------------End: proguard configuration for dom4j ----------
