cmake_minimum_required(VERSION 3.16)
project(CCBoardGameCpp VERSION 0.1.0 LANGUAGES CXX)

set(CCBG_STANDALONE OFF)
if(CMAKE_CURRENT_SOURCE_DIR STREQUAL CMAKE_SOURCE_DIR)
    set(CCBG_STANDALONE ON)
endif()
option(CCBG_BUILD_CLI "Build the terminal Othello application" ${CCBG_STANDALONE})
option(CCBG_BUILD_TESTS "Build Othello regression tests" ${CCBG_STANDALONE})

add_library(ccbg_othello STATIC src/othello.cpp src/search.cpp)
add_library(ccbg::othello ALIAS ccbg_othello)
target_include_directories(ccbg_othello PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(ccbg_othello PUBLIC cxx_std_17)
set_target_properties(ccbg_othello PROPERTIES CXX_EXTENSIONS OFF)
if(MSVC)
    target_compile_options(ccbg_othello PRIVATE /W4 /permissive-)
else()
    target_compile_options(ccbg_othello PRIVATE -Wall -Wextra -Wpedantic)
endif()

if(CCBG_BUILD_CLI)
    add_executable(othello_cli src/main.cpp)
    target_link_libraries(othello_cli PRIVATE ccbg::othello)
endif()

if(CCBG_BUILD_TESTS)
    enable_testing()
    add_executable(othello_tests tests/othello_tests.cpp)
    target_link_libraries(othello_tests PRIVATE ccbg::othello)
    add_test(NAME rules_and_search COMMAND othello_tests)
    set_tests_properties(rules_and_search PROPERTIES TIMEOUT 90)

    if(CCBG_BUILD_CLI)
        add_test(NAME cli_help COMMAND othello_cli --help)
        set_tests_properties(cli_help PROPERTIES PASS_REGULAR_EXPRESSION "Usage:")
        add_test(NAME cli_complete_game
            COMMAND othello_cli minimax mcts --depth 2 --iterations 64 --seed 27)
        set_tests_properties(cli_complete_game PROPERTIES
            PASS_REGULAR_EXPRESSION "Game over" TIMEOUT 60)
        add_test(NAME cli_input_and_errors
            COMMAND ${CMAKE_COMMAND} -DOTHELLO_CLI=$<TARGET_FILE:othello_cli>
                -P ${CMAKE_CURRENT_SOURCE_DIR}/tests/cli_checks.cmake)
        set_tests_properties(cli_input_and_errors PROPERTIES TIMEOUT 30)
    endif()
endif()
