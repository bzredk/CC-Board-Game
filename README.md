# CC-Board-Game

Othello with human players, Minimax, MCTS, and an experimental TD-learning strategy.

## Structure

```text
java/
  src/main/java/othello/   Application, game logic, and JavaFX interface
  src/main/resources/     JavaFX layout
  src/test/java/          JUnit tests
  pom.xml                 Maven build
docs/
  othello-class-diagram.pdf
```

## Java version

Requires JDK 21. Maven is downloaded by the included wrapper.

```sh
cd java
./mvnw test
./mvnw javafx:run -Djavafx.args="human mcts"
```

On Windows, use:

```powershell
cd java
.\mvnw.cmd test
.\mvnw.cmd javafx:run "-Djavafx.args=human mcts"
```

The two arguments select the black and white players:

| Arguments | Mode |
| --- | --- |
| `human human` | Two human players on one board |
| `human minimax` | Human vs. Minimax |
| `human mcts` | Human vs. MCTS |
| `minimax mcts` | Minimax vs. MCTS |
| `human custom` | Human vs. the experimental TD-learning strategy |

Click **Take Computer Turn** to advance a computer player's turn. The `custom`
strategy loads the baseline weights in `java/models/othello-td.zip`.

## TD-learning

The bundled baseline was trained for 20 self-play games with seed 27. Both colors
share one network, learning between consecutive afterstates from the same
player's perspective. Terminal rewards are +1 for a win, 0 for a draw, and -1 for
a loss. Training uses gamma 0.99, Adam learning rate 0.001, and exploration 0.1;
inference disables exploration and does not update weights.

From `java/`, train or continue an existing checkpoint:

```sh
./mvnw compile exec:java -Dexec.args="--games 20 --model models/othello-td.zip --seed 27"
./mvnw javafx:run -Djavafx.args="human custom"
```

Windows:

```powershell
.\mvnw.cmd compile exec:java "-Dexec.args=--games 20 --model models/othello-td.zip --seed 27"
.\mvnw.cmd javafx:run "-Djavafx.args=human custom"
```

Each completed game saves a checkpoint. An existing model is resumed; a new model
path starts from seeded initial weights. To play with a different checkpoint, add
`-Dothello.model=path/to/model.zip` (quote the entire argument in PowerShell).
Twenty games validate the training workflow; playing strength needs separate
evaluation.

The default test run excludes the training-loop checks in `TestLearningTrain`.
Run those checks separately with:

```sh
./mvnw -Ptraining-tests test
```

GUI integration checks load the JavaFX window and play complete games. They
require a desktop display and run separately from the default unit tests:

```sh
./mvnw -Dtest=othello.gui.OthelloGuiIT test
```

On Windows: `.\mvnw.cmd "-Dtest=othello.gui.OthelloGuiIT" test`.
