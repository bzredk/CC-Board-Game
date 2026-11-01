#pragma once

#include "ccbg/othello.hpp"

#include <cstdint>
#include <optional>

namespace ccbg::othello
{

std::optional<Move> choose_minimax(const Board& board, Disc player, int depth = 3);

std::optional<Move> choose_mcts(const Board& board, Disc player, int iterations = 500,
                              std::uint64_t seed = 27);

}
