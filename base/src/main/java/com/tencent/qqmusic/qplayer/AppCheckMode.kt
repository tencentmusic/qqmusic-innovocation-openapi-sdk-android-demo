package com.tencent.qqmusic.qplayer

import androidx.annotation.IntDef
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.CAR_APK
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.CUSTOM
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.STRICT
import com.tencent.qqmusic.qplayer.AppCheckMode.Companion.UNCHECK

/**
 * Author: hevinzhou
 * Created: 2025/2/13
 * Description:
 */
@IntDef(STRICT, UNCHECK, CAR_APK, CUSTOM)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE
)
annotation class AppCheckMode {
    companion object {
        const val STRICT = 0 // 严格模式
        const val UNCHECK = 1 // 无检查模式

        const val CAR_APK = 2 // 车载apk
        const val CUSTOM = 3 // 自定义模式
    }
}