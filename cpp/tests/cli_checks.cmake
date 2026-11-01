if(NOT DEFINED OTHELLO_CLI)
    message(FATAL_ERROR "OTHELLO_CLI is required")
endif()

file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/cli-input.txt" "z9\nd3\nquit\n")
execute_process(COMMAND "${OTHELLO_CLI}" human human
    INPUT_FILE "${CMAKE_CURRENT_BINARY_DIR}/cli-input.txt"
    RESULT_VARIABLE status OUTPUT_VARIABLE output ERROR_VARIABLE errors TIMEOUT 10)
if(NOT status EQUAL 0)
    message(FATAL_ERROR "Interactive game failed: ${status}\n${errors}")
endif()
foreach(expected "Invalid move" "Black plays d3" "Black: 4  White: 1" "White [(]human[)] to move" "Game stopped")
    if(NOT "${output}" MATCHES "${expected}")
        message(FATAL_ERROR "Missing interactive output: ${expected}\n${output}")
    endif()
endforeach()

function(expect_bad_arguments)
    execute_process(COMMAND "${OTHELLO_CLI}" ${ARGN}
        RESULT_VARIABLE status OUTPUT_VARIABLE output ERROR_VARIABLE errors TIMEOUT 10)
    if(NOT status EQUAL 2 OR NOT "${errors}" MATCHES "Error:")
        message(FATAL_ERROR "Bad arguments were not rejected: ${ARGN}\n${output}\n${errors}")
    endif()
endfunction()

expect_bad_arguments(human custom)
expect_bad_arguments(--depth 0)
expect_bad_arguments(--iterations -1)
expect_bad_arguments(--seed -1)
expect_bad_arguments(--seed 12x)
expect_bad_arguments(--iterations)
expect_bad_arguments(--unknown)
expect_bad_arguments(human)
