#include "ccbg/othello.hpp"
#include "ccbg/search.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <functional>
#include <iostream>
#include <random>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace
{
using namespace ccbg::othello;
using Cells = std::array<char, 64>;

void check(bool condition, const char* expression, int line)
{
    if (!condition)
    {
        throw std::runtime_error("line " + std::to_string(line) + ": " + expression);
    }
}

#define CHECK(expression) check(static_cast<bool>(expression), #expression, __LINE__)

template <typename Exception, typename Function>
void expect_throw(Function function)
{
    try
    {
        function();
    }
    catch (const Exception&)
    {
        return;
    }
    catch (const std::exception& error)
    {
        throw std::runtime_error("unexpected exception type: " + std::string(error.what()));
    }
    throw std::runtime_error("expected an exception");
}

int index(int row, int col)
{
    return row * 8 + col;
}

char other(char player)
{
    return player == 'B' ? 'W' : 'B';
}

Disc disc(char player)
{
    return player == 'B' ? Disc::Black : Disc::White;
}

Cells filled(char value)
{
    Cells cells;
    cells.fill(value);
    return cells;
}

Cells initial_cells()
{
    Cells cells = filled('.');
    cells[index(3, 3)] = 'W';
    cells[index(3, 4)] = 'B';
    cells[index(4, 3)] = 'B';
    cells[index(4, 4)] = 'W';
    return cells;
}

std::string text(const Cells& cells)
{
    return std::string(cells.begin(), cells.end());
}

int count(const Cells& cells, char value)
{
    return static_cast<int>(std::count(cells.begin(), cells.end(), value));
}

// The oracle uses only characters and never calls production rule methods.
std::vector<int> captures(const Cells& cells, char player, Move move)
{
    std::vector<int> result;
    if (move.row < 0 || move.row >= 8 || move.col < 0 || move.col >= 8)
    {
        return result;
    }
    if (cells[index(move.row, move.col)] != '.')
    {
        return result;
    }
    for (int dr = -1; dr <= 1; ++dr)
    {
        for (int dc = -1; dc <= 1; ++dc)
        {
            if (dr == 0 && dc == 0)
            {
                continue;
            }
            std::vector<int> line;
            int row = move.row + dr;
            int col = move.col + dc;
            while (row >= 0 && row < 8 && col >= 0 && col < 8
                   && cells[index(row, col)] == other(player))
            {
                line.push_back(index(row, col));
                row += dr;
                col += dc;
            }
            if (row >= 0 && row < 8 && col >= 0 && col < 8
                && cells[index(row, col)] == player)
            {
                result.insert(result.end(), line.begin(), line.end());
            }
        }
    }
    return result;
}

std::vector<Move> legal_moves(const Cells& cells, char player)
{
    std::vector<Move> result;
    for (int row = 0; row < 8; ++row)
    {
        for (int col = 0; col < 8; ++col)
        {
            Move move{row, col};
            if (!captures(cells, player, move).empty())
            {
                result.push_back(move);
            }
        }
    }
    return result;
}

Cells applied(Cells cells, char player, Move move)
{
    const auto flipped = captures(cells, player, move);
    CHECK(!flipped.empty());
    cells[index(move.row, move.col)] = player;
    for (int square : flipped)
    {
        cells[square] = player;
    }
    return cells;
}

bool finished(const Cells& cells)
{
    return legal_moves(cells, 'B').empty() && legal_moves(cells, 'W').empty();
}

std::vector<int> canonical(const std::vector<Move>& moves)
{
    std::vector<int> result;
    for (Move move : moves)
    {
        result.push_back(index(move.row, move.col));
    }
    std::sort(result.begin(), result.end());
    return result;
}

void check_board(const Board& board, const Cells& oracle)
{
    CHECK(board.to_string() == text(oracle));
    CHECK(board.count(Disc::Black) == count(oracle, 'B'));
    CHECK(board.count(Disc::White) == count(oracle, 'W'));
    CHECK(board.count(Disc::Empty) == count(oracle, '.'));
    CHECK(board.finished() == finished(oracle));
    CHECK(canonical(board.legal_moves(Disc::Black)) == canonical(legal_moves(oracle, 'B')));
    CHECK(canonical(board.legal_moves(Disc::White)) == canonical(legal_moves(oracle, 'W')));
}

void initial_board()
{
    Board board;
    check_board(board, initial_cells());
    CHECK(board.count(Disc::Black) == 2);
    CHECK(board.count(Disc::White) == 2);
    CHECK(board.count(Disc::Empty) == 60);
    CHECK(canonical(board.legal_moves(Disc::Black)) == std::vector<int>({19, 26, 37, 44}));
    CHECK(canonical(board.legal_moves(Disc::White)) == std::vector<int>({20, 29, 34, 43}));
}

void serialization_and_bounds()
{
    const auto oracle = initial_cells();
    Board board = Board::from_string(text(oracle));
    CHECK(board.to_string() == text(oracle));
    CHECK(board.at(3, 3) == Disc::White);
    CHECK(board.at(3, 4) == Disc::Black);
    CHECK(board.at(0, 0) == Disc::Empty);
    expect_throw<std::invalid_argument>([] { Board::from_string(std::string(63, '.')); });
    expect_throw<std::invalid_argument>([] { Board::from_string(std::string(65, '.')); });
    expect_throw<std::invalid_argument>([] { Board::from_string(std::string(64, 'x')); });
    expect_throw<std::invalid_argument>([] { Board::from_string(std::string(63, '.') + "\n"); });
    expect_throw<std::out_of_range>([&] { board.at(-1, 0); });
    expect_throw<std::out_of_range>([&] { board.at(0, 8); });
}

void player_validation()
{
    CHECK(opponent(Disc::Black) == Disc::White);
    CHECK(opponent(Disc::White) == Disc::Black);
    Board board;
    expect_throw<std::invalid_argument>([] { opponent(Disc::Empty); });
    expect_throw<std::invalid_argument>([&] { board.legal_moves(Disc::Empty); });
    expect_throw<std::invalid_argument>([&] { board.is_legal(Disc::Empty, Move{2, 3}); });
    expect_throw<std::invalid_argument>([&] { board.play(Disc::Empty, Move{2, 3}); });
    expect_throw<std::invalid_argument>([&] { Game game(board, Disc::Empty); });
}

void flips_in_all_eight_directions()
{
    Cells oracle = filled('.');
    for (int dr = -1; dr <= 1; ++dr)
    {
        for (int dc = -1; dc <= 1; ++dc)
        {
            if (dr == 0 && dc == 0)
            {
                continue;
            }
            oracle[index(3 + dr, 3 + dc)] = 'W';
            oracle[index(3 + 2 * dr, 3 + 2 * dc)] = 'B';
        }
    }
    Board board = Board::from_string(text(oracle));
    board.play(Disc::Black, Move{3, 3});
    oracle = applied(oracle, 'B', Move{3, 3});
    check_board(board, oracle);
    CHECK(board.count(Disc::Black) == 17);
    CHECK(board.count(Disc::White) == 0);
}

void illegal_moves_are_atomic()
{
    Board board;
    const std::string before = board.to_string();
    for (Move move : std::vector<Move>{{0, 0}, {3, 3}, {-1, 3}, {8, 3}})
    {
        CHECK(!board.is_legal(Disc::Black, move));
        expect_throw<std::invalid_argument>([&] { board.play(Disc::Black, move); });
        CHECK(board.to_string() == before);
    }
}

void turn_changes_and_illegal_pass()
{
    Game game;
    CHECK(game.turn() == Disc::Black);
    const auto before = game.board().to_string();
    expect_throw<std::invalid_argument>([&] { game.pass(); });
    expect_throw<std::invalid_argument>([&] { game.play(Move{0, 0}); });
    CHECK(game.board().to_string() == before);
    CHECK(game.turn() == Disc::Black);
    game.play(Move{2, 3});
    CHECK(game.turn() == Disc::White);
    check_board(game.board(), applied(initial_cells(), 'B', Move{2, 3}));
}

Cells forced_win(char winner)
{
    Cells cells = filled(winner);
    cells[index(0, 0)] = '.';
    cells[index(0, 1)] = other(winner);
    return cells;
}

void forced_pass_for_both_colors()
{
    for (char winner : std::string("BW"))
    {
        Cells oracle = forced_win(winner);
        Game game(Board::from_string(text(oracle)), disc(other(winner)));
        CHECK(!game.finished());
        CHECK(game.board().legal_moves(game.turn()).empty());
        game.pass();
        CHECK(game.turn() == disc(winner));
        CHECK(game.board().to_string() == text(oracle));
        expect_throw<std::invalid_argument>([&] { game.pass(); });
        game.play(Move{0, 0});
        oracle = applied(oracle, winner, Move{0, 0});
        check_board(game.board(), oracle);
        CHECK(game.finished());
        CHECK(game.board().count(disc(winner)) == 64);
        expect_throw<std::invalid_argument>([&] { game.pass(); });
        expect_throw<std::invalid_argument>([&] { game.play(Move{0, 0}); });
    }
}

void terminal_before_board_is_full()
{
    Cells oracle = filled('B');
    oracle[index(0, 0)] = '.';
    Game game(Board::from_string(text(oracle)), Disc::White);
    check_board(game.board(), oracle);
    CHECK(game.finished());
    CHECK(game.board().count(Disc::Black) == 63);
    CHECK(game.board().count(Disc::Empty) == 1);
    expect_throw<std::invalid_argument>([&] { game.pass(); });
    CHECK(!choose_minimax(game.board(), Disc::Black, 3));
    CHECK(!choose_mcts(game.board(), Disc::White, 16, 27));
}

void terminal_scores_include_draws_and_both_winners()
{
    Cells draw = filled('B');
    std::fill(draw.begin(), draw.begin() + 32, 'W');
    Board board = Board::from_string(text(draw));
    check_board(board, draw);
    CHECK(board.finished());
    CHECK(board.count(Disc::Black) == board.count(Disc::White));
    for (char winner : std::string("BW"))
    {
        const Cells oracle = filled(winner);
        const Board won = Board::from_string(text(oracle));
        check_board(won, oracle);
        CHECK(won.count(disc(winner)) == 64);
        CHECK(won.count(disc(other(winner))) == 0);
    }
}

void search_rejects_invalid_arguments()
{
    Board board;
    expect_throw<std::invalid_argument>([&] { choose_minimax(board, Disc::Black, 0); });
    expect_throw<std::invalid_argument>([&] { choose_minimax(board, Disc::Black, -1); });
    expect_throw<std::invalid_argument>([&] { choose_mcts(board, Disc::Black, 0, 27); });
    expect_throw<std::invalid_argument>([&] { choose_mcts(board, Disc::Black, -1, 27); });
    expect_throw<std::invalid_argument>([&] { choose_minimax(board, Disc::Empty, 3); });
    expect_throw<std::invalid_argument>([&] { choose_mcts(board, Disc::Empty, 16, 27); });
}

void search_is_legal_reproducible_and_read_only()
{
    Board board;
    const auto before = board.to_string();
    const auto minimax = choose_minimax(board, Disc::Black, 3);
    CHECK(minimax.has_value());
    CHECK(board.is_legal(Disc::Black, *minimax));
    CHECK(board.to_string() == before);
    const auto first = choose_mcts(board, Disc::Black, 128, 27);
    const auto repeated = choose_mcts(board, Disc::Black, 128, 27);
    CHECK(first.has_value());
    CHECK(repeated.has_value());
    CHECK(*first == *repeated);
    CHECK(board.is_legal(Disc::Black, *first));
    CHECK(board.to_string() == before);
}

void search_finds_forced_wins_and_returns_no_move_for_a_pass()
{
    for (char winner : std::string("BW"))
    {
        Board board = Board::from_string(text(forced_win(winner)));
        const auto minimax = choose_minimax(board, disc(winner), 3);
        const auto mcts = choose_mcts(board, disc(winner), 16, 27);
        CHECK(minimax.has_value());
        CHECK(mcts.has_value());
        CHECK((*minimax == Move{0, 0}));
        CHECK((*mcts == Move{0, 0}));
        CHECK(!choose_minimax(board, disc(other(winner)), 3));
        CHECK(!choose_mcts(board, disc(other(winner)), 16, 27));
        board.play(disc(winner), *mcts);
        CHECK(board.finished());
        CHECK(board.count(disc(winner)) == 64);
    }
}

int exact_outcome(const Cells& cells, char turn, char root)
{
    const auto moves = legal_moves(cells, turn);
    if (moves.empty())
    {
        if (legal_moves(cells, other(turn)).empty())
        {
            const int difference = count(cells, root) - count(cells, other(root));
            return (difference > 0) - (difference < 0);
        }
        return exact_outcome(cells, other(turn), root);
    }
    int best = turn == root ? -2 : 2;
    for (Move move : moves)
    {
        const int value = exact_outcome(applied(cells, turn, move), other(turn), root);
        best = turn == root ? std::max(best, value) : std::min(best, value);
        if ((turn == root && best == 1) || (turn != root && best == -1))
        {
            break;
        }
    }
    return best;
}

void minimax_matches_independent_endgame_solver()
{
    int checked = 0;
    for (std::uint64_t seed = 21; seed < 27; ++seed)
    {
        std::mt19937_64 random(seed);
        Cells oracle = initial_cells();
        char turn = 'B';
        while (count(oracle, '.') > 6 && !finished(oracle))
        {
            const auto moves = legal_moves(oracle, turn);
            if (!moves.empty())
            {
                oracle = applied(oracle, turn, moves[random() % moves.size()]);
            }
            turn = other(turn);
        }
        if (finished(oracle))
        {
            continue;
        }
        if (legal_moves(oracle, turn).empty())
        {
            turn = other(turn);
        }
        const Board board = Board::from_string(text(oracle));
        const auto selected = choose_minimax(board, disc(turn), 8);
        CHECK(selected.has_value());
        CHECK(!captures(oracle, turn, *selected).empty());
        const int expected = exact_outcome(oracle, turn, turn);
        const int actual = exact_outcome(applied(oracle, turn, *selected), other(turn), turn);
        CHECK(actual == expected);
        ++checked;
    }
    CHECK(checked == 6);
}

enum class Strategy
{
    Random,
    Minimax,
    Mcts
};

void full_game(Strategy black, Strategy white, std::uint64_t seed)
{
    Game game;
    Cells oracle = initial_cells();
    char turn = 'B';
    std::mt19937_64 random(seed);
    int moves_played = 0;
    int actions = 0;
    while (!finished(oracle))
    {
        CHECK(actions < 128);
        CHECK(!game.finished());
        CHECK(game.turn() == disc(turn));
        check_board(game.board(), oracle);
        const auto moves = legal_moves(oracle, turn);
        if (moves.empty())
        {
            const auto before = game.board().to_string();
            game.pass();
            CHECK(game.board().to_string() == before);
        }
        else
        {
            Move chosen = moves[random() % moves.size()];
            const auto strategy = turn == 'B' ? black : white;
            if (strategy == Strategy::Minimax)
            {
                const auto searched = choose_minimax(game.board(), disc(turn), 3);
                CHECK(searched.has_value());
                chosen = *searched;
            }
            else if (strategy == Strategy::Mcts)
            {
                const auto searched = choose_mcts(game.board(), disc(turn), 128,
                                                  seed + static_cast<std::uint64_t>(actions));
                CHECK(searched.has_value());
                chosen = *searched;
            }
            CHECK(!captures(oracle, turn, chosen).empty());
            const int occupied_before = 64 - count(oracle, '.');
            oracle = applied(oracle, turn, chosen);
            game.play(chosen);
            CHECK(64 - count(oracle, '.') == occupied_before + 1);
            ++moves_played;
        }
        turn = other(turn);
        ++actions;
        CHECK(game.turn() == disc(turn));
        check_board(game.board(), oracle);
    }
    CHECK(game.finished());
    CHECK(moves_played <= 60);
    CHECK(moves_played + 4 == game.board().count(Disc::Black) + game.board().count(Disc::White));
    std::cout << "  complete game seed=" << seed << " moves=" << moves_played
              << " black=" << game.board().count(Disc::Black)
              << " white=" << game.board().count(Disc::White) << '\n';
}

void complete_random_games()
{
    for (std::uint64_t seed : {1, 2, 3})
    {
        full_game(Strategy::Random, Strategy::Random, seed);
    }
}

void complete_human_simulated_ai_games()
{
    full_game(Strategy::Random, Strategy::Mcts, 4);
    full_game(Strategy::Random, Strategy::Mcts, 5);
    full_game(Strategy::Mcts, Strategy::Random, 6);
    full_game(Strategy::Random, Strategy::Minimax, 7);
    full_game(Strategy::Random, Strategy::Minimax, 8);
    full_game(Strategy::Minimax, Strategy::Random, 9);
}

void complete_ai_game()
{
    full_game(Strategy::Minimax, Strategy::Mcts, 10);
}
}

