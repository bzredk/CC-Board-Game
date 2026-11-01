package othello;

public class Constants {
    public static final int[][] BOARD_WEIGHTS =
            {{200, -70, 30, 25, 25, 30, -70, 200},
                    {-70, -100, -10, -10, -10, -10, -100, -70},
                    {30, -10, 2, 2, 2, 2, -10, 30},
                    {25, -10, 2, 2, 2, 2, -10, 25},
                    {25, -10, 2, 2, 2, 2, -10, 25},
                    {30, -10, 2, 2, 2, 2, -10, 30},
                    {-70, -100, -10, -10, -10, -10, -100, -70},
                    {200, -70, 30, 25, 25, 30, -70, 200}};

    public static final double EXPLORATION_PARAM = Math.sqrt(2);
}
