#pragma once

#include <array>
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>

namespace ccbg::othello
{

enum class Disc
{
    Empty,
    Black,
    White
};

Disc opponent(Disc player);

struct Move
{
    int row;
    int col;

    bool operator==(const Move& other) const noexcept
    {
        return row == other.row && col == other.col;
    }
};

class Board
{
public:
    static constexpr int size = 8;

    Board();
    static Board from_string(std::string_view cells);
    std::string to_string() const;
    Disc at(int row, int col) const;
    std::vector<Move> legal_moves(Disc player) const;
    bool is_legal(Disc player, Move move) const;
    void play(Disc player, Move move);
    int count(Disc disc) const;
    bool finished() const;

private:
    static bool in_bounds(Move move) noexcept;
    static std::size_t offset(int row, int col) noexcept;
    bool captures(Disc player, Move move, Move direction) const;

    std::array<Disc, size * size> cells_;
};

class Game
{
public:
    Game();
    Game(Board board, Disc turn);
    const Board& board() const;
    Disc turn() const;
    bool finished() const;
    void play(Move move);
    void pass();

private:
    Board board_;
    Disc turn_;
};

}
