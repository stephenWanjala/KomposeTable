package io.github.stephenwanjala.komposetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.stephenwanjala.komposetable.KomposeTableState.Companion.Saver
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A composable function that displays a table with customizable columns, data, and behavior.
 *
 * @param T The type of data displayed in the table. Must be a non-nullable `Any` type.
 * @param columns A list of [TableSortColumn] objects defining the table's columns.
 * @param tableData A list of objects of type `T` to be displayed in the table.
 * @param modifier A [Modifier] to be applied to the table.
 * @param selectionModel A [TableSelectionModel] to manage row selection.
 * @param sortState A [MutableState] of [SortState] to manage column sorting.
 * @param state A [KomposeTableState] to manage table configuration, including default selections.
 * @param colors A [KomposeTableColors] object for table colors.
 * @param rowTextStyle The [TextStyle] for the text in table rows.
 * @param headerTextStyle The [TextStyle] for the text in the table header.
 * @param onRowClick A callback invoked when a row is clicked, providing the item and its index.
 * @param onRowDoubleClick A callback invoked when a row is double-clicked, providing the item and its index.
 * @param onSelectionChange A callback invoked when the selection changes, providing the list of selected items.
 */
@Composable
inline fun <reified T : Any> KomposeTable(
    columns: List<TableSortColumn<T>>,
    tableData: List<T>,
    modifier: Modifier = Modifier,
    selectionModel: TableSelectionModel<T> = remember { TableSelectionModel<T>() },
    sortState: MutableState<SortState> = remember { mutableStateOf(SortState()) },
    state: KomposeTableState = rememberKomposeTableState(),
    colors: KomposeTableColors = KomposeTableDefaults.colors,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    headerTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    noinline onRowClick: ((T, Int) -> Unit)? = null,
    noinline onRowDoubleClick: ((T, Int) -> Unit)? = null,
    noinline onSelectionChange: ((List<T>) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()

    // Initialize default selections once
    LaunchedEffect(state.defaultSelectedIndices, state.selectionMode, tableData) {
        selectionModel.selectionMode = state.selectionMode
        selectionModel.clearSelection(onSelectionChange)
        val validIndices = state.defaultSelectedIndices.filter { it in tableData.indices }
        validIndices.forEach { index ->
            selectionModel.selectItem(tableData[index], index)
        }
        if (validIndices.isNotEmpty()) {
            onSelectionChange?.invoke(selectionModel.selectedItems)
        }
    }

    // Observe selection changes
    LaunchedEffect(selectionModel) {
        snapshotFlow { selectionModel.selectedItems.toList() }
            .map { it.map { item -> item.hashCode() } } // Use hash codes to detect content changes
            .distinctUntilChanged()
            .collect { onSelectionChange?.invoke(selectionModel.selectedItems) }
    }

    // Track table width for constrained resizing
    var tableWidth by remember { mutableStateOf(0.dp) }

    // Column widths state for resizing
    val columnWidths = remember {
        mutableStateMapOf<String, Dp>().apply {
            columns.forEach { column ->
                put(column.id, column.width)
            }
        }
    }

    // Hover state
    var hoveredRowIndex by remember { mutableStateOf(-1) }

    // Sort the data based on current sort state
    val sortedData = remember(tableData, sortState.value) {
        val currentSort = sortState.value
        if (currentSort.order == SortOrder.NONE) {
            tableData
        } else {
            val column = columns.find { it.id == currentSort.columnId }
            val comparator =
                column?.comparator ?: compareBy { column?.valueExtractor?.invoke(it) ?: "" }
            when (currentSort.order) {
                SortOrder.ASCENDING -> tableData.sortedWith(comparator)
                SortOrder.DESCENDING -> tableData.sortedWith(comparator.reversed())
                SortOrder.NONE -> tableData
            }
        }
    }

    // Distribute extra width in CONSTRAINED mode
    val distributedColumnWidths: Map<String, Dp> =
        remember(tableWidth, columnWidths, state.columnResizeMode, columns) {
            if (state.columnResizeMode == ColumnResizeMode.CONSTRAINED && tableWidth > 0.dp) {
                val visibleColumns = columns.filter { it.visible }
                val currentWidths = visibleColumns.map { column ->
                    columnWidths[column.id] ?: column.width
                }
                val totalCurrentWidth = currentWidths.sumOf { it.value.toDouble() }.dp

                if (totalCurrentWidth < tableWidth) {
                    val extraSpace = (tableWidth - totalCurrentWidth) / visibleColumns.size
                    visibleColumns.associate { column ->
                        val baseWidth = columnWidths[column.id] ?: column.width
                        column.id to (baseWidth + extraSpace)
                    }
                } else {
                    columnWidths.toMap()
                }
            } else {
                columnWidths.toMap()
            }
        }

    val tableContent = @Composable {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    tableWidth = with(density) { coordinates.size.width.toDp() }
                }
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(colors.headerBackgroundColor)
                    .horizontalScroll(horizontalScrollState)
                    .height(48.dp),
            ) {
                columns.filter { it.visible }.forEach { column ->
                    val currentWidth = distributedColumnWidths[column.id] ?: column.width

                    Box(
                        modifier = Modifier
                            .width(currentWidth)
                            .fillMaxHeight()
                            .then(
                                if (state.showVerticalDividers) {
                                    Modifier.border(
                                        width = state.dividerThickness,
                                        color = colors.headerBorderColor,
                                    )
                                } else Modifier
                            )
                            .then(
                                if (state.enableSorting && column.sortable) {
                                    Modifier.clickable {
                                        val currentSort = sortState.value
                                        val newOrder = when {
                                            currentSort.columnId != column.id -> SortOrder.ASCENDING
                                            currentSort.order == SortOrder.ASCENDING -> SortOrder.DESCENDING
                                            currentSort.order == SortOrder.DESCENDING -> SortOrder.NONE
                                            else -> SortOrder.ASCENDING
                                        }
                                        sortState.value = SortState(column.id, newOrder)
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = column.title,
                                style = headerTextStyle,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )

                            if (state.enableSorting && column.sortable) {
                                val sortIcon = when {
                                    sortState.value.columnId == column.id && sortState.value.order == SortOrder.ASCENDING -> KeyboardArrowUp
                                    sortState.value.columnId == column.id && sortState.value.order == SortOrder.DESCENDING -> ArrowDropDown
                                    else -> ArrowDropDown
                                }

                                Icon(
                                    imageVector = sortIcon,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (sortState.value.columnId == column.id) {
                                        MaterialTheme.colorScheme.primary
                                    } else Color.Gray,
                                )
                            }
                        }

                        // Column resize handle
                        if (state.enableColumnResizing && column.resizable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .align(Alignment.CenterEnd)
                                    .background(Color.Transparent)
                                    .platformResizeCursor()
                                    .pointerInput(column.id, state.columnResizeMode, tableWidth) {
                                        detectDragGestures { change, _ ->
                                            val currentWidth =
                                                columnWidths[column.id] ?: column.width
                                            val newWidthPx = with(density) {
                                                (currentWidth.toPx() + change.position.x).coerceIn(
                                                    column.minWidth.toPx(),
                                                    column.maxWidth.toPx()
                                                )
                                            }
                                            val newWidth = with(density) { newWidthPx.toDp() }

                                            if (state.columnResizeMode == ColumnResizeMode.CONSTRAINED) {
                                                columnWidths[column.id] = newWidth
                                            } else {
                                                columnWidths[column.id] = newWidth
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Horizontal divider after header
            if (state.showHorizontalDividers) {
                HorizontalDivider(
                    thickness = state.dividerThickness,
                    color = colors.dividerColor,
                )
            }

            // Table Body
            LazyColumn(
                state = verticalScrollState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(sortedData) { index, item ->
                    val isSelected = selectionModel.isSelected(item)
                    val isHovered = hoveredRowIndex == index && state.enableHover

                    val rowBackgroundColor = when {
                        isSelected -> colors.selectedRowColor
                        isHovered -> colors.hoveredRowColor
                        index % 2 == 0 -> colors.alternatingRowColors[0]
                        else -> colors.alternatingRowColors.getOrElse(1) { colors.alternatingRowColors[0] }
                    }

                    Row(
                        modifier = Modifier
                            .background(rowBackgroundColor)
                            .horizontalScroll(horizontalScrollState)
                            .height(40.dp)
                            .then(
                                if (state.enableSelection) {
                                    Modifier.clickable {
                                        if (isSelected) {
                                            selectionModel.deselectItem(
                                                item,
                                                index,
                                                onSelectionChange
                                            )
                                        } else {
                                            selectionModel.selectItem(
                                                item,
                                                index,
                                                onSelectionChange
                                            )
                                        }
                                        onRowClick?.invoke(item, index)
                                    }
                                } else if (onRowClick != null) {
                                    Modifier.clickable { onRowClick.invoke(item, index) }
                                } else Modifier
                            )
                            .then(
                                if (state.enableHover) {
                                    Modifier.pointerInput(index) {
                                        detectDragGestures(
                                            onDragStart = { hoveredRowIndex = index },
                                            onDragEnd = { hoveredRowIndex = -1 },
                                        ) { _, _ -> }
                                    }
                                } else Modifier
                            ),
                    ) {
                        columns.filter { it.visible }.forEach { column ->
                            val currentWidth = distributedColumnWidths[column.id] ?: column.width

                            Box(
                                modifier = Modifier
                                    .width(currentWidth)
                                    .fillMaxHeight()
                                    .then(
                                        if (state.showVerticalDividers) {
                                            Modifier.border(
                                                width = state.dividerThickness,
                                                color = colors.dividerColor,
                                            )
                                        } else Modifier
                                    )
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                column.cellFactory(item)
                            }
                        }
                    }

                    // Horizontal divider after each row
                    if (state.showHorizontalDividers && index < sortedData.size - 1) {
                        HorizontalDivider(
                            thickness = state.dividerThickness,
                            color = colors.dividerColor,
                        )
                    }
                }
            }
        }
    }

    if (state.outlinedTable) {
        if (state.outlinedCardBorder != null) {
            OutlinedCard(
                border = state.outlinedCardBorder,
                shape = RoundedCornerShape(state.outlinedCardShape),
                colors = CardDefaults.cardColors(containerColor = colors.outlinedCardColor),
                modifier = modifier,
            ) {
                tableContent()
            }
        } else {
            Card(
                shape = RoundedCornerShape(state.outlinedCardShape),
                colors = CardDefaults.cardColors(containerColor = colors.outlinedCardColor),
                modifier = modifier,
            ) {
                tableContent()
            }
        }
    } else {
        tableContent()
    }
}

/**
 * A variant of [KomposeTable] that allows specifying default selected indices and selection mode directly.
 *
 * @param T The type of data displayed in the table. Must be a non-nullable `Any` type.
 * @param columns A list of [TableSortColumn] objects defining the table's columns.
 * @param tableData A list of objects of type `T` to be displayed in the table.
 * @param defaultSelectedIndices A set of indices to be selected by default.
 * @param selectionMode The [SelectionMode] for the table (SINGLE or MULTIPLE).
 * @param modifier A [Modifier] to be applied to the table.
 * @param selectionModel A [TableSelectionModel] to manage row selection.
 * @param sortState A [MutableState] of [SortState] to manage column sorting.
 * @param state A [KomposeTableState] to manage table configuration.
 * @param colors A [KomposeTableColors] object for table colors.
 * @param rowTextStyle The [TextStyle] for the text in table rows.
 * @param headerTextStyle The [TextStyle] for the text in the table header.
 * @param onRowClick A callback invoked when a row is clicked, providing the item and its index.
 * @param onRowDoubleClick A callback invoked when a row is double-clicked, providing the item and its index.
 * @param onSelectionChange A callback invoked when the selection changes, providing the list of selected items.
 */
@Composable
inline fun <reified T : Any> KomposeTable(
    columns: List<TableSortColumn<T>>,
    tableData: List<T>,
    defaultSelectedIndices: Set<Int>,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    modifier: Modifier = Modifier,
    selectionModel: TableSelectionModel<T> = remember { TableSelectionModel<T>() },
    sortState: MutableState<SortState> = remember { mutableStateOf(SortState()) },
    state: KomposeTableState = rememberKomposeTableState(
        defaultSelectedIndices = defaultSelectedIndices,
        selectionMode = selectionMode
    ),
    colors: KomposeTableColors = KomposeTableDefaults.colors,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    headerTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    noinline onRowClick: ((T, Int) -> Unit)? = null,
    noinline onRowDoubleClick: ((T, Int) -> Unit)? = null,
    noinline onSelectionChange: ((List<T>) -> Unit)? = null,
) {
    // Validate defaultSelectedIndices based on selectionMode
    val validatedIndices = when (selectionMode) {
        SelectionMode.SINGLE -> defaultSelectedIndices.take(1).toSet()
        SelectionMode.MULTIPLE -> defaultSelectedIndices
    }

    // Initialize selection model
    LaunchedEffect(validatedIndices, selectionMode, tableData) {
        selectionModel.selectionMode = state.selectionMode
        selectionModel.clearSelection(onSelectionChange)
        validatedIndices.filter { it in tableData.indices }.forEach { index ->
            selectionModel.selectItem(tableData[index], index)
        }
        if (validatedIndices.isNotEmpty()) {
            onSelectionChange?.invoke(selectionModel.selectedItems)
        }
    }

    // Observe selection changes
    LaunchedEffect(selectionModel) {
        snapshotFlow { selectionModel.selectedItems.toList() }
            .map { it.map { item -> item.hashCode() } }
            .distinctUntilChanged()
            .collect { onSelectionChange?.invoke(selectionModel.selectedItems) }
    }

    // Reuse the original KomposeTable with updated state
    KomposeTable(
        columns = columns,
        tableData = tableData,
        modifier = modifier,
        selectionModel = selectionModel,
        sortState = sortState,
        state = state.copy(
            defaultSelectedIndices = validatedIndices,
            selectionMode = selectionMode
        ),
        colors = colors,
        rowTextStyle = rowTextStyle,
        headerTextStyle = headerTextStyle,
        onRowClick = onRowClick,
        onRowDoubleClick = onRowDoubleClick,
        onSelectionChange = onSelectionChange,
    )
}

/**
 * Represents a column in the [KomposeTable].
 *
 * @param T The type of data displayed in the table.
 * @param id A unique identifier for the column.
 * @param title The title displayed in the column header.
 * @param width The initial width of the column.
 * @param minWidth The minimum width the column can be resized to.
 * @param maxWidth The maximum width the column can be resized to.
 * @param resizable Whether the column can be resized.
 * @param sortable Whether the column can be sorted.
 * @param visible Whether the column is visible.
 * @param valueExtractor A function to extract a string value from the data for sorting or display.
 * @param cellFactory A composable function to render the cell content.
 * @param comparator An optional comparator for sorting the column.
 */
data class TableSortColumn<T>(
    val id: String,
    val title: String,
    val width: Dp = 120.dp,
    val minWidth: Dp = 50.dp,
    val maxWidth: Dp = 300.dp,
    val resizable: Boolean = true,
    val sortable: Boolean = true,
    val visible: Boolean = true,
    val valueExtractor: (T) -> String = { "" },
    val cellFactory: @Composable (T) -> Unit = { data ->
        Text(
            text = valueExtractor(data),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    },
    val comparator: Comparator<T>? = null,
)

/**
 * Defines the sort state for the table.
 *
 * @param columnId The ID of the column being sorted.
 * @param order The [SortOrder] applied to the column.
 */
data class SortState(
    val columnId: String = "",
    val order: SortOrder = SortOrder.NONE,
)

/**
 * Defines the sort order for a column.
 */
enum class SortOrder { NONE, ASCENDING, DESCENDING }

/**
 * Defines how columns in the table should behave when resized.
 */
enum class ColumnResizeMode {
    UNCONSTRAINED,
    CONSTRAINED,
}

/**
 * Manages the selection state of items in a table.
 *
 * @param T The type of data in the table.
 */
class TableSelectionModel<T> {
    private val _selectedItems = mutableStateListOf<T>()
    private val _selectedIndices = mutableStateListOf<Int>()

    /** The list of currently selected items. */
    val selectedItems: List<T> = _selectedItems

    /** The list of indices of currently selected items. */
    val selectedIndices: List<Int> = _selectedIndices

    /** The selection mode (SINGLE or MULTIPLE). */
    var selectionMode by mutableStateOf(SelectionMode.SINGLE)

    /**
     * Selects an item at the given index.
     *
     * @param item The item to select.
     * @param index The index of the item in the table.
     * @param onSelectionChange An optional callback to invoke with the updated selection.
     */
    fun selectItem(item: T, index: Int, onSelectionChange: ((List<T>) -> Unit)? = null) {
        when (selectionMode) {
            SelectionMode.SINGLE -> {
                _selectedItems.clear()
                _selectedIndices.clear()
                _selectedItems.add(item)
                _selectedIndices.add(index)
            }

            SelectionMode.MULTIPLE -> {
                if (!_selectedItems.contains(item)) {
                    _selectedItems.add(item)
                    _selectedIndices.add(index)
                }
            }
        }
        onSelectionChange?.invoke(_selectedItems)
    }

    /**
     * Deselects an item at the given index.
     *
     * @param item The item to deselect.
     * @param index The index of the item in the table.
     * @param onSelectionChange An optional callback to invoke with the updated selection.
     */
    fun deselectItem(item: T, index: Int, onSelectionChange: ((List<T>) -> Unit)? = null) {
        _selectedItems.remove(item)
        _selectedIndices.remove(index)
        onSelectionChange?.invoke(_selectedItems)
    }

    /**
     * Checks if an item is selected.
     *
     * @param item The item to check.
     * @return True if the item is selected, false otherwise.
     */
    fun isSelected(item: T): Boolean = _selectedItems.contains(item)

    /**
     * Checks if an index is selected.
     *
     * @param index The index to check.
     * @return True if the index is selected, false otherwise.
     */
    fun isSelected(index: Int): Boolean = _selectedIndices.contains(index)

    /**
     * Clears all selected items and indices.
     *
     * @param onSelectionChange An optional callback to invoke with the updated selection.
     */
    fun clearSelection(onSelectionChange: ((List<T>) -> Unit)? = null) {
        _selectedItems.clear()
        _selectedIndices.clear()
        onSelectionChange?.invoke(_selectedItems)
    }

    /**
     * Selects all items in the provided list.
     *
     * @param items The list of items to select.
     * @param onSelectionChange An optional callback to invoke with the updated selection.
     */
    fun selectAll(items: List<T>, onSelectionChange: ((List<T>) -> Unit)? = null) {
        if (selectionMode == SelectionMode.MULTIPLE) {
            _selectedItems.clear()
            _selectedIndices.clear()
            _selectedItems.addAll(items)
            _selectedIndices.addAll(items.indices)
            onSelectionChange?.invoke(_selectedItems)
        }
    }
}

/**
 * Defines the selection behavior for the table.
 */
enum class SelectionMode { SINGLE, MULTIPLE }

/**
 * Default values for [KomposeTable].
 */
@Immutable
object KomposeTableDefaults {
    /** Provides default colors for the table. */
    val colors: KomposeTableColors
        @Composable get() = KomposeTableColors(
            selectedRowColor = MaterialTheme.colorScheme.primaryContainer,
            hoveredRowColor = Color(0xFFE3F2FD),
            headerBorderColor = Color(0xFFBDBDBD),
            headerBackgroundColor = Color(0xFFE0E0E0),
            dividerColor = Color(0xFFE0E0E0),
            outlinedCardColor = Color.Transparent,
            alternatingRowColors = listOf(Color.White, Color(0xFFF5F5F5))
        )
}

/**
 * Color configuration for [KomposeTable].
 *
 * @param selectedRowColor The background color for selected rows.
 * @param hoveredRowColor The background color for hovered rows.
 * @param headerBorderColor The border color for the header.
 * @param headerBackgroundColor The background color for the header.
 * @param dividerColor The color of dividers between rows and columns.
 * @param outlinedCardColor The background color for the outlined card.
 * @param alternatingRowColors A list of colors for alternating row backgrounds.
 */
@Immutable
data class KomposeTableColors(
    val selectedRowColor: Color,
    val hoveredRowColor: Color,
    val headerBorderColor: Color,
    val headerBackgroundColor: Color,
    val dividerColor: Color,
    val outlinedCardColor: Color,
    val alternatingRowColors: List<Color>
)

/**
 * State configuration for [KomposeTable].
 *
 * @param outlinedTable Whether the table is displayed within an outlined card.
 * @param outlinedCardBorder The border stroke for the outlined card, if any.
 * @param outlinedCardShape The corner radius for the outlined card.
 * @param showVerticalDividers Whether to show vertical dividers between columns.
 * @param showHorizontalDividers Whether to show horizontal dividers between rows.
 * @param dividerThickness The thickness of dividers.
 * @param enableSorting Whether sorting is enabled for columns.
 * @param enableSelection Whether row selection is enabled.
 * @param enableColumnResizing Whether column resizing is enabled.
 * @param enableHover Whether row hover effects are enabled.
 * @param columnResizeMode The [ColumnResizeMode] for column resizing behavior.
 * @param defaultSelectedIndices A set of indices to be selected by default.
 * @param selectionMode The [SelectionMode] for the table (SINGLE or MULTIPLE).
 */
@Stable
data class KomposeTableState(
    val outlinedTable: Boolean = true,
    val outlinedCardBorder: BorderStroke? = null,
    val outlinedCardShape: Dp = 8.dp,
    val showVerticalDividers: Boolean = true,
    val showHorizontalDividers: Boolean = true,
    val dividerThickness: Dp = 1.dp,
    val enableSorting: Boolean = true,
    val enableSelection: Boolean = true,
    val enableColumnResizing: Boolean = true,
    val enableHover: Boolean = true,
    val columnResizeMode: ColumnResizeMode = ColumnResizeMode.UNCONSTRAINED,
    val defaultSelectedIndices: Set<Int> = emptySet(),
    val selectionMode: SelectionMode = SelectionMode.SINGLE,
) {
    companion object {
        /**
         * A [Saver] for [KomposeTableState] to handle saving and restoring its properties.
         */
        val Saver: Saver<KomposeTableState, Any> = object : Saver<KomposeTableState, Any> {
            override fun SaverScope.save(value: KomposeTableState): Any {
                return listOf(
                    value.outlinedTable,
                    value.outlinedCardBorder?.width?.value ?: -1f,
                    value.outlinedCardShape.value,
                    value.showVerticalDividers,
                    value.showHorizontalDividers,
                    value.dividerThickness.value,
                    value.enableSorting,
                    value.enableSelection,
                    value.enableColumnResizing,
                    value.enableHover,
                    value.columnResizeMode.name,
                    value.defaultSelectedIndices.toList(),
                    value.selectionMode.name,
                )
            }

            override fun restore(value: Any): KomposeTableState? {
                if (value !is List<*> || value.size != 13) return null
                return try {
                    val outlinedTable = value[0] as? Boolean ?: return null
                    val borderWidth = value[1] as? Float ?: return null
                    val borderColorValue = value[2] as? Long ?: return null
                    val outlinedCardShape = value[3] as? Float ?: return null
                    val showVerticalDividers = value[4] as? Boolean ?: return null
                    val showHorizontalDividers = value[5] as? Boolean ?: return null
                    val dividerThickness = value[6] as? Float ?: return null
                    val enableSorting = value[7] as? Boolean ?: return null
                    val enableSelection = value[8] as? Boolean ?: return null
                    val enableColumnResizing = value[9] as? Boolean ?: return null
                    val enableHover = value[10] as? Boolean ?: return null
                    val columnResizeModeName = value[11] as? String ?: return null
                    val defaultSelectedIndices =
                        (value[12] as? List<*>)?.mapNotNull { it as? Int }?.toSet() ?: return null
                    val selectionModeName = value[13] as? String ?: return null

                    val borderStroke = if (borderWidth >= 0f && borderColorValue >= 0) {
                        BorderStroke(
                            width = borderWidth.dp,
                            color = Color(borderColorValue.toULong())
                        )
                    } else {
                        null
                    }

                    KomposeTableState(
                        outlinedTable = outlinedTable,
                        outlinedCardBorder = borderStroke,
                        outlinedCardShape = outlinedCardShape.dp,
                        showVerticalDividers = showVerticalDividers,
                        showHorizontalDividers = showHorizontalDividers,
                        dividerThickness = dividerThickness.dp,
                        enableSorting = enableSorting,
                        enableSelection = enableSelection,
                        enableColumnResizing = enableColumnResizing,
                        enableHover = enableHover,
                        columnResizeMode = ColumnResizeMode.valueOf(columnResizeModeName),
                        defaultSelectedIndices = defaultSelectedIndices,
                        selectionMode = SelectionMode.valueOf(selectionModeName),
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Creates and remembers a [KomposeTableState] that survives configuration changes.
 *
 * @param outlinedTable Whether the table is displayed within an outlined card.
 * @param outlinedCardBorder The border stroke for the outlined card, if any.
 * @param outlinedCardShape The corner radius for the outlined card.
 * @param showVerticalDividers Whether to show vertical dividers between columns.
 * @param showHorizontalDividers Whether to show horizontal dividers between rows.
 * @param dividerThickness The thickness of dividers.
 * @param enableSorting Whether sorting is enabled for columns.
 * @param enableSelection Whether row selection is enabled.
 * @param enableColumnResizing Whether column resizing is enabled.
 * @param enableHover Whether row hover effects are enabled.
 * @param columnResizeMode The [ColumnResizeMode] for column resizing behavior.
 * @return A [KomposeTableState] instance.
 */
@Composable
fun rememberKomposeTableState(
    outlinedTable: Boolean = true,
    outlinedCardBorder: BorderStroke? = CardDefaults.outlinedCardBorder(),
    outlinedCardShape: Dp = 8.dp,
    showVerticalDividers: Boolean = true,
    showHorizontalDividers: Boolean = true,
    dividerThickness: Dp = 1.dp,
    enableSorting: Boolean = true,
    enableSelection: Boolean = true,
    enableColumnResizing: Boolean = true,
    enableHover: Boolean = true,
    columnResizeMode: ColumnResizeMode = ColumnResizeMode.UNCONSTRAINED,
): KomposeTableState {
    return rememberSaveable(saver = KomposeTableState.Saver) {
        KomposeTableState(
            outlinedTable = outlinedTable,
            outlinedCardBorder = outlinedCardBorder,
            outlinedCardShape = outlinedCardShape,
            showVerticalDividers = showVerticalDividers,
            showHorizontalDividers = showHorizontalDividers,
            dividerThickness = dividerThickness,
            enableSorting = enableSorting,
            enableSelection = enableSelection,
            enableColumnResizing = enableColumnResizing,
            enableHover = enableHover,
            columnResizeMode = columnResizeMode,
            defaultSelectedIndices = emptySet(),
            selectionMode = SelectionMode.SINGLE,
        )
    }
}

/**
 * Creates and remembers a [KomposeTableState] with default selected indices and selection mode.
 *
 * @param defaultSelectedIndices A set of indices to be selected by default.
 * @param selectionMode The [SelectionMode] for the table (SINGLE or MULTIPLE).
 * @param outlinedTable Whether the table is displayed within an outlined card.
 * @param outlinedCardBorder The border stroke for the outlined card, if any.
 * @param outlinedCardShape The corner radius for the outlined card.
 * @param showVerticalDividers Whether to show vertical dividers between columns.
 * @param showHorizontalDividers Whether to show horizontal dividers between rows.
 * @param dividerThickness The thickness of dividers.
 * @param enableSorting Whether sorting is enabled for columns.
 * @param enableSelection Whether row selection is enabled.
 * @param enableColumnResizing Whether column resizing is enabled.
 * @param enableHover Whether row hover effects are enabled.
 * @param columnResizeMode The [ColumnResizeMode] for column resizing behavior.
 * @return A [KomposeTableState] instance.
 */
@Composable
fun rememberKomposeTableState(
    defaultSelectedIndices: Set<Int>,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    outlinedTable: Boolean = true,
    outlinedCardBorder: BorderStroke? = CardDefaults.outlinedCardBorder(),
    outlinedCardShape: Dp = 8.dp,
    showVerticalDividers: Boolean = true,
    showHorizontalDividers: Boolean = true,
    dividerThickness: Dp = 1.dp,
    enableSorting: Boolean = true,
    enableSelection: Boolean = true,
    enableColumnResizing: Boolean = true,
    enableHover: Boolean = true,
    columnResizeMode: ColumnResizeMode = ColumnResizeMode.UNCONSTRAINED,
): KomposeTableState {
    // Validate defaultSelectedIndices based on selectionMode
    val validatedIndices = when (selectionMode) {
        SelectionMode.SINGLE -> defaultSelectedIndices.take(1).toSet()
        SelectionMode.MULTIPLE -> defaultSelectedIndices
    }

    return rememberSaveable(saver = KomposeTableState.Saver) {
        KomposeTableState(
            outlinedTable = outlinedTable,
            outlinedCardBorder = outlinedCardBorder,
            outlinedCardShape = outlinedCardShape,
            showVerticalDividers = showVerticalDividers,
            showHorizontalDividers = showHorizontalDividers,
            dividerThickness = dividerThickness,
            enableSorting = enableSorting,
            enableSelection = enableSelection,
            enableColumnResizing = enableColumnResizing,
            enableHover = enableHover,
            columnResizeMode = columnResizeMode,
            defaultSelectedIndices = validatedIndices,
            selectionMode = selectionMode,
        )
    }
}
