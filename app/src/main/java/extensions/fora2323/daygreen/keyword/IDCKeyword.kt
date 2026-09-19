package extensions.fora2323.daygreen.keyword

import kotlin.jvm.JvmField

object IDCKeyword {
    @JvmField
    val KEYWORDS = arrayOf(
        // C Keywords (C89 - C23)
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
        "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
        "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
        "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
        "_Alignas", "_Alignof", "_Atomic", "_Bool", "_Complex", "_Generic", "_Imaginary",
        "_Noreturn", "_Static_assert", "_Thread_local", "_BitInt",
        "alignas", "alignof", "bool", "true", "false", "nullptr", "constexpr",
        "static_assert", "thread_local", "typeof", "typeof_unqual",

        // Preprocessor
        "#include", "#define", "#undef", "#if", "#ifdef", "#ifndef", "#elif", "#elifdef",
        "#elifndef", "#else", "#endif", "#error", "#warning", "#pragma", "#line",
        "defined", "once", "__FILE__", "__LINE__", "__DATE__", "__TIME__", "__func__",
        "__STDC__", "__STDC_VERSION__", "__VA_ARGS__", "__attribute__",

        // Standard & Common Headers
        "<stdio.h>", "<stdlib.h>", "<string.h>", "<math.h>", "<stdint.h>", "<stdbool.h>",
        "<stddef.h>", "<limits.h>", "<ctype.h>", "<time.h>", "<errno.h>", "<assert.h>",
        "<signal.h>", "<stdarg.h>", "<float.h>", "<locale.h>", "<setjmp.h>", "<inttypes.h>",
        "<unistd.h>", "<pthread.h>", "<fcntl.h>", "<dirent.h>", "<dlfcn.h>",
        "<sys/types.h>", "<sys/stat.h>", "<sys/socket.h>", "<netinet/in.h>", "<arpa/inet.h>",
        "<jni.h>", "<android/log.h>",

        // Types & Constants
        "size_t", "ssize_t", "ptrdiff_t", "wchar_t", "off_t", "time_t", "FILE", "va_list",
        "jmp_buf", "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t", "uint16_t",
        "uint32_t", "uint64_t", "intptr_t", "uintptr_t", "NULL", "EOF",
        "INT_MAX", "INT_MIN", "UINT_MAX", "LONG_MAX", "LONG_MIN", "ULONG_MAX", "CHAR_BIT",
        "SIZE_MAX", "INT32_MAX", "INT64_MAX", "UINT32_MAX", "UINT64_MAX",
        "EXIT_SUCCESS", "EXIT_FAILURE", "RAND_MAX", "BUFSIZ", "CLOCKS_PER_SEC",

        // stdio.h
        "printf", "fprintf", "sprintf", "snprintf", "vprintf", "vfprintf", "vsnprintf",
        "scanf", "fscanf", "sscanf", "puts", "fputs", "putchar", "getchar", "fgetc", "fputc",
        "fgets", "getc", "putc", "ungetc", "fopen", "fclose", "freopen", "fread", "fwrite",
        "fseek", "ftell", "rewind", "fflush", "feof", "ferror", "clearerr", "remove",
        "rename", "tmpfile", "perror", "setvbuf", "stdin", "stdout", "stderr",

        // stdlib.h
        "malloc", "calloc", "realloc", "free", "aligned_alloc", "exit", "_Exit", "abort",
        "atexit", "atoi", "atol", "atoll", "atof", "strtol", "strtoul", "strtoll", "strtoull",
        "strtod", "strtof", "rand", "srand", "qsort", "bsearch", "abs", "labs", "llabs",
        "div", "ldiv", "getenv", "system",

        // string.h
        "strlen", "strnlen", "strcpy", "strncpy", "strcat", "strncat", "strcmp", "strncmp",
        "strcasecmp", "strncasecmp", "strchr", "strrchr", "strstr", "strtok", "strdup",
        "strerror", "strspn", "strcspn", "strpbrk", "memcpy", "memmove", "memset",
        "memcmp", "memchr",

        // ctype.h
        "isalpha", "isdigit", "isalnum", "isspace", "isupper", "islower", "ispunct",
        "isxdigit", "isprint", "iscntrl", "isgraph", "toupper", "tolower",

        // math.h
        "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "sinh", "cosh", "tanh",
        "exp", "log", "log10", "log2", "pow", "sqrt", "cbrt", "ceil", "floor", "round",
        "trunc", "fabs", "fmod", "hypot", "fmin", "fmax", "sinf", "cosf", "sqrtf", "powf",
        "fabsf", "floorf", "ceilf", "M_PI", "M_E", "INFINITY", "NAN", "HUGE_VAL",

        // time.h
        "time", "clock", "difftime", "mktime", "strftime", "localtime", "gmtime",
        "clock_gettime", "nanosleep", "usleep", "sleep", "CLOCK_MONOTONIC", "CLOCK_REALTIME",

        // assert / errno / setjmp / signal / stdarg
        "assert", "errno", "setjmp", "longjmp", "signal", "raise", "SIGINT", "SIGTERM",
        "SIGSEGV", "va_start", "va_arg", "va_end", "va_copy",

        // POSIX / pthread
        "pthread_t", "pthread_create", "pthread_join", "pthread_detach", "pthread_exit",
        "pthread_self", "pthread_mutex_t", "pthread_mutex_init", "pthread_mutex_lock",
        "pthread_mutex_unlock", "pthread_mutex_destroy", "pthread_cond_t",
        "pthread_cond_wait", "pthread_cond_signal", "pthread_cond_broadcast",
        "open", "close", "read", "write", "lseek", "mmap", "munmap", "fork", "execvp",
        "waitpid", "getpid", "dup2", "pipe", "access", "unlink", "mkdir", "stat",
        "opendir", "readdir", "closedir", "socket", "bind", "listen", "accept", "connect",
        "send", "recv", "dlopen", "dlsym", "dlclose",

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