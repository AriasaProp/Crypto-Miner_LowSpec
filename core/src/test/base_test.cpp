#include <iostream>
#include "base.hpp"

bool base_test() {
    bool testResult = true;
    std::cout << "base:" << std::endl;
    {
        std::cout << " - Add test: ";
        //Add test
        int result = 7;
        bool t = (7 == Add(4, 3));
        std::cout << (t?"Success":"Failure") << std::endl;
        testResult |= t;
    }
    return testResult;
}
