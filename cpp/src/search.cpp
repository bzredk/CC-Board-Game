#include "ccbg/search.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <limits>
#include <memory>
#include <random>
#include <stdexcept>
#include <utility>
#include <vector>

namespace ccbg::othello
{
namespace
{

constexpr int terminal_score = 1000000;

constexpr std::array<std::array<int, 8>, 8> positional_weights = {{
    {{200, -70, 30, 25, 25, 30, -70, 200}},
    {{-70, -100, -10, -10, -10, -10, -100, -70}},
    {{30, -10, 2, 2, 2, 2, -10, 30}},
    {{25, -10, 2, 2, 2, 2, -10, 25}},
    {{25, -10, 2, 2, 2, 2, -10, 25}},
    {{30, -10, 2, 2, 2, 2, -10, 30}},
    {{-70, -100, -10, -10, -10, -10, -100, -70}},
    {{200, -70, 30, 25, 25, 30, -70, 200}}
}};

void validate_player(Disc player)
{
    if (player != Disc::Black && player != Disc::White)
    {
        throw std::invalid_argument("A search player must be black or white");
    }
}

int evaluate_position(const Board& board, Disc player)
{
    int score = 0;
    const Disc other = opponent(player);
    for (int row = 0; row < 8; ++row)
    {
        for (int col = 0; col < 8; ++col)
        {
            if (board.at(row, col) == player)
            {
                score += positional_weights[row][col];
            }
            else if (board.at(row, col) == other)
            {
                score -= positional_weights[row][col];
            }
        }
    }
    return score;
}

int evaluate_terminal(const Board& board, Disc player)
{
    const int difference = board.count(player) - board.count(opponent(player));
    if (difference > 0)
    {
        return terminal_score + difference;
    }
    if (difference < 0)
    {
        return -terminal_score + difference;
    }
    return 0;
}

int minimax_value(const Board& board, Disc turn, Disc root_player, int depth,
                  int alpha, int beta)
{
    const auto moves = board.legal_moves(turn);
    const Disc next = opponent(turn);
    if (moves.empty())
    {
        if (board.legal_moves(next).empty())
        {
            return evaluate_terminal(board, root_player);
        }
        // A forced pass does not consume a placement from the search budget.
        return minimax_value(board, next, root_player, depth, alpha, beta);
    }
    if (depth == 0)
    {
        return evaluate_position(board, root_player);
    }

    const bool maximizing = turn == root_player;
    int best = maximizing ? std::numeric_limits<int>::min()
                          : std::numeric_limits<int>::max();
    for (const Move move : moves)
    {
        Board child = board;
        child.play(turn, move);
        const int value = minimax_value(child, next, root_player, depth - 1, alpha, beta);
        if (maximizing)
        {
            best = std::max(best, value);
            alpha = std::max(alpha, best);
        }
        else
        {
            best = std::min(best, value);
            beta = std::min(beta, best);
        }
        if (alpha >= beta)
        {
            break;
        }
    }
    return best;
}

struct MctsNode
{
    Board board;
    Disc turn;
    std::optional<Move> move;
    MctsNode* parent;
    std::vector<std::optional<Move>> untried;
    std::vector<std::unique_ptr<MctsNode>> children;
    int visits = 0;
    double root_returns = 0.0;

    MctsNode(Board position, Disc player, std::optional<Move> action, MctsNode* ancestor)
        : board(std::move(position)), turn(player), move(action), parent(ancestor)
    {
        for (const Move candidate : board.legal_moves(turn))
        {
            untried.emplace_back(candidate);
        }
        if (untried.empty() && !board.legal_moves(opponent(turn)).empty())
        {
            untried.emplace_back(std::nullopt);
        }
    }
};

std::size_t random_index(std::size_t size, std::mt19937_64& random)
{
    return std::uniform_int_distribution<std::size_t>(0, size - 1)(random);
}

MctsNode* select_child(MctsNode& node, Disc root_player)
{
    MctsNode* best = nullptr;
    double best_value = -std::numeric_limits<double>::infinity();
    for (const auto& child : node.children)
    {
        double value = std::numeric_limits<double>::infinity();
        if (child->visits > 0)
        {
            const double mean = child->root_returns / child->visits;
            // The player choosing this edge optimizes its own outcome.
            const double exploitation = node.turn == root_player ? mean : 1.0 - mean;
            const double exploration = std::sqrt(2.0 * std::log(node.visits) / child->visits);
            value = exploitation + exploration;
        }
        if (value > best_value)
        {
            best_value = value;
            best = child.get();
        }
    }
    return best;
}

double rollout(Board board, Disc turn, Disc root_player, std::mt19937_64& random)
{
    int passes = 0;
    while (passes < 2)
    {
        const auto moves = board.legal_moves(turn);
        if (moves.empty())
        {
            ++passes;
        }
        else
        {
            board.play(turn, moves[random_index(moves.size(), random)]);
            passes = 0;
        }
        turn = opponent(turn);
    }
    const int difference = board.count(root_player) - board.count(opponent(root_player));
    if (difference > 0)
    {
        return 1.0;
    }
    if (difference < 0)
    {
        return 0.0;
    }
    return 0.5;
}

}

std::optional<Move> choose_minimax(const Board& board, Disc player, int depth)
{
    validate_player(player);
    if (depth <= 0)
    {
        throw std::invalid_argument("Minimax depth must be positive");
    }
    const auto moves = board.legal_moves(player);
    if (moves.empty())
    {
        return std::nullopt;
    }

    Move best_move = moves.front();
    int best_value = std::numeric_limits<int>::min();
    int alpha = std::numeric_limits<int>::min();
    const int beta = std::numeric_limits<int>::max();
    for (const Move move : moves)
    {
        Board child = board;
        child.play(player, move);
        const int value = minimax_value(child, opponent(player), player, depth - 1, alpha, beta);
        if (value > best_value)
        {
            best_value = value;
            best_move = move;
        }
        alpha = std::max(alpha, best_value);
    }
    return best_move;
}

std::optional<Move> choose_mcts(const Board& board, Disc player, int iterations, std::uint64_t seed)
{
    validate_player(player);
    if (iterations <= 0)
    {
        throw std::invalid_argument("MCTS iterations must be positive");
    }
    if (board.legal_moves(player).empty())
    {
        return std::nullopt;
    }

    std::mt19937_64 random(seed);
    MctsNode root(board, player, std::nullopt, nullptr);
    for (int iteration = 0; iteration < iterations; ++iteration)
    {
        MctsNode* node = &root;
        while (node->untried.empty() && !node->children.empty())
        {
            node = select_child(*node, player);
        }
        if (!node->untried.empty())
        {
            const std::size_t index = random_index(node->untried.size(), random);
            const auto move = node->untried[index];
            node->untried.erase(node->untried.begin() + static_cast<std::ptrdiff_t>(index));
            Board child = node->board;
            if (move.has_value())
            {
                child.play(node->turn, *move);
            }
            node->children.push_back(std::make_unique<MctsNode>(
                std::move(child), opponent(node->turn), move, node));
            node = node->children.back().get();
        }

        const double result = rollout(node->board, node->turn, player, random);
        while (node != nullptr)
        {
            ++node->visits;
            node->root_returns += result;
            node = node->parent;
        }
    }

    const MctsNode* best = nullptr;
    for (const auto& child : root.children)
    {
        if (best == nullptr || child->visits > best->visits)
        {
            best = child.get();
        }
        else if (child->visits == best->visits
                 && child->root_returns / child->visits > best->root_returns / best->visits)
        {
            best = child.get();
        }
    }
    return best->move;
}

}
