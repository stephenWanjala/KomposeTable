package io.github.stephenwanjala.komposetable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.platformResizeCursor(): Modifier {
    return this.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}