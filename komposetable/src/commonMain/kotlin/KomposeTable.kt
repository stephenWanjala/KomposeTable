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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable function that displays a table with customizable columns, data, and behavior.
 *
 * @param T The type of data displayed in the table. Must be a non-nullable `Any` type.
 * @param columns A list of `TableSortColumn` objects defining the table's columns.
 * @param tableData A list of objects of type `T` to be displayed in the table.
 * @param modifier A `Modifier` to be applied to the table.
 * @param selectionModel A `TableSelectionModel` to manage row selection. Defaults to a new instance.
 * @param sortState A `MutableState` of `SortState` to manage column sorting. Defaults to a new instance.
 * @param outlinedTable A boolean indicating whether the table should be wrapped in an `OutlinedCard`. Defaults to `true`.
 * @param outlinedCardBorder The `BorderStroke` for the `OutlinedCard` if `outlinedTable` is `true`. Defaults to `CardDefaults.outlinedCardBorder()`.
 * @param outlinedCardShape The corner radius for the `OutlinedCard` if `outlinedTable` is `true`. Defaults to `8.dp`.
 * @param outlinedCardColor The background color for the `OutlinedCard` if `outlinedTable` is `true`. Defaults to `Color.Transparent`.
 * @param rowTextStyle The `TextStyle` for the text in table rows. Defaults to `MaterialTheme.typography.bodyMedium`.
 * @param alternatingRowColors A list of colors to be used for alternating row backgrounds. Defaults to `listOf(Color.White, Color(0xFFF5F5F5))`.
 * @param selectedRowColor The background color for selected rows. Defaults to `MaterialTheme.colorScheme.primaryContainer`.
 * @param hoveredRowColor The background color for hovered rows. Defaults to `Color(0xFFE3F2FD)`.
 * @param headerTextStyle The `TextStyle` for the text in the table header. Defaults to `MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)`.
 * @param headerBackgroundColor The background color for the table header. Defaults to `Color(0xFFE0E0E0)`.
 * @param headerBorderColor The color of the border around header cells. Defaults to `Color(0xFFBDBDBD)`.
 * @param showVerticalDividers A boolean indicating whether to show vertical dividers between columns. Defaults to `true`.
 * @param showHorizontalDividers A boolean indicating whether to show horizontal dividers between rows. Defaults to `true`.
 * @param dividerThickness The thickness of dividers. Defaults to `1.dp`.
 * @param dividerColor The color of dividers. Defaults to `Color(0xFFE0E0E0)`.
 * @param enableSorting A boolean indicating whether sorting is enabled for sortable columns. Defaults to `true`.
 * @param enableSelection A boolean indicating whether row selection is enabled. Defaults to `true`.
 * @param enableColumnResizing A boolean indicating whether column resizing is enabled for resizable columns. Defaults to `true`.
 * @param enableHover A boolean indicating whether row hover effects are enabled. Defaults to `true`.
 * @param columnResizeMode The resizing behavior for columns. Defaults to `ColumnResizeMode.UNCONSTRAINED`.
 * @param onRowClick A callback invoked when a row is clicked, providing the item and its index. Defaults to `null`.
 * @param onRowDoubleClick A callback invoked when a row is double-clicked, providing the item and its index. Defaults to `null`.
 * @param onSelectionChange A callback invoked when the selection changes, providing the list of selected items. Defaults to `null`.
 */
