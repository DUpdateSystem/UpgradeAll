package net.xzos.upgradeall.core.utils.log

class ObjectTag(
    // 分类
    var sort: String,
    // 对象名称（日志需要，即时更新）
    var name: String
){
    companion object{
        const val core = "Core"
    }
}