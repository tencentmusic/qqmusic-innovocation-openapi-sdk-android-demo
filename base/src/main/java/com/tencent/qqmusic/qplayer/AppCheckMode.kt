package com.tencent.qqmusic.qplayer

import androidx.annotation.IntDef
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.CUSTOM
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.STRICT
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.UNCHECK

/**
 * Author: hevinzhou
 * Created: 2025/2/13
 * Description:
 */
@IntDef(STRICT, UNCHECK, CUSTOM)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE
)
annotation class AppCheckMode {
    companion object {
        const val STRICT = 0 // 严格模式
        const val UNCHECK = 1 // 无检查模式
        const val CUSTOM = 2 // 自定义模式
    }
}