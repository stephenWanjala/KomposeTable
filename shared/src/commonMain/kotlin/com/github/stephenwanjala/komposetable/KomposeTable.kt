package com.github.stephenwanjala.komposetable

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.github.stephenwanjala.komposetable.utils.extractMembers


/**
 * A composable function that displays a table with customizable columns, data, and behavior.
 *
 * @param T The type of data displayed in the table. Must be a non-nullable `Any` type.
 * @param columns A list of `TableColumn` objects defining the table's columns.
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
 */
@Composable
inline fun <reified T : Any> KomposeTable(
    columns: List<TableColumn<T>>,
    tableData: List<T>,
    modifier: Modifier = Modifier,
    selectionModel: TableSelectionModel<T> = remember { TableSelectionModel<T>() },
    sortState: MutableState<SortState> = remember { mutableStateOf(SortState()) },

    // Styling options
    outlinedTable: Boolean = true,
    outlinedCardBorder: BorderStroke? = CardDefaults.outlinedCardBorder(),
    outlinedCardShape: Dp = 8.dp,
    outlinedCardColor: Color = Color.Transparent,

    // Row styling
    rowTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    alternatingRowColors: List<Color> = listOf(Color.White, Color(0xFFF5F5F5)),
    selectedRowColor: Color = MaterialTheme.colorScheme.primaryContainer,
    hoveredRowColor: Color = Color(0xFFE3F2FD),

    // Header styling
    headerTextStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    headerBackgroundColor: Color = Color(0xFFE0E0E0),
    headerBorderColor: Color = Color(0xFFBDBDBD),

    // Dividers and borders
    showVerticalDividers: Boolean = true,
    showHorizontalDividers: Boolean = true,
    dividerThickness: Dp = 1.dp,
    dividerColor: Color = Color(0xFFE0E0E0),

    // Behavior
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
            val comparator = column?.comparator ?: compareBy<T> {
                extractCellValue(it as Any, currentSort.columnId)
            }

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
                                },
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
                                },
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
                                    sortState.value.columnId == column.id && sortState.value.order == SortOrder.ASCENDING -> Icons.Default.KeyboardArrowUp
                                    sortState.value.columnId == column.id && sortState.value.order == SortOrder.DESCENDING -> Icons.Default.ArrowDropDown
                                    else -> Icons.Default.ArrowDropDown
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
                                    .platformResizeCursor()
                                    .pointerInput(column.id) {
                                        detectDragGestures { change, _ ->
                                            val currentWidth =
                                                columnWidths[column.id] ?: column.width
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
                    color = dividerColor
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
                                },
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
                                },
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
                                        },
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
                            color = dividerColor
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
            // Fallback if outlinedCardBorder is null but outlinedTable is true
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
 * @param id A unique identifier for the column. This is used internally for managing state
 *           like column widths and sorting. It's also used by the default `cellFactory`
 *           to extract data if `T` is a data class and `id` matches a property name.
 * @param title The text displayed in the header of this column.
 * @param width The initial width of the column.
 * @param minWidth The minimum width the column can be resized to.
 * @param maxWidth The maximum width the column can be resized to.
 * @param resizable Whether this column can be resized by the user.
 * @param sortable Whether this column can be sorted by the user.
 * @param visible Whether this column is currently visible in the table.
 * @param cellFactory A composable function that defines how to render the content of a cell
 *                    in this column for a given data item. The default implementation
 *                    attempts to extract a property from the data item whose name matches the `id`
 *                    of the column and displays it as `Text`.
 * @param comparator An optional `Comparator` used for sorting this column. If not provided,
 *                   a default comparator will be used based on the extracted cell value (as a String).
 *                   Provide a custom comparator for more complex sorting logic or for types
 *                   that don't have a natural string representation suitable for sorting.
 */
data class TableColumn<T>(
    val id: String,
    val title: String,
    val width: Dp = 120.dp,
    val minWidth: Dp = 50.dp,
    val maxWidth: Dp = 300.dp,
    val resizable: Boolean = true,
    val sortable: Boolean = true,
    val visible: Boolean = true,
    val cellFactory: @Composable (T) -> Unit = { data ->
        Text(
            text = extractCellValue(data as Any, id),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    },
    val comparator: Comparator<T>? = null,
)

/**
 * Sort State for columns
 */
enum class SortOrder { NONE, ASCENDING, DESCENDING }

/**
 * Defines how columns in the table should behave when resized.
 *
 * This enum controls the strategy used for adjusting column widths.
 *
 * @property UNCONSTRAINED Allows columns to be resized freely. If the total width of columns
 *                         is less than the available table width, empty space will be visible.
 *                         If the total width exceeds the table width, horizontal scrolling will be enabled.
 * @property CONSTRAINED Ensures that columns always fill the entire available width of the table.
 *                       When a column is resized, other columns will adjust their widths proportionally
 *                       to maintain the full table width. This mode prevents empty space within the table
 *                       and might restrict resizing if the minimum widths of other columns are reached.
 */
enum class ColumnResizeMode {
    UNCONSTRAINED,
    CONSTRAINED,
}

data class SortState(
    val columnId: String = "",
    val order: SortOrder = SortOrder.NONE,
)

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
 * - [SINGLE]: Only one row can be selected at a time.
 * - [MULTIPLE]: Multiple rows can be selected simultaneously.
 */
enum class SelectionMode { SINGLE, MULTIPLE }

/*
 * Helper function to extract cell value from data object
 */
fun extractCellValue(data: Any, columnId: String): String {
    val members = extractMembers(data)
    return members.find { it.first == columnId }?.second ?: ""
}