
package com.tencent.qqmusic.qplayer.ui.activity.ui

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2

@Composable
fun QQMusicSlider(
    modifier: Modifier = Modifier,
    state: SliderState = rememberSliderState(),
    value: Float,
    thumbValue: Float = value,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    @FloatRange(from = 0.0, to = 1.0)
    progressBegin: Float = 0f,
    readAheadValue: Float = lerp(range.start, range.endInclusive, progressBegin),
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    segments: List<Segment> = emptyList(),
    enabled: Boolean = true,
    colors: DefaultSliderColor = SliderDefaults.seekerColors(),
    dimensions: DefaultSliderDimensions = SliderDefaults.seekerDimensions(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {

    val onValueChangeState by rememberUpdatedState(onValueChange)

    BoxWithConstraints(
        modifier = modifier
            .requiredSizeIn(
                minHeight = SliderDefaults.ThumbRippleRadius * 2,
                minWidth = SliderDefaults.ThumbRippleRadius * 2
            )
            .progressSemantics(value, range, onValueChange, onValueChangeFinished, enabled)
            .focusable(enabled, interactionSource)
    ) {
        val thumbRadius by dimensions.thumbRadius()
        val trackStart: Float
        val endPx = constraints.maxWidth.toFloat()
        val widthPx: Float

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        with(LocalDensity.current) {
            trackStart = thumbRadius.toPx()
            widthPx = endPx - (trackStart * 2)
        }

        val segmentStarts = remember(segments, range, widthPx) {
            segmentToPxValues(segments, range, widthPx)
        }

        LaunchedEffect(thumbValue, segments) {
            state.currentSegment(thumbValue, segments)
        }

        val valuePx = remember(value, widthPx, range) {
            valueToPx(value, widthPx, range)
        }

        val beginValuePx = remember(progressBegin, widthPx, range) {
            valueToPx(progressBegin, widthPx, range)
        }

        val thumbValuePx = remember(thumbValue, widthPx, range) {
            when (thumbValue) {
                value -> valuePx // reuse valuePx if thumbValue equal to value
                else -> valueToPx(thumbValue, widthPx, range)
            }
        }

        val readAheadValuePx = remember(readAheadValue, widthPx, range) {
            valueToPx(readAheadValue, widthPx, range)
        }

        var dragPositionX by remember { mutableStateOf(0f) }
        var pressOffset by remember { mutableStateOf(0f) }

        val scope = rememberCoroutineScope()

        val draggableState = state.draggableState

        LaunchedEffect(widthPx, range) {
            state.onDrag = {
                dragPositionX += it + pressOffset

                pressOffset = 0f
                onValueChangeState(pxToValue(dragPositionX, widthPx, range))
            }
        }

        val press = Modifier.pointerInput(
                range,
                widthPx,
                endPx,
                isRtl,
                enabled,
                thumbRadius,
                interactionSource
            ) {
                detectTapGestures(
                    onPress = { position ->
                        dragPositionX = 0f
                        pressOffset = if (!isRtl) position.x - trackStart else (endPx - position.x) - trackStart
                    },
                    onTap = {
                        scope.launch {
                            draggableState.drag(MutatePriority.UserInput) {
                                dragBy(0f)
                            }
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }

        val drag = Modifier.draggable(
            state = draggableState,
            reverseDirection = isRtl,
            orientation = Orientation.Horizontal,
            onDragStopped = {
                onValueChangeFinished?.invoke()
            },
            interactionSource = interactionSource
        )

        QQMusicSlider(
            modifier = if (enabled) press.then(drag) else Modifier,
            widthPx = widthPx,
            valuePx = valuePx,
            thumbValuePx = thumbValuePx,
            progressBegin = beginValuePx,
            readAheadValuePx = readAheadValuePx,
            enabled = enabled,
            segments = segmentStarts,
            colors = colors,
            dimensions = dimensions,
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun QQMusicSlider(
    modifier: Modifier,
    widthPx: Float,
    valuePx: Float,
    thumbValuePx: Float,
    progressBegin: Float,
    readAheadValuePx: Float,
    enabled: Boolean,
    segments: List<SegmentPxs>,
    colors: DefaultSliderColor,
    dimensions: DefaultSliderDimensions,
    interactionSource: MutableInteractionSource
) {
    Box(
        modifier = modifier.defaultSeekerDimensions(dimensions),
        contentAlignment = Alignment.CenterStart
    ) {
        Track(
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            segments = segments,
            colors = colors,
            widthPx = widthPx,
            valuePx = valuePx,
            progressBegin = progressBegin,
            readAheadValuePx = readAheadValuePx,
            dimensions = dimensions
        )
        Thumb(
            valuePx = { thumbValuePx },
            dimensions = dimensions,
            colors = colors,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun Track(
    modifier: Modifier,
    enabled: Boolean,
    segments: List<SegmentPxs>,
    colors: DefaultSliderColor,
    widthPx: Float,
    valuePx: Float,
    progressBegin: Float,
    readAheadValuePx: Float,
    dimensions: DefaultSliderDimensions
) {
    val trackColor by colors.trackColor(enabled)
    val progressColor by colors.progressColor(enabled)
    val readAheadColor by colors.readAheadColor(enabled)
    val thumbRadius by dimensions.thumbRadius()
    val trackHeight by dimensions.trackHeight()
    val progressHeight by dimensions.progressHeight()
    val segmentGap by dimensions.gap()

    Canvas(
        modifier = modifier.graphicsLayer {
            alpha = 0.99f
        }
    ) {
        val isRtl = false
        val left = thumbRadius.toPx()

        translate(left = left) {
            if (segments.isEmpty()) {
                // draw the track with a single line.
                drawLine(
                    start = Offset(rtlAware(0f, widthPx, isRtl), center.y),
                    end = Offset(rtlAware(widthPx, widthPx, isRtl), center.y),
                    color = trackColor,
                    strokeWidth = trackHeight.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                // draw segments in their respective color,
                // excluding gaps (which will be cleared out later)
                for (index in segments.indices) {
                    val segment = segments[index]
                    val segmentColor = when (segment.color) {
                        Color.Unspecified -> trackColor
                        else -> segment.color
                    }
                    val endPx = rtlAware(segment.endPx, widthPx, isRtl)
                    if (index == 0) {
                        drawSegment(
                            startPx = rtlAware(0f, widthPx, isRtl),
                            endPx = endPx,
                            trackColor = segmentColor,
                            trackHeight = trackHeight.toPx(),
                            blendMode = BlendMode.SrcOver,
                            startCap = null,
                            endCap = null
                        )
                    }else{
                        drawSegment(
                            startPx = rtlAware(segment.startPx, widthPx, isRtl),
                            endPx = endPx,
                            trackColor = segmentColor,
                            trackHeight = trackHeight.toPx(),
                            blendMode = BlendMode.SrcOver,
                            startCap = if (index == 0) StrokeCap.Round else null,
                            endCap = if (index == segments.lastIndex) StrokeCap.Round else null
                        )
                    }

                }
            }

            // readAhead indicator
            val beginValuePx = if (progressBegin >= 0) progressBegin else 0F
            if (readAheadValuePx > 0) {
                drawLine(
                    start = Offset(rtlAware(0f, widthPx, isRtl), center.y),
                    end = Offset(rtlAware(readAheadValuePx, widthPx, isRtl), center.y),
                    color = readAheadColor,
                    strokeWidth = progressHeight.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // progress indicator
            drawLine(
                start = Offset(rtlAware(beginValuePx, widthPx, isRtl), center.y),
                end = Offset(rtlAware(valuePx, widthPx, isRtl), center.y),
                color = progressColor,
                strokeWidth = progressHeight.toPx(),
                cap = StrokeCap.Round
            )

            // clear segment gaps
            for (index in segments.indices) {
                if (index == segments.lastIndex) break // skip "gap" after last segment
                val segment = segments[index]
                drawGap(
                    startPx = rtlAware(segment.endPx - segmentGap.toPx(), widthPx, isRtl),
                    endPx = rtlAware(segment.endPx, widthPx, isRtl),
                    trackHeight = trackHeight.toPx(),
                )
            }
        }
    }
}

private fun DrawScope.drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float = Stroke.HairlineWidth,
    startCap: StrokeCap? = null,
    endCap: StrokeCap? = null,
    blendMode: BlendMode
) {
    val endOffset = if (endCap != null) {
        end.copy(x = end.x - strokeWidth)
    } else {
        end
    }
    inset(horizontal = strokeWidth / 2) {
        drawLine(
            color = color,
            start = start,
            end = endOffset,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )

        startCap?.let {
            drawCap(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = it,
                blendMode = blendMode
            )
        }

        endCap?.let {
            drawCap(
                color = color,
                start = endOffset,
                end = start,
                strokeWidth = strokeWidth,
                cap = it,
                blendMode = blendMode
            )
        }
    }
}

private fun DrawScope.drawCap(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float = Stroke.HairlineWidth,
    cap: StrokeCap,
    blendMode: BlendMode
) {
    when (cap) {
        StrokeCap.Butt -> Unit
        StrokeCap.Round -> {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = start - Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(strokeWidth, strokeWidth),
                blendMode = blendMode,
            )
        }
        StrokeCap.Square -> {
            val offset = Offset(strokeWidth / 2, strokeWidth / 2)
            val size = Size(strokeWidth, strokeWidth)

            rotateRad(
                radians = (end - start).run { atan2(x, y) },
                pivot = start
            ) {
                drawRect(color, topLeft = start - offset, size = size, blendMode = blendMode)
            }
        }
    }
}

private fun DrawScope.drawSegment(
    startPx: Float,
    endPx: Float,
    trackColor: Color,
    trackHeight: Float,
    blendMode: BlendMode,
    startCap: StrokeCap? = null,
    endCap: StrokeCap? = null
) {
    drawLine(
        start = Offset(startPx, center.y),
        end = Offset(endPx, center.y),
        color = trackColor,
        strokeWidth = trackHeight,
        blendMode = blendMode,
        endCap = endCap,
        startCap = startCap
    )
}

private fun DrawScope.drawGap(
    startPx: Float,
    endPx: Float,
    trackHeight: Float,
) {
    drawLine(
        start = Offset(startPx, center.y),
        end = Offset(endPx, center.y),
        color = Color.Black, // any color will do
        strokeWidth = trackHeight + 2, // add 2 to prevent hairline borders from rounding
        blendMode = BlendMode.Clear
    )
}

@Composable
private fun Thumb(
    valuePx: () -> Float,
    dimensions: DefaultSliderDimensions,
    colors: DefaultSliderColor,
    enabled: Boolean,
    interactionSource: MutableInteractionSource
) {
    Spacer(
        modifier = Modifier
            .offset {
                IntOffset(x = valuePx().toInt(), 0)
            }
            .indication(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = SliderDefaults.ThumbRippleRadius
                )
            )
            .hoverable(interactionSource)
            .size(dimensions.thumbRadius().value * 2)
            .clip(CircleShape)
            .background(colors.thumbColor(enabled = enabled).value)
    )
}

private fun Modifier.defaultSeekerDimensions(dimensions: DefaultSliderDimensions) = composed {
    with(dimensions) {
        Modifier
            .heightIn(
                max = (thumbRadius().value * 2).coerceAtLeast(SliderDefaults.MinSliderHeight)
            )
            .widthIn(
                min = SliderDefaults.MinSliderWidth
            )
    }
}

private fun Modifier.progressSemantics(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean
): Modifier {
    val coerced = value.coerceIn(range.start, range.endInclusive)
    return semantics {
        if (!enabled) disabled()
        setProgress { targetValue ->
            val newValue = targetValue.coerceIn(range.start, range.endInclusive)

            if (newValue == coerced) {
                false
            } else {
                onValueChange(newValue)
                onValueChangeFinished?.invoke()
                true
            }
        }
    }.progressSemantics(value, range, 0)
}

object SliderDefaults {

    @Composable
     fun seekerColors(
        progressColor: Color = MaterialTheme.colors.primary,
        trackColor: Color = TrackColor,
        disabledProgressColor: Color = MaterialTheme.colors.onSurface.copy(alpha = DisabledProgressAlpha),
        disabledTrackColor: Color = disabledProgressColor
            .copy(alpha = DisabledTrackAlpha)
            .compositeOver(MaterialTheme.colors.onSurface),
        thumbColor: Color = MaterialTheme.colors.primary,
        disabledThumbColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        readAheadColor: Color = ReadAheadColor
    ): DefaultSliderColor = DefaultSliderColor(
        progressColor = progressColor,
        trackColor = trackColor,
        disabledProgressColor = disabledProgressColor,
        disabledTrackColor = disabledTrackColor,
        thumbColor = thumbColor,
        disabledThumbColor = disabledThumbColor,
        readAheadColor = readAheadColor
    )

    @Composable
    fun seekerDimensions(
        trackHeight: Dp = TrackHeight,
        progressHeight: Dp = trackHeight,
        thumbRadius: Dp = ThumbRadius,
        gap: Dp = Gap
    ): DefaultSliderDimensions = DefaultSliderDimensions(
        trackHeight = trackHeight,
        progressHeight = progressHeight,
        thumbRadius = thumbRadius,
        gap = gap
    )

    private val TrackColor = Color(0xFFD9D9D9)
    private val ReadAheadColor = Color(0xFFBDBDBD)

    private const val TrackAlpha = 0.24f
    private const val ReadAheadAlpha = 0.44f
    private const val DisabledTrackAlpha = 0.22f
    private const val DisabledProgressAlpha = 0.32f

    internal val ThumbRadius = 10.dp
    private val TrackHeight = 4.dp
    private val Gap = 2.dp

    internal val MinSliderHeight = 48.dp
    internal val MinSliderWidth = ThumbRadius * 2

    internal val ThumbDefaultElevation = 1.dp
    internal val ThumbPressedElevation = 6.dp

    internal val ThumbRippleRadius = 24.dp
}



@Immutable
class DefaultSliderDimensions(
    val trackHeight: Dp,
    val progressHeight: Dp,
    val gap: Dp,
    val thumbRadius: Dp
)  {
    @Composable
     fun trackHeight(): State<Dp> {
        return rememberUpdatedState(trackHeight)
    }

    @Composable
     fun progressHeight(): State<Dp> {
        return rememberUpdatedState(progressHeight)
    }

    @Composable
     fun gap(): State<Dp> {
        return rememberUpdatedState(gap)
    }

    @Composable
     fun thumbRadius(): State<Dp> {
        return rememberUpdatedState(thumbRadius)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSliderDimensions

        if (trackHeight != other.trackHeight) return false
        if (progressHeight != other.progressHeight) return false
        if (gap != other.gap) return false
        if (thumbRadius != other.thumbRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackHeight.hashCode()

        result = 31 * result + progressHeight.hashCode()
        result = 31 * result + gap.hashCode()
        result = 31 * result + thumbRadius.hashCode()

        return result
    }
}

@Immutable
class DefaultSliderColor(
    val progressColor: Color,
    val trackColor: Color,
    val disabledTrackColor: Color,
    val disabledProgressColor: Color,
    val thumbColor: Color,
    val disabledThumbColor: Color,
    val readAheadColor: Color
)  {
    @Composable
    fun trackColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) trackColor else disabledTrackColor
        )
    }

    @Composable
    fun thumbColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) thumbColor else disabledThumbColor
        )
    }

    @Composable
    fun progressColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) progressColor else disabledProgressColor
        )
    }

    @Composable
    fun readAheadColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            readAheadColor
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSliderColor

        if (progressColor != other.progressColor) return false
        if (trackColor != other.trackColor) return false
        if (disabledTrackColor != other.disabledTrackColor) return false
        if (disabledProgressColor != other.disabledProgressColor) return false
        if (thumbColor != other.thumbColor) return false
        if (disabledThumbColor != other.disabledThumbColor) return false
        if (readAheadColor != other.readAheadColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = progressColor.hashCode()

        result = 31 * result + trackColor.hashCode()
        result = 31 * result + disabledTrackColor.hashCode()
        result = 31 * result + disabledProgressColor.hashCode()
        result = 31 * result + thumbColor.hashCode()
        result = 31 * result + disabledThumbColor.hashCode()
        result = 31 * result + readAheadColor.hashCode()

        return result
    }
}

/**
 * A state object which can be hoisted to observe the current segment of Seeker. In most cases this
 * will be created by [rememberSliderState]
 * */
@Stable
class SliderState {

    /**
     * The current segment corresponding to the current seeker value.
     * */
    var currentSegment: Segment by mutableStateOf(Segment.Unspecified)

    internal var onDrag: ((Float) -> Unit)? = null

    internal val draggableState = DraggableState {
        onDrag?.invoke(it)
    }

    internal fun currentSegment(
        value: Float,
        segments: List<Segment>
    ) = (segments.findLast { value >= it.start } ?: Segment.Unspecified).also { this.currentSegment = it }
}


@Composable
fun rememberSliderState(): SliderState = remember {
    SliderState()
}

@Immutable
data class Segment(
    val name: String,
    val start: Float,
    val color: Color = Color.Unspecified
) {
    companion object {
        val Unspecified = Segment(name = "", start = 0f)
    }
}

@Immutable
internal data class SegmentPxs(
    val name: String,
    val startPx: Float,
    val endPx: Float,
    val color: Color
)

internal fun valueToPx(
    value: Float,
    widthPx: Float,
    range: ClosedFloatingPointRange<Float>
): Float {
    val rangeSIze = range.endInclusive - range.start
    val p = value.coerceIn(range.start, range.endInclusive)
    val progressPercent = (p - range.start) * 100 / rangeSIze
    return (progressPercent * widthPx / 100)
}

// returns the corresponding progress value for a position in slider
internal fun pxToValue(
    position: Float,
    widthPx: Float,
    range: ClosedFloatingPointRange<Float>
): Float {
    val rangeSize = range.endInclusive - range.start
    val percent = position * 100 / widthPx
    return ((percent * (rangeSize) / 100) + range.start).coerceIn(
        range.start,
        range.endInclusive
    )
}

// converts the start value of a segment to the corresponding start and end pixel values
// at which the segment will start and end on the track.
internal fun segmentToPxValues(
    segments: List<Segment>,
    range: ClosedFloatingPointRange<Float>,
    widthPx: Float,
): List<SegmentPxs> {

    val rangeSize = range.endInclusive - range.start
    val sortedSegments = segments.distinct().sortedBy { it.start }
    val segmentStartPxs = sortedSegments.map { segment ->

        // percent of the start of this segment in the range size
        val percent = (segment.start - range.start) / rangeSize
        val startPx = percent * widthPx
        startPx
    }

    return sortedSegments.mapIndexed { index, segment ->
        val endPx = if (index != sortedSegments.lastIndex) segmentStartPxs[index + 1] else widthPx
        val startPx = segmentStartPxs[index]
        SegmentPxs(
            name = segment.name,
            color = segment.color,
            startPx = startPx,
            endPx = endPx
        )
    }
}

internal fun rtlAware(value: Float, widthPx: Float, isRtl: Boolean) =
    if (isRtl) widthPx - value else value

internal fun lerp(start: Float, end: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * end
}
