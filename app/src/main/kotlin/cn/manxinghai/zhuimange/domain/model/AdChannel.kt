package cn.manxinghai.zhuimange.domain.model

/**
 * 广告通道，远程 API 下发 [code] 决定当前使用哪家广告 SDK。
 */
enum class AdChannel(val code: Int, val displayName: String) {
    Umeng(0, "友盟联盟"),
    Csj(1, "穿山甲"),
    Gdt(2, "优量汇"),
    Baidu(3, "百度网盟");

    companion object {
        fun fromCode(code: Int): AdChannel = entries.find { it.code == code } ?: Umeng
    }
}
