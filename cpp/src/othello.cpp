#include "ccbg/othello.hpp"

#include <algorithm>
#include <stdexcept>
#include <utility>

namespace ccbg::othello
{
namespace
{

constexpr std::array<Move, 8> directions
{{
    {-1, -1},
    {-1, 0},
    {-1, 1},
    {0, -1},
    {0, 1},
    {1, -1},
    {1, 0},
    {1, 1}
}};

void require_player(Disc player)
{
    if (player != Disc::Black && player != Disc::White)
    {
        throw std::invalid_argument("A player must use black or white discs");
    }
}

}

Disc opponent(Disc player)
{
    require_player(player);
    return player == Disc::Black ? Disc::White : Disc::Black;
}

Board::Board()
{
    cells_.fill(Disc::Empty);
    cells_[offset(3, 3)] = Disc::White;
    cells_[offset(3, 4)] = Disc::Black;
    cells_[offset(4, 3)] = Disc::Black;
    cells_[offset(4, 4)] = Disc::White;
}

Board Board::from_string(std::string_view cells)
{
    if (cells.size() != size * size)
    {
        throw std::invalid_argument("An Othello board requires exactly 64 cells");
    }
    Board board;
    for (std::size_t index = 0; index < cells.size(); ++index)
    {
        switch (cells[index])
        {
            case 'B':
                board.cells_[index] = Disc::Black;
                break;
            case 'W':
                board.cells_[index] = Disc::White;
                break;
            case '.':
                board.cells_[index] = Disc::Empty;
                break;
            default:
                throw std::invalid_argument("Board cells must be B, W, or .");
        }
    }
    return board;
}

std::string Board::to_string() const
{
    std::string result;
    result.reserve(cells_.size());
    for (Disc disc : cells_)
    {
        switch (disc)
        {
            case Disc::Black:
                result.push_back('B');
                break;
            case Disc::White:
                result.push_back('W');
                break;
            case Disc::Empty:
                result.push_back('.');
                break;
        }
    }
    return result;
}

bool Board::in_bounds(Move move) noexcept
{
    return move.row >= 0 && move.row < size && move.col >= 0 && move.col < size;
}

std::size_t Board::offset(int row, int col) noexcept
{
    return static_cast<std::size_t>(row * size + col);
}

Disc Board::at(int row, int col) const
{
    if (!in_bounds({row, col}))
    {
        throw std::out_of_range("Board coordinates must be between 0 and 7");
    }
    return cells_[offset(row, col)];
}

bool Board::captures(Disc player, Move move, Move direction) const
{
    const Disc other = opponent(player);
    Move cursor{move.row + direction.row, move.col + direction.col};
    bool found_opponent = false;
    while (in_bounds(cursor) && cells_[offset(cursor.row, cursor.col)] == other)
    {
        found_opponent = true;
        cursor.row += direction.row;
        cursor.col += direction.col;
    }
    return found_opponent && in_bounds(cursor)
        && cells_[offset(cursor.row, cursor.col)] == player;
}

bool Board::is_legal(Disc player, Move move) const
{
    require_player(player);
    if (!in_bounds(move) || cells_[offset(move.row, move.col)] != Disc::Empty)
    {
        return false;
    }
    for (Move direction : directions)
    {
        if (captures(player, move, direction))
        {
            return true;
        }
    }
    return false;
}

std::vector<Move> Board::legal_moves(Disc player) const
{
    require_player(player);
    std::vector<Move> moves;
    for (int row = 0; row < size; ++row)
    {
        for (int col = 0; col < size; ++col)
        {
            Move move{row, col};
            if (is_legal(player, move))
            {
                moves.push_back(move);
            }
        }
    }
    return moves;
}

void Board::play(Disc player, Move move)
{
    if (!is_legal(player, move))
    {
        throw std::invalid_argument("A move must capture at least one opposing disc");
    }
    const Disc other = opponent(player);
    for (Move direction : directions)
    {
        if (!captures(player, move, direction))
        {
            continue;
        }
        Move cursor{move.row + direction.row, move.col + direction.col};
        while (cells_[offset(cursor.row, cursor.col)] == other)
        {
            cells_[offset(cursor.row, cursor.col)] = player;
            cursor.row += direction.row;
            cursor.col += direction.col;
        }
    }
    cells_[offset(move.row, move.col)] = player;
}

int Board::count(Disc disc) const
{
    if (disc != Disc::Empty && disc != Disc::Black && disc != Disc::White)
    {
        throw std::invalid_argument("Unknown disc value");
    }
    return static_cast<int>(std::count(cells_.begin(), cells_.end(), disc));
}

bool Board::finished() const
{
    for (int row = 0; row < size; ++row)
    {
        for (int col = 0; col < size; ++col)
        {
            if (is_legal(Disc::Black, {row, col}) || is_legal(Disc::White, {row, col}))
            {
                return false;
            }
        }
    }
    return true;
}

Game::Game()
    : board_(), turn_(Disc::Black)
{
}

Game::Game(Board board, Disc turn)
    : board_(std::move(board)), turn_(turn)
{
    require_player(turn);
}

const Board& Game::board() const
{
    return board_;
}

Disc Game::turn() const
{
    return turn_;
}

bool Game::finished() const
{
    return board_.finished();
}

void Game::play(Move move)
{
    if (finished())
    {
        throw std::invalid_argument("A finished game cannot accept another move");
    }
    board_.play(turn_, move);
    turn_ = opponent(turn_);
}

void Game::pass()
{
    if (finished() || !board_.legal_moves(turn_).empty())
    {
        throw std::invalid_argument("Passing requires no legal move in an unfinished game");
    }
    turn_ = opponent(turn_);
}

}