@Composable
inline fun <reified T : Any> KomposeTable(
    columns: List<TableSortColumn<T>>,
    tableData: List<T>,
    modifier: Modifier = Modifier,
    selectionModel: TableSelectionModel<T> = remember { TableSelectionModel<T>() },
    sortState: MutableState<SortState> = remember { mutableStateOf(SortState()) },
    outlinedTable: Boolean = true,
    outlinedCardBorder: BorderStroke? = CardDefaults.outlinedCardBorder(),
    outlinedCardShape: Dp = 8.dp,
    outlinedCardColor: Color = Color.Transparent,
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    alternatingRowColors: List<Color> = listOf(Color.White, Color(0xFFF5F5F5)),
    selectedRowColor: Color = MaterialTheme.colorScheme.primaryContainer,
    hoveredRowColor: Color = Color(0xFFE3F2FD),
    headerTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    headerBackgroundColor: Color = Color(0xFFE0E0E0),
    headerBorderColor: Color = Color(0xFFBDBDBD),
    showVerticalDividers: Boolean = true,
    showHorizontalDividers: Boolean = true,
    dividerThickness: Dp = 1.dp,
    dividerColor: Color = Color(0xFFE0E0E0),
    enableSorting: Boolean = true,
    enableSelection: Boolean = true,
    enableColumnResizing: Boolean = true,
    enableHover: Boolean = true,
    columnResizeMode: ColumnResizeMode = ColumnResizeMode.UNCONSTRAINED,
    noinline onRowClick: ((T, Int) -> Unit)? = null,
    noinline onRowDoubleClick: ((T, Int) -> Unit)? = null,
    noinline onSelectionChange: ((List<T>) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()

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
            val comparator = column?.comparator ?: compareBy { column?.valueExtractor?.invoke(it) ?: "" }
            when (currentSort.order) {
                SortOrder.ASCENDING -> tableData.sortedWith(comparator)
                SortOrder.DESCENDING -> tableData.sortedWith(comparator.reversed())
                SortOrder.NONE -> tableData
            }
        }
    }

    // Handle selection changes
    LaunchedEffect(selectionModel.selectedItems) {
        onSelectionChange?.invoke(selectionModel.selectedItems)
    }

    val tableContent = @Composable {
        Column(modifier = modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(headerBackgroundColor)
                    .horizontalScroll(horizontalScrollState)
                    .height(48.dp),
            ) {
                columns.filter { it.visible }.forEach { column ->
                    val currentWidth = columnWidths[column.id] ?: column.width

                    Box(
                        modifier = Modifier
                            .width(currentWidth)
                            .fillMaxHeight()
                            .then(
                                if (showVerticalDividers) {
                                    Modifier.border(
                                        width = dividerThickness,
                                        color = headerBorderColor,
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (enableSorting && column.sortable) {
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
                                } else {
                                    Modifier
                                }
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

                            if (enableSorting && column.sortable) {
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
                                    } else {
                                        Color.Gray
                                    },
                                )
                            }
                        }

                        // Column resize handle
                        if (enableColumnResizing && column.resizable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .align(Alignment.CenterEnd)
                                    .platformResizeCursor() // Placeholder; implement as needed
                                    .pointerInput(column.id) {
                                        detectDragGestures { change, _ ->
                                            val currentWidth = columnWidths[column.id] ?: column.width
                                            val newWidth = with(density) {
                                                (currentWidth.toPx() + change.position.x).toDp()
                                            }
                                            columnWidths[column.id] = newWidth.coerceIn(
                                                column.minWidth,
                                                column.maxWidth,
                                            )
                                        }
                                    }
                                    .background(Color.Transparent),
                            )
                        }
                    }
                }
            }

            // Horizontal divider after header
            if (showHorizontalDividers) {
                HorizontalDivider(
                    thickness = dividerThickness,
                    color = dividerColor,
                )
            }

            // Table Body
            LazyColumn(
                state = verticalScrollState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(sortedData) { index, item ->
                    val isSelected = selectionModel.isSelected(item)
                    val isHovered = hoveredRowIndex == index && enableHover

                    val rowBackgroundColor = when {
                        isSelected -> selectedRowColor
                        isHovered -> hoveredRowColor
                        index % 2 == 0 -> alternatingRowColors[0]
                        else -> alternatingRowColors.getOrElse(1) { alternatingRowColors[0] }
                    }

                    Row(
                        modifier = Modifier
                            .background(rowBackgroundColor)
                            .horizontalScroll(horizontalScrollState)
                            .height(40.dp)
                            .then(
                                if (enableSelection) {
                                    Modifier.clickable {
                                        if (selectionModel.isSelected(item)) {
                                            selectionModel.deselectItem(item, index)
                                        } else {
                                            selectionModel.selectItem(item, index)
                                        }
                                        onRowClick?.invoke(item, index)
                                    }
                                } else if (onRowClick != null) {
                                    Modifier.clickable { onRowClick.invoke(item, index) }
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (enableHover) {
                                    Modifier.pointerInput(index) {
                                        detectDragGestures(
                                            onDragStart = { hoveredRowIndex = index },
                                            onDragEnd = { hoveredRowIndex = -1 },
                                        ) { _, _ -> }
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        columns.filter { it.visible }.forEach { column ->
                            val currentWidth = columnWidths[column.id] ?: column.width

                            Box(
                                modifier = Modifier
                                    .width(currentWidth)
                                    .fillMaxHeight()
                                    .then(
                                        if (showVerticalDividers) {
                                            Modifier.border(
                                                width = dividerThickness,
                                                color = dividerColor,
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                column.cellFactory(item)
                            }
                        }
                    }

                    // Horizontal divider after each row
                    if (showHorizontalDividers && index < sortedData.size - 1) {
                        HorizontalDivider(
                            thickness = dividerThickness,
                            color = dividerColor,
                        )
                    }
                }
            }
        }
    }

    if (outlinedTable) {
        if (outlinedCardBorder != null) {
            OutlinedCard(
                border = outlinedCardBorder,
                shape = RoundedCornerShape(outlinedCardShape),
                colors = CardDefaults.cardColors(containerColor = outlinedCardColor),
                modifier = modifier,
            ) {
                tableContent()
            }
        } else {
            Card(
                shape = RoundedCornerShape(outlinedCardShape),
                colors = CardDefaults.cardColors(containerColor = outlinedCardColor),
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
 * Represents a column in the `KomposeTable`.
 *
 * @param T The type of data displayed in the cells of this column.
 * @param id A unique identifier for the column, used for managing state like column widths and sorting.
 * @param title The text displayed in the header of this column.
 * @param width The initial width of the column.
 * @param minWidth The minimum width the column can be resized to.
 * @param maxWidth The maximum width the column can be resized to.
 * @param resizable Whether this column can be resized by the user.
 * @param sortable Whether this column can be sorted by the user.
 * @param visible Whether this column is currently visible in the table.
 * @param valueExtractor A lambda that extracts the display value for the column from a data item
 *                       as a `String`. Used by the default `cellFactory` and default comparator.
 * @param cellFactory A composable function that defines how to render the content of a cell
 *                    in this column for a given data item. Defaults to displaying the value
 *                    extracted by `valueExtractor` as `Text`.
 * @param comparator An optional `Comparator` used for sorting this column. If not provided,
 *                   a default comparator will use the `valueExtractor` for sorting.
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
 * @param T The type of items in the table.
 */
class TableSelectionModel<T> {
    private val _selectedItems = mutableStateListOf<T>()
    private val _selectedIndices = mutableStateListOf<Int>()

    val selectedItems: List<T> = _selectedItems
    val selectedIndices: List<Int> = _selectedIndices

    var selectionMode by mutableStateOf(SelectionMode.SINGLE)

    fun selectItem(item: T, index: Int) {
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
    }

    fun deselectItem(item: T, index: Int) {
        _selectedItems.remove(item)
        _selectedIndices.remove(index)
    }

    fun isSelected(item: T): Boolean = _selectedItems.contains(item)
    fun isSelected(index: Int): Boolean = _selectedIndices.contains(index)

    fun clearSelection() {
        _selectedItems.clear()
        _selectedIndices.clear()
    }

    fun selectAll(items: List<T>) {
        if (selectionMode == SelectionMode.MULTIPLE) {
            _selectedItems.clear()
            _selectedIndices.clear()
            _selectedItems.addAll(items)
            _selectedIndices.addAll(items.indices)
        }
    }
}

/**
 * Defines the selection behavior for the table.
 */
enum class SelectionMode { SINGLE, MULTIPLE }

