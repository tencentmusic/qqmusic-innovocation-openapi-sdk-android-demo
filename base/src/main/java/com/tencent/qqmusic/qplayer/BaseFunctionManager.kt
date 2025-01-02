package com.tencent.qqmusic.qplayer

object BaseFunctionManager {
    var proxy: IBaseFunction

    init {
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName("com.tencent.qqmusic.qplayer.BaseFuncProxy")
        } catch (ignore: Exception) {
        }
        proxy = if (clazz != null) {
            clazz.newInstance() as IBaseFunction
        } else {
            DefaultBaseFunction()
        }

    }


}