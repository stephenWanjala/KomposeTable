package com.github.stephenwanjala.komposetable

import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun androidx.compose.ui.Modifier.platformResizeCursor(): androidx.compose.ui.Modifier {
    return this.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}