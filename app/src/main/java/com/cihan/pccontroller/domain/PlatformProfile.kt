package com.cihan.pccontroller.domain

import com.cihan.pccontroller.R
import com.cihan.pccontroller.bluetooth.ConsumerAction
import com.cihan.pccontroller.bluetooth.HidReports
import com.cihan.pccontroller.bluetooth.KeyCode

enum class PlatformId {
    WEB_VIDEO,
    YOUTUBE,
    NETFLIX,
    SPOTIFY,
    PRESENTATION,
    GENERAL
}

/**
 * Kumanda üzerindeki bir buton yuvası: hangi aksiyon + hangi etiket.
 * Görünmeyen yuvalar [visible]=false ile gizlenir.
 */
data class RemoteButtonSpec(
    val action: RemoteAction,
    val labelRes: Int,
    val visible: Boolean = true,
    val primary: Boolean = false
)

/**
 * Profile göre kumanda düzeni — UI bunu uygular, HID bilmez.
 */
data class RemoteLayout(
    val topPrimary: RemoteButtonSpec?,
    /** Önceki / Play / Sonraki satırı (Spotify, YouTube) */
    val mediaRow: Triple<RemoteButtonSpec, RemoteButtonSpec, RemoteButtonSpec>?,
    val dpadUp: RemoteButtonSpec?,
    val dpadLeft: RemoteButtonSpec?,
    val dpadCenter: RemoteButtonSpec?,
    val dpadRight: RemoteButtonSpec?,
    val dpadDown: RemoteButtonSpec?,
    /** -10 / +10 veya Space / Esc gibi yan yana çiftler */
    val rowA: Pair<RemoteButtonSpec, RemoteButtonSpec>?,
    val rowB: Pair<RemoteButtonSpec, RemoteButtonSpec>?,
    val extra: RemoteButtonSpec?
)

data class PlatformProfile(
    val id: PlatformId,
    val titleRes: Int,
    val subtitleRes: Int,
    val mapping: Map<RemoteAction, HidCommand>,
    val layout: RemoteLayout
) {
    fun resolve(action: RemoteAction): HidCommand? = mapping[action]
}

object PlatformCatalog {

    val all: List<PlatformProfile> by lazy {
        // Web/Film üstte: HBO Max, tarayıcı oynatıcıları, “her yerde video” için varsayılan
        listOf(webVideo, youtube, netflix, spotify, presentation, general)
    }

    fun byId(id: PlatformId): PlatformProfile =
        all.first { it.id == id }

    fun byIdOrGeneral(id: String?): PlatformProfile {
        val parsed = id?.let { runCatching { PlatformId.valueOf(it) }.getOrNull() }
        return if (parsed != null) byId(parsed) else webVideo
    }

