cmake_minimum_required(VERSION 3.22.1)
project(pinyinime)

# 16 KB page alignment for compatibility with 16 KB page size devices (Android 15+)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(jni_pinyinime SHARED
        pinyin_jni.cpp
        share/dictbuilder.cpp
        share/dictlist.cpp
        share/dicttrie.cpp
        share/lpicache.cpp
        share/matrixsearch.cpp
        share/mystdlib.cpp
        share/ngram.cpp
        share/pinyinime.cpp
        share/searchutility.cpp
        share/spellingtable.cpp
        share/spellingtrie.cpp
        share/splparser.cpp
        share/userdict.cpp
        share/utf16char.cpp
        share/utf16reader.cpp
        share/sync.cpp
)

target_include_directories(jni_pinyinime PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

find_library(log-lib log)

target_link_libraries(jni_pinyinime
        ${log-lib}
)
