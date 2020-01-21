package net.xzos.upgradeAll.data.json.gson

/**
 * [
 *   version_number :
 *   change_log :
 *   assets : [{"name":"","download_url":""}]
 * ]
 */
data class JSReturnData(
        var releaseInfoList: List<ReleaseInfoBean> = listOf()
) {

    /**
     * version_number :
     * change_log :
     * assets : [{"name":"","download_url":""}]
     */
    data class ReleaseInfoBean(
            var version_number: String,
            var change_log: String? = null,  // 考虑到可能无法获取到日志
            var assets: List<AssetsBean> = listOf()
    ) {

        /**
         * name:
         * download_url:
         */
        data class AssetsBean(
                var name: String,
                var download_url: String
        )
    }
}