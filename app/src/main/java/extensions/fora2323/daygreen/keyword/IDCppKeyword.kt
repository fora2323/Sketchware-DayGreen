package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField

object IDCppKeyword {
    @JvmField
    val KEYWORDS = arrayOf(
        // C++ Keywords (C++98 - C++23)
        "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor", "bool",
        "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t", "class", "compl",
        "concept", "const", "consteval", "constexpr", "constinit", "const_cast", "continue",
        "co_await", "co_return", "co_yield", "decltype", "default", "delete", "do", "double",
        "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false", "float",
        "for", "friend", "goto", "if", "inline", "int", "long", "mutable", "namespace", "new",
        "noexcept", "not", "not_eq", "nullptr", "operator", "or", "or_eq", "private",
        "protected", "public", "register", "reinterpret_cast", "requires", "return", "short",
        "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch",
        "template", "this", "thread_local", "throw", "true", "try", "typedef", "typeid",
        "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t",
        "while", "xor", "xor_eq", "final", "override", "import", "module",
        "extern \"C\"", "using namespace std;",

        // Preprocessor
        "#include", "#define", "#undef", "#if", "#ifdef", "#ifndef", "#elif", "#elifdef",
        "#elifndef", "#else", "#endif", "#error", "#warning", "#pragma", "#line",
        "defined", "once", "__cplusplus", "__FILE__", "__LINE__", "__func__",
        "__VA_ARGS__", "__attribute__",

        // Standard C++ Headers
        "<iostream>", "<string>", "<vector>", "<map>", "<unordered_map>", "<set>",
        "<unordered_set>", "<list>", "<deque>", "<queue>", "<stack>", "<array>", "<tuple>",
        "<utility>", "<algorithm>", "<functional>", "<memory>", "<thread>", "<mutex>",
        "<condition_variable>", "<atomic>", "<future>", "<chrono>", "<cmath>", "<cstdio>",
        "<cstdlib>", "<cstring>", "<cstdint>", "<cassert>", "<cctype>", "<ctime>",
        "<fstream>", "<sstream>", "<iomanip>", "<regex>", "<random>", "<optional>",
        "<variant>", "<any>", "<string_view>", "<span>", "<filesystem>", "<numeric>",
        "<iterator>", "<limits>", "<exception>", "<stdexcept>", "<type_traits>",
        "<initializer_list>", "<bitset>", "<complex>", "<ranges>", "<concepts>", "<format>",
        "<coroutine>", "<jni.h>", "<android/log.h>",

        // std Types & Containers
        "std", "string", "wstring", "vector", "map", "unordered_map", "set", "unordered_set",
        "multimap", "multiset", "list", "deque", "queue", "priority_queue", "stack", "array",
        "pair", "tuple", "optional", "variant", "any", "string_view", "span", "bitset",
        "shared_ptr", "unique_ptr", "weak_ptr", "make_shared", "make_unique",
        "size_t", "ptrdiff_t", "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t",
        "uint16_t", "uint32_t", "uint64_t", "intptr_t", "uintptr_t", "NULL",
        "std::string", "std::vector", "std::map", "std::unordered_map", "std::set",
        "std::unordered_set", "std::list", "std::deque", "std::queue", "std::stack",
        "std::array", "std::pair", "std::tuple", "std::optional", "std::variant", "std::any",
        "std::string_view", "std::span", "std::shared_ptr", "std::unique_ptr",
        "std::weak_ptr", "std::make_shared", "std::make_unique", "std::function",

        // std I/O
        "cout", "cin", "cerr", "clog", "endl", "getline", "ifstream", "ofstream", "fstream",
        "stringstream", "istringstream", "ostringstream", "setw", "setprecision", "fixed",
        "std::cout", "std::cin", "std::cerr", "std::clog", "std::endl", "std::getline",
        "std::ifstream", "std::ofstream", "std::fstream", "std::stringstream",
        "std::istringstream", "std::ostringstream", "std::setw", "std::setprecision",

        // std Algorithms & Utilities
        "std::move", "std::forward", "std::swap", "std::sort", "std::find", "std::find_if",
        "std::for_each", "std::transform", "std::accumulate", "std::min", "std::max",
        "std::reverse", "std::unique", "std::lower_bound", "std::upper_bound",
        "std::binary_search", "std::begin", "std::end", "std::to_string", "std::stoi",
        "std::stol", "std::stof", "std::stod", "std::bind", "std::ref", "std::tie",
        "sort", "find", "find_if", "for_each", "transform", "accumulate", "reverse", "swap",
        "move", "forward", "to_string", "stoi", "stol", "stof", "stod", "min", "max",
        "function", "bind", "tie",

        // Common Container / String Methods
        "push_back", "emplace_back", "pop_back", "push_front", "pop_front", "emplace",
        "insert", "erase", "clear", "size", "empty", "resize", "reserve", "capacity",
        "front", "back", "at", "begin", "end", "rbegin", "rend", "data", "c_str", "substr",
        "append", "length", "count", "first", "second", "get", "reset", "release",

        // Concurrency
        "std::thread", "std::mutex", "std::lock_guard", "std::unique_lock",
        "std::condition_variable", "std::atomic", "std::async", "std::future",
        "std::promise", "std::chrono", "thread", "mutex", "lock_guard", "unique_lock",
        "condition_variable", "atomic", "async", "future", "promise", "chrono",
        "join", "detach", "lock", "unlock",

        // Exceptions
        "std::exception", "std::runtime_error", "std::invalid_argument",
        "std::out_of_range", "std::logic_error", "exception", "runtime_error",
        "invalid_argument", "out_of_range", "logic_error", "what",

        // Random
        "std::random_device", "std::mt19937", "std::uniform_int_distribution",
        "random_device", "mt19937", "uniform_int_distribution",

        // Common C Library (via <cstdio>, <cstdlib>, <cstring>, <cmath>)
        "printf", "fprintf", "sprintf", "snprintf", "scanf", "puts", "fopen", "fclose",
        "fread", "fwrite", "malloc", "calloc", "realloc", "free", "exit", "abort", "atoi",
        "rand", "srand", "strlen", "strcpy", "strcmp", "strncmp", "strcat", "memcpy",
        "memmove", "memset", "memcmp", "sin", "cos", "tan", "sqrt", "pow", "abs", "floor",
        "ceil", "round", "fabs", "log", "exp", "assert", "errno", "time", "clock",

        // Android NDK / JNI
        "JNIEXPORT", "JNICALL", "JNIEnv", "JavaVM", "jclass", "jobject", "jstring",
        "jint", "jlong", "jboolean", "jbyte", "jchar", "jshort", "jfloat", "jdouble",
        "jsize", "jarray", "jbyteArray", "jintArray", "jobjectArray", "jmethodID", "jfieldID",
        "JNI_OnLoad", "JNI_OnUnload", "JNI_VERSION_1_6", "JNI_TRUE", "JNI_FALSE",
        "GetStringUTFChars", "ReleaseStringUTFChars", "NewStringUTF", "FindClass",
        "GetMethodID", "GetStaticMethodID", "CallVoidMethod", "GetArrayLength", "GetEnv",
        "AttachCurrentThread", "DetachCurrentThread",
        "__android_log_print", "ANDROID_LOG_VERBOSE", "ANDROID_LOG_DEBUG", "ANDROID_LOG_INFO",
        "ANDROID_LOG_WARN", "ANDROID_LOG_ERROR", "LOGD", "LOGE", "LOGI", "LOGW"
    )
}