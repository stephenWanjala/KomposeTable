# KomposeTable ![Maven Central Version](https://img.shields.io/maven-central/v/io.github.stephenwanjala/komposetable)

**KomposeTable** is a highly customizable table component with A nealy Similar API as JavaFx Table for Compose Multiplatform, offering features like sorting, column resizing, row selection, and theming.

## Features

*   **Column Sorting:** Click on column headers to sort data in ascending or descending order.
*   **Column Resizing:** Easily resize columns by dragging the column dividers.
*   **Row Selection:** Support for single and multiple row selection.
*   **Customizable Appearance:** Control table and cell styling, including colors, borders, and dividers.
*   **Alternating Row Colors:** Improve readability with alternating row background colors.
*   **Hover Effects:** Provide visual feedback when hovering over rows.
*   **Outlined Table:** Option to display the table within an outlined card.
*   **Compose Multiplatform:** Designed to work seamlessly across different platforms supported by Compose.

## Example Usage
See sample Usage in [CRSP CalcKMP ](https://github.com/stephenWanjala/CRSPCalcKMp/tree/useTable) on the `useTable` branch

Here's an example of how to use `KomposeTable` to display a list of football teams:

```kotlin
@Serializable
data class FootballTeam(
    val team: String,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val gamesPlayed: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    val position: Int,      
    val xG: Double,          // Expected Goals
    val xGA: Double,         // Expected Goals Against
    val marketValue: Int     // Example, in millions
)


@Composable
fun FootballLeagueTableScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val teams =
            listOf(
                FootballTeam("Arsenal", 25, 10, 3, 38, 78, 28, 50, 85, 1, 58.0, 22.0, 88),
                FootballTeam("Chelsea", 24, 11, 3, 38, 75, 30, 45, 83, 2, 57.5, 21.5, 85),
                FootballTeam("Tottenham", 23, 12, 3, 38, 72, 35, 37, 81, 3, 55.0, 20.0, 82),
                FootballTeam("West Ham", 22, 13, 3, 38, 68, 40, 28, 79, 4, 50.0, 18.0, 79),
                FootballTeam("Leicester", 21, 14, 3, 38, 65, 45, 20, 77, 5, 48.5, 17.5, 76),
                // ... (add more teams or use the truncated list from your example)
                FootballTeam("Norwich", 9, 3, 26, 38, 23, 84, -61, 30, 20, 18.0, 50.0, 40) // Example of a last place team
            )

        val selectionModel = remember { TableSelectionModel<FootballTeam>() }
        val sortState = remember { mutableStateOf(SortState()) }

        val columns = listOf(
            TableColumn<FootballTeam>(
                id = "team",
                title = "Team",
                width = 180.dp,
                cellFactory = { team ->
                    Text(
                        text = team.team,
                        fontWeight = FontWeight.Medium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                },
                comparator = compareBy { it.team },
            ),
            TableColumn<FootballTeam>(
                id = "wins",
                title = "W",
                width = 60.dp,
                cellFactory = { team ->
                    Text(
                        text = team.wins.toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                comparator = compareBy { it.wins },
            ),
            TableColumn<FootballTeam>(
                id = "draws",
                title = "D",
                width = 60.dp,
                cellFactory = { team ->
                    Text(
                        text = team.draws.toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                comparator = compareBy { it.draws },
            ),
            TableColumn<FootballTeam>(
                id = "losses",
                title = "L",
                width = 60.dp,
                cellFactory = { team ->
                    Text(
                        text = team.losses.toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                comparator = compareBy { it.losses },
            ),
            TableColumn<FootballTeam>(
                id = "points",
                title = "Points",
                width = 80.dp,
                cellFactory = { team ->
                    Text(
                        text = team.points.toString(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                comparator = compareBy { it.points },
            ),
            TableColumn<FootballTeam>(
                id = "goalDifference",
                title = "GD",
                width = 70.dp,
                cellFactory = { team ->
                    Text(
                        text = if (team.goalDifference >= 0) "+${team.goalDifference}" else team.goalDifference.toString(),
                        textAlign = TextAlign.Center,
                        color = when {
                            team.goalDifference > 0 -> Color(0xFF4CAF50)
                            team.goalDifference < 0 -> Color(0xFFF44336)
                            else -> Color.Gray
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                comparator = compareBy { it.goalDifference },
            ),
        )

        KomposeTable(
            columns = columns,
            tableData = teams,
            selectionModel = selectionModel,
            sortState = sortState,
            enableSorting = true,
            enableSelection = true,
            enableColumnResizing = true,
            enableHover = true,
            onRowClick = { team, index ->
                println("Clicked on ${team.team} at index $index")
            },
            onSelectionChange = { selectedTeams ->
                println("Selected teams: ${selectedTeams.map { it.team }}")
            },
        )
    }
}
```
*(Note: The `FootballTeam` data class and its sample data might need adjustments based on your actual data structure. The example above includes a placeholder data class and corrected sample data for demonstration.)*

## TODO

*   Add a GIF or video demonstrating the features.
*   Add installation instructions.
*   Add detailed usage examples.
*   Add API documentation.