int main()
{
    const std::vector<std::pair<const char*, std::function<void()>>> tests = {
        {"initial board and legal moves", initial_board},
        {"serialization and bounds", serialization_and_bounds},
        {"player validation", player_validation},
        {"all eight flip directions", flips_in_all_eight_directions},
        {"illegal moves leave the board unchanged", illegal_moves_are_atomic},
        {"turn changes and illegal passes", turn_changes_and_illegal_pass},
        {"forced passes for both colors", forced_pass_for_both_colors},
        {"terminal position before board is full", terminal_before_board_is_full},
        {"draw and both winning colors", terminal_scores_include_draws_and_both_winners},
        {"invalid search arguments", search_rejects_invalid_arguments},
        {"legal deterministic read-only search", search_is_legal_reproducible_and_read_only},
        {"forced wins and search passes", search_finds_forced_wins_and_returns_no_move_for_a_pass},
        {"Minimax against exact endgame oracle", minimax_matches_independent_endgame_solver},
        {"complete random games", complete_random_games},
        {"complete simulated human versus AI games", complete_human_simulated_ai_games},
        {"complete Minimax versus MCTS game", complete_ai_game}
    };
    int passed = 0;
    for (const auto& test : tests)
    {
        try
        {
            test.second();
            ++passed;
            std::cout << "PASS " << test.first << '\n';
        }
        catch (const std::exception& error)
        {
            std::cerr << "FAIL " << test.first << ": " << error.what() << '\n';
            return 1;
        }
    }
    std::cout << "All " << passed << " C++ test groups passed.\n";
    return 0;
}
