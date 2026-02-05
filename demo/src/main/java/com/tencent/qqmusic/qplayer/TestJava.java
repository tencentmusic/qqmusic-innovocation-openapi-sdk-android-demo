package com.tencent.qqmusic.qplayer;

import android.app.Notification;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.tencent.qqmusic.openapisdk.core.OpenApiSDK;
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApi;
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse;
import com.tencent.qqmusic.openapisdk.core.player.ISDKSpecialNeedInterface;
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums;
import com.tencent.qqmusic.openapisdk.model.Album;
import com.tencent.qqmusic.openapisdk.model.SongInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Created by clydeazhang on 2021/9/25 10:55 上午.
 * Copyright (c) 2021 Tencent. All rights reserved.
 */
class TestJava {
    public static void main(String[] args) {
        OpenApiSDK.getOpenApi().fetchAlbumDetail("1L", "",
                new Function1<OpenApiResponse<Album>, Unit>() {
                    @Override
                    public Unit invoke(OpenApiResponse<Album> albumOpenApiResponse) {
                        return null;
                    }
                });


        OpenApiSDK.getPlayerApi().setCurrentPlaySongQuality(PlayerEnums.Quality.HQ);
    }
}