    /**
     * HBO Max / Disney+ / tarayıcı video / HTML5 oynatıcılar.
     * Siteye özel kısayol yoksa Space · oklar · F · Esc çoğu yerde iş görür.
     */
    private val webVideo = PlatformProfile(
        id = PlatformId.WEB_VIDEO,
        titleRes = R.string.platform_web,
        subtitleRes = R.string.platform_web_sub,
        mapping = mapOf(
            RemoteAction.Fullscreen to HidCommand.Key(KeyCode.F),
            RemoteAction.PlayPause to HidCommand.Key(KeyCode.SPACE),
            RemoteAction.NavUp to HidCommand.Key(KeyCode.UP_ARROW),
            RemoteAction.NavDown to HidCommand.Key(KeyCode.DOWN_ARROW),
            RemoteAction.NavLeft to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.NavRight to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.SeekBack to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.SeekForward to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.Confirm to HidCommand.Key(KeyCode.ENTER),
            RemoteAction.Escape to HidCommand.Key(KeyCode.ESC),
            RemoteAction.Space to HidCommand.Key(KeyCode.SPACE),
            RemoteAction.Mute to HidCommand.Key(KeyCode.M),
            RemoteAction.VolumeDown to HidCommand.Consumer(ConsumerAction.VOLUME_DOWN),
            RemoteAction.VolumeUp to HidCommand.Consumer(ConsumerAction.VOLUME_UP)
        ),
        layout = RemoteLayout(
            topPrimary = RemoteButtonSpec(RemoteAction.Fullscreen, R.string.key_fullscreen, primary = true),
            mediaRow = Triple(
                RemoteButtonSpec(RemoteAction.SeekBack, R.string.key_seek_back),
                RemoteButtonSpec(RemoteAction.PlayPause, R.string.key_play_symbol),
                RemoteButtonSpec(RemoteAction.SeekForward, R.string.key_seek_fwd)
            ),
            dpadUp = RemoteButtonSpec(RemoteAction.NavUp, R.string.key_up),
            dpadLeft = RemoteButtonSpec(RemoteAction.NavLeft, R.string.key_left),
            dpadCenter = RemoteButtonSpec(RemoteAction.Confirm, R.string.key_ok),
            dpadRight = RemoteButtonSpec(RemoteAction.NavRight, R.string.key_right),
            dpadDown = RemoteButtonSpec(RemoteAction.NavDown, R.string.key_down),
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.Escape, R.string.key_esc),
                RemoteButtonSpec(RemoteAction.Mute, R.string.key_mute)
            ),
            rowB = Pair(
                RemoteButtonSpec(RemoteAction.VolumeDown, R.string.key_vol_down),
                RemoteButtonSpec(RemoteAction.VolumeUp, R.string.key_vol_up)
            ),
            extra = null
        )
    )

    private val youtube = PlatformProfile(
        id = PlatformId.YOUTUBE,
        titleRes = R.string.platform_youtube,
        subtitleRes = R.string.platform_youtube_sub,
        mapping = mapOf(
            RemoteAction.Fullscreen to HidCommand.Key(KeyCode.F),
            // J / K / L — YouTube'da en güvenilir
            RemoteAction.Prev to HidCommand.Key(KeyCode.J),
            RemoteAction.PlayPause to HidCommand.Key(KeyCode.K),
            RemoteAction.Next to HidCommand.Key(KeyCode.L),
            RemoteAction.SeekBack to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.SeekForward to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.NavUp to HidCommand.Key(KeyCode.UP_ARROW),
            RemoteAction.NavDown to HidCommand.Key(KeyCode.DOWN_ARROW),
            RemoteAction.NavLeft to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.NavRight to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.Confirm to HidCommand.Key(KeyCode.K),
            // Playlist: Shift+P / Shift+N (liste yoksa önceki sessiz kalabilir)
            RemoteAction.PlaylistPrev to HidCommand.Key(KeyCode.P, HidReports.MOD_LEFT_SHIFT),
            RemoteAction.PlaylistNext to HidCommand.Key(KeyCode.N, HidReports.MOD_LEFT_SHIFT),
            RemoteAction.VolumeDown to HidCommand.Consumer(ConsumerAction.VOLUME_DOWN),
            RemoteAction.VolumeUp to HidCommand.Consumer(ConsumerAction.VOLUME_UP),
            RemoteAction.Escape to HidCommand.Key(KeyCode.ESC),
            RemoteAction.Space to HidCommand.Key(KeyCode.SPACE)
        ),
        layout = RemoteLayout(
            topPrimary = RemoteButtonSpec(RemoteAction.Fullscreen, R.string.key_fullscreen, primary = true),
            mediaRow = Triple(
                RemoteButtonSpec(RemoteAction.Prev, R.string.key_seek_back_10),
                RemoteButtonSpec(RemoteAction.PlayPause, R.string.key_play_symbol),
                RemoteButtonSpec(RemoteAction.Next, R.string.key_seek_fwd_10)
            ),
            dpadUp = RemoteButtonSpec(RemoteAction.NavUp, R.string.key_up),
            dpadLeft = RemoteButtonSpec(RemoteAction.NavLeft, R.string.key_seek_back),
            dpadCenter = RemoteButtonSpec(RemoteAction.Confirm, R.string.key_play_symbol),
            dpadRight = RemoteButtonSpec(RemoteAction.NavRight, R.string.key_seek_fwd),
            dpadDown = RemoteButtonSpec(RemoteAction.NavDown, R.string.key_down),
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.PlaylistPrev, R.string.key_prev),
                RemoteButtonSpec(RemoteAction.PlaylistNext, R.string.key_next)
            ),
            rowB = Pair(
                RemoteButtonSpec(RemoteAction.VolumeDown, R.string.key_vol_down),
                RemoteButtonSpec(RemoteAction.VolumeUp, R.string.key_vol_up)
            ),
            extra = RemoteButtonSpec(RemoteAction.Escape, R.string.key_esc)
        )
    )

    private val netflix = PlatformProfile(
        id = PlatformId.NETFLIX,
        titleRes = R.string.platform_netflix,
        subtitleRes = R.string.platform_netflix_sub,
        mapping = mapOf(
            RemoteAction.Fullscreen to HidCommand.Key(KeyCode.F),
            RemoteAction.PlayPause to HidCommand.Key(KeyCode.SPACE),
            RemoteAction.NavLeft to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.NavRight to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.NavUp to HidCommand.Key(KeyCode.UP_ARROW),
            RemoteAction.NavDown to HidCommand.Key(KeyCode.DOWN_ARROW),
            RemoteAction.Confirm to HidCommand.Key(KeyCode.ENTER),
            RemoteAction.VolumeDown to HidCommand.Consumer(ConsumerAction.VOLUME_DOWN),
            RemoteAction.VolumeUp to HidCommand.Consumer(ConsumerAction.VOLUME_UP),
            RemoteAction.Escape to HidCommand.Key(KeyCode.ESC),
            RemoteAction.SeekBack to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.SeekForward to HidCommand.Key(KeyCode.RIGHT_ARROW)
        ),
        layout = RemoteLayout(
            topPrimary = RemoteButtonSpec(RemoteAction.Fullscreen, R.string.key_fullscreen, primary = true),
            mediaRow = null,
            dpadUp = RemoteButtonSpec(RemoteAction.NavUp, R.string.key_up),
            dpadLeft = RemoteButtonSpec(RemoteAction.NavLeft, R.string.key_left),
            dpadCenter = RemoteButtonSpec(RemoteAction.Confirm, R.string.key_ok),
            dpadRight = RemoteButtonSpec(RemoteAction.NavRight, R.string.key_right),
            dpadDown = RemoteButtonSpec(RemoteAction.NavDown, R.string.key_down),
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.PlayPause, R.string.key_play_pause),
                RemoteButtonSpec(RemoteAction.Escape, R.string.key_esc)
            ),
            rowB = Pair(
                RemoteButtonSpec(RemoteAction.VolumeDown, R.string.key_vol_down),
                RemoteButtonSpec(RemoteAction.VolumeUp, R.string.key_vol_up)
            ),
            extra = null
        )
    )

    private val spotify = PlatformProfile(
        id = PlatformId.SPOTIFY,
        titleRes = R.string.platform_spotify,
        subtitleRes = R.string.platform_spotify_sub,
        mapping = mapOf(
            RemoteAction.Prev to HidCommand.Consumer(ConsumerAction.SCAN_PREVIOUS),
            RemoteAction.PlayPause to HidCommand.Consumer(ConsumerAction.PLAY_PAUSE),
            RemoteAction.Next to HidCommand.Consumer(ConsumerAction.SCAN_NEXT),
            RemoteAction.SeekBack to HidCommand.Consumer(ConsumerAction.SCAN_PREVIOUS),
            RemoteAction.SeekForward to HidCommand.Consumer(ConsumerAction.SCAN_NEXT),
            RemoteAction.VolumeDown to HidCommand.Consumer(ConsumerAction.VOLUME_DOWN),
            RemoteAction.VolumeUp to HidCommand.Consumer(ConsumerAction.VOLUME_UP),
            RemoteAction.Mute to HidCommand.Consumer(ConsumerAction.MUTE)
        ),
        layout = RemoteLayout(
            topPrimary = null,
            mediaRow = Triple(
                RemoteButtonSpec(RemoteAction.Prev, R.string.key_prev),
                RemoteButtonSpec(RemoteAction.PlayPause, R.string.key_play_symbol),
                RemoteButtonSpec(RemoteAction.Next, R.string.key_next)
            ),
            dpadUp = null,
            dpadLeft = null,
            dpadCenter = null,
            dpadRight = null,
            dpadDown = null,
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.VolumeDown, R.string.key_vol_down),
                RemoteButtonSpec(RemoteAction.VolumeUp, R.string.key_vol_up)
            ),
            rowB = null,
            extra = RemoteButtonSpec(RemoteAction.Mute, R.string.key_mute, primary = true)
        )
    )

    private val presentation = PlatformProfile(
        id = PlatformId.PRESENTATION,
        titleRes = R.string.platform_presentation,
        subtitleRes = R.string.platform_presentation_sub,
        mapping = mapOf(
            RemoteAction.StartPresentation to HidCommand.Key(KeyCode.F5),
            RemoteAction.NavLeft to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.NavRight to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.NavUp to HidCommand.Key(KeyCode.UP_ARROW),
            RemoteAction.NavDown to HidCommand.Key(KeyCode.DOWN_ARROW),
            RemoteAction.SeekBack to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.SeekForward to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.Escape to HidCommand.Key(KeyCode.ESC),
            RemoteAction.BlackScreen to HidCommand.Key(KeyCode.B)
        ),
        layout = RemoteLayout(
            topPrimary = RemoteButtonSpec(
                RemoteAction.StartPresentation,
                R.string.key_start_presentation,
                primary = true
            ),
            mediaRow = null,
            dpadUp = RemoteButtonSpec(RemoteAction.NavUp, R.string.key_up),
            dpadLeft = RemoteButtonSpec(RemoteAction.NavLeft, R.string.key_left),
            dpadCenter = null,
            dpadRight = RemoteButtonSpec(RemoteAction.NavRight, R.string.key_right),
            dpadDown = RemoteButtonSpec(RemoteAction.NavDown, R.string.key_down),
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.Escape, R.string.key_esc),
                RemoteButtonSpec(RemoteAction.BlackScreen, R.string.key_black_screen)
            ),
            rowB = null,
            extra = null
        )
    )

    private val general = PlatformProfile(
        id = PlatformId.GENERAL,
        titleRes = R.string.platform_general,
        subtitleRes = R.string.platform_general_sub,
        mapping = mapOf(
            RemoteAction.NavUp to HidCommand.Key(KeyCode.UP_ARROW),
            RemoteAction.NavDown to HidCommand.Key(KeyCode.DOWN_ARROW),
            RemoteAction.NavLeft to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.NavRight to HidCommand.Key(KeyCode.RIGHT_ARROW),
            RemoteAction.PlayPause to HidCommand.Consumer(ConsumerAction.PLAY_PAUSE),
            RemoteAction.Space to HidCommand.Key(KeyCode.SPACE),
            RemoteAction.Escape to HidCommand.Key(KeyCode.ESC),
            RemoteAction.Fullscreen to HidCommand.Key(KeyCode.F),
            RemoteAction.VolumeDown to HidCommand.Consumer(ConsumerAction.VOLUME_DOWN),
            RemoteAction.VolumeUp to HidCommand.Consumer(ConsumerAction.VOLUME_UP),
            RemoteAction.SeekBack to HidCommand.Key(KeyCode.LEFT_ARROW),
            RemoteAction.SeekForward to HidCommand.Key(KeyCode.RIGHT_ARROW)
        ),
        layout = RemoteLayout(
            topPrimary = RemoteButtonSpec(RemoteAction.Fullscreen, R.string.key_fullscreen, primary = true),
            mediaRow = null,
            dpadUp = RemoteButtonSpec(RemoteAction.NavUp, R.string.key_up),
            dpadLeft = RemoteButtonSpec(RemoteAction.NavLeft, R.string.key_left),
            dpadCenter = RemoteButtonSpec(RemoteAction.PlayPause, R.string.key_play_symbol),
            dpadRight = RemoteButtonSpec(RemoteAction.NavRight, R.string.key_right),
            dpadDown = RemoteButtonSpec(RemoteAction.NavDown, R.string.key_down),
            rowA = Pair(
                RemoteButtonSpec(RemoteAction.Space, R.string.key_space),
                RemoteButtonSpec(RemoteAction.Escape, R.string.key_esc)
            ),
            rowB = Pair(
                RemoteButtonSpec(RemoteAction.VolumeDown, R.string.key_vol_down),
                RemoteButtonSpec(RemoteAction.VolumeUp, R.string.key_vol_up)
            ),
            extra = null
        )
    )
}
