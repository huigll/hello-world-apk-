cmake_minimum_required(VERSION 3.22.1)
project(pinyinime)

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
