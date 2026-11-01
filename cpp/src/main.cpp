#include "ccbg/othello.hpp"
#include "ccbg/search.hpp"

#include <algorithm>
#include <charconv>
#include <cctype>
#include <cstdint>
#include <iostream>
#include <optional>
#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

namespace
{
using namespace ccbg::othello;

enum class Mode
{
    Human,
    Minimax,
    Mcts
};

struct Settings
{
    Mode black = Mode::Human;
    Mode white = Mode::Mcts;
    int depth = 3;
    int iterations = 500;
    std::uint64_t seed = 27;
    bool help = false;
};

Mode parse_mode(std::string_view name)
{
    if (name == "human")
    {
        return Mode::Human;
    }
    if (name == "minimax")
    {
        return Mode::Minimax;
    }
    if (name == "mcts")
    {
        return Mode::Mcts;
    }
    throw std::invalid_argument("Player must be human, minimax, or mcts");
}

template <typename Number>
Number parse_number(std::string_view text, std::string_view option)
{
    Number value{};
    const auto result = std::from_chars(text.data(), text.data() + text.size(), value);
    if (text.empty() || result.ec != std::errc{} || result.ptr != text.data() + text.size())
    {
        throw std::invalid_argument("Invalid number for " + std::string(option));
    }
    return value;
}

Settings parse_settings(int argc, char* argv[])
{
    Settings settings;
    std::vector<std::string_view> players;
    for (int i = 1; i < argc; ++i)
    {
        const std::string_view argument = argv[i];
        if (argument == "--help" || argument == "-h")
        {
            settings.help = true;
        }
        else if (argument == "--depth" || argument == "--iterations" || argument == "--seed")
        {
            if (i + 1 == argc)
            {
                throw std::invalid_argument("Missing value for " + std::string(argument));
            }
            ++i;
            if (argument == "--depth")
            {
                settings.depth = parse_number<int>(argv[i], argument);
            }
            else if (argument == "--iterations")
            {
                settings.iterations = parse_number<int>(argv[i], argument);
            }
            else
            {
                settings.seed = parse_number<std::uint64_t>(argv[i], argument);
            }
        }
        else if (argument.substr(0, 1) == "-")
        {
            throw std::invalid_argument("Unknown option: " + std::string(argument));
        }
        else
        {
            players.push_back(argument);
        }
    }
    if (settings.help)
    {
        return settings;
    }
    if (!players.empty())
    {
        if (players.size() != 2)
        {
            throw std::invalid_argument("Specify both black and white players");
        }
        settings.black = parse_mode(players[0]);
        settings.white = parse_mode(players[1]);
    }
    if (settings.depth <= 0 || settings.iterations <= 0)
    {
        throw std::invalid_argument("Depth and iterations must be positive");
    }
    return settings;
}

std::string_view color_name(Disc color)
{
    return color == Disc::Black ? "Black" : "White";
}

std::string_view mode_name(Mode mode)
{
    if (mode == Mode::Human)
    {
        return "human";
    }
    if (mode == Mode::Minimax)
    {
        return "minimax";
    }
    return "mcts";
}

std::string coordinate(Move move)
{
    return std::string{static_cast<char>('a' + move.col), static_cast<char>('1' + move.row)};
}

void draw_board(const Board& board, const std::vector<Move>& legal)
{
    std::cout << "\n    a b c d e f g h\n";
    for (int row = 0; row < Board::size; ++row)
    {
        std::cout << ' ' << row + 1 << "  ";
        for (int col = 0; col < Board::size; ++col)
        {
            const Disc disc = board.at(row, col);
            char symbol = '.';
            if (disc == Disc::Black)
            {
                symbol = 'B';
            }
            else if (disc == Disc::White)
            {
                symbol = 'W';
            }
            else if (std::find(legal.begin(), legal.end(), Move{row, col}) != legal.end())
            {
                symbol = '*';
            }
            std::cout << symbol << ' ';
        }
        std::cout << '\n';
    }
    std::cout << "Black: " << board.count(Disc::Black)
              << "  White: " << board.count(Disc::White) << "\n";
}

std::optional<Move> human_move(const Board& board, Disc turn)
{
    for (;;)
    {
        std::cout << "Move (for example d3, or quit): " << std::flush;
        std::string input;
        if (!std::getline(std::cin, input))
        {
            return std::nullopt;
        }
        std::string compact;
        for (unsigned char character : input)
        {
            if (!std::isspace(character))
            {
                compact.push_back(static_cast<char>(std::tolower(character)));
            }
        }
        if (compact == "quit" || compact == "q" || compact == "exit")
        {
            return std::nullopt;
        }
        if (compact.size() == 2 && compact[0] >= 'a' && compact[0] <= 'h'
            && compact[1] >= '1' && compact[1] <= '8')
        {
            const Move move{compact[1] - '1', compact[0] - 'a'};
            if (board.is_legal(turn, move))
            {
                return move;
            }
        }
        std::cout << "Invalid move. Choose a square marked *.\n";
    }
}

int run_game(const Settings& settings)
{
    Game game;
    std::uint64_t searches = 0;
    std::cout << "Othello: B = black, W = white, * = legal move.\n";
    while (!game.finished())
    {
        const Disc turn = game.turn();
        const auto legal = game.board().legal_moves(turn);
        if (legal.empty())
        {
            std::cout << color_name(turn) << " has no legal moves; passes.\n";
            game.pass();
            continue;
        }
        draw_board(game.board(), legal);
        const Mode mode = turn == Disc::Black ? settings.black : settings.white;
        std::cout << color_name(turn) << " (" << mode_name(mode) << ") to move.\n";
        std::optional<Move> move;
        if (mode == Mode::Human)
        {
            move = human_move(game.board(), turn);
            if (!move)
            {
                std::cout << "Game stopped.\n";
                return 0;
            }
        }
        else if (mode == Mode::Minimax)
        {
            move = choose_minimax(game.board(), turn, settings.depth);
        }
        else
        {
            move = choose_mcts(game.board(), turn, settings.iterations, settings.seed + searches);
            ++searches;
        }
        if (!move)
        {
            throw std::runtime_error("Search returned no move for a playable position");
        }
        std::cout << color_name(turn) << " plays " << coordinate(*move) << ".\n";
        game.play(*move);
    }
    draw_board(game.board(), {});
    const int black = game.board().count(Disc::Black);
    const int white = game.board().count(Disc::White);
    std::cout << "Game over. ";
    if (black == white)
    {
        std::cout << "Draw.\n";
    }
    else
    {
        std::cout << (black > white ? "Black" : "White") << " wins.\n";
    }
    return 0;
}
}

int main(int argc, char* argv[])
{
    try
    {
        const Settings settings = parse_settings(argc, argv);
        if (settings.help)
        {
            std::cout << "Usage: othello_cli [black-player white-player] [options]\n"
                      << "Players: human, minimax, mcts. Default: human mcts.\n"
                      << "Options: --depth N --iterations N --seed N --help\n"
                      << "Enter coordinates a1 through h8. Passing is automatic when required.\n";
            return 0;
        }
        return run_game(settings);
    }
    catch (const std::exception& failure)
    {
        std::cerr << "Error: " << failure.what() << "\nUse --help for usage.\n";
        return 2;
    }
}
