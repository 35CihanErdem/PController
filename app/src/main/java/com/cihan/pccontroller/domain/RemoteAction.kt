package com.cihan.pccontroller.domain

/**
 * Anlamlı kumanda aksiyonları. HID tuşuna değil, kullanıcı niyetine karşılık gelir.
 * Profile mapping [PlatformProfile] içinde yapılır.
 */
sealed class RemoteAction {
    data object NavUp : RemoteAction()
    data object NavDown : RemoteAction()
    data object NavLeft : RemoteAction()
    data object NavRight : RemoteAction()
    data object Confirm : RemoteAction()
    data object Escape : RemoteAction()
    data object Space : RemoteAction()
    data object Fullscreen : RemoteAction()
    data object PlayPause : RemoteAction()
    data object SeekBack : RemoteAction()
    data object SeekForward : RemoteAction()
    data object Prev : RemoteAction()
    data object Next : RemoteAction()
    data object PlaylistPrev : RemoteAction()
    data object PlaylistNext : RemoteAction()
    data object VolumeUp : RemoteAction()
    data object VolumeDown : RemoteAction()
    data object Mute : RemoteAction()
    data object Subtitle : RemoteAction()
    data object StartPresentation : RemoteAction()
    data object BlackScreen : RemoteAction()
}
