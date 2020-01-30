package net.xzos.upgradeAll.data.json.gson

/**
 * [
 *   version_number:
 *   change_log:
 *   assets: [{"name": "", "download_url": "", "file_type": ""}]
 * ]
 */
data class JSReturnData(
        var releaseInfoList: List<ReleaseInfoBean> = listOf()
) {

    /**
     * version_number:
     * change_log:
     * assets: [{"name": "", "download_url": "", "file_type": ""}]
     */
    data class ReleaseInfoBean(
            var version_number: String,
            var change_log: String? = null,  // 考虑到可能无法获取到日志
            var assets: List<AssetsBean> = listOf()
    ) {

        /**
         * name:
         * download_url:  下载地址(最好为直链，否则提供直接导向网址)
         * file_type:  预测的下载文件类型 格式示例: app/arm64
         */
        data class AssetsBean(
                var name: String,
                var download_url: String,
                var file_type: String? = null
        )
    }
}